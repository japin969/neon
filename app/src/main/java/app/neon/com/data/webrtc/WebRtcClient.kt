package app.neon.com.data.webrtc

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class CallConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

/**
 * WebRtcClient - Client WebRTC Audio & Video Call
 * Menyediakan kontrol:
 * - toggleAudio() (Mute / Unmute mic)
 * - toggleVideo() (Aktif / Nonaktifkan kamera)
 * - toggleSpeaker() (Loudspeaker / Earpiece)
 * - switchCamera() (Kamera Depan / Belakang)
 * - close() (Tutup koneksi PeerConnection & rilis resource)
 */
class WebRtcClient(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var durationTimerJob: Job? = null
    private var elapsedSeconds = 0

    private val audioManager = runCatching {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }.getOrNull()

    private val _connectionState = MutableStateFlow(CallConnectionState.IDLE)
    val connectionState: StateFlow<CallConnectionState> = _connectionState.asStateFlow()

    private val _isAudioMuted = MutableStateFlow(false)
    val isAudioMuted: StateFlow<Boolean> = _isAudioMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _durationFormatted = MutableStateFlow("00:00")
    val durationFormatted: StateFlow<String> = _durationFormatted.asStateFlow()

    /**
     * Memulai panggilan WebRTC
     */
    fun startCall(chatId: String, isVideo: Boolean) {
        _connectionState.value = CallConnectionState.CONNECTING
        _isVideoEnabled.value = isVideo
        _isAudioMuted.value = false
        _isSpeakerOn.value = true
        elapsedSeconds = 0
        _durationFormatted.value = "00:00"

        // Konfigurasi audio routing ke speaker saat video call
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = isVideo
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        scope.launch {
            // Simulasi handshake WebRTC ICE Candidate & SDP exchange (~1 detik)
            delay(1000)
            _connectionState.value = CallConnectionState.CONNECTED
            startDurationTimer()
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (isActive && _connectionState.value == CallConnectionState.CONNECTED) {
                delay(1000)
                elapsedSeconds++
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                _durationFormatted.value = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }

    /**
     * Tombol Mute: toggle audio mic
     */
    fun toggleAudio(): Boolean {
        val newState = !_isAudioMuted.value
        _isAudioMuted.value = newState
        audioManager?.isMicrophoneMute = newState
        return newState
    }

    /**
     * Tombol Video: toggle video track
     */
    fun toggleVideo(): Boolean {
        val newState = !_isVideoEnabled.value
        _isVideoEnabled.value = newState
        return newState
    }

    /**
     * Tombol Speaker: toggle handsfree / earpiece
     */
    fun toggleSpeaker(): Boolean {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        audioManager?.isSpeakerphoneOn = newState
        return newState
    }

    /**
     * Switch antara kamera depan dan belakang
     */
    fun switchCamera(): Boolean {
        val newState = !_isFrontCamera.value
        _isFrontCamera.value = newState
        return newState
    }

    /**
     * Tombol End Call: menutup koneksi WebRTC & melepaskan hardware
     */
    fun close() {
        durationTimerJob?.cancel()
        _connectionState.value = CallConnectionState.DISCONNECTED
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
                am.isMicrophoneMute = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _durationFormatted.value = "00:00"
    }
}
