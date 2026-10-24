package xyz.aprildown.timer.component.key

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri

class VideoPlayerActivity : ComponentActivity() {

    private lateinit var videoView: VideoView
    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            finish()
            return
        }
        position = savedInstanceState?.getInt(STATE_POSITION) ?: 0

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        videoView = VideoView(this)
        root.addView(
            videoView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        setContentView(root)

        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.setOnPreparedListener { player ->
            player.isLooping = true
            videoView.seekTo(position)
            videoView.start()
        }
        videoView.setOnErrorListener { _, _, _ ->
            finish()
            true
        }
        playPath(path)

        addOnNewIntentListener { newIntent ->
            newIntent.getStringExtra(EXTRA_PATH)?.takeIf { it.isNotBlank() }?.let {
                position = 0
                playPath(it)
            }
        }
    }

    private fun playPath(path: String) {
        videoView.setVideoURI(path.toUri())
    }

    override fun onPause() {
        super.onPause()
        if (::videoView.isInitialized) {
            position = videoView.currentPosition
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::videoView.isInitialized) {
            videoView.seekTo(position)
            videoView.start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_POSITION, position)
    }

    companion object {
        private const val EXTRA_PATH = "path"
        private const val STATE_POSITION = "position"

        fun getIntent(context: Context, path: String): Intent {
            return Intent(context, VideoPlayerActivity::class.java)
                .putExtra(EXTRA_PATH, path)
        }
    }
}
