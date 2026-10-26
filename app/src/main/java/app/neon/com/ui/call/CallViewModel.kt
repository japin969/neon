package app.neon.com.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val formattedDuration: String = "00:00"
)

@HiltViewModel
class CallViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initCall(targetUserId: String, targetUserName: String, isVideo: Boolean) {
        _uiState.update {
            it.copy(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                isVideo = isVideo,
                callState = CallState.CONNECTING,
                durationSeconds = 0,
                formattedDuration = "00:00"
            )
        }

        // Simulate connecting handshake then transitioning to Connected state
        viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(callState = CallState.RINGING) }
            delay(1500)
            _uiState.update { it.copy(callState = CallState.CONNECTED) }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
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

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCamera() {
        _uiState.update { it.copy(isCameraOff = !it.isCameraOff) }
    }

    fun endCall(onEnded: () -> Unit) {
        timerJob?.cancel()
        _uiState.update { it.copy(callState = CallState.ENDED) }
        viewModelScope.launch {
            delay(400)
            onEnded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
