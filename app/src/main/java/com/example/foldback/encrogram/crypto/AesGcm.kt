package com.example.foldback.encrogram.crypto

import com.example.foldback.encrogram.format.TencdecMessage
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM aus der Java-Kryptografie von Android: verschlüsselt und authentifiziert in einem Schritt.
 * Jede Veränderung an Geheimtext, Nonce oder AAD lässt [open] scheitern.
 */
internal object AesGcm {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun seal(key: SecretKey, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, nonce, aad).doFinal(plaintext)

    /** null, wenn der Tag nicht passt: falscher Schlüssel oder veränderte Daten. */
    fun open(key: SecretKey, nonce: ByteArray, aad: ByteArray, sealed: ByteArray): ByteArray? =
        try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, aad).doFinal(sealed)
        } catch (_: BadPaddingException) {
            // AEADBadTagException ist eine Unterklasse davon.
            null
        }

    private fun cipher(mode: Int, key: SecretKey, nonce: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, key.toKeySpec(), GCMParameterSpec(TencdecMessage.TAG_SIZE * 8, nonce))
            updateAAD(aad)
        }
}
