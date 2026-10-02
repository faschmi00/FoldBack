package com.example.foldback.encrogram

import com.example.foldback.encrogram.TestFixtures.anna
import com.example.foldback.encrogram.TestFixtures.encryptor
import com.example.foldback.encrogram.TestFixtures.key
import com.example.foldback.encrogram.TestFixtures.salt
import com.example.foldback.encrogram.crypto.DecryptResult
import com.example.foldback.encrogram.crypto.Decryptor
import com.example.foldback.encrogram.format.TencdecMessage
import org.junit.Assert.assertEquals
import org.junit.Test

/** Jede Veränderung unterwegs muss auffallen – auch wenn der richtige Schlüssel schon bereitliegt. */
class TamperingTest {

    private val original = encryptor().encrypt("Treffen um 8", anna, key(), salt)

    private fun ByteArray.flipped(index: Int) = copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }

    private fun assertRejected(message: TencdecMessage) =
        assertEquals(DecryptResult.WrongKey, Decryptor().decrypt(message, key()))

    @Test
    fun changedSaltIsRejected() =
        assertRejected(TencdecMessage(original.salt.flipped(0), original.nonce, original.sealed))

    @Test
    fun changedNonceIsRejected() =
        assertRejected(TencdecMessage(original.salt, original.nonce.flipped(5), original.sealed))

    @Test
    fun changedCiphertextIsRejected() {
        for (index in listOf(0, 20, original.sealed.size - TencdecMessage.TAG_SIZE - 1)) {
            assertRejected(TencdecMessage(original.salt, original.nonce, original.sealed.flipped(index)))
        }
    }

    @Test
    fun changedTagIsRejected() =
        assertRejected(TencdecMessage(original.salt, original.nonce, original.sealed.flipped(original.sealed.size - 1)))

    @Test
    fun truncatedCiphertextIsRejected() =
        assertRejected(TencdecMessage(original.salt, original.nonce, original.sealed.copyOf(original.sealed.size - 64)))
}
