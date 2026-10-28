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

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val url = intent.getStringExtra("url")
        if (url.isNullOrEmpty()) { finish(); return }
        val v = PlayerView(this)
        v.useController = true
        v.controllerShowTimeoutMs = 2500
        setContentView(v)
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        player = ExoPlayer.Builder(this).build().also { p ->
            v.player = p
            p.setMediaItem(MediaItem.fromUri(url))
            p.playWhenReady = true
            p.prepare()
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) { finish(); return true }
        if ((e.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || e.keyCode == KeyEvent.KEYCODE_SPACE) && e.action == KeyEvent.ACTION_UP) {
            player?.let { it.playWhenReady = !it.playWhenReady }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    // A TV player never plays on in the background: leaving the screen ends the stream.
    override fun onStop() { super.onStop(); player?.release(); player = null; finish() }
}
