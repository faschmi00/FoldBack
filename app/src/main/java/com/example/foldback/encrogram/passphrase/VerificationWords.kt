package com.example.foldback.encrogram.passphrase

import com.example.foldback.encrogram.crypto.KeyDeriver
import java.util.Locale

/**
 * Zwei Prüfwörter, die nur vom Satz abhängen, z. B. "TIGER · LAMPE".
 * Beide Seiten vergleichen sie mündlich und erkennen so Tippfehler, ohne den Satz zu verraten.
 *
 * Sie werden nur angezeigt, nie gespeichert oder gesendet: Gespeichert wären sie ein
 * Prüfwert, mit dem ein Dieb Sätze offline durchprobieren könnte.
 */
class VerificationWords(private val wordlist: Wordlist, private val keyDeriver: KeyDeriver) {

    fun of(passphrase: Passphrase): String =
        keyDeriver.derive(passphrase, SALT).use { key ->
            key.withBytes { "${word(it, 0)} · ${word(it, 2)}" }
        }

    private fun word(bytes: ByteArray, offset: Int): String {
        val index = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        return wordlist[index % wordlist.size].uppercase(Locale.ROOT)
    }

    private companion object {
        /** Fester Salt, damit gleiche Sätze auf allen Geräten gleiche Prüfwörter ergeben. Genau 16 Byte. */
        val SALT = "ENCROGRAM-VERIFY".toByteArray(Charsets.US_ASCII)
    }
}
