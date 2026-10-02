package com.example.foldback.encrogram.passphrase

import java.security.SecureRandom

/** Erzeugt zufällige Sätze aus der [Wordlist]. Echte Würfel sind nicht nötig – SecureRandom ist genauso zufällig. */
class PassphraseGenerator(
    private val wordlist: Wordlist,
    private val random: SecureRandom = SecureRandom(),
) {

    /** `nextInt(bound)` ist gleichverteilt, ohne Modulo-Verzerrung. */
    fun generate(wordCount: Int = Passphrase.RECOMMENDED_WORDS): List<String> =
        List(wordCount) { wordlist[random.nextInt(wordlist.size)] }

    fun entropyBits(wordCount: Int = Passphrase.RECOMMENDED_WORDS): Double = wordCount * wordlist.bitsPerWord
}
