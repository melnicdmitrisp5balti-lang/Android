package com.parentalcontrol.app.streaming

import android.content.Context
import android.util.Log
import com.parentalcontrol.app.cloud.CloudSignalingClient
import com.parentalcontrol.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
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
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Manages the complete WebRTC lifecycle for both the child (caller/sender) and
 * parent (answerer/receiver) sides.
 *
 * Signaling is performed through [CloudSignalingClient] which uses the Firebase
 * Realtime Database REST API — no Firebase Android SDK required.
 *
 * Usage (child side):
 *   manager.initialize()
 *   manager.startAsChild(code)
 *
 * Usage (parent side):
 *   manager.initialize()
 *   manager.startAsParent(code, surfaceViewRenderer)
 */
class WebRtcManager(private val context: Context) {

    companion object {
        private const val TAG = "WebRtcManager"
        private const val VIDEO_TRACK_ID = "video0"
        private const val AUDIO_TRACK_ID = "audio0"
        private const val STREAM_ID = "stream0"
        private const val CAPTURE_WIDTH = 1280
        private const val CAPTURE_HEIGHT = 720
        private const val CAPTURE_FPS = 30
        private const val POLL_INTERVAL_MS = 1_500L
        private const val POLL_MAX_ATTEMPTS = 40
    }

    interface Listener {
        fun onConnectionStateChanged(state: String)
        fun onRemoteVideoTrack(track: VideoTrack)
        fun onError(message: String)
    }

