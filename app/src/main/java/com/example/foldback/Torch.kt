package com.example.foldback

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.Toast

/** Schaltet die Taschenlampe direkt an und aus, ohne eine App zu öffnen. */
object Torch {
    private var cameraId: String? = null
    private var enabled = false

    /**
     * Merkt sich den aktuellen Zustand der Lampe, auch wenn sie z. B. über die
     * Schnelleinstellungen geschaltet wurde. Einmal beim Start aufrufen.
     */
    fun init(context: Context) {
        if (cameraId != null) return
        // App-Context, damit der Callback keine Activity festhält.
        val manager = context.applicationContext.getSystemService(CameraManager::class.java) ?: return
        cameraId = try {
            manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: CameraAccessException) {
            null
        }
        manager.registerTorchCallback(
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, isEnabled: Boolean) {
                    if (id == cameraId) enabled = isEnabled
                }
            },
            null,
        )
    }

    fun toggle(context: Context) {
        init(context)
        val id = cameraId
        if (id == null) {
            Toast.makeText(context, "Keine Taschenlampe vorhanden", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.getSystemService(CameraManager::class.java).setTorchMode(id, !enabled)
        } catch (_: CameraAccessException) {
            // Z. B. wenn gerade eine Kamera-App die Kamera belegt.
            Toast.makeText(context, "Taschenlampe gerade nicht verfügbar", Toast.LENGTH_SHORT).show()
        }
    }
}
