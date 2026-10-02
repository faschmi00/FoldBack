package com.example.foldback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color.Black
private val Foreground = Color.White
private val Muted = Color(0xFF8A8A8A)

@Composable
fun HomeScreen(showMore: Boolean, onShowMoreChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    // Auf der Seite "Menü" führt "Zurück" zur Hauptseite.
    BackHandler(enabled = showMore) { onShowMoreChange(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BatteryIndicator()
        }
        Spacer(Modifier.height(24.dp))
        Clock()
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Foreground.copy(alpha = 0.25f))
        Spacer(Modifier.height(12.dp))
        if (showMore) {
            MenuRow("Zurück", Icons.AutoMirrored.Outlined.ArrowBack) { onShowMoreChange(false) }
            moreApps.forEach { app ->
                MenuRow(app.label, app.icon) { context.launch(app) }
            }
        } else {
            homeApps.forEach { app ->
                MenuRow(app.label, app.icon) { context.launch(app) }
            }
            MenuRow("Menü", Icons.Outlined.MoreHoriz) { onShowMoreChange(true) }
        }
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    // Ausgewählt = invertiert. So bleibt alles schwarz-weiß und ist mit der Tastatur gut sichtbar.
    val selected = focused || pressed
    val fg = if (selected) Background else Foreground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Foreground else Background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, color = fg, fontSize = 22.sp)
    }
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    val now = rememberBroadcastState(
        IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        },
    ) { Date() }

    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")

    Text(
        SimpleDateFormat(timePattern, locale).format(now),
        color = Foreground,
        fontSize = 80.sp,
        fontWeight = FontWeight.Light,
    )
    Text(
        SimpleDateFormat(datePattern, locale).format(now),
        color = Muted,
        fontSize = 18.sp,
    )
}

private data class BatteryState(val percent: Int, val charging: Boolean)

@Composable
private fun BatteryIndicator() {
    val battery = rememberBroadcastState(IntentFilter(Intent.ACTION_BATTERY_CHANGED)) { intent ->
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        BatteryState(
            percent = if (level >= 0 && scale > 0) level * 100 / scale else 0,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (battery.charging) {
            Icon(Icons.Outlined.Bolt, contentDescription = "lädt", tint = Foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
        }
        BatteryIcon(battery.percent / 100f)
        Spacer(Modifier.width(8.dp))
        Text("${battery.percent} %", color = Foreground, fontSize = 15.sp)
    }
}

/** Akku-Symbol: Umriss mit Füllstand, ganz in Weiß. */
@Composable
private fun BatteryIcon(fraction: Float) {
    Canvas(Modifier.size(width = 26.dp, height = 13.dp)) {
        val stroke = 1.5.dp.toPx()
        val nub = 2.5.dp.toPx()
        val body = Size(size.width - nub, size.height)
        drawRoundRect(
            color = Foreground,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(body.width - stroke, body.height - stroke),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRect(
            color = Foreground,
            topLeft = Offset(body.width, size.height * 0.3f),
            size = Size(nub, size.height * 0.4f),
        )
        val inset = stroke * 2
        drawRect(
            color = Foreground,
            topLeft = Offset(inset, inset),
            size = Size((body.width - inset * 2) * fraction.coerceIn(0f, 1f), body.height - inset * 2),
        )
    }
}

/**
 * Registriert einen BroadcastReceiver, solange das Composable sichtbar ist,
 * und liefert den jeweils zuletzt gemappten Wert. Sticky-Broadcasts (Akku)
 * liefern sofort einen Startwert.
 */
@Composable
private fun <T> rememberBroadcastState(filter: IntentFilter, map: (Intent?) -> T): T {
    val context = LocalContext.current
    val state = remember { mutableStateOf(map(null)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                state.value = map(intent)
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (sticky != null) state.value = map(sticky)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state.value
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(showMore = false, onShowMoreChange = {})
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun MoreScreenPreview() {
    HomeScreen(showMore = true, onShowMoreChange = {})
}
