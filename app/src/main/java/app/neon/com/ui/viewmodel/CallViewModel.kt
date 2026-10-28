package app.neon.com.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.neon.com.data.webrtc.CallConnectionState
import app.neon.com.data.webrtc.WebRtcClient
import kotlinx.coroutines.flow.StateFlow

/**
 * CallViewModel - Menghubungkan CallScreen ke WebRtcClient
 * - Tombol End Call: Memanggil webrtcClient.close() dan callback navigasi
 * - Tombol Mute: Memanggil webrtcClient.toggleAudio()
 * - Tombol Video: Memanggil webrtcClient.toggleVideo()
 */
class CallViewModel(application: Application) : AndroidViewModel(application) {

    val webrtcClient = WebRtcClient(application.applicationContext)

    val isAudioMuted: StateFlow<Boolean> = webrtcClient.isAudioMuted
    val isVideoEnabled: StateFlow<Boolean> = webrtcClient.isVideoEnabled
    val isSpeakerOn: StateFlow<Boolean> = webrtcClient.isSpeakerOn
    val isFrontCamera: StateFlow<Boolean> = webrtcClient.isFrontCamera
    val durationFormatted: StateFlow<String> = webrtcClient.durationFormatted
    val connectionState: StateFlow<CallConnectionState> = webrtcClient.connectionState

    fun initCall(chatId: String, isVideo: Boolean) {
        webrtcClient.startCall(chatId, isVideo)
    }

    fun toggleAudio() {
        webrtcClient.toggleAudio()
    }

    fun toggleVideo() {
        webrtcClient.toggleVideo()
    }

    fun toggleSpeaker() {
        webrtcClient.toggleSpeaker()
    }

    fun switchCamera() {
        webrtcClient.switchCamera()
    }

    fun endCall(onEnded: () -> Unit) {
        webrtcClient.close()
        onEnded()
    }

    override fun onCleared() {
        super.onCleared()
        webrtcClient.close()
    }
}
