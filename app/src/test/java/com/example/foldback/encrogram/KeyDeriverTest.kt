package com.example.foldback.encrogram

import com.example.foldback.encrogram.crypto.KeyDeriver
import com.example.foldback.encrogram.passphrase.Passphrase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Prüft die echten Produktionsparameter (64 MiB, 3 Durchläufe) – die übrigen Tests nutzen billige. */
class KeyDeriverTest {

    private val deriver = KeyDeriver()

    private fun derive(phrase: String, salt: ByteArray) =
        deriver.derive(Passphrase.fromInput(phrase), salt).withBytes { it.copyOf() }

    @Test
    fun productionParametersAreDeterministicAndSaltDependent() {
        val first = derive(TestFixtures.PHRASE, TestFixtures.salt)
        assertArrayEquals(first, derive(TestFixtures.PHRASE, TestFixtures.salt))
        assertFalse(first.contentEquals(derive(TestFixtures.PHRASE, ByteArray(16))))
    }
}
