package dev.couchtv.ontv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowInsets
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Plays one stream link full screen (the Live tab's channels and films). Back leaves. */
class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private var url: String = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        url = intent.getStringExtra("url").orEmpty()
        if (url.isEmpty()) { finish(); return }
        val v = PlayerView(this)
        v.useController = true
        v.controllerShowTimeoutMs = 2500
        setContentView(v)
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        player = ExoPlayer.Builder(this).build().also { p ->
            v.player = p
            p.setMediaItem(MediaItem.fromUri(url))
            // pick up where this film was stopped (live channels have no saved mark)
            Resume.get(url)?.let { m -> if (m.pos in 1 until m.dur) p.seekTo(m.pos) }
            p.playWhenReady = true
            p.prepare()
        }
    }

    /** Remember how far in we are, so Home can resume it later. Called before the player is gone. */
    private fun mark() {
        val p = player ?: return
        val dur = p.duration
        if (dur > 0) Resume.save(url, p.currentPosition, dur)
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) { finish(); return true }
        if ((e.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || e.keyCode == KeyEvent.KEYCODE_SPACE) && e.action == KeyEvent.ACTION_UP) {
            player?.let { it.playWhenReady = !it.playWhenReady }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    // Save the position on pause: the screen being returned to resumes before this one stops, so
    // marking in onStop alone left Home's Continue Watching one step behind.
    override fun onPause() { super.onPause(); mark() }

    // A TV player never plays on in the background: leaving the screen ends the stream.
    override fun onStop() { super.onStop(); mark(); player?.release(); player = null; finish() }
}
