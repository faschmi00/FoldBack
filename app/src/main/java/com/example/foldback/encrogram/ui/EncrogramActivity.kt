package com.example.foldback.encrogram.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.foldback.encrogram.transfer.ClipboardReader
import com.example.foldback.encrogram.transfer.MessengerSender

/**
 * Hauptseite von Encrogram. Eine eigene Activity statt einer Launcher-Unterseite,
 * damit FLAG_SECURE und das Autofill-Verbot nur hier gelten und nicht auf dem Home-Bildschirm.
 */
class EncrogramActivity : ComponentActivity() {

    private val viewModel: EncrogramViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureWindow()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            EncrogramScreen(
                viewModel = viewModel,
                onSend = { MessengerSender(this).send(it) },
                readClipboard = { ClipboardReader(this).takeText() },
            )
        }
    }

    /** Jede Berührung zählt als Aktivität und verschiebt die automatische Sperre. */
    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.touch()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onHidden()
    }
}
