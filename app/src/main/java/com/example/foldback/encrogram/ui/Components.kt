package com.example.foldback.encrogram.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.example.foldback.encrogram.DecryptOutcome
import com.example.foldback.encrogram.SenderWarning
import com.example.foldback.encrogram.passphrase.Passphrase
import com.example.foldback.encrogram.passphrase.PassphraseNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val Background = Color.Black
internal val Foreground = Color.White
internal val Muted = Color(0xFF8A8A8A)

/** Keine Screenshots, keine Vorschau in der App-Übersicht, kein Autofill und kein „Passwort speichern?“. */
internal fun Activity.secureWindow() {
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}

/** Schwarz-weiße Schaltfläche im Stil des Launchers: beim Drücken invertiert. */
@Composable
internal fun EncroButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inverted = pressed && enabled
    val color = if (enabled) Foreground else Muted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color)
            .background(if (inverted) Foreground else Background)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(label, color = if (inverted) Background else color, fontSize = 18.sp)
    }
}

@Composable
internal fun Hint(text: String, color: Color = Muted, bold: Boolean = false) {
    Text(
        text,
        color = color,
        fontSize = 15.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    )
}

internal enum class SecureFieldKind(val inputType: Int, val minLines: Int) {
    MESSAGE(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        minLines = 4,
    ),

    /** Sichtbares Passwortfeld: Tastaturen schalten hier Lernen und Vorschläge ab. */
    PASSPHRASE(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        minLines = 2,
    ),
    NAME(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        minLines = 1,
    ),
}

/**
 * Textfeld als klassisches EditText, weil Compose IME_FLAG_NO_PERSONALIZED_LEARNING nicht anbietet:
 * Die Tastatur lernt nichts dazu, schlägt nichts vor, und der Inhalt landet nicht im Instance-State.
 */
@SuppressLint("InlinedApi")
@Composable
internal fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    kind: SecureFieldKind,
    modifier: Modifier = Modifier,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Muted),
        factory = { context ->
            EditText(context).apply {
                inputType = kind.inputType
                // Ältere Tastaturen ignorieren das Flag einfach.
                imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                isSaveEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                }
                background = null
                gravity = Gravity.TOP or Gravity.START
                minLines = kind.minLines
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTextColor(Foreground.toArgb())
                setHintTextColor(Muted.toArgb())
                setHint(hint)
                val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics).toInt()
                setPadding(padding, padding, padding, padding)
                doAfterTextChanged { currentOnValueChange(it?.toString().orEmpty()) }
            }
        },
        update = { if (it.text.toString() != value) it.setText(value) },
    )
}

/** Eingabe des Sicherheitssatzes. Warnt bei weniger als 8 Wörtern, erlaubt sie aber. */
@Composable
internal fun PassphraseForm(busy: Boolean, onUnlock: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val words = PassphraseNormalizer.wordCount(input)

    SecureTextField(input, { input = it }, hint = "Sicherheitssatz", kind = SecureFieldKind.PASSPHRASE)
    if (words in 1 until Passphrase.RECOMMENDED_WORDS) {
        Spacer(Modifier.height(8.dp))
        Hint(
            "⚠ Nur $words ${if (words == 1) "Wort" else "Wörter"}. Empfohlen sind ${Passphrase.RECOMMENDED_WORDS} " +
                "zufällige Wörter – kürzere oder selbst ausgedachte Sätze lassen sich leichter erraten.",
            color = Foreground,
        )
    }
    Spacer(Modifier.height(12.dp))
    EncroButton(if (busy) "Satz wird geprüft …" else "Entsperren", enabled = !busy && words > 0) {
        onUnlock(input)
        input = ""
    }
}

/** Ergebnis einer Entschlüsselung. Der Klartext ist bewusst nicht markierbar, damit er nicht kopiert wird. */
@Composable
internal fun DecryptOutcomeView(outcome: DecryptOutcome) {
    when (outcome) {
        is DecryptOutcome.Success -> Column {
            val payload = outcome.payload
            Hint("Von: ${payload.senderName.ifBlank { "Unbekannt" }} · ${formatSentAt(payload.sentAt)}")
            Hint("Satz: ${outcome.verification}")
            outcome.warning?.let {
                Spacer(Modifier.height(6.dp))
                Hint("⚠ ${it.message}", color = Foreground, bold = true)
            }
            Spacer(Modifier.height(10.dp))
            Text(payload.text, color = Foreground, fontSize = 20.sp)
        }
        DecryptOutcome.NotAMessage -> Hint("Keine Encrogram-Nachricht. Markiere den ganzen Text ab „TENCDEC:“.")
        DecryptOutcome.Malformed -> Hint("Die Nachricht ist beschädigt oder unvollständig.")
        DecryptOutcome.UnsupportedVersion -> Hint("Mit einer neueren Encrogram-Version erstellt.")
        DecryptOutcome.Locked -> Hint("Gib den Sicherheitssatz ein, um die Nachricht zu lesen.")
        DecryptOutcome.NoMatchingPhrase -> Hint("Mit den entsperrten Sätzen nicht lesbar. Weiteren Satz eingeben?")
    }
}

private val SenderWarning.message: String
    get() = when (this) {
        SenderWarning.THIS_DEVICE ->
            "Diese Nachricht wurde auf diesem Gerät verschlüsselt. Sie stammt nicht von deinem Gegenüber."
        SenderWarning.OWN_NAME ->
            "Diese Nachricht trägt deinen Namen. Sie stammt vermutlich nicht von deinem Gegenüber."
    }

private fun formatSentAt(unixSeconds: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY).format(Date(unixSeconds * 1000))
