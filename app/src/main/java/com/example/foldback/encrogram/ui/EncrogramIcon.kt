package com.example.foldback.encrogram.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Encrogram als Strich-Icon: das kantige, nach rechts offene „E“ (angelehnt an EncroChat),
 * dessen Mittelbalken ein Papierflieger (angelehnt an Telegram) ist.
 */
val EncrogramIcon: ImageVector = ImageVector.Builder(
    name = "Encrogram",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 1.7f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
) {
    // Abgeschrägte Klammer des „E“
    moveTo(15f, 4f)
    horizontalLineTo(7.5f)
    lineTo(4f, 7.5f)
    verticalLineTo(16.5f)
    lineTo(7.5f, 20f)
    horizontalLineTo(15f)

    // Papierflieger als Mittelbalken
    moveTo(8.5f, 12.2f)
    lineTo(20.5f, 7f)
    lineTo(17.6f, 18f)
    lineTo(13.4f, 14.6f)
    close()
    moveTo(13.4f, 14.6f)
    lineTo(20.5f, 7f)
    moveTo(13.4f, 14.6f)
    lineTo(12.6f, 17.6f)
    lineTo(14.9f, 16.1f)
}.build()
