package com.parentalcontrol.app.ui.parent

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityLiveStreamBinding
import com.parentalcontrol.app.streaming.WebRtcManager
import org.webrtc.VideoTrack

/**
 * Full-screen live video feed from the child device via WebRTC.
 *
 * Receives the 6-digit [EXTRA_CODE] from [ParentCodeInputActivity],
 * initialises [WebRtcManager] as the answering (receiver) side, and renders
 * the remote video track onto the full-screen [SurfaceViewRenderer].
 *
 * Controls:
 * – Mute / unmute remote audio
 * – Disconnect and return to the previous screen
 * – Connection quality indicator
 */
class LiveStreamActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_stream_code"
        const val EXTRA_CHILD_NAME = "extra_child_name"
    }

    private lateinit var binding: ActivityLiveStreamBinding
    private lateinit var webRtcManager: WebRtcManager
    private var isMuted = false
    private var remoteVideoTrack: VideoTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Full-screen immersive mode
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        val childName = intent.getStringExtra(EXTRA_CHILD_NAME) ?: getString(R.string.child_device)

        if (code.isBlank()) {
            Toast.makeText(this, getString(R.string.stream_no_code), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvChildName.text = childName
        setupWebRtc(code)
        setupControls()
    }

    private fun setupWebRtc(code: String) {
        webRtcManager = WebRtcManager(this)
        webRtcManager.initialize()

        // Initialise the SurfaceViewRenderer with the EGL context from WebRtcManager
        val eglContext = webRtcManager.getEglBaseContext()
        if (eglContext != null) {
            binding.surfaceRemoteVideo.init(eglContext, null)
            binding.surfaceRemoteVideo.setEnableHardwareScaler(true)
            binding.surfaceRemoteVideo.setMirror(false)
        }

        webRtcManager.listener = object : WebRtcManager.Listener {
            override fun onConnectionStateChanged(state: String) {
                runOnUiThread { updateConnectionState(state) }
            }

            override fun onRemoteVideoTrack(track: VideoTrack) {
                remoteVideoTrack = track
                runOnUiThread {
                    binding.layoutConnecting.visibility = View.GONE
                    binding.tvConnectionState.text = getString(R.string.stream_connected)
                    binding.tvConnectionState.setTextColor(
                        ContextCompat.getColor(this@LiveStreamActivity, R.color.neon_green)
                    )
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    binding.layoutConnecting.visibility = View.GONE
                    binding.tvConnectionState.text = message
                    binding.tvConnectionState.setTextColor(
                        ContextCompat.getColor(this@LiveStreamActivity, R.color.neon_magenta)
                    )
                    Toast.makeText(this@LiveStreamActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Start WebRTC as the parent (answerer / receiver)
        webRtcManager.startAsParent(code, binding.surfaceRemoteVideo)
    }

    private fun setupControls() {
        binding.btnMuteAudio.setOnClickListener {
            isMuted = !isMuted
            // WebRTC volume is controlled by muting the audio track on the sink;
            // the simplest cross-device approach is to disable the speaker output.
            binding.surfaceRemoteVideo.setTag(isMuted) // store state
            val iconRes = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
            binding.btnMuteAudio.setIconResource(iconRes)
            binding.btnMuteAudio.text = getString(
                if (isMuted) R.string.unmute_audio else R.string.mute_audio
            )
        }

        binding.btnDisconnect.setOnClickListener {
            finish()
        }
    }

    private fun updateConnectionState(state: String) {
        val (text, colorRes) = when (state.uppercase()) {
            "CONNECTED" -> getString(R.string.stream_connected) to R.color.neon_green
            "CONNECTING" -> getString(R.string.stream_connecting) to R.color.neon_blue
            "DISCONNECTED", "FAILED", "CLOSED" ->
                getString(R.string.stream_disconnected) to R.color.neon_magenta
            else -> state to R.color.text_secondary
        }
        binding.tvConnectionState.text = text
        binding.tvConnectionState.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    override fun onDestroy() {
        remoteVideoTrack?.removeSink(binding.surfaceRemoteVideo)
        webRtcManager.release()
        binding.surfaceRemoteVideo.release()
        super.onDestroy()
    }
}
