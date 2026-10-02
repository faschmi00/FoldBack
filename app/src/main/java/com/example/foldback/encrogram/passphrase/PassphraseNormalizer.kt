package com.example.foldback.encrogram.passphrase

import java.text.Normalizer
import java.util.Locale

/**
 * Vereinheitlicht einen Sicherheitssatz, damit kleine Unterschiede beim Tippen
 * nicht zu einem anderen Schlüssel führen: "Ofen  Wolke " und "ofen wolke" sind derselbe Satz.
 */
object PassphraseNormalizer {

    private val WHITESPACE = Regex("\\s+")

    fun normalize(input: CharSequence): String =
        Normalizer.normalize(input, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .trim()
            .replace(WHITESPACE, " ")

    fun wordCount(input: CharSequence): Int {
        val normalized = normalize(input)
        return if (normalized.isEmpty()) 0 else normalized.count { it == ' ' } + 1
    }
}
