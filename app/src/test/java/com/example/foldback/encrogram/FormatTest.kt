package com.example.foldback.encrogram

import com.example.foldback.encrogram.crypto.Padding
import com.example.foldback.encrogram.format.Base64Url
import com.example.foldback.encrogram.format.InnerPayload
import com.example.foldback.encrogram.passphrase.Passphrase
import com.example.foldback.encrogram.passphrase.PassphraseNormalizer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Kleine Bausteine: Base64url, Auffüllen, innere Nutzlast, Normalisierung. */
class FormatTest {

    @Test
    fun base64UrlRoundTripsAllLengths() {
        for (size in 0..70) {
            val data = Random(size).nextBytes(size)
            val encoded = Base64Url.encode(data)
            assertEquals(Base64Url.encodedLength(size), encoded.length)
            assertFalse(encoded.any { it == '+' || it == '/' || it == '=' })
            assertArrayEquals(data, Base64Url.decode(encoded))
        }
    }

    @Test
    fun base64UrlRejectsInvalidInput() {
        assertNull(Base64Url.decode("ab+c"))
        assertNull(Base64Url.decode("abcde"))
    }

    @Test
    fun paddingAlwaysFillsWholeBlocksAndIsReversible() {
        for (size in 0..200) {
            val data = Random(size).nextBytes(size)
            val padded = Padding.pad(data)
            assertEquals(0, padded.size % Padding.BLOCK_SIZE)
            assertTrue(padded.size > data.size)
            assertArrayEquals(data, Padding.unpad(padded))
        }
    }

    @Test
    fun brokenPaddingIsRejected() {
        assertNull(Padding.unpad(ByteArray(64)))
        assertNull(Padding.unpad(ByteArray(10)))
    }

    @Test
    fun innerPayloadRoundTrips() {
        val payload = InnerPayload(42L, ByteArray(8) { 3 }, "Jürgen", "Hallo 👋")
        val decoded = InnerPayload.decode(payload.encode())!!
        assertEquals(42L, decoded.sentAt)
        assertEquals("Jürgen", decoded.senderName)
        assertEquals("Hallo 👋", decoded.text)
    }

    @Test
    fun nameLengthIsLimited() {
        assertTrue(InnerPayload.nameFits("a".repeat(InnerPayload.MAX_NAME_BYTES)))
        assertFalse(InnerPayload.nameFits("a".repeat(InnerPayload.MAX_NAME_BYTES + 1)))
        assertFalse(InnerPayload.nameFits("ü".repeat(17)))
    }

    @Test
    fun normalizerIgnoresCaseSpacesAndUnicodeForm() {
        val composed = "Bär  Öl\tGRÜN "
        val decomposed = "bär öl grün"
        assertEquals("bär öl grün", PassphraseNormalizer.normalize(composed))
        assertEquals("bär öl grün", PassphraseNormalizer.normalize(decomposed))
    }

    @Test
    fun countsWordsAndFlagsWeakPassphrases() {
        assertEquals(0, PassphraseNormalizer.wordCount("   "))
        assertEquals(3, PassphraseNormalizer.wordCount(" a  b c "))
        assertTrue(Passphrase.fromInput("nur drei worte").isWeak)
        assertFalse(Passphrase.fromInput(TestFixtures.PHRASE).isWeak)
    }

    @Test
    fun passphrasesCompareAfterNormalizing() {
        assertTrue(Passphrase.fromInput("Ofen Wolke").sameAs(Passphrase.fromInput("ofen  wolke")))
        assertFalse(Passphrase.fromInput("ofen wolke").sameAs(Passphrase.fromInput("ofen wolken")))
    }
}
