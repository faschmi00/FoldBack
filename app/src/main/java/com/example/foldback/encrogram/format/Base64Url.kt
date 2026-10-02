package com.example.foldback.encrogram.format

/**
 * Base64url ohne Padding (RFC 4648, Abschnitt 5).
 *
 * Eigene Umsetzung, weil java.util.Base64 erst ab API 26 existiert und android.util.Base64
 * in JVM-Unit-Tests nicht verfügbar ist. Base64url enthält keine Zeichen wie `+` oder `/`,
 * die Messenger als Formatierung oder Link deuten könnten.
 */
internal object Base64Url {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val VALUES = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { value, char -> table[char.code] = value }
    }

    fun isAlphabet(char: Char): Boolean = char.code < 128 && VALUES[char.code] >= 0

    fun encodedLength(byteCount: Int): Int = (byteCount * 4 + 2) / 3

    fun encode(data: ByteArray): String {
        val out = StringBuilder(encodedLength(data.size))
        var buffer = 0
        var bits = 0
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 6) {
                bits -= 6
                out.append(ALPHABET[(buffer shr bits) and 63])
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (6 - bits)) and 63])
        return out.toString()
    }

    /** null bei ungültigen Zeichen oder einer Länge, die kein Base64 sein kann. */
    fun decode(text: CharSequence): ByteArray? {
        if (text.length % 4 == 1) return null
        val out = ByteArray(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var position = 0
        for (char in text) {
            if (!isAlphabet(char)) return null
            buffer = (buffer shl 6) or VALUES[char.code]
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[position++] = (buffer shr bits).toByte()
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return out
    }
}
