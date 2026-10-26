package app.neon.com.ui.call

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.webrtc.SignalingEvent
import app.neon.com.data.webrtc.SignalingRepository
import app.neon.com.data.webrtc.WebRTCClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.Locale
import javax.inject.Inject

enum class CallState {
    CONNECTING,
    RINGING,
    CONNECTED,
    ENDED
}

data class CallUiState(
    val targetUserId: String = "",
    val targetUserName: String = "",
    val isVideo: Boolean = false,
    val callState: CallState = CallState.CONNECTING,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOff: Boolean = false,
    val durationSeconds: Int = 0,
    val formattedDuration: String = "00:00",
    val isRemoteConnected: Boolean = false
)

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val webRTCClient: WebRTCClient,
    private val signalingRepository: SignalingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var signalingJob: Job? = null
    private var activeRoomId: String = ""
    private var isCallInitialized: Boolean = false

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun initCall(
        targetUserId: String,
        targetUserName: String,
        isVideo: Boolean,
        localRenderer: SurfaceViewRenderer?,
        remoteRenderer: SurfaceViewRenderer?
    ) {
        if (isCallInitialized) return
        isCallInitialized = true

        val currentUserId = authRepository.getCachedUser()?.uid
            ?: authRepository.getCurrentFirebaseUser()?.uid
            ?: ""
        // Buat room session deterministik dari kedua UID pengguna
        activeRoomId = if (currentUserId.isNotBlank() && targetUserId.isNotBlank()) {
            if (currentUserId < targetUserId) "${currentUserId}_${targetUserId}" else "${targetUserId}_${currentUserId}"
        } else {
            targetUserId.ifBlank { "default_room" }
        }

        _uiState.update {
            it.copy(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                isVideo = isVideo,
                callState = CallState.CONNECTING,
                durationSeconds = 0,
                formattedDuration = "00:00",
                isSpeakerOn = isVideo // Jika video call, default speaker aktif
            )
        }

        // Konfigurasi audio speaker
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = isVideo

        // Inisialisasi SurfaceView jika video call
        if (isVideo) {
            localRenderer?.let { webRTCClient.initSurfaceView(it, isMirror = true) }
            remoteRenderer?.let { webRTCClient.initSurfaceView(it, isMirror = false) }
        }

        // Mulai media lokal (Audio & Video)
        webRTCClient.startLocalMedia(localRenderer, remoteRenderer, isVideo)

        // Callback ketika ICE Candidate lokal terbentuk
        webRTCClient.onIceCandidateCreated = { iceCandidate ->
            signalingRepository.sendIceCandidate(activeRoomId, iceCandidate, currentUserId)
        }

        // Callback perubahan status koneksi WebRTC Peer
        webRTCClient.onConnectionStateChanged = { state ->
            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    _uiState.update {
                        it.copy(
                            callState = CallState.CONNECTED,
                            isRemoteConnected = true
                        )
                    }
                    startTimer()
                }
                PeerConnection.PeerConnectionState.DISCONNECTED,
                PeerConnection.PeerConnectionState.FAILED,
                PeerConnection.PeerConnectionState.CLOSED -> {
                    _uiState.update {
                        it.copy(
                            callState = CallState.ENDED,
                            isRemoteConnected = false
                        )
                    }
                    stopTimer()
                }
                PeerConnection.PeerConnectionState.CONNECTING -> {
                    _uiState.update { it.copy(callState = CallState.CONNECTING) }
                }
                else -> Unit
            }
        }

        // Dengarkan event signaling dari Firebase Realtime Database
        signalingJob = viewModelScope.launch {
            signalingRepository.listenSignaling(activeRoomId, currentUserId).collect { event ->
                when (event) {
                    is SignalingEvent.OfferReceived -> {
                        _uiState.update { it.copy(callState = CallState.RINGING) }
                        webRTCClient.setRemoteDescription(event.sdp) {
                            webRTCClient.createAnswer { answerSdp ->
                                signalingRepository.sendAnswer(activeRoomId, answerSdp, currentUserId)
                            }
                        }
                    }
                    is SignalingEvent.AnswerReceived -> {
                        webRTCClient.setRemoteDescription(event.sdp) {
                            _uiState.update {
                                it.copy(
                                    callState = CallState.CONNECTED,
                                    isRemoteConnected = true
                                )
                            }
                            startTimer()
                        }
                    }
                    is SignalingEvent.IceCandidateReceived -> {
                        webRTCClient.addIceCandidate(event.candidate)
                    }
                    is SignalingEvent.CallEnded -> {
                        endCallLocally()
                    }
                }
            }
        }

        // Kirim initial WebRTC Offer sebagai caller
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(callState = CallState.RINGING) }
            webRTCClient.createOffer { offerSdp ->
                signalingRepository.sendOffer(activeRoomId, offerSdp, currentUserId)
            }
        }
    }

    private fun startTimer() {
        if (timerJob != null && timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { current ->
                    val newSecs = current.durationSeconds + 1
                    val minutes = newSecs / 60
                    val seconds = newSecs % 60
                    val formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    current.copy(
                        durationSeconds = newSecs,
                        formattedDuration = formatted
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleMute() {
        val nextMute = !_uiState.value.isMuted
        webRTCClient.toggleAudio(!nextMute)
        _uiState.update { it.copy(isMuted = nextMute) }
    }

    fun toggleSpeaker() {
        val nextSpeaker = !_uiState.value.isSpeakerOn
        audioManager?.isSpeakerphoneOn = nextSpeaker
        _uiState.update { it.copy(isSpeakerOn = nextSpeaker) }
    }

    fun toggleCamera() {
        val nextCameraOff = !_uiState.value.isCameraOff
        webRTCClient.toggleVideo(!nextCameraOff)
        _uiState.update { it.copy(isCameraOff = nextCameraOff) }
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    fun endCall(onEnded: () -> Unit) {
        stopTimer()
        signalingJob?.cancel()
        signalingJob = null

        if (activeRoomId.isNotBlank()) {
            signalingRepository.endCall(activeRoomId)
        }

        webRTCClient.close()
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false

        _uiState.update { it.copy(callState = CallState.ENDED) }
        viewModelScope.launch {
            delay(300)
            onEnded()
        }
    }

    private fun endCallLocally() {
        stopTimer()
        webRTCClient.close()
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false
        _uiState.update { it.copy(callState = CallState.ENDED) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        signalingJob?.cancel()
        webRTCClient.close()
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false
    }
}
