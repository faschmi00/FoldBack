package com.example.foldback.encrogram

import com.example.foldback.encrogram.TestFixtures.anna
import com.example.foldback.encrogram.TestFixtures.encryptor
import com.example.foldback.encrogram.TestFixtures.key
import com.example.foldback.encrogram.TestFixtures.salt
import com.example.foldback.encrogram.crypto.DecryptResult
import com.example.foldback.encrogram.crypto.Decryptor
import org.junit.Assert.assertEquals
import org.junit.Test

class WrongPassphraseTest {

    @Test
    fun wrongPassphraseIsANormalResult() {
        val message = encryptor().encrypt("Hallo", anna, key(), salt)
        val result = Decryptor().decrypt(message, key("ofen wolke spitz ruder kamel neun birke see"))
        assertEquals(DecryptResult.WrongKey, result)
    }

    @Test
    fun wipedKeyNoLongerDecrypts() {
        val message = encryptor().encrypt("Hallo", anna, key(), salt)
        val wiped = key().also { it.wipe() }
        assertEquals(DecryptResult.WrongKey, Decryptor().decrypt(message, wiped))
    }
}
