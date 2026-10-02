package com.example.foldback.encrogram.transfer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Übergibt fertigen Geheimtext an Telegram. Dort wählt man den Chat und tippt auf Senden.
 * Klartext kommt nie hierher – so landet er auch nie in einem Telegram-Entwurf.
 */
class MessengerSender(private val context: Context) {

    fun send(ciphertext: String) {
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, ciphertext)
        // startActivity braucht keine <queries>-Einträge; fehlt eine App, kommt einfach die Exception.
        for (pkg in TELEGRAM_PACKAGES) {
            try {
                context.startActivity(Intent(share).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
                // Nächste Telegram-Variante versuchen.
            }
        }
        context.startActivity(Intent.createChooser(share, "Senden mit"))
    }

    companion object {
        /** Offizielles Telegram, die Version von telegram.org und Telegram X. */
        val TELEGRAM_PACKAGES = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
        )
    }
}
