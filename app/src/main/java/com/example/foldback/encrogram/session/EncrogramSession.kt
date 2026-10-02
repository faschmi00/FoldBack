package com.example.foldback.encrogram.session

import com.example.foldback.encrogram.passphrase.Passphrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

/**
 * Die entsperrten Sätze. Mehrere können gleichzeitig entsperrt sein (ein Satz pro Gesprächspartner).
 * Nichts davon wird gespeichert; [lockAll] überschreibt alles mit Nullen.
 */
class EncrogramSession(
    /** Berechnet die Prüfwörter eines Satzes (langsam, Argon2id). */
    private val verify: (Passphrase) -> String,
    private val random: SecureRandom,
) {
    private val _phrases = MutableStateFlow<List<UnlockedPhrase>>(emptyList())
    val phrases: StateFlow<List<UnlockedPhrase>> = _phrases.asStateFlow()

    val isUnlocked: Boolean get() = _phrases.value.isNotEmpty()

    /** Zählt jede Sperre. So wird ein Entsperren verworfen, das während einer Sperre noch lief. */
    private var lockCount = 0

    /**
     * Entsperrt [passphrase]. Ist derselbe Satz schon entsperrt, wird dieser zurückgegeben.
     * null, wenn währenddessen gesperrt wurde. Langsam – nicht im UI-Thread aufrufen.
     */
    fun unlock(passphrase: Passphrase): UnlockedPhrase? {
        val lockCountAtStart = synchronized(this) {
            _phrases.value.firstOrNull { it.matches(passphrase) }?.let {
                passphrase.wipe()
                return it
            }
            lockCount
        }
        val phrase = UnlockedPhrase(passphrase, verify(passphrase), random)
        synchronized(this) {
            if (lockCount != lockCountAtStart) {
                phrase.wipe()
                return null
            }
            _phrases.value += phrase
        }
        return phrase
    }

    fun lockAll() {
        val old = synchronized(this) {
            lockCount++
            _phrases.value.also { _phrases.value = emptyList() }
        }
        old.forEach(UnlockedPhrase::wipe)
    }
}
