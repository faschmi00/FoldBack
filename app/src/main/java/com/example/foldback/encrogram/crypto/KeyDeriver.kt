package com.example.foldback.encrogram.crypto

import com.example.foldback.encrogram.passphrase.Passphrase
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Kosten von Argon2id. Höher = jeder Rateversuch eines Angreifers wird teurer, aber auch jedes Entsperren. */
data class Argon2Params(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        /** 64 MiB Speicher, 3 Durchläufe, 1 Thread. */
        val DEFAULT = Argon2Params(memoryKiB = 64 * 1024, iterations = 3, parallelism = 1)
    }
}

/**
 * Macht aus Sicherheitssatz und Salt einen 256-Bit-Schlüssel (Argon2id, Version 1.3).
 *
 * Argon2id ist absichtlich langsam und speicherhungrig. Das bremst Rateversuche auf
 * Grafikkarten und Spezialhardware. Deshalb nie im UI-Thread aufrufen.
 */
class KeyDeriver(private val params: Argon2Params = Argon2Params.DEFAULT) {

    fun derive(passphrase: Passphrase, salt: ByteArray): SecretKey {
        val generator = Argon2BytesGenerator()
        generator.init(
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(params.memoryKiB)
                .withIterations(params.iterations)
                .withParallelism(params.parallelism)
                .withSalt(salt)
                .build(),
        )
        val key = ByteArray(SecretKey.SIZE)
        passphrase.withBytes { generator.generateBytes(it, key) }
        return SecretKey(key)
    }
}
