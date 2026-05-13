package uk.legaxia.cercadeti.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.legaxia.cercadeti.CercaApp
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.alert.AlertManager
import uk.legaxia.cercadeti.detector.RiskDetector
import uk.legaxia.cercadeti.detector.RiskLevel
import uk.legaxia.cercadeti.ui.MainActivity

/**
 * Servicio principal que mantiene activos los monitores y el detector.
 *
 * Corre como ForegroundService con tipos `microphone` + `location` (requerido
 * por Android 14 cuando se accede a esos sensores desde background).
 *
 * Ciclo:
 * 1. Inicia los monitores (audio, movimiento, ubicación)
 * 2. Cada 2 segundos evalúa el riesgo combinado
 * 3. Si el riesgo supera umbral, escala al AlertManager
 * 4. AlertManager decide si dispara cuenta atrás o alerta directa
 */
class GuardianService : LifecycleService() {

    private lateinit var audioMonitor: AudioMonitor
    private lateinit var motionMonitor: MotionMonitor
    private lateinit var locationMonitor: LocationMonitor
    private lateinit var detector: RiskDetector
    private lateinit var alertManager: AlertManager

    private var detectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService onCreate")

        audioMonitor = AudioMonitor(this)
        motionMonitor = MotionMonitor(this)
        locationMonitor = LocationMonitor(this)
        detector = RiskDetector(this)
        alertManager = AlertManager(this)

        iniciarForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "GuardianService onStartCommand")

        when (intent?.action) {
            ACTION_PAUSAR -> { pausar(); return START_STICKY }
            ACTION_REANUDAR -> { reanudar(); return START_STICKY }
            ACTION_DETENER -> { stopSelf(); return START_NOT_STICKY }
        }

        audioMonitor.iniciar()
        motionMonitor.iniciar()
        locationMonitor.iniciar()

        // Exponer el ring buffer del audio para que AlertManager pueda volcarlo
        // al confirmar una alerta (CountdownActivity vive en otra instancia).
        AlertManager.audioProvider = { audioMonitor.volcarBuffer() }

        iniciarBucleDetector()

        // START_STICKY: Android reinicia el servicio si lo mata el sistema
        return START_STICKY
    }

    private fun iniciarForeground() {
        val intentApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intentApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CercaApp.CHANNEL_SERVICIO)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.notif_servicio_titulo))
            .setContentText(getString(R.string.notif_servicio_texto))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                CercaApp.NOTIF_ID_SERVICIO,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(CercaApp.NOTIF_ID_SERVICIO, notif)
        }
    }

    /**
     * Bucle que cada 2 segundos toma una ventana de los buffers y la evalúa.
     */
    private fun iniciarBucleDetector() {
        detectorJob?.cancel()
        detectorJob = lifecycleScope.launch {
            while (true) {
                try {
                    val ventana = construirVentana()
                    val score = detector.evaluar(ventana)

                    if (score.level != RiskLevel.LOW) {
                        Log.w(TAG, "Riesgo detectado: ${score.level} (total=${score.total})")
                        alertManager.procesarRiesgo(score, ventana)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle detector", e)
                }
                delay(EVAL_INTERVAL_MS)
            }
        }
    }

    private fun construirVentana(): SensorWindow {
        return SensorWindow(
            audio = audioMonitor.snapshot(),
            motion = motionMonitor.snapshot(),
            location = locationMonitor.snapshot(),
            device = DeviceState.capturar(this),
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun pausar() {
        Log.i(TAG, "Pausando monitores")
        audioMonitor.detener()
        motionMonitor.detener()
        locationMonitor.detener()
        detectorJob?.cancel()
    }

    private fun reanudar() {
        Log.i(TAG, "Reanudando monitores")
        audioMonitor.iniciar()
        motionMonitor.iniciar()
        locationMonitor.iniciar()
        iniciarBucleDetector()
    }

    override fun onDestroy() {
        Log.i(TAG, "GuardianService onDestroy")
        detectorJob?.cancel()
        audioMonitor.detener()
        motionMonitor.detener()
        locationMonitor.detener()
        AlertManager.audioProvider = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null  // Servicio no bindable; comunicación vía Intents
    }

    companion object {
        private const val TAG = "GuardianService"
        private const val EVAL_INTERVAL_MS = 2_000L

        const val ACTION_PAUSAR = "uk.legaxia.cercadeti.PAUSAR"
        const val ACTION_REANUDAR = "uk.legaxia.cercadeti.REANUDAR"
        const val ACTION_DETENER = "uk.legaxia.cercadeti.DETENER"
    }
}
