package uk.legaxia.cercadeti.detector

import android.content.Context
import android.util.Log
import uk.legaxia.cercadeti.service.SensorWindow
import uk.legaxia.cercadeti.storage.SettingsRepo

/**
 * Detector de riesgo basado en reglas heurísticas.
 *
 * Diseño deliberadamente simple, auditable y explicable:
 * - Cada señal contribuye un puntaje específico
 * - Los umbrales son ajustables y están todos documentados
 * - No usa ML en MVP (Fase 0)
 * - Cada disparo puede explicar exactamente qué señales contribuyeron
 *
 * En Fase 1 se complementará con un BaselineLearner que personalice los umbrales
 * a cada usuario, y un modelo TensorFlow Lite para clasificación de estrés vocal.
 */
class RiskDetector(context: Context) {

    private val settings = SettingsRepo(context)
    private val baseline = BaselineLearner(context)

    fun evaluar(ventana: SensorWindow): RiskScore {
        val contribuciones = mutableListOf<Contribucion>()
        var total = 0

        // ============ SEÑAL 1: AUDIO ============

        // Volumen alto sostenido
        val baseDb = baseline.audio.avgDb
        if (ventana.audio.avgDbAbove(baseDb + 12.0, durationSec = 5)) {
            val puntos = 30
            total += puntos
            contribuciones.add(Contribucion("audio_volumen_alto", puntos,
                "Volumen ${ventana.audio.avgDb.toInt()}dB > baseline ${baseDb.toInt()}dB +12dB"))
        }

        // Palabras clave detectadas
        val palabrasUsuario = settings.palabrasClave
        if (palabrasUsuario.isNotEmpty() && ventana.audio.containsKeyword(palabrasUsuario)) {
            val puntos = 100  // disparo directo
            total += puntos
            contribuciones.add(Contribucion("audio_palabra_clave", puntos,
                "Palabra clave detectada: ${ventana.audio.palabrasClaveDetectadas}"))
        }

        // Varianza de pitch elevada (proxy de estrés vocal)
        if (ventana.audio.pitchVarianceAbove(baseline.audio.pitchVar * 2.5)) {
            val puntos = 20
            total += puntos
            contribuciones.add(Contribucion("audio_pitch_anomalo", puntos,
                "Varianza de pitch ${ventana.audio.pitchVariance.toInt()} > 2.5x baseline"))
        }

        // ============ SEÑAL 2: MOVIMIENTO ============

        // Impacto fuerte (caída o golpe)
        if (ventana.motion.maxAccelMagnitude > 25.0) {
            val puntos = 25
            total += puntos
            contribuciones.add(Contribucion("motion_impacto", puntos,
                "Aceleración máxima ${ventana.motion.maxAccelMagnitude.toInt()} m/s²"))
        }

        // Caída + inmovilidad súbita
        if (ventana.motion.suddenStop(thresholdSec = 3)) {
            val puntos = 30
            total += puntos
            contribuciones.add(Contribucion("motion_caida_inmovilidad", puntos,
                "Parada brusca tras movimiento intenso"))
        }

        // Movimiento intenso continuo (forcejeo)
        if (ventana.motion.continuousHighIntensity(durationSec = 8)) {
            val puntos = 25
            total += puntos
            contribuciones.add(Contribucion("motion_forcejeo", puntos,
                "Movimiento intenso continuo ${ventana.motion.continuousHighIntensityMs / 1000}s"))
        }

        // Cambios bruscos de orientación
        if (ventana.motion.orientationChanges > 3) {
            val puntos = 10
            total += puntos
            contribuciones.add(Contribucion("motion_orientacion", puntos,
                "${ventana.motion.orientationChanges} cambios de orientación"))
        }

        // ============ SEÑAL 3: UBICACIÓN ============

        if (ventana.location.cambioRapidoZona() && !settings.estaEnTransitoEsperado()) {
            val puntos = 15
            total += puntos
            contribuciones.add(Contribucion("location_zona_anomala", puntos,
                "Velocidad ${ventana.location.speedMs.toInt()} m/s en zona no habitual"))
        }

        // ============ SEÑAL 4: DISPOSITIVO ============

        if (ventana.device.intentosDesbloqueoFallidos > 3) {
            val puntos = 20
            total += puntos
            contribuciones.add(Contribucion("device_unlock_failed", puntos,
                "${ventana.device.intentosDesbloqueoFallidos} intentos de desbloqueo fallidos"))
        }

        if (ventana.device.pulsacionesBotonEncendido > 3) {
            val puntos = 25
            total += puntos
            contribuciones.add(Contribucion("device_power_button", puntos,
                "Intento repetido de apagado"))
        }

        val level = when {
            total >= 100 -> RiskLevel.CRITICAL
            total >= 70 -> RiskLevel.HIGH
            total >= 50 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        if (level != RiskLevel.LOW) {
            Log.w(TAG, "Riesgo $level (total=$total): $contribuciones")
        }

        // Alimentar al baseline learner con esta ventana (siempre, para que aprenda lo normal)
        baseline.observar(ventana)

        return RiskScore(total = total, level = level, contribuciones = contribuciones)
    }

    companion object {
        private const val TAG = "RiskDetector"
    }
}

data class RiskScore(
    val total: Int,
    val level: RiskLevel,
    val contribuciones: List<Contribucion>
)

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class Contribucion(
    val codigo: String,
    val puntos: Int,
    val descripcion: String
)
