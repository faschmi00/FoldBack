package com.example.foldback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Eine App auf dem Homescreen. [intent] liefert null, wenn sie nicht installiert ist. */
data class LauncherApp(
    val label: String,
    val icon: ImageVector,
    val intent: (Context) -> Intent?,
)

/** Direkt auf dem Homescreen. */
val homeApps = listOf(
    LauncherApp("Telefon", Icons.Outlined.Phone) {
        Intent(Intent.ACTION_DIAL)
    },
    LauncherApp("WhatsApp", Icons.AutoMirrored.Outlined.Chat) {
        it.launchIntentFor("com.whatsapp", "com.whatsapp.w4b")
    },
    LauncherApp("Kamera", Icons.Outlined.PhotoCamera) {
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    },
)

/** Seltener genutzte Apps auf der Seite "Menü". */
val moreApps = listOf(
    LauncherApp("Uhr", Icons.Outlined.Alarm) {
        Intent(AlarmClock.ACTION_SHOW_ALARMS)
    },
    LauncherApp("E-Mail", Icons.Outlined.Email) {
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
    },
    // Für Notizen gibt es keine Standard-Kategorie, daher bekannte Apps der Reihe nach.
    LauncherApp("Notizen", Icons.Outlined.EditNote) {
        it.launchIntentFor(
            "com.google.android.keep",
            "com.samsung.android.app.notes",
            "com.miui.notes",
            "com.oneplus.note",
            "com.coloros.note",
            "com.huawei.notepad",
        )
    },
    LauncherApp("Kalender", Icons.Outlined.CalendarMonth) {
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
    },
    LauncherApp("Spotify", Icons.Outlined.MusicNote) {
        it.launchIntentFor("com.spotify.music")
    },
    LauncherApp("Snapchat", GhostIcon) {
        it.launchIntentFor("com.snapchat.android")
    },
    LauncherApp("Einstellungen", Icons.Outlined.Settings) {
        Intent(Settings.ACTION_SETTINGS)
    },
)

private fun Context.launchIntentFor(vararg packages: String): Intent? =
    packages.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }

fun Context.launch(app: LauncherApp) {
    val intent = app.intent(this)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        if (intent == null) throw ActivityNotFoundException()
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "${app.label} ist nicht installiert", Toast.LENGTH_SHORT).show()
    }
}
