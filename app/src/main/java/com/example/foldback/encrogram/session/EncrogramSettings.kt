package com.example.foldback.encrogram.session

import android.content.Context
import com.example.foldback.encrogram.format.Base64Url
import com.example.foldback.encrogram.format.InnerPayload
import java.security.SecureRandom

/**
 * Das Einzige, was Encrogram dauerhaft speichert. Nichts davon verrät etwas über einen Satz.
 * Die Datei ist von Backups ausgeschlossen, damit ein wiederhergestelltes Gerät eine eigene Kennung bekommt.
 */
class EncrogramSettings(context: Context) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Zufällige Kennung dieses Geräts; erkennt eigene Nachrichten, die als fremde zurückkommen. */
    val deviceTag: ByteArray
        @Synchronized get() = prefs.getString(KEY_DEVICE_TAG, null)?.let(Base64Url::decode)
            ?: ByteArray(InnerPayload.DEVICE_TAG_SIZE).also {
                SecureRandom().nextBytes(it)
                prefs.edit().putString(KEY_DEVICE_TAG, Base64Url.encode(it)).apply()
            }

    /** Name, der verschlüsselt mit jeder Nachricht mitgeht. */
    var senderName: String
        get() = prefs.getString(KEY_SENDER_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SENDER_NAME, value).apply()

    /** Wann zuletzt ein Satz im Generator erzeugt und übernommen wurde (Unix-Millisekunden). */
    var lastPhraseCreatedAt: Long?
        get() = prefs.getLong(KEY_LAST_PHRASE, -1).takeIf { it >= 0 }
        set(value) = prefs.edit().putLong(KEY_LAST_PHRASE, value ?: -1).apply()

    companion object {
        /** Ohne Endung; als "encrogram.xml" in den Backup-Regeln ausgeschlossen. */
        const val FILE_NAME = "encrogram"
        private const val KEY_DEVICE_TAG = "device_tag"
        private const val KEY_SENDER_NAME = "sender_name"
        private const val KEY_LAST_PHRASE = "last_phrase_created_at"
    }
}
