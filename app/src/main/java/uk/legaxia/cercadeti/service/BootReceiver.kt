package uk.legaxia.cercadeti.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import uk.legaxia.cercadeti.storage.SettingsRepo

/**
 * Re-inicia el GuardianService después de reinicio del dispositivo,
 * solo si el usuario tenía la app activa.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "BootReceiver onReceive: $action")

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val settings = SettingsRepo(context)
        if (!settings.servicioActivado) {
            Log.i(TAG, "Servicio no estaba activado; no se inicia")
            return
        }

        val svcIntent = Intent(context, GuardianService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svcIntent)
        } else {
            context.startService(svcIntent)
        }
        Log.i(TAG, "GuardianService re-iniciado tras boot")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
