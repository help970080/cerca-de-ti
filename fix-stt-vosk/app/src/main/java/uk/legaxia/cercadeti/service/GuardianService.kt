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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.legaxia.cercadeti.CercaApp
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.alert.AlertManager
import uk.legaxia.cercadeti.detector.RiskDetector
import uk.legaxia.cercadeti.detector.RiskLevel
import uk.legaxia.cercadeti.storage.SettingsRepo
import uk.legaxia.cercadeti.stt.KeywordSpotter
import uk.legaxia.cercadeti.stt.VoskManager
import uk.legaxia.cercadeti.ui.MainActivity

class GuardianService : LifecycleService() {

    private lateinit var audioMonitor: AudioMonitor
    private lateinit var motionMonitor: MotionMonitor
    private lateinit var locationMonitor: LocationMonitor
    private lateinit var detector: RiskDetector
    private lateinit var alertManager: AlertManager
    private lateinit var voskManager: VoskManager
    private lateinit var settings: SettingsRepo

    private var detectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService onCreate")

        audioMonitor = AudioMonitor(this)
        motionMonitor = MotionMonitor(this)
        locationMonitor = LocationMonitor(this)
        detector = RiskDetector(this)
        alertManager = AlertManager(this)
        voskManager = VoskManager(this)
        settings = SettingsRepo(this)

        iniciarForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "GuardianService onStartCommand action=${intent?.action}")

        when (intent?.action) {
            ACTION_PAUSAR -> { pausar(); return START_STICKY }
            ACTION_REANUDAR -> { reanudar(); return START_STICKY }
            ACTION_DETENER -> { stopSelf(); return START_NOT_STICKY }
        }

        audioMonitor.iniciar()
        motionMonitor.iniciar()
        locationMonitor.iniciar()
        AlertManager.audioProvider = { audioMonitor.volcarBuffer() }

        // Cargar Vosk si el modelo está en disco y hay palabras clave configuradas
        cargarVoskSiCorresponde()

        iniciarBucleDetector()
        return START_STICKY
    }

    private fun cargarVoskSiCorresponde() {
        if (settings.palabrasClave.isEmpty()) {
            Log.i(TAG, "No hay palabras clave configuradas; Vosk no se carga")
            return
        }
        if (!voskManager.modeloListoEnDisco()) {
            Log.w(TAG, "Modelo Vosk no descargado; las palabras clave no funcionarán hasta descargar")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ok = voskManager.descargarYCargar { _, _ -> /* ya está descargado */ }
                if (ok) {
                    val modelo = voskManager.modelo() ?: return@launch
                    val spotter = KeywordSpotter(modelo, settings.palabrasClave)
                    audioMonitor.setKeywordSpotter(spotter)
                    Log.i(TAG, "KeywordSpotter integrado con ${settings.palabrasClave.size} palabras")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando KeywordSpotter", e)
            }
        }
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
        audioMonitor.detener()
        motionMonitor.detener()
        locationMonitor.detener()
        detectorJob?.cancel()
    }

    private fun reanudar() {
        audioMonitor.iniciar()
        motionMonitor.iniciar()
        locationMonitor.iniciar()
        cargarVoskSiCorresponde()
        iniciarBucleDetector()
    }

    override fun onDestroy() {
        Log.i(TAG, "GuardianService onDestroy")
        detectorJob?.cancel()
        audioMonitor.detener()
        motionMonitor.detener()
        locationMonitor.detener()
        voskManager.cerrar()
        AlertManager.audioProvider = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private const val TAG = "GuardianService"
        private const val EVAL_INTERVAL_MS = 2_000L

        const val ACTION_PAUSAR = "uk.legaxia.cercadeti.PAUSAR"
        const val ACTION_REANUDAR = "uk.legaxia.cercadeti.REANUDAR"
        const val ACTION_DETENER = "uk.legaxia.cercadeti.DETENER"
    }
}
