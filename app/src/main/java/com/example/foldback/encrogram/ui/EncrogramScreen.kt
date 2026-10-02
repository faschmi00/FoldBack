package com.example.foldback.encrogram.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foldback.encrogram.format.InnerPayload
import com.example.foldback.encrogram.session.UnlockedPhrase
import kotlin.math.roundToInt

@Composable
fun EncrogramScreen(
    viewModel: EncrogramViewModel,
    onSend: (String) -> Unit,
    readClipboard: () -> String?,
) {
    val phrases by viewModel.phrases.collectAsState()
    val generated = viewModel.generated

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("Encrogram", color = Foreground, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Hint("Sperrt bei Bildschirm aus und nach 2 Minuten ohne Aktivität.")
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Foreground.copy(alpha = 0.25f))
        Spacer(Modifier.height(16.dp))

        if (viewModel.reminderDue) {
            Hint(
                "Seit 30 Tagen kein neuer Satz. Beim nächsten Treffen wechseln?",
                color = Foreground,
                bold = true,
            )
            Spacer(Modifier.height(16.dp))
        }

        when {
            generated != null -> GeneratorContent(viewModel, generated)
            phrases.isEmpty() || viewModel.addingPhrase -> UnlockContent(viewModel, canCancel = phrases.isNotEmpty())
            else -> MainContent(viewModel, phrases, onSend, readClipboard)
        }

        viewModel.error?.let {
            Spacer(Modifier.height(16.dp))
            Hint("⚠ $it", color = Foreground, bold = true)
        }
    }
}

@Composable
private fun UnlockContent(viewModel: EncrogramViewModel, canCancel: Boolean) {
    BackHandler(enabled = canCancel) { viewModel.addingPhrase = false }

    NameSection(viewModel)
    Spacer(Modifier.height(24.dp))
    Hint("Gib den Satz ein, den ihr mündlich vereinbart habt.")
    Spacer(Modifier.height(8.dp))
    key(viewModel.resetCount) {
        PassphraseForm(busy = viewModel.busy, onUnlock = viewModel::unlock)
    }
    Spacer(Modifier.height(12.dp))
    EncroButton("Neuen Satz erzeugen", enabled = !viewModel.busy, onClick = viewModel::generate)
    if (canCancel) {
        Spacer(Modifier.height(12.dp))
        EncroButton("Abbrechen") { viewModel.addingPhrase = false }
    }
}

@Composable
private fun GeneratorContent(viewModel: EncrogramViewModel, words: List<String>) {
    BackHandler(onBack = viewModel::cancelGenerated)

    Text("Neuer Sicherheitssatz", color = Foreground, fontSize = 22.sp)
    Spacer(Modifier.height(12.dp))
    words.forEachIndexed { index, word ->
        Row(Modifier.padding(vertical = 2.dp)) {
            Text("${index + 1}", color = Muted, fontSize = 22.sp, modifier = Modifier.width(36.dp))
            Text(word, color = Foreground, fontSize = 26.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    Hint("≈ ${viewModel.generatedBits.roundToInt()} Bit Zufall.")
    Spacer(Modifier.height(4.dp))
    Hint(
        "Lies den Satz deinem Gegenüber unter vier Augen vor. Nicht aufschreiben, nicht fotografieren, " +
            "nicht über Telegram oder SMS schicken. Vergleicht danach die Prüfwörter.",
    )
    Spacer(Modifier.height(16.dp))
    EncroButton("Diesen Satz verwenden", enabled = !viewModel.busy, onClick = viewModel::useGenerated)
    Spacer(Modifier.height(12.dp))
    EncroButton("Neu würfeln", enabled = !viewModel.busy, onClick = viewModel::generate)
    Spacer(Modifier.height(12.dp))
    EncroButton("Abbrechen", onClick = viewModel::cancelGenerated)
}

@Composable
private fun MainContent(
    viewModel: EncrogramViewModel,
    phrases: List<UnlockedPhrase>,
    onSend: (String) -> Unit,
    readClipboard: () -> String?,
) {
    NameSection(viewModel)
    Spacer(Modifier.height(24.dp))

    Hint("Satz (Prüfwörter)")
    Spacer(Modifier.height(6.dp))
    phrases.forEach { phrase ->
        PhraseRow(phrase, selected = phrase == viewModel.selected) { viewModel.select(phrase) }
    }
    Text(
        "+ Weiteren Satz entsperren",
        color = Muted,
        fontSize = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.addingPhrase = true }
            .padding(vertical = 10.dp),
    )
    Spacer(Modifier.height(16.dp))

    SecureTextField(
        value = viewModel.message,
        onValueChange = { viewModel.message = it },
        hint = "Nachricht",
        kind = SecureFieldKind.MESSAGE,
    )
    Spacer(Modifier.height(6.dp))
    val remaining = viewModel.remainingBytes
    if (remaining >= 0) {
        Hint("noch $remaining Zeichen")
    } else {
        Hint("${-remaining} Zeichen zu lang", color = Foreground, bold = true)
    }
    Spacer(Modifier.height(12.dp))

    val hasName = viewModel.senderName.isNotBlank()
    EncroButton(
        if (viewModel.busy) "Bitte warten …" else "Verschlüsselt senden",
        enabled = !viewModel.busy && hasName && viewModel.message.isNotBlank() && remaining >= 0,
    ) { viewModel.encrypt(onSend) }
    if (!hasName) {
        Spacer(Modifier.height(6.dp))
        Hint("Lege zuerst deinen Namen fest.")
    }

    Spacer(Modifier.height(32.dp))
    EncroButton("Aus Zwischenablage entschlüsseln", enabled = !viewModel.busy) {
        viewModel.decrypt(readClipboard())
    }
    viewModel.result?.let { outcome ->
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Muted)
                .padding(16.dp),
        ) {
            DecryptOutcomeView(outcome)
            Spacer(Modifier.height(12.dp))
            EncroButton("Ausblenden", onClick = viewModel::clearResult)
        }
    }

    Spacer(Modifier.height(32.dp))
    EncroButton("Sperren", onClick = viewModel::lock)
}

@Composable
private fun PhraseRow(phrase: UnlockedPhrase, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) Background else Foreground
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Foreground else Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(phrase.verification, color = fg, fontSize = 20.sp, modifier = Modifier.weight(1f))
        if (phrase.isWeak) Text("schwach", color = if (selected) Background else Muted, fontSize = 15.sp)
    }
}

/** Absendername, der verschlüsselt mit jeder Nachricht mitgeht. */
@Composable
private fun NameSection(viewModel: EncrogramViewModel) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var tooLong by remember { mutableStateOf(false) }

    if (!editing && viewModel.senderName.isNotBlank()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    draft = viewModel.senderName
                    editing = true
                }
                .padding(vertical = 6.dp),
        ) {
            Hint("Dein Name: ")
            Text(viewModel.senderName, color = Foreground, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Hint("ändern")
        }
        return
    }

    Hint("Dein Name. Er geht verschlüsselt mit jeder Nachricht mit, damit dein Gegenüber sieht, von wem sie ist.")
    Spacer(Modifier.height(8.dp))
    SecureTextField(draft, { draft = it; tooLong = false }, hint = "Name", kind = SecureFieldKind.NAME)
    if (tooLong) {
        Spacer(Modifier.height(6.dp))
        Hint("Höchstens ${InnerPayload.MAX_NAME_BYTES} Zeichen.", color = Foreground)
    }
    Spacer(Modifier.height(8.dp))
    EncroButton("Name speichern", enabled = draft.isNotBlank()) {
        if (viewModel.saveName(draft)) editing = false else tooLong = true
    }
}
