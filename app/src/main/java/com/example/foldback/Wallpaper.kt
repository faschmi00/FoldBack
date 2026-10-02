package com.example.foldback

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.edit

private const val PREFS = "foldback"
private const val KEY_BLACK_WALLPAPER_SET = "black_wallpaper_set"

/**
 * Setzt beim ersten Start ein schwarzes Hintergrundbild für Sperr- und Homebildschirm.
 * Nur einmal, damit ein später selbst gewähltes Bild nicht überschrieben wird.
 */
fun Context.ensureBlackWallpaper() {
    val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_BLACK_WALLPAPER_SET, false)) return

    val app = applicationContext
    Thread {
        val black = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        try {
            WallpaperManager.getInstance(app).setBitmap(
                black, null, true, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM,
            )
            prefs.edit { putBoolean(KEY_BLACK_WALLPAPER_SET, true) }
        } catch (_: Exception) {
            // IOException oder SecurityException, z. B. wenn Hintergrundbilder per Richtlinie gesperrt sind.
            // Beim nächsten Start wird es erneut versucht.
        }
    }.start()
}
