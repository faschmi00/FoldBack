package com.example.foldback.encrogram.format

import java.nio.ByteBuffer

/** Wer eine Nachricht schreibt. Beides steht nur verschlüsselt in der Nachricht. */
class Sender(val name: String, val deviceTag: ByteArray)

/**
 * Was vor der Verschlüsselung in einer Nachricht steckt:
 *
 * `sentAt[8] ‖ deviceTag[8] ‖ nameLength[1] ‖ name ‖ text`
 *
 * Weil das alles verschlüsselt und authentifiziert ist, kann niemand ohne den Satz
 * Absender oder Zeit fälschen. Eine eigene Nachricht, die als fremde zurückkommt, fällt so auf.
 */
class InnerPayload(
    /** Sendezeit in Unix-Sekunden. */
    val sentAt: Long,
    val deviceTag: ByteArray,
    val senderName: String,
    val text: String,
) {
    init {
        require(deviceTag.size == DEVICE_TAG_SIZE) { "Geräte-Kennung muss $DEVICE_TAG_SIZE Byte haben" }
    }

    fun encode(): ByteArray {
        val name = senderName.toByteArray(Charsets.UTF_8)
        require(name.size <= MAX_NAME_BYTES) { "Name ist länger als $MAX_NAME_BYTES Byte" }
        val body = text.toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(HEADER_SIZE + name.size + body.size)
            .putLong(sentAt)
            .put(deviceTag)
            .put(name.size.toByte())
            .put(name)
            .put(body)
            .array()
    }

    companion object {
        const val DEVICE_TAG_SIZE = 8
        const val MAX_NAME_BYTES = 32
        const val HEADER_SIZE = Long.SIZE_BYTES + DEVICE_TAG_SIZE + 1

        fun nameFits(name: String): Boolean = name.toByteArray(Charsets.UTF_8).size <= MAX_NAME_BYTES

        /** null, wenn die Bytes nicht diesem Aufbau entsprechen. */
        fun decode(bytes: ByteArray): InnerPayload? {
            if (bytes.size < HEADER_SIZE) return null
            val buffer = ByteBuffer.wrap(bytes)
            val sentAt = buffer.long
            val deviceTag = ByteArray(DEVICE_TAG_SIZE).also { buffer.get(it) }
            val nameLength = buffer.get().toInt() and 0xFF
            if (nameLength > MAX_NAME_BYTES || nameLength > buffer.remaining()) return null
            val name = ByteArray(nameLength).also { buffer.get(it) }
            val text = ByteArray(buffer.remaining()).also { buffer.get(it) }
            return InnerPayload(sentAt, deviceTag, String(name, Charsets.UTF_8), String(text, Charsets.UTF_8))
        }
    }
}
