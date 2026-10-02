package com.example.foldback

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Schlichter Geist als Strich-Icon für Snapchat, passend zu den Outlined-Material-Icons. */
val GhostIcon: ImageVector = ImageVector.Builder(
    name = "Ghost",
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
    moveTo(12f, 3f)
    curveTo(9f, 3f, 7f, 5.2f, 7f, 8.2f)
    verticalLineTo(10.5f)
    lineTo(5.2f, 11.4f)
    lineTo(7f, 12.6f)
    curveTo(6.6f, 14.4f, 5.4f, 15.8f, 3.5f, 16.4f)
    curveTo(4.3f, 17.2f, 5.5f, 17.3f, 6.4f, 17.6f)
    curveTo(6.6f, 18.4f, 7f, 19f, 8f, 19f)
    curveTo(9.4f, 19f, 10.4f, 20.5f, 12f, 20.5f)
    curveTo(13.6f, 20.5f, 14.6f, 19f, 16f, 19f)
    curveTo(17f, 19f, 17.4f, 18.4f, 17.6f, 17.6f)
    curveTo(18.5f, 17.3f, 19.7f, 17.2f, 20.5f, 16.4f)
    curveTo(18.6f, 15.8f, 17.4f, 14.4f, 17f, 12.6f)
    lineTo(18.8f, 11.4f)
    lineTo(17f, 10.5f)
    verticalLineTo(8.2f)
    curveTo(17f, 5.2f, 15f, 3f, 12f, 3f)
    close()
}.build()
