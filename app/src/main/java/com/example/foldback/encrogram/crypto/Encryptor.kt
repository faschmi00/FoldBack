package com.example.foldback.encrogram.crypto

import com.example.foldback.encrogram.format.InnerPayload
import com.example.foldback.encrogram.format.Sender
import com.example.foldback.encrogram.format.TencdecMessage
import java.security.SecureRandom

/** Verschlüsselt Text zu einer [TencdecMessage]. Absender und Sendezeit stecken verschlüsselt mit darin. */
class Encryptor(
    private val random: SecureRandom = SecureRandom(),
    /** Aktuelle Zeit in Unix-Sekunden. */
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    fun encrypt(text: String, sender: Sender, key: SecretKey, salt: ByteArray): TencdecMessage {
        require(fits(text, sender.name)) { "Text ist zu lang für eine Telegram-Nachricht" }
        val payload = InnerPayload(clock(), sender.deviceTag, sender.name, text)
        val nonce = ByteArray(TencdecMessage.NONCE_SIZE).also(random::nextBytes)
        val sealed = AesGcm.seal(key, nonce, TencdecMessage.associatedData(salt), Padding.pad(payload.encode()))
        return TencdecMessage(salt, nonce, sealed)
    }

    companion object {
        /** Größte aufgefüllte Nutzlast, mit der die fertige Nachricht noch in eine Telegram-Nachricht passt. */
        val MAX_PADDED_SIZE: Int = generateSequence(Padding.BLOCK_SIZE) { it + Padding.BLOCK_SIZE }
            .takeWhile { TencdecMessage.textLength(it + TencdecMessage.TAG_SIZE) <= TencdecMessage.MAX_TEXT_LENGTH }
            .last()

        /** Wie viele Byte Text noch hineinpassen; negativ, wenn der Text zu lang ist. */
        fun remainingBytes(text: String, senderName: String): Int =
            MAX_PADDED_SIZE - 1 - InnerPayload.HEADER_SIZE -
                senderName.toByteArray(Charsets.UTF_8).size - text.toByteArray(Charsets.UTF_8).size

        fun fits(text: String, senderName: String): Boolean =
            InnerPayload.nameFits(senderName) && remainingBytes(text, senderName) >= 0
    }
}
