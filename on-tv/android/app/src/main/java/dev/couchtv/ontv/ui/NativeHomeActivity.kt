package dev.couchtv.ontv.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.couchtv.ontv.App

/** The native home screen: everything Compose, the bridge service alive for the phone remote. */
class NativeHomeActivity : ComponentActivity() {
    // The TV's own sleep is a couple of idle minutes, which blanks the home (and drops its network).
    // Hold the panel awake while the home is used and let it sleep only after this much real idle.
    private val idleTimeoutMs = 10 * 60_000L
    private val idle = Handler(Looper.getMainLooper())
    private val letSleep = Runnable { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    private fun stayAwake() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        idle.removeCallbacks(letSleep)
        idle.postDelayed(letSleep, idleTimeoutMs)   // after 10 idle minutes, allow the TV to sleep
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        App.startService(this)
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        stayAwake()
        setContent { CouchApp() }
    }

    override fun onUserInteraction() { super.onUserInteraction(); stayAwake() }   // any remote press resets the timer
    override fun onResume() { super.onResume(); stayAwake() }
    override fun onDestroy() { super.onDestroy(); idle.removeCallbacks(letSleep) }
}
