package uk.legaxia.cercadeti.detector

import android.content.Context
import android.util.Log
import uk.legaxia.cercadeti.service.SensorWindow
import uk.legaxia.cercadeti.storage.SettingsRepo

/**
 * Detector de riesgo v2 — basado en reglas mejoradas.
 *
 * Cambios respecto a v1:
 * - Se distinguen GRITOS reales (sostenidos, pitch alto) de golpes/ruidos cortos
 * - Volumen alto solo cuenta si va acompañado de duración mínima
 * - Pitch alto (proxy del ZCR) pesa más como indicador de auxilio humano
 * - Para llegar a HIGH/CRITICAL ahora se requieren al menos 2 categorías
 *   (audio + movimiento, o audio + palabra clave)
 * - Sensibilidad de "patrón fonético de auxilio" — dispara aunque no haya STT real
 *
 * Categorías de señales:
 *   AUDIO: volumen alto sostenido, pitch alterado, patrón de grito
 *   MOVIMIENTO: impacto, forcejeo, caída
 *   UBICACIÓN: zona anómala, velocidad inusual
 *   DISPOSITIVO: intentos de apagado, desbloqueo fallido
 */
class RiskDetector(context: Context) {

    private val settings = SettingsRepo(context)
    private val baseline = BaselineLearner(context)

    fun evaluar(ventana: SensorWindow): RiskScore {
        val contribuciones = mutableListOf<Contribucion>()
        val categoriasActivas = mutableSetOf<String>()
        var total = 0

        // ============ SEÑAL 1: AUDIO ============

        val baseDb = baseline.audio.avgDb
        val deltaDb = ventana.audio.avgDb - baseDb

        // a) Grito sostenido — volumen alto + duración + voz humana
        //    Esto es el discriminador clave: distingue grito real de golpe puntual
        if (deltaDb > 15 &&
            ventana.audio.voiceActivityRatio > 0.4 &&
            ventana.audio.windowDurationSec >= 3) {
            val puntos = 40
            total += puntos
            categoriasActivas.add("AUDIO")
            contribuciones.add(Contribucion("audio_grito_sostenido", puntos,
                "Volumen +${deltaDb.toInt()}dB sobre baseline, voz ${(ventana.audio.voiceActivityRatio * 100).toInt()}% del tiempo"))
        }
        // b) Volumen muy alto (independiente del baseline) — para gritos extremos
        else if (ventana.audio.avgDb > 65 && ventana.audio.windowDurationSec >= 2) {
            val puntos = 25
            total += puntos
            categoriasActivas.add("AUDIO")
            contribuciones.add(Contribucion("audio_volumen_alto", puntos,
                "Volumen ${ventana.audio.avgDb.toInt()}dB sostenido"))
        }

        // c) Pitch alterado (proxy de tono agudo/estrés)
        val basePitch = baseline.audio.pitchVar
        if (basePitch > 0 && ventana.audio.pitchVariance > basePitch * 2.5) {
            val puntos = 25
            total += puntos
            categoriasActivas.add("AUDIO")
            contribuciones.add(Contribucion("audio_pitch_anomalo", puntos,
                "Pitch ${ventana.audio.pitchVariance.toInt()} vs baseline ${basePitch.toInt()}"))
        }

        // d) Patrón fonético de auxilio: volumen alto + pico muy fuerte + voz
        //    Esto cubre el caso de gritar "AUXILIO" o "AYUDA" sin necesitar STT
        if (ventana.audio.peakDb > 75 &&
            ventana.audio.voiceActivityRatio > 0.3 &&
            deltaDb > 10) {
            val puntos = 35
            total += puntos
            categoriasActivas.add("AUDIO")
            contribuciones.add(Contribucion("audio_patron_auxilio", puntos,
                "Pico ${ventana.audio.peakDb.toInt()}dB con patrón de grito vocal"))
        }

        // e) Palabras clave (solo si STT está integrado, vacío en MVP)
        val palabrasUsuario = settings.palabrasClave
        if (palabrasUsuario.isNotEmpty() && ventana.audio.containsKeyword(palabrasUsuario)) {
            val puntos = 100
            total += puntos
            categoriasActivas.add("PALABRA_CLAVE")
            contribuciones.add(Contribucion("audio_palabra_clave", puntos,
                "Palabra clave detectada: ${ventana.audio.palabrasClaveDetectadas}"))
        }

        // ============ SEÑAL 2: MOVIMIENTO ============

        if (ventana.motion.maxAccelMagnitude > 25.0) {
            val puntos = 25
            total += puntos
            categoriasActivas.add("MOVIMIENTO")
            contribuciones.add(Contribucion("motion_impacto", puntos,
                "Aceleración máxima ${ventana.motion.maxAccelMagnitude.toInt()} m/s²"))
        }

        if (ventana.motion.suddenStop(thresholdSec = 3)) {
            val puntos = 30
            total += puntos
            categoriasActivas.add("MOVIMIENTO")
            contribuciones.add(Contribucion("motion_caida_inmovilidad", puntos,
                "Parada brusca tras movimiento intenso"))
        }

        if (ventana.motion.continuousHighIntensity(durationSec = 8)) {
            val puntos = 30
            total += puntos
            categoriasActivas.add("MOVIMIENTO")
            contribuciones.add(Contribucion("motion_forcejeo", puntos,
                "Movimiento intenso continuo ${ventana.motion.continuousHighIntensityMs / 1000}s"))
        }

        if (ventana.motion.orientationChanges > 5) {
            val puntos = 15
            total += puntos
            categoriasActivas.add("MOVIMIENTO")
            contribuciones.add(Contribucion("motion_orientacion", puntos,
                "${ventana.motion.orientationChanges} cambios de orientación"))
        }

        // ============ SEÑAL 3: UBICACIÓN ============

        if (ventana.location.cambioRapidoZona() && !settings.estaEnTransitoEsperado()) {
            val puntos = 15
            total += puntos
            categoriasActivas.add("UBICACION")
            contribuciones.add(Contribucion("location_zona_anomala", puntos,
                "Velocidad ${ventana.location.speedMs.toInt()} m/s en zona no habitual"))
        }

        // ============ DETERMINAR NIVEL ============

        // Política de seguridad anti-falsos-positivos:
        // - Para alcanzar HIGH se requieren al menos 2 categorías diferentes
        //   (excepción: palabra clave dispara solita = CRITICAL inmediato)
        // - Esto evita disparos por ruido aislado (licuadora) o sacudida aislada
        val categoriaCount = categoriasActivas.size
        val tienePalabraClave = "PALABRA_CLAVE" in categoriasActivas

        val level = when {
            tienePalabraClave -> RiskLevel.CRITICAL
            total >= 80 && categoriaCount >= 2 -> RiskLevel.CRITICAL
            total >= 60 && categoriaCount >= 2 -> RiskLevel.HIGH
            total >= 40 -> RiskLevel.MEDIUM  // Pre-aviso, no dispara cuenta atrás
            else -> RiskLevel.LOW
        }

        if (level != RiskLevel.LOW) {
            Log.w(TAG, "Riesgo $level (total=$total, categorías=$categoriasActivas)")
        }

        // Alimentar baseline solo si no parece anómalo
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
