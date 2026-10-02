package com.example.foldback.encrogram.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Ruft [onScreenOff] auf, sobald der Bildschirm ausgeht.
 * ACTION_SCREEN_OFF kommt nur bei dynamisch registrierten Empfängern an, nicht über das Manifest.
 */
class ScreenOffLocker(private val onScreenOff: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) onScreenOff()
    }

    /** Am Application-Context registriert, damit der Empfänger so lange lebt wie der Prozess. */
    fun register(context: Context) {
        ContextCompat.registerReceiver(
            context.applicationContext,
            this,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
