package com.example.foldback.encrogram

import android.content.Context
import com.example.foldback.encrogram.crypto.DecryptResult
import com.example.foldback.encrogram.crypto.Decryptor
import com.example.foldback.encrogram.crypto.Encryptor
import com.example.foldback.encrogram.crypto.KeyDeriver
import com.example.foldback.encrogram.format.InnerPayload
import com.example.foldback.encrogram.format.ParseResult
import com.example.foldback.encrogram.format.Sender
import com.example.foldback.encrogram.format.TencdecMessage
import com.example.foldback.encrogram.passphrase.Passphrase
import com.example.foldback.encrogram.passphrase.PassphraseGenerator
import com.example.foldback.encrogram.passphrase.VerificationWords
import com.example.foldback.encrogram.passphrase.Wordlist
import com.example.foldback.encrogram.session.EncrogramSession
import com.example.foldback.encrogram.session.EncrogramSettings
import com.example.foldback.encrogram.session.InactivityLocker
import com.example.foldback.encrogram.session.RotationReminder
import com.example.foldback.encrogram.session.ScreenOffLocker
import com.example.foldback.encrogram.session.UnlockedPhrase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/**
 * Einstiegspunkt für alle Encrogram-Funktionen.
 *
 * Gibt es einmal pro Prozess, damit die Haupt-Activity und das Entschlüsseln-Overlay
 * dieselbe Sitzung teilen. Die Sitzung wird gesperrt, sobald der Bildschirm ausgeht
 * oder [InactivityLocker.TIMEOUT_MS] lang nichts passiert.
 */
class Encrogram private constructor(context: Context) {

    val settings = EncrogramSettings(context)
    val rotationReminder = RotationReminder(settings)

    private val random = SecureRandom()
    private val keyDeriver = KeyDeriver()
    private val encryptor = Encryptor(random)
    private val decryptor = Decryptor()

    private val wordlist by lazy { Wordlist.load(context) }
    private val generator by lazy { PassphraseGenerator(wordlist, random) }
    private val verificationWords by lazy { VerificationWords(wordlist, keyDeriver) }

    val session = EncrogramSession(verify = { verificationWords.of(it) }, random = random)
    private val inactivity = InactivityLocker(::lock)

    init {
        ScreenOffLocker(::lock).register(context)
    }

    fun lock() {
        inactivity.cancel()
        session.lockAll()
    }

    /** Meldet Aktivität; verschiebt die automatische Sperre. */
    fun touch() {
        if (session.isUnlocked) inactivity.touch()
    }

    suspend fun generatePhrase(): List<String> = withContext(Dispatchers.Default) { generator.generate() }

    fun entropyBits(wordCount: Int): Double = generator.entropyBits(wordCount)

    /** null, wenn während des Entsperrens gesperrt wurde. */
    suspend fun unlock(input: CharSequence): UnlockedPhrase? {
        val phrase = withContext(Dispatchers.Default) { session.unlock(Passphrase.fromInput(input)) }
        touch()
        return phrase
    }

    /** Fertiger Geheimtext für Telegram; null, wenn [phrase] inzwischen gesperrt wurde. */
    suspend fun encrypt(text: String, phrase: UnlockedPhrase): String? = withContext(Dispatchers.Default) {
        touch()
        val sender = Sender(settings.senderName, settings.deviceTag)
        phrase.withKey(phrase.sendSalt, keyDeriver) { key ->
            encryptor.encrypt(text, sender, key, phrase.sendSalt).toText()
        }
    }

    /** Probiert alle entsperrten Sätze durch. */
    suspend fun decrypt(text: CharSequence): DecryptOutcome = withContext(Dispatchers.Default) {
        touch()
        val message = when (val parsed = TencdecMessage.parse(text)) {
            is ParseResult.Ok -> parsed.message
            ParseResult.NotAMessage -> return@withContext DecryptOutcome.NotAMessage
            ParseResult.Malformed -> return@withContext DecryptOutcome.Malformed
            is ParseResult.UnsupportedVersion -> return@withContext DecryptOutcome.UnsupportedVersion
        }
        val phrases = session.phrases.value
        if (phrases.isEmpty()) return@withContext DecryptOutcome.Locked

        for (phrase in phrases) {
            when (val result = phrase.withKey(message.salt, keyDeriver) { decryptor.decrypt(message, it) }) {
                is DecryptResult.Success -> return@withContext success(result.payload, phrase)
                DecryptResult.Malformed -> return@withContext DecryptOutcome.Malformed
                DecryptResult.WrongKey, null -> Unit
            }
        }
        DecryptOutcome.NoMatchingPhrase
    }

    private fun success(payload: InnerPayload, phrase: UnlockedPhrase): DecryptOutcome.Success {
        val ownName = settings.senderName
        val warning = when {
            payload.deviceTag.contentEquals(settings.deviceTag) -> SenderWarning.THIS_DEVICE
            ownName.isNotBlank() && payload.senderName.equals(ownName, ignoreCase = true) -> SenderWarning.OWN_NAME
            else -> null
        }
        return DecryptOutcome.Success(payload, phrase.verification, warning)
    }

    companion object {
        @Volatile
        private var instance: Encrogram? = null

        fun get(context: Context): Encrogram =
            instance ?: synchronized(this) {
                instance ?: Encrogram(context.applicationContext).also { instance = it }
            }
    }
}

sealed interface DecryptOutcome {
    data class Success(
        val payload: InnerPayload,
        /** Prüfwörter des Satzes, mit dem es geklappt hat. */
        val verification: String,
        val warning: SenderWarning?,
    ) : DecryptOutcome

    data object NotAMessage : DecryptOutcome
    data object Malformed : DecryptOutcome
    data object UnsupportedVersion : DecryptOutcome
    data object Locked : DecryptOutcome
    data object NoMatchingPhrase : DecryptOutcome
}

/** Hinweise auf eine gespiegelte Nachricht: Sie stammt eigentlich von dir selbst. */
enum class SenderWarning { THIS_DEVICE, OWN_NAME }
