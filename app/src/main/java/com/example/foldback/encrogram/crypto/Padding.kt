package com.example.foldback.encrogram.crypto

/**
 * Auffüllen nach ISO/IEC 7816-4: ein Byte 0x80, danach Nullbytes bis zum nächsten Vielfachen
 * von [BLOCK_SIZE]. Von außen ist so nur die ungefähre Länge einer Nachricht sichtbar.
 */
object Padding {

    const val BLOCK_SIZE = 64

    private const val MARKER = 0x80.toByte()

    /** Größe nach dem Auffüllen. Es kommt immer mindestens ein Byte dazu. */
    fun paddedSize(length: Int): Int = (length / BLOCK_SIZE + 1) * BLOCK_SIZE

    fun pad(data: ByteArray): ByteArray =
        data.copyOf(paddedSize(data.size)).also { it[data.size] = MARKER }

    /** null, wenn [data] nicht korrekt aufgefüllt ist. */
    fun unpad(data: ByteArray): ByteArray? {
        if (data.isEmpty() || data.size % BLOCK_SIZE != 0) return null
        val end = data.indexOfLast { it != 0.toByte() }
        if (end < 0 || data[end] != MARKER) return null
        return data.copyOf(end)
    }
}
