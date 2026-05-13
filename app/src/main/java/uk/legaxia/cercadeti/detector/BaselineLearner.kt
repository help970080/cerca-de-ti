package uk.legaxia.cercadeti.detector

import android.content.Context
import android.content.SharedPreferences
import uk.legaxia.cercadeti.service.SensorWindow

/**
 * BaselineLearner — aprende los patrones normales del usuario.
 *
 * Usa EWMA (Exponentially Weighted Moving Average) que se ajusta lentamente
 * a los valores típicos. Excluye automáticamente ventanas donde el detector
 * encontró riesgo (para no contaminar el baseline con eventos anómalos).
 *
 * En Fase 0 los valores iniciales son defaults razonables.
 * Con uso continuo durante 1-2 semanas, el baseline se personaliza.
 */
class BaselineLearner(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val audio: AudioBaseline get() = AudioBaseline(
        avgDb = prefs.getFloat(KEY_AUDIO_DB, 35f).toDouble(),
        pitchVar = prefs.getFloat(KEY_PITCH_VAR, 5f).toDouble()
    )

    val motion: MotionBaseline get() = MotionBaseline(
        avgAccel = prefs.getFloat(KEY_MOTION_AVG, 1f).toDouble()
    )

    /**
     * Actualiza el baseline con una nueva observación si la ventana parece "normal".
     */
    fun observar(ventana: SensorWindow) {
        // Solo actualizamos baseline en ventanas claramente normales,
        // para evitar que eventos de peligro contaminen los promedios.
        if (parecePotencialmenteAnomala(ventana)) return

        val nuevoDb = ewma(audio.avgDb, ventana.audio.avgDb, ALPHA)
        val nuevoPitch = ewma(audio.pitchVar, ventana.audio.pitchVariance, ALPHA)
        val nuevoAccel = ewma(motion.avgAccel, ventana.motion.avgAccelMagnitude, ALPHA)

        prefs.edit()
            .putFloat(KEY_AUDIO_DB, nuevoDb.toFloat())
            .putFloat(KEY_PITCH_VAR, nuevoPitch.toFloat())
            .putFloat(KEY_MOTION_AVG, nuevoAccel.toFloat())
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    private fun parecePotencialmenteAnomala(v: SensorWindow): Boolean {
        return v.audio.avgDb > 75 ||
               v.motion.maxAccelMagnitude > 15 ||
               v.audio.pitchVariance > 50
    }

    private fun ewma(viejo: Double, nuevo: Double, alpha: Double): Double =
        alpha * nuevo + (1 - alpha) * viejo

    data class AudioBaseline(val avgDb: Double, val pitchVar: Double)
    data class MotionBaseline(val avgAccel: Double)

    companion object {
        private const val PREFS = "cdt_baseline"
        private const val KEY_AUDIO_DB = "audio_db"
        private const val KEY_PITCH_VAR = "pitch_var"
        private const val KEY_MOTION_AVG = "motion_avg"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val ALPHA = 0.01  // muy lento; ~100 muestras para acomodar cambios
    }
}
