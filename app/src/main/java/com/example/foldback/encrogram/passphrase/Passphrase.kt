package com.example.foldback.encrogram.passphrase

import java.security.MessageDigest

/**
 * Ein normalisierter Sicherheitssatz, gehalten als Bytes im Arbeitsspeicher.
 * Wird nie gespeichert. [wipe] überschreibt ihn mit Nullen.
 */
class Passphrase private constructor(private val bytes: ByteArray, val wordCount: Int) : AutoCloseable {

    /** Selbst gewählte Sätze unter [RECOMMENDED_WORDS] Wörtern sind erlaubt, aber schwächer. */
    val isWeak: Boolean get() = wordCount < RECOMMENDED_WORDS

    internal fun <T> withBytes(block: (ByteArray) -> T): T = block(bytes)

    /** Vergleich in konstanter Zeit. */
    fun sameAs(other: Passphrase): Boolean = MessageDigest.isEqual(bytes, other.bytes)

    fun wipe() = bytes.fill(0)

    override fun close() = wipe()

    override fun toString() = "Passphrase(***)"

    companion object {
        const val RECOMMENDED_WORDS = 8

        fun fromInput(input: CharSequence): Passphrase {
            val normalized = PassphraseNormalizer.normalize(input)
            return Passphrase(normalized.toByteArray(Charsets.UTF_8), PassphraseNormalizer.wordCount(normalized))
        }
    }
}