    var listener: Listener? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cloudSignaling = CloudSignalingClient()

    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var capturer: Camera2Capturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var signalingJob: Job? = null

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /** Must be called before [startAsChild] or [startAsParent]. */
    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        eglBase = EglBase.create()
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase!!.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()
        Log.d(TAG, "WebRtcManager initialized")
    }

    /** Returns the [EglBase.Context] needed to initialise a [SurfaceViewRenderer]. */
    fun getEglBaseContext(): EglBase.Context? = eglBase?.eglBaseContext

    // -------------------------------------------------------------------------
    // Child side — caller / sender
    // -------------------------------------------------------------------------

    /**
     * Start WebRTC as the child (media sender).
     * Captures the front (or back) camera and microphone, creates an SDP offer,
     * publishes it to Firebase, then waits for the parent's answer.
     */
    fun startAsChild(code: String) {
        val pc = createPeerConnection(code, "child") ?: return
        addLocalTracks(pc)

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(buildSimpleObserver {
                    scope.launch {
                        if (cloudSignaling.postOffer(code, sdp.description)) {
                            Log.d(TAG, "Offer posted to Firebase for code $code")
                            signalingJob = launch { waitForAnswer(code, pc) }
                        } else {
                            listener?.onError("Не удалось опубликовать предложение в Firebase")
                        }
                    }
                }, sdp)
            }
            override fun onCreateFailure(error: String?) {
                listener?.onError("createOffer failed: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun addLocalTracks(pc: PeerConnection) {
        val fac = factory ?: return
        val egl = eglBase ?: return

        // Video
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
        if (cameraName != null) {
            capturer = Camera2Capturer(context, cameraName, null)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", egl.eglBaseContext)
            videoSource = fac.createVideoSource(false)
            capturer!!.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            capturer!!.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)
            localVideoTrack = fac.createVideoTrack(VIDEO_TRACK_ID, videoSource!!)
            pc.addTrack(localVideoTrack!!, listOf(STREAM_ID))
        }

        // Audio
        audioSource = fac.createAudioSource(MediaConstraints())
        localAudioTrack = fac.createAudioTrack(AUDIO_TRACK_ID, audioSource!!)
        pc.addTrack(localAudioTrack!!, listOf(STREAM_ID))
    }

    private suspend fun waitForAnswer(code: String, pc: PeerConnection) {
        repeat(POLL_MAX_ATTEMPTS) { attempt ->
            val answer = cloudSignaling.getAnswer(code)
            if (answer != null) {
                Log.d(TAG, "Answer received from Firebase (attempt $attempt)")
                pc.setRemoteDescription(buildSimpleObserver {
                    scope.launch { pollAndApplyIceCandidates(code, "parent", pc) }
                }, SessionDescription(SessionDescription.Type.ANSWER, answer))
                return
            }
            delay(POLL_INTERVAL_MS)
        }
        listener?.onError("Тайм-аут ожидания ответа родителя")
    }

    // -------------------------------------------------------------------------
    // Parent side — answerer / receiver
    // -------------------------------------------------------------------------

    /**
     * Start WebRTC as the parent (media receiver).
     * Retrieves the child's SDP offer from Firebase, creates an answer,
     * then renders the incoming video on [remoteRenderer].
     */
    fun startAsParent(code: String, remoteRenderer: SurfaceViewRenderer) {
        // Set the renderer before creating the PeerConnection so the onTrack
        // callback can attach the video track as soon as it arrives.
        remoteRendererRef = remoteRenderer

        val pc = createPeerConnection(code, "parent") ?: return

        scope.launch {
            // Wait until the child posts its offer
            var offer: String? = null
            for (attempt in 0 until POLL_MAX_ATTEMPTS) {
                offer = cloudSignaling.getOffer(code)
                if (offer != null) {
                    Log.d(TAG, "Offer found on Firebase (attempt $attempt)")
                    break
                }
                delay(POLL_INTERVAL_MS)
            }

            if (offer == null) {
                listener?.onError("Устройство ребёнка не найдено или не готово")
                return@launch
            }

            pc.setRemoteDescription(buildSimpleObserver {
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        pc.setLocalDescription(buildSimpleObserver {
                            scope.launch {
                                if (cloudSignaling.postAnswer(code, sdp.description)) {
                                    Log.d(TAG, "Answer posted to Firebase for code $code")
                                    signalingJob = launch {
                                        pollAndApplyIceCandidates(code, "child", pc)
                                    }
                                } else {
                                    listener?.onError("Не удалось опубликовать ответ в Firebase")
                                }
                            }
                        }, sdp)
                    }
                    override fun onCreateFailure(error: String?) {
                        listener?.onError("createAnswer failed: $error")
                    }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                }, MediaConstraints())
            }, SessionDescription(SessionDescription.Type.OFFER, offer!!))
        }
    }

    private var remoteRendererRef: SurfaceViewRenderer? = null

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private fun createPeerConnection(code: String, localSide: String): PeerConnection? {
        val fac = factory ?: run {
            listener?.onError("WebRtcManager not initialized")
            return null
        }
        val iceServers = buildIceServers()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = fac.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "Local ICE candidate ($localSide): ${candidate.sdp}")
                scope.launch {
                    cloudSignaling.postIceCandidate(
                        code, localSide,
                        candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex
                    )
                }
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "Connection state: $newState")
                listener?.onConnectionStateChanged(newState.name)
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    Log.d(TAG, "Remote video track received")
                    remoteRendererRef?.let { track.addSink(it) }
                    listener?.onRemoteVideoTrack(track)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })
        peerConnection = pc
        return pc
    }

    /**
     * Poll Firebase for ICE candidates posted by [side] ("child" or "parent")
     * and add them to [pc] as they appear.
     */
    private suspend fun pollAndApplyIceCandidates(
        code: String,
        side: String,
        pc: PeerConnection
    ) {
        val applied = mutableSetOf<String>()
        repeat(POLL_MAX_ATTEMPTS) {
            val candidates = cloudSignaling.getIceCandidates(code, side)
            for (json in candidates) {
                val sdp = json.optString("candidate")
                if (sdp.isNotBlank() && applied.add(sdp)) {
                    pc.addIceCandidate(
                        IceCandidate(
                            json.optString("sdpMid"),
                            json.optInt("sdpMLineIndex"),
                            sdp
                        )
                    )
                    Log.d(TAG, "Applied ICE candidate from $side")
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun buildIceServers(): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()
        servers.add(
            PeerConnection.IceServer.builder(Constants.STUN_SERVER_URI).createIceServer()
        )
        if (Constants.TURN_SERVER_URI.isNotBlank()) {
            servers.add(
                PeerConnection.IceServer.builder(Constants.TURN_SERVER_URI)
                    .setUsername(Constants.TURN_USERNAME)
                    .setPassword(Constants.TURN_CREDENTIAL)
                    .createIceServer()
            )
        }
        return servers
    }

    /** Helper that creates an [SdpObserver] whose [SdpObserver.onSetSuccess] runs [onSuccess]. */
    private fun buildSimpleObserver(onSuccess: () -> Unit): SdpObserver = object : SdpObserver {
        override fun onSetSuccess() { onSuccess() }
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "SDP createFailure: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.e(TAG, "SDP setFailure: $error")
            listener?.onError("SDP setFailure: $error")
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    /** Release all WebRTC resources. Call from onDestroy / onStop. */
    fun release() {
        signalingJob?.cancel()
        scope.cancel()
        try {
            capturer?.stopCapture()
            capturer?.dispose()
        } catch (_: Exception) {}
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        factory?.dispose()
        eglBase?.release()
        peerConnection = null
        factory = null
        eglBase = null
        Log.d(TAG, "WebRtcManager released")
    }
}
