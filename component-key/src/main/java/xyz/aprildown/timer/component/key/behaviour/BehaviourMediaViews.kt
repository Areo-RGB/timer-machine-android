package xyz.aprildown.timer.component.key.behaviour

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil3.load
import coil3.request.crossfade
import xyz.aprildown.timer.component.key.R
import xyz.aprildown.timer.component.key.VideoPlayerActivity
import xyz.aprildown.timer.component.key.databinding.LayoutBehaviourVideoBinding
import xyz.aprildown.timer.component.key.databinding.LayoutEditableBehaviourImageBinding
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.VideoAction
import xyz.aprildown.timer.domain.entities.toImageAction
import xyz.aprildown.timer.domain.entities.toVideoAction

/**
 * Show image and video previews of [behaviours] in [container].
 * Clicking a video plays it inline; long clicking opens [VideoPlayerActivity].
 * @param autoPlayVideo Start the video inline right away, e.g. when its step is active.
 */
internal fun ViewGroup.bindBehaviourMedia(
    behaviours: Collection<BehaviourEntity>,
    onImageClick: ((ImageAction) -> Unit)?,
    onVideoClick: ((VideoAction) -> Unit)? = null,
    autoPlayVideo: Boolean = false,
) {
    // Keep the existing views so a rebind doesn't restart a playing video.
    val mediaKey = behaviours
        .filter { it.type == BehaviourType.IMAGE || it.type == BehaviourType.VIDEO }
        .joinToString { "${it.type}:${it.str1}" }
    if (tag == mediaKey && childCount > 0) {
        updateVideoAutoPlay(autoPlayVideo)
        return
    }
    tag = mediaKey
    removeAllViews()
    val inflater = LayoutInflater.from(context)
    behaviours.find { it.type == BehaviourType.IMAGE }?.toImageAction()?.let { action ->
        val imageView = LayoutEditableBehaviourImageBinding.inflate(inflater, this, true).root
        imageView.setOnClickListener { onImageClick?.invoke(action) }
        imageView.load(action) {
            crossfade(true)
        }
    }
    behaviours.find { it.type == BehaviourType.VIDEO }?.toVideoAction()?.let { action ->
        val videoBinding = LayoutBehaviourVideoBinding.inflate(inflater, this, true)
        videoBinding.bindInlinePlayback(action)
        videoBinding.root.setOnLongClickListener {
            if (onVideoClick != null) {
                onVideoClick(action)
            } else {
                context.startActivity(VideoPlayerActivity.getIntent(context, action.data))
            }
            true
        }
        videoBinding.imageVideoThumbnail.load(action.data.toUri()) {
            crossfade(true)
            listener(onSuccess = { _, result ->
                videoBinding.applyAspectRatio(result.image.width, result.image.height)
            })
        }
    }
    isVisible = childCount > 0
    updateVideoAutoPlay(autoPlayVideo)
}

private fun ViewGroup.updateVideoAutoPlay(autoPlay: Boolean) {
    val videoRoot = (0 until childCount).map { getChildAt(it) }
        .find { it.id == R.id.layoutBehaviourVideo } ?: return
    val binding = LayoutBehaviourVideoBinding.bind(videoRoot)
    val isPlaying = binding.videoInline.isPlaying
    if (autoPlay != isPlaying) {
        // Shares the play/pause toggle with a user's click.
        videoRoot.performClick()
    }
}

/**
 * Size the video box to the video's own shape, capped at 45% of the screen height.
 */
private fun LayoutBehaviourVideoBinding.applyAspectRatio(videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0) return
    val parent = root.parent as? View ?: return
    parent.doOnLayout {
        val availableWidth = parent.width - parent.paddingLeft - parent.paddingRight
        if (availableWidth <= 0) return@doOnLayout
        val maxHeight = (root.resources.displayMetrics.heightPixels * 0.45f).toInt()
        var height = availableWidth * videoHeight / videoWidth
        var width = availableWidth
        if (height > maxHeight) {
            height = maxHeight
            width = maxHeight * videoWidth / videoHeight
        }
        root.updateLayoutParams<LinearLayout.LayoutParams> {
            this.width = width
            this.height = height
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }
}

private fun LayoutBehaviourVideoBinding.bindInlinePlayback(action: VideoAction) {
    var prepared = false
    videoInline.setOnPreparedListener { player ->
        player.isLooping = true
        prepared = true
        applyAspectRatio(player.videoWidth, player.videoHeight)
    }
    videoInline.setOnErrorListener { _, _, _ ->
        videoInline.isVisible = false
        imageVideoThumbnail.isVisible = true
        imageVideoPlay.isVisible = true
        prepared = false
        true
    }
    root.setOnClickListener {
        if (videoInline.isPlaying) {
            videoInline.pause()
            imageVideoPlay.isVisible = true
        } else {
            if (!videoInline.isVisible) {
                videoInline.isVisible = true
                imageVideoThumbnail.isVisible = false
            }
            if (!prepared) videoInline.setVideoURI(action.data.toUri())
            videoInline.start()
            imageVideoPlay.isVisible = false
        }
    }
}
