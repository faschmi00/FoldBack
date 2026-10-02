package com.example.foldback.encrogram

import com.example.foldback.encrogram.TestFixtures.fastDeriver
import com.example.foldback.encrogram.TestFixtures.projectWordlist
import com.example.foldback.encrogram.passphrase.Passphrase
import com.example.foldback.encrogram.passphrase.PassphraseGenerator
import com.example.foldback.encrogram.passphrase.VerificationWords
import com.example.foldback.encrogram.passphrase.Wordlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordlistTest {

    private val wordlist = projectWordlist()

    @Test
    fun projectWordlistIsLargeEnough() {
        assertTrue("nur ${wordlist.size} Wörter", wordlist.size >= Wordlist.MIN_SIZE)
    }

    @Test
    fun eightWordsGiveAtLeast103Bits() {
        assertTrue(PassphraseGenerator(wordlist).entropyBits(8) >= 103.0)
    }

    @Test
    fun parserSkipsCommentsUmlautsAndFragments() {
        // Genug gültige Füllwörter "baaaa", "baaab", … damit die Liste groß genug ist.
        val filler = List(Wordlist.MIN_SIZE) { index ->
            "b" + (0 until 4).map { 'a' + (index / pow26(it)) % 26 }.joinToString("")
        }
        val odd = listOf("# Kommentar", "", "abfahrt", "abfahrts", "bär", "auto", "11111 birke", "xy", "AUTO")
        val parsed = Wordlist.parse((odd + filler).asSequence())
        val words = (0 until parsed.size).map { parsed[it] }.toSet()

        assertEquals(Wordlist.MIN_SIZE + 3, parsed.size)
        assertTrue(words.containsAll(listOf("abfahrt", "auto", "birke")))
        assertTrue(words.none { it in listOf("abfahrts", "bär", "xy") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooSmallListIsRejected() {
        Wordlist.parse(sequenceOf("ofen", "wolke"))
    }

    private fun pow26(exponent: Int): Int = (0 until exponent).fold(1) { acc, _ -> acc * 26 }

    @Test
    fun generatorUsesOnlyListWords() {
        val words = PassphraseGenerator(wordlist).generate()
        assertEquals(8, words.size)
        assertTrue(words.all { it.matches(Regex("[a-z]{4,9}")) })
    }

    @Test
    fun verificationWordsDependOnlyOnThePassphrase() {
        val verification = VerificationWords(wordlist, fastDeriver)
        val a = verification.of(Passphrase.fromInput(TestFixtures.PHRASE))
        val b = verification.of(Passphrase.fromInput(TestFixtures.PHRASE.uppercase()))
        val c = verification.of(Passphrase.fromInput(TestFixtures.PHRASE + " extra"))
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertTrue(a.matches(Regex("[A-Z]+ · [A-Z]+")))
    }
}
