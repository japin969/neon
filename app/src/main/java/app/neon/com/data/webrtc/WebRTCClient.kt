package app.neon.com.data.webrtc

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * WebRTCClient Native Engine
 * Mengelola PeerConnectionFactory, IceServers (Metered TURN/STUN), Audio/Video Tracks,
 * SurfaceViewRenderer rendering, dan SDP/ICE negotiations.
 */
class WebRTCClient(
    private val context: Context
) {

    val eglBase: EglBase = EglBase.create()

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        initPeerConnectionFactory()
    }

    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var remoteVideoSink: SurfaceViewRenderer? = null
    private var localVideoSink: SurfaceViewRenderer? = null

    // Callbacks
    var onIceCandidateCreated: ((IceCandidate) -> Unit)? = null
    var onConnectionStateChanged: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    private val iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("0a146a9c7f58392cd6533163")
            .setPassword("pYSbt7h/oD6THVzZ")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("0a146a9c7f58392cd6533163")
            .setPassword("pYSbt7h/oD6THVzZ")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("0a146a9c7f58392cd6533163")
            .setPassword("pYSbt7h/oD6THVzZ")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername("0a146a9c7f58392cd6533163")
            .setPassword("pYSbt7h/oD6THVzZ")
            .createIceServer()
    )

    private fun initPeerConnectionFactory(): PeerConnectionFactory {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initSurfaceView(renderer: SurfaceViewRenderer, isMirror: Boolean = false) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(isMirror)
    }

    fun startLocalMedia(
        localRenderer: SurfaceViewRenderer?,
        remoteRenderer: SurfaceViewRenderer?,
        isVideo: Boolean
    ) {
        localVideoSink = localRenderer
        remoteVideoSink = remoteRenderer

        // Setup Audio Track
        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)

        // Setup Video Track if Video Call
        if (isVideo && localRenderer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            val capturer = createCameraCapturer()
            videoCapturer = capturer
            if (capturer != null) {
                videoSource = peerConnectionFactory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                capturer.startCapture(1280, 720, 30)

                localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
                localVideoTrack?.setEnabled(true)
                localVideoTrack?.addSink(localRenderer)
            }
        }

        buildPeerConnection()
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Cari kamera depan (Front Facing)
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }

        // Fallback ke kamera belakang jika depan tidak tersedia
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    private fun buildPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}

                override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    newState?.let { onConnectionStateChanged?.invoke(it) }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { onIceCandidateCreated?.invoke(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {
                    if (stream != null && stream.videoTracks.isNotEmpty()) {
                        val remoteVideoTrack = stream.videoTracks[0]
                        remoteVideoSink?.let { sink ->
                            remoteVideoTrack.addSink(sink)
                        }
                    }
                }

                override fun onRemoveStream(stream: MediaStream?) {}

                override fun onDataChannel(channel: DataChannel?) {}

                override fun onRenegotiationNeeded() {}

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        remoteVideoSink?.let { sink ->
                            track.addSink(sink)
                        }
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        remoteVideoSink?.let { sink ->
                            track.addSink(sink)
                        }
                    }
                }
            }
        )

        // Tambahkan local tracks ke PeerConnection
        localAudioTrack?.let { audio ->
            peerConnection?.addTrack(audio, listOf("ARDAMS"))
        }
        localVideoTrack?.let { video ->
            peerConnection?.addTrack(video, listOf("ARDAMS"))
        }
    }

    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            onSuccess(it)
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {}
                    }, it)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun createAnswer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            onSuccess(it)
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {}
                    }, it)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, onComplete: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                onComplete()
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        val capturer = videoCapturer as? CameraVideoCapturer
        capturer?.switchCamera(null)
    }

    fun close() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {}
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        videoSource?.dispose()
        videoSource = null

        audioSource?.dispose()
        audioSource = null

        try {
            localVideoSink?.release()
        } catch (_: Exception) {}
        localVideoSink = null

        try {
            remoteVideoSink?.release()
        } catch (_: Exception) {}
        remoteVideoSink = null

        peerConnection?.close()
        peerConnection = null
    }
}
