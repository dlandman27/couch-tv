package dev.couchtv.ontv.ui

import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.couchtv.ontv.App

/** The native home screen: everything Compose, the bridge service alive for the phone remote. */
class NativeHomeActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        App.startService(this)
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        setContent { CouchApp() }
    }
}
