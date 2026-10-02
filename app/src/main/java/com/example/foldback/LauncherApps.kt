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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.foldback.encrogram.transfer.MessengerSender
import com.example.foldback.encrogram.ui.EncrogramActivity
import com.example.foldback.encrogram.ui.EncrogramIcon

/** Ein Eintrag im Launcher: entweder eine App oder eine Unterseite. */
sealed interface MenuEntry {
    val label: String
    val icon: ImageVector
}

/**
 * Eine App im Launcher. [intent] liefert null, wenn sie nicht installiert ist.
 * Ist [action] gesetzt, wird statt einer App diese Aktion ausgeführt.
 */
data class LauncherApp(
    override val label: String,
    override val icon: ImageVector,
    val action: ((Context) -> Unit)? = null,
    val intent: (Context) -> Intent? = { null },
) : MenuEntry

/** Eine Unterseite mit weiteren Einträgen. */
data class MenuFolder(
    override val label: String,
    override val icon: ImageVector,
    val entries: List<MenuEntry>,
) : MenuEntry

private val messagesFolder = MenuFolder(
    "Nachrichten", Icons.Outlined.Forum,
    listOf(
        LauncherApp("SMS", Icons.Outlined.Sms) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
        },
        LauncherApp("E-Mail", Icons.Outlined.Email) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
        },
        LauncherApp("Snapchat", GhostIcon) {
            it.launchIntentFor("com.snapchat.android")
        },
        LauncherApp("Telegram", Icons.AutoMirrored.Outlined.Send) {
            it.launchIntentFor(*MessengerSender.TELEGRAM_PACKAGES.toTypedArray())
        },
    ),
)

private val planFolder = MenuFolder(
    "Planen", Icons.Outlined.Event,
    listOf(
        LauncherApp("Uhr / Wecker", Icons.Outlined.Alarm) {
            Intent(AlarmClock.ACTION_SHOW_ALARMS)
        },
        LauncherApp("Kalender", Icons.Outlined.CalendarMonth) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
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
    ),
)

private val mediaFolder = MenuFolder(
    "Medien", Icons.Outlined.LibraryMusic,
    listOf(
        LauncherApp("Spotify", Icons.Outlined.MusicNote) {
            it.launchIntentFor("com.spotify.music")
        },
        LauncherApp("Galerie", Icons.Outlined.PhotoLibrary) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
        },
    ),
)

private val CALCULATOR_PACKAGES = arrayOf(
    "com.google.android.calculator",
    "com.sec.android.app.popupcalculator",
    "com.miui.calculator",
    "com.oneplus.calculator",
    "com.coloros.calculator",
    "com.huawei.calculator",
    "com.android.calculator2",
)

private val toolsFolder = MenuFolder(
    "Werkzeuge", Icons.Outlined.Handyman,
    listOf(
        LauncherApp("Karten", Icons.Outlined.Map) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)
        },
        // Viele Hersteller-Rechner melden die Standard-Kategorie nicht, daher zuerst bekannte Apps.
        LauncherApp("Rechner", Icons.Outlined.Calculate) {
            it.launchIntentFor(*CALCULATOR_PACKAGES)
                ?: Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
        },
        LauncherApp("Taschenlampe", Icons.Outlined.FlashlightOn, action = { Torch.toggle(it) }),
        LauncherApp("Encrogram", EncrogramIcon) {
            Intent(it, EncrogramActivity::class.java)
        },
    ),
)

/** Seltener genutzte Apps, nach Themen gruppiert. */
private val moreFolder = MenuFolder(
    "Menü", Icons.Outlined.MoreHoriz,
    listOf(
        messagesFolder,
        planFolder,
        mediaFolder,
        toolsFolder,
        LauncherApp("Einstellungen", Icons.Outlined.Settings) {
            Intent(Settings.ACTION_SETTINGS)
        },
    ),
)

/** Die Startseite des Launchers. */
val homeFolder = MenuFolder(
    "Start", Icons.Outlined.MoreHoriz,
    listOf(
        LauncherApp("Telefon", Icons.Outlined.Phone) {
            Intent(Intent.ACTION_DIAL)
        },
        LauncherApp("WhatsApp", Icons.AutoMirrored.Outlined.Chat) {
            it.launchIntentFor("com.whatsapp", "com.whatsapp.w4b")
        },
        LauncherApp("Kamera", Icons.Outlined.PhotoCamera) {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        },
        moreFolder,
    ),
)

private fun Context.launchIntentFor(vararg packages: String): Intent? =
    packages.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }

fun Context.launch(app: LauncherApp) {
    app.action?.let { return it(this) }
    val intent = app.intent(this)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        if (intent == null) throw ActivityNotFoundException()
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "${app.label} ist nicht installiert", Toast.LENGTH_SHORT).show()
    }
}
