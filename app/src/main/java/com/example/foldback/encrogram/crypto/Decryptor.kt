package com.example.foldback.encrogram.crypto

import com.example.foldback.encrogram.format.InnerPayload
import com.example.foldback.encrogram.format.TencdecMessage

/** Entschlüsselt eine [TencdecMessage]. Fehler sind normale Ergebnisse, keine Exceptions. */
class Decryptor {

    fun decrypt(message: TencdecMessage, key: SecretKey): DecryptResult {
        val padded = AesGcm.open(key, message.nonce, TencdecMessage.associatedData(message.salt), message.sealed)
            ?: return DecryptResult.WrongKey
        val payload = Padding.unpad(padded)?.let(InnerPayload::decode)
            ?: return DecryptResult.Malformed
        return DecryptResult.Success(payload)
    }
}

sealed interface DecryptResult {
    data class Success(val payload: InnerPayload) : DecryptResult

    /** Falscher Satz oder unterwegs veränderte Nachricht – beides ist nicht unterscheidbar. */
    data object WrongKey : DecryptResult

    /** Echt, aber mit unerwartetem Inhalt. Kann nur von jemandem mit dem Satz stammen. */
    data object Malformed : DecryptResult
}
