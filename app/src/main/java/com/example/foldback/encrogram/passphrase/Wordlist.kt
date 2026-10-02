package com.example.foldback.encrogram.passphrase

import android.content.Context
import com.example.foldback.R
import kotlin.math.log2

/**
 * Wörter für den [PassphraseGenerator]. Wird nur zum Erzeugen eines Satzes gebraucht,
 * nicht zum Ver- oder Entschlüsseln – das Gegenüber braucht die Liste also nicht.
 */
class Wordlist(words: Collection<String>) {

    private val words: List<String> = words.toList()

    init {
        require(this.words.size >= MIN_SIZE) { "Wortliste zu klein: ${this.words.size} statt mindestens $MIN_SIZE" }
        require(this.words.toSet().size == this.words.size) { "Wortliste enthält doppelte Wörter" }
        require(this.words.all(VALID_WORD::matches)) { "Wortliste enthält ungültige Wörter" }
    }

    val size: Int get() = words.size

    val bitsPerWord: Double get() = log2(size.toDouble())

    operator fun get(index: Int): String = words[index]

    companion object {
        /** 6⁵ – so viele Wörter hat eine klassische Diceware-Liste (12,9 Bit pro Wort). */
        const val MIN_SIZE = 7776

        /** Nur a–z (keine Umlaute, kein ß) und 4–9 Buchstaben: gut auszusprechen und zu tippen. */
        private val VALID_WORD = Regex("[a-z]{4,9}")

        /** Diceware-Format "11111 wort". */
        private val DICEWARE_LINE = Regex("""\d+\s+(\S+)""")

        /**
         * Liest eine Wortliste und behält nur geeignete Wörter.
         * Kommentarzeilen (#) und leere Zeilen werden übersprungen.
         */
        fun parse(lines: Sequence<String>): Wordlist {
            val candidates = lines
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { DICEWARE_LINE.matchEntire(it)?.groupValues?.get(1) ?: it }
                .filter(VALID_WORD::matches)
                .toSortedSet()
            // Fugen-s-Fragmente aus Zusammensetzungen ("abfahrts" neben "abfahrt") sind keine eigenen Wörter.
            return Wordlist(candidates.filterNot { it.endsWith('s') && it.dropLast(1) in candidates })
        }

        fun load(context: Context): Wordlist =
            context.resources.openRawResource(R.raw.german_words).bufferedReader().useLines(::parse)
    }
}
