package com.example.foldback.encrogram.format

/**
 * Eine verschlüsselte Nachricht, wie sie über Telegram verschickt wird:
 *
 * `TENCDEC:1:<base64url( salt[16] ‖ nonce[12] ‖ geheimtext ‖ tag[16] )>`
 *
 * [sealed] ist Geheimtext und Tag zusammen, so wie AES-GCM sie liefert.
 */
class TencdecMessage(val salt: ByteArray, val nonce: ByteArray, val sealed: ByteArray) {

    init {
        require(salt.size == SALT_SIZE) { "Salt muss $SALT_SIZE Byte haben" }
        require(nonce.size == NONCE_SIZE) { "Nonce muss $NONCE_SIZE Byte haben" }
        require(sealed.size >= TAG_SIZE) { "Geheimtext ist kürzer als der Tag" }
    }

    fun toText(): String = HEADER + Base64Url.encode(salt + nonce + sealed)

    companion object {
        const val PREFIX = "TENCDEC:"
        const val VERSION = 1
        const val HEADER = "$PREFIX$VERSION:"

        const val SALT_SIZE = 16
        const val NONCE_SIZE = 12
        const val TAG_SIZE = 16

        /** Längste Nachricht, die Telegram in einem Stück verschickt. */
        const val MAX_TEXT_LENGTH = 4096

        private val VERSION_TAG: ByteArray = "$PREFIX$VERSION".toByteArray(Charsets.US_ASCII)

        /**
         * Zusätzliche authentifizierte Daten: Formatversion und Salt. Wer eins davon verändert,
         * lässt die Entschlüsselung scheitern – auch wenn der Schlüssel schon im Speicher liegt.
         */
        fun associatedData(salt: ByteArray): ByteArray = VERSION_TAG + salt

        /** Länge der fertigen Nachricht in Zeichen, wenn Geheimtext und Tag [sealedSize] Byte haben. */
        fun textLength(sealedSize: Int): Int =
            HEADER.length + Base64Url.encodedLength(SALT_SIZE + NONCE_SIZE + sealedSize)

        /**
         * Sucht eine Nachricht in [text]. Davor und danach darf anderer Text stehen,
         * denn beim Markieren in Telegram rutscht leicht etwas mit hinein.
         */
        fun parse(text: CharSequence): ParseResult {
            val start = text.indexOf(PREFIX)
            if (start < 0) return ParseResult.NotAMessage

            val rest = text.substring(start + PREFIX.length)
            val colon = rest.indexOf(':')
            val version = rest.take(colon.coerceAtLeast(0)).toIntOrNull()
                ?: return ParseResult.Malformed
            if (version != VERSION) return ParseResult.UnsupportedVersion(version)

            val body = rest.substring(colon + 1).takeWhile(Base64Url::isAlphabet)
            val bytes = Base64Url.decode(body) ?: return ParseResult.Malformed
            if (bytes.size < SALT_SIZE + NONCE_SIZE + TAG_SIZE) return ParseResult.Malformed

            return ParseResult.Ok(
                TencdecMessage(
                    salt = bytes.copyOfRange(0, SALT_SIZE),
                    nonce = bytes.copyOfRange(SALT_SIZE, SALT_SIZE + NONCE_SIZE),
                    sealed = bytes.copyOfRange(SALT_SIZE + NONCE_SIZE, bytes.size),
                ),
            )
        }
    }
}

sealed interface ParseResult {
    data class Ok(val message: TencdecMessage) : ParseResult
    data object NotAMessage : ParseResult
    data object Malformed : ParseResult
    data class UnsupportedVersion(val version: Int) : ParseResult
}
