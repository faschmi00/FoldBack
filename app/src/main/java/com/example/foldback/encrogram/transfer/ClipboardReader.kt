package com.example.foldback.encrogram.transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * Ausweichweg, falls Telegram „Entschlüsseln“ nicht im Textmenü anzeigt:
 * Geheimtext in Telegram kopieren, hier lesen. Die Zwischenablage wird sofort geleert.
 */
class ClipboardReader(private val context: Context) {

    private val clipboard = context.getSystemService(ClipboardManager::class.java)

    fun takeText(): String? {
        val clip = clipboard.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        clear()
        return text
    }

    private fun clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
