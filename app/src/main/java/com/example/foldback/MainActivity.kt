package com.example.foldback

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private var showMore by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBlackWallpaper()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            // Der Launcher ist die unterste Ebene – "Zurück" hat hier nichts zu tun.
            BackHandler {}
            HomeScreen(showMore = showMore, onShowMoreChange = { showMore = it })
        }
    }

    /** Home-Taste, während der Launcher schon läuft: immer zurück zur Hauptseite. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showMore = false
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    /** Eigene Uhr und Akkuanzeige ersetzen die Android-Statusleiste. */
    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}
