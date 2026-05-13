package uk.legaxia.cercadeti

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Cerca de Ti — Aplicación de detección pasiva de situaciones de peligro
 *
 * Aplicación gratuita, sin fines de lucro, código abierto bajo licencia MIT.
 *
 * Repositorio: https://github.com/help970080/cerca-de-ti
 */
class CercaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        crearCanalesNotificacion()
    }

    private fun crearCanalesNotificacion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = getSystemService<NotificationManager>() ?: return

        // Canal del servicio en background (notificación persistente)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICIO,
                "Servicio Guardián",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación permanente que indica que Cerca de Ti está activo"
                setShowBadge(false)
            }
        )

        // Canal de alertas críticas
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTA,
                "Alertas de Peligro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cuenta atrás de cancelación cuando se detecta una posible situación de peligro"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
        )

        // Canal de eventos confirmados
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTO,
                "Eventos Confirmados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Confirmaciones de alertas enviadas a contactos"
            }
        )
    }

    companion object {
        const val CHANNEL_SERVICIO = "cdt_servicio"
        const val CHANNEL_ALERTA = "cdt_alerta"
        const val CHANNEL_EVENTO = "cdt_evento"

        const val NOTIF_ID_SERVICIO = 1001
        const val NOTIF_ID_ALERTA = 1002
        const val NOTIF_ID_EVENTO = 1003

        // Configuración del detector
        const val AUDIO_BUFFER_SECONDS = 60
        const val AUDIO_SAMPLE_RATE = 16000
        const val GPS_BUFFER_MINUTES = 10
        const val COUNTDOWN_SECONDS = 30

        // Umbrales del detector basado en reglas
        const val RISK_THRESHOLD_CRITICAL = 100
        const val RISK_THRESHOLD_HIGH = 70
        const val RISK_THRESHOLD_MEDIUM = 50
    }
}
