package com.example.foldback.encrogram.crypto

import javax.crypto.spec.SecretKeySpec

/**
 * Ein 256-Bit-Schlüssel, der nur im Arbeitsspeicher lebt.
 * [wipe] überschreibt ihn mit Nullen; danach ist er unbrauchbar.
 */
class SecretKey(private val bytes: ByteArray) : AutoCloseable {

    init {
        require(bytes.size == SIZE) { "Schlüssel muss $SIZE Byte haben" }
    }

    internal fun <T> withBytes(block: (ByteArray) -> T): T = block(bytes)

    /** Die Spezifikation kopiert die Bytes; die Kopie lebt nur für einen einzelnen Aufruf. */
    internal fun toKeySpec(): SecretKeySpec = SecretKeySpec(bytes, "AES")

    fun wipe() = bytes.fill(0)

    override fun close() = wipe()

    override fun toString() = "SecretKey(***)"

    companion object {
        const val SIZE = 32
    }
}
