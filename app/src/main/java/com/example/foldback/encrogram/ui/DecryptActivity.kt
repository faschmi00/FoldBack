package com.example.foldback.encrogram.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foldback.encrogram.DecryptOutcome
import com.example.foldback.encrogram.Encrogram
import kotlinx.coroutines.launch

/**
 * Overlay für „Entschlüsseln“ im Textmenü anderer Apps (ACTION_PROCESS_TEXT).
 * Der markierte Text kommt direkt von Android, ohne Zwischenablage.
 *
 * Gibt bewusst kein Ergebnis per setResult zurück – sonst könnte der Klartext den
 * markierten Text in Telegram ersetzen.
 */
class DecryptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureWindow()
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        val encrogram = Encrogram.get(this)
        setContent { DecryptDialog(text, encrogram, onClose = ::finish) }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        Encrogram.get(this).touch()
    }

    /** Overlay verlassen oder Bildschirm aus: Klartext weg. */
    override fun onStop() {
        super.onStop()
        finish()
    }
}

@Composable
private fun DecryptDialog(text: String, encrogram: Encrogram, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val phrases by encrogram.session.phrases.collectAsState()
    var outcome by remember { mutableStateOf<DecryptOutcome?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Neu entschlüsseln, sobald sich die entsperrten Sätze ändern (Entsperren oder Sperre).
    LaunchedEffect(phrases) {
        busy = true
        outcome = encrogram.decrypt(text)
        busy = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Background)
            .border(1.dp, Muted)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Encrogram", color = Foreground, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))

        val current = outcome
        if (current == null) Hint("Entschlüssele …") else DecryptOutcomeView(current)

        if (current == DecryptOutcome.Locked || current == DecryptOutcome.NoMatchingPhrase) {
            Spacer(Modifier.height(16.dp))
            PassphraseForm(busy = busy) { input ->
                scope.launch {
                    busy = true
                    encrogram.unlock(input)
                    busy = false
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        EncroButton("Schließen", onClick = onClose)
    }
}
