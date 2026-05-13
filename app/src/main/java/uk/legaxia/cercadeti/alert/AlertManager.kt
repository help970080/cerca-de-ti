package uk.legaxia.cercadeti.alert

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uk.legaxia.cercadeti.CercaApp
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.detector.RiskLevel
import uk.legaxia.cercadeti.detector.RiskScore
import uk.legaxia.cercadeti.service.SensorWindow

/**
 * Coordinador central de alertas.
 *
 * Reglas de escalado:
 * - LOW: nada
 * - MEDIUM: notificación silenciosa al usuario (pre-aviso)
 * - HIGH: cuenta atrás de 30s; si no se cancela → enviar alerta
 * - CRITICAL: cuenta atrás de 10s; si no se cancela → enviar alerta
 *
 * Estado compartido entre instancias (GuardianService que inicia, CountdownActivity
 * que confirma o cancela) vía companion object.
 */
class AlertManager(private val context: Context) {

    fun procesarRiesgo(score: RiskScore, ventana: SensorWindow) {
        val ahora = System.currentTimeMillis()

        // Anti-spam: no apilar alertas
        alertaActiva?.let { activa ->
            if (ahora - activa.iniciadaMs < ANTI_SPAM_MS) {
                Log.d(TAG, "Alerta ya en curso (hace ${ahora - activa.iniciadaMs}ms); se ignora")
                return
            }
        }

        when (score.level) {
            RiskLevel.LOW -> { /* no-op */ }
            RiskLevel.MEDIUM -> mostrarPreaviso(score)
            RiskLevel.HIGH -> iniciarCuentaAtras(score, ventana, COUNTDOWN_HIGH_SECONDS)
            RiskLevel.CRITICAL -> iniciarCuentaAtras(score, ventana, COUNTDOWN_CRITICAL_SECONDS)
        }
    }

    private fun mostrarPreaviso(score: RiskScore) {
        val notif = NotificationCompat.Builder(context, CercaApp.CHANNEL_ALERTA)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.alerta_preaviso_titulo))
            .setContentText(context.getString(R.string.alerta_preaviso_texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(CercaApp.NOTIF_ID_ALERTA, notif)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso POST_NOTIFICATIONS", e)
        }
    }

    private fun iniciarCuentaAtras(score: RiskScore, ventana: SensorWindow, segundos: Int) {
        alertaActiva = AlertaEnCurso(
            iniciadaMs = System.currentTimeMillis(),
            score = score,
            ventana = ventana,
            segundosCuentaAtras = segundos
        )

        Log.w(TAG, "Iniciando cuenta atrás de ${segundos}s para alerta ${score.level}")

        val intent = Intent(context, CountdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CountdownActivity.EXTRA_SEGUNDOS, segundos)
            putExtra(CountdownActivity.EXTRA_NIVEL, score.level.name)
            putExtra(CountdownActivity.EXTRA_TOTAL, score.total)
        }
        context.startActivity(intent)
    }

    /** Llamado por CountdownActivity cuando el usuario cancela. */
    fun cancelarAlerta() {
        Log.i(TAG, "Alerta cancelada por el usuario")
        alertaActiva = null
    }

    /** Llamado por CountdownActivity cuando expira la cuenta atrás. */
    fun confirmarAlerta() {
        val alerta = alertaActiva ?: return
        Log.w(TAG, "Confirmando alerta: ejecutando envío y guardado de evidencia")

        val audioPcm = audioProvider?.invoke() ?: ShortArray(0)

        val packer = EvidencePacker(context)
        val paquete = packer.empaquetar(alerta.score, alerta.ventana, audioPcm)

        val relay = RelayClient(context)
        relay.enviarAlerta(paquete)

        notificarEnvio()
        alertaActiva = null
    }

    private fun notificarEnvio() {
        val notif = NotificationCompat.Builder(context, CercaApp.CHANNEL_EVENTO)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.alerta_enviada_titulo))
            .setContentText(context.getString(R.string.alerta_enviada_texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(CercaApp.NOTIF_ID_EVENTO, notif)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso POST_NOTIFICATIONS", e)
        }
    }

    internal data class AlertaEnCurso(
        val iniciadaMs: Long,
        val score: RiskScore,
        val ventana: SensorWindow,
        val segundosCuentaAtras: Int
    )

    companion object {
        private const val TAG = "AlertManager"
        private const val COUNTDOWN_HIGH_SECONDS = 30
        private const val COUNTDOWN_CRITICAL_SECONDS = 10
        private const val ANTI_SPAM_MS = 60_000L

        /**
         * Proveedor estático del audio del ring buffer. GuardianService lo configura
         * al iniciar AudioMonitor; AlertManager.confirmarAlerta() lo lee.
         */
        @Volatile var audioProvider: (() -> ShortArray)? = null

        /** Estado de la alerta en curso, compartido entre instancias. */
        @Volatile internal var alertaActiva: AlertaEnCurso? = null
    }
}
