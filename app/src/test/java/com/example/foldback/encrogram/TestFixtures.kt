package com.example.foldback.encrogram

import com.example.foldback.encrogram.crypto.Argon2Params
import com.example.foldback.encrogram.crypto.Encryptor
import com.example.foldback.encrogram.crypto.KeyDeriver
import com.example.foldback.encrogram.crypto.SecretKey
import com.example.foldback.encrogram.format.Sender
import com.example.foldback.encrogram.format.TencdecMessage
import com.example.foldback.encrogram.passphrase.Passphrase
import com.example.foldback.encrogram.passphrase.Wordlist
import java.io.File

/** Gemeinsame Bausteine für die Tests. Argon2 läuft hier absichtlich billig, damit die Tests schnell sind. */
object TestFixtures {

    val fastDeriver = KeyDeriver(Argon2Params(memoryKiB = 64, iterations = 1, parallelism = 1))

    val salt = ByteArray(TencdecMessage.SALT_SIZE) { it.toByte() }

    val anna = Sender("Anna", ByteArray(8) { 1 })

    const val PHRASE = "ofen wolke spitz ruder kamel neun birke tal"

    fun key(phrase: String = PHRASE, salt: ByteArray = this.salt): SecretKey =
        fastDeriver.derive(Passphrase.fromInput(phrase), salt)

    fun encryptor(time: Long = 1_760_000_000L) = Encryptor(clock = { time })

    /** Die Wortliste aus dem Projekt; Unit-Tests laufen im Modulordner app/. */
    fun projectWordlist(): Wordlist =
        File("src/main/res/raw/german_words.txt").useLines(Charsets.UTF_8) { Wordlist.parse(it) }
}
