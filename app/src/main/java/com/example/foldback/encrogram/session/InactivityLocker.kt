package com.example.foldback.encrogram.session

import android.os.Handler
import android.os.Looper

/** Ruft [onTimeout] auf, wenn [TIMEOUT_MS] lang kein [touch] kam. */
class InactivityLocker(private val onTimeout: () -> Unit) {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { onTimeout() }

    fun touch() {
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    fun cancel() = handler.removeCallbacks(timeout)

    companion object {
        const val TIMEOUT_MS = 2 * 60 * 1000L
    }
}
