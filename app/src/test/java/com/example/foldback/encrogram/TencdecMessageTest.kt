package com.example.foldback.encrogram

import com.example.foldback.encrogram.TestFixtures.anna
import com.example.foldback.encrogram.TestFixtures.encryptor
import com.example.foldback.encrogram.TestFixtures.key
import com.example.foldback.encrogram.TestFixtures.salt
import com.example.foldback.encrogram.format.ParseResult
import com.example.foldback.encrogram.format.TencdecMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TencdecMessageTest {

    private val text = encryptor().encrypt("Hallo", anna, key(), salt).toText()

    @Test
    fun parsesItsOwnOutput() {
        val parsed = TencdecMessage.parse(text) as ParseResult.Ok
        assertArrayEquals(salt, parsed.message.salt)
        assertEquals(text, parsed.message.toText())
    }

    @Test
    fun toleratesSurroundingText() {
        assertTrue(TencdecMessage.parse("Anna, 14:32\n$text\nGesendet") is ParseResult.Ok)
        assertTrue(TencdecMessage.parse("  $text.") is ParseResult.Ok)
    }

    @Test
    fun recognisesOtherText() {
        assertEquals(ParseResult.NotAMessage, TencdecMessage.parse("Hallo, wie geht's?"))
        assertEquals(ParseResult.NotAMessage, TencdecMessage.parse(""))
    }

    @Test
    fun rejectsBrokenMessages() {
        assertEquals(ParseResult.Malformed, TencdecMessage.parse("TENCDEC:"))
        assertEquals(ParseResult.Malformed, TencdecMessage.parse("TENCDEC:1:abc"))
        assertEquals(ParseResult.Malformed, TencdecMessage.parse("TENCDEC:x:" + text.substringAfterLast(':')))
        assertEquals(ParseResult.Malformed, TencdecMessage.parse(text.dropLast(text.length - 40)))
    }

    @Test
    fun reportsNewerVersions() {
        assertEquals(ParseResult.UnsupportedVersion(2), TencdecMessage.parse(text.replace("TENCDEC:1:", "TENCDEC:2:")))
    }
}
