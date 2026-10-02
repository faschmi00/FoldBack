package com.example.foldback.encrogram.session

import com.example.foldback.encrogram.crypto.KeyDeriver
import com.example.foldback.encrogram.crypto.SecretKey
import com.example.foldback.encrogram.format.TencdecMessage
import com.example.foldback.encrogram.passphrase.Passphrase
import java.security.SecureRandom

/**
 * Ein entsperrter Satz mit seinen abgeleiteten Schlüsseln. Lebt nur im Arbeitsspeicher.
 *
 * Argon2id ist langsam, deshalb wird pro Salt nur einmal abgeleitet: Eigene Nachrichten dieser
 * Sitzung teilen sich [sendSalt], und mehrere Nachrichten aus einer Sitzung des Gegenübers
 * kosten ebenfalls nur eine Ableitung.
 */
class UnlockedPhrase internal constructor(
    private val passphrase: Passphrase,
    /** Prüfwörter; dienen in der Oberfläche auch als Name des Satzes. */
    val verification: String,
    random: SecureRandom,
) {
    val isWeak: Boolean = passphrase.isWeak

    internal val sendSalt: ByteArray = ByteArray(TencdecMessage.SALT_SIZE).also(random::nextBytes)

    private val keys = HashMap<String, SecretKey>()
    private var wiped = false

    internal fun matches(other: Passphrase): Boolean = passphrase.sameAs(other)

    /**
     * Führt [block] mit dem Schlüssel für [salt] aus; null, wenn der Satz inzwischen gesperrt wurde.
     * Synchronisiert, damit eine Sperre nie mitten in einer Verschlüsselung den Schlüssel überschreibt.
     */
    @Synchronized
    internal fun <T> withKey(salt: ByteArray, keyDeriver: KeyDeriver, block: (SecretKey) -> T): T? {
        if (wiped) return null
        val key = keys.getOrPut(salt.toHex()) { keyDeriver.derive(passphrase, salt) }
        return block(key)
    }

    @Synchronized
    internal fun wipe() {
        wiped = true
        passphrase.wipe()
        keys.values.forEach(SecretKey::wipe)
        keys.clear()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
