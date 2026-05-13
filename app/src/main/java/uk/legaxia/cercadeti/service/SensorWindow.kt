package uk.legaxia.cercadeti.service

import android.app.KeyguardManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Ventana agregada de todos los sensores en un momento dado.
 * Esta es la unidad de evaluación del detector.
 */
data class SensorWindow(
    val audio: AudioSnapshot,
    val motion: MotionSnapshot,
    val location: LocationSnapshot,
    val device: DeviceState,
    val timestampMs: Long
)

/**
 * Snapshot de las propiedades de audio de los últimos N segundos.
 * IMPORTANTE: no contiene el audio crudo. Solo estadísticas derivadas.
 * El audio crudo solo se materializa al disparar una alerta.
 */
data class AudioSnapshot(
    val avgDb: Double,                       // dB promedio en la ventana
    val peakDb: Double,                      // dB máximo
    val pitchVariance: Double,               // varianza del pitch fundamental
    val voiceActivityRatio: Double,          // 0..1, qué fracción del tiempo había voz
    val palabrasClaveDetectadas: List<String>, // palabras clave coincidentes en ventana reciente
    val windowDurationSec: Int
) {
    fun avgDbAbove(threshold: Double, durationSec: Int): Boolean =
        avgDb > threshold && windowDurationSec >= durationSec

    fun containsKeyword(palabras: Set<String>): Boolean =
        palabrasClaveDetectadas.any { it in palabras }

    fun pitchVarianceAbove(threshold: Double): Boolean =
        pitchVariance > threshold
}

/**
 * Snapshot del movimiento del dispositivo.
 */
data class MotionSnapshot(
    val maxAccelMagnitude: Double,           // magnitud de aceleración máxima (m/s²)
    val avgAccelMagnitude: Double,
    val maxGyroMagnitude: Double,            // rotación angular máxima (rad/s)
    val suddenStops: Int,                    // # de paradas bruscas en la ventana
    val continuousHighIntensityMs: Long,     // ms de movimiento intenso continuo
    val orientationChanges: Int,             // # de cambios bruscos de orientación
    val windowDurationSec: Int
) {
    fun continuousHighIntensity(durationSec: Int): Boolean =
        continuousHighIntensityMs >= durationSec * 1000L

    fun suddenStop(thresholdSec: Int): Boolean =
        suddenStops > 0 && continuousHighIntensityMs >= thresholdSec * 1000L
}

/**
 * Snapshot de ubicación y movimiento espacial.
 */
data class LocationSnapshot(
    val latActual: Double?,
    val lonActual: Double?,
    val speedMs: Double,                     // velocidad actual en m/s
    val distanceFromBaseline: Double,        // metros desde zona habitual
    val trayectoria: List<TrayectoriaPunto>, // últimos puntos GPS
    val precisionMeters: Float
) {
    fun esZonaHabitual(): Boolean = distanceFromBaseline < 200

    fun cambioRapidoZona(): Boolean = distanceFromBaseline > 500 && speedMs > 5.0
}

data class TrayectoriaPunto(
    val lat: Double,
    val lon: Double,
    val timestampMs: Long,
    val speedMs: Float
)

/**
 * Estado del dispositivo capturado al momento.
 */
data class DeviceState(
    val pantallaBloqueada: Boolean,
    val intentosDesbloqueoFallidos: Int,
    val pulsacionesBotonEncendido: Int,
    val bateriaPct: Int,
    val cargando: Boolean,
    val modoAvion: Boolean,
    val tieneConexion: Boolean
) {
    companion object {
        fun capturar(context: Context): DeviceState {
            val km = context.getSystemService<KeyguardManager>()
            return DeviceState(
                pantallaBloqueada = km?.isKeyguardLocked ?: false,
                intentosDesbloqueoFallidos = 0,  // TODO: requiere DeviceAdmin para acceso real
                pulsacionesBotonEncendido = 0,   // TODO: trackear vía PowerManager events
                bateriaPct = 100,                 // TODO: BatteryManager
                cargando = false,
                modoAvion = false,
                tieneConexion = true
            )
        }
    }
}
