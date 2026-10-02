package com.example.foldback.encrogram.session

import java.util.concurrent.TimeUnit

/**
 * Erinnert daran, den Satz zu wechseln. Gespeichert wird nur das Datum des zuletzt erzeugten Satzes.
 * Encrogram weiß nicht, welcher Satz zu wem gehört; die Erinnerung gilt deshalb für alle Sätze gemeinsam.
 */
class RotationReminder(
    private val settings: EncrogramSettings,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val isDue: Boolean
        get() = settings.lastPhraseCreatedAt?.let { now() - it >= INTERVAL_MS } ?: false

    fun markNewPhrase() {
        settings.lastPhraseCreatedAt = now()
    }

    companion object {
        val INTERVAL_MS = TimeUnit.DAYS.toMillis(30)
    }
}
