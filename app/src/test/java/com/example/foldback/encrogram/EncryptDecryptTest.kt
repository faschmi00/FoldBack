package com.example.foldback.encrogram

import com.example.foldback.encrogram.TestFixtures.anna
import com.example.foldback.encrogram.TestFixtures.encryptor
import com.example.foldback.encrogram.TestFixtures.key
import com.example.foldback.encrogram.TestFixtures.salt
import com.example.foldback.encrogram.crypto.DecryptResult
import com.example.foldback.encrogram.crypto.Decryptor
import com.example.foldback.encrogram.crypto.Encryptor
import com.example.foldback.encrogram.format.ParseResult
import com.example.foldback.encrogram.format.TencdecMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptDecryptTest {

    private fun roundTrip(text: String): DecryptResult.Success {
        val sent = encryptor().encrypt(text, anna, key(), salt).toText()
        val parsed = TencdecMessage.parse(sent) as ParseResult.Ok
        return Decryptor().decrypt(parsed.message, key()) as DecryptResult.Success
    }

    @Test
    fun textSurvivesRoundTrip() {
        for (text in listOf("Hallo", "Grüße aus Köln, Straße 5 👋", "", "Zeile 1\nZeile 2")) {
            assertEquals(text, roundTrip(text).payload.text)
        }
    }

    @Test
    fun senderAndTimeAreCarriedEncrypted() {
        val payload = roundTrip("Hi").payload
        assertEquals("Anna", payload.senderName)
        assertArrayEquals(anna.deviceTag, payload.deviceTag)
        assertEquals(1_760_000_000L, payload.sentAt)
    }

    @Test
    fun outputIsPrefixedAndHidesPlaintext() {
        val sent = encryptor().encrypt("geheimes Treffen", anna, key(), salt).toText()
        assertTrue(sent.startsWith("TENCDEC:1:"))
        assertFalse(sent.contains("geheim"))
        assertFalse(sent.contains("Anna"))
    }

    @Test
    fun sameTextTwiceGivesDifferentCiphertext() {
        val first = encryptor().encrypt("Hallo", anna, key(), salt).toText()
        val second = encryptor().encrypt("Hallo", anna, key(), salt).toText()
        assertNotEquals(first, second)
    }

    @Test
    fun longestAllowedMessageFitsIntoTelegram() {
        val text = "a".repeat(Encryptor.remainingBytes("", anna.name))
        val sent = encryptor().encrypt(text, anna, key(), salt).toText()
        assertTrue(sent.length <= TencdecMessage.MAX_TEXT_LENGTH)
        assertEquals(text, roundTrip(text).payload.text)
        assertFalse(Encryptor.fits(text + "a", anna.name))
    }

    @Test
    fun lengthIsHiddenInBlocksOf64Bytes() {
        val short = encryptor().encrypt("a", anna, key(), salt).toText()
        val longer = encryptor().encrypt("a".repeat(40), anna, key(), salt).toText()
        assertEquals(short.length, longer.length)
    }

    @Test
    fun differentlyTypedPassphrasesGiveSameKey() {
        val a = key("Ofen  Wolke\tSPITZ ").withBytesCopy()
        val b = key("ofen wolke spitz").withBytesCopy()
        assertArrayEquals(a, b)
    }

    private fun com.example.foldback.encrogram.crypto.SecretKey.withBytesCopy(): ByteArray = withBytes { it.copyOf() }
}
