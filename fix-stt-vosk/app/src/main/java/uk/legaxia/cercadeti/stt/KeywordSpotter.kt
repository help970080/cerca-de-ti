package uk.legaxia.cercadeti.stt

import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.text.Normalizer

/**
 * Detector de palabras clave usando Vosk STT.
 *
 * Recibe chunks de audio PCM 16kHz mono del AudioMonitor, los pasa al
 * Recognizer de Vosk, y compara los resultados parciales y finales
 * contra la lista de palabras clave del usuario.
 *
 * Las palabras detectadas se exponen vía obtenerPalabrasRecientes(), que
 * el AudioMonitor lee para incluirlas en el AudioSnapshot.
 *
 * Performance: Vosk en arm64 procesa audio en tiempo real, ~5% CPU.
 */
class KeywordSpotter(
    modelo: Model,
    private val palabrasClave: Set<String>
) {

    private val recognizer: Recognizer = Recognizer(modelo, 16000f)
    private val palabrasNormalizadas: Set<String> = palabrasClave.map { normalizar(it) }.toSet()

    /**
     * Historial de detecciones recientes (últimos 10 segundos).
     * Los snapshots del AudioMonitor leen esto.
     */
    @Volatile private var ultimasDetecciones = mutableListOf<DeteccionPalabra>()

    /**
     * Procesa un chunk de audio PCM 16bit mono a 16kHz.
     * Vosk acepta arrays de short en little-endian.
     */
    @Synchronized
    fun procesarChunk(audio: ShortArray, length: Int) {
        if (palabrasClave.isEmpty()) return

        try {
            val esFinal = recognizer.acceptWaveForm(audio, length)
            val resultado = if (esFinal) recognizer.result else recognizer.partialResult
            extraerYBuscarPalabras(resultado)
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando chunk de audio: ${e.message}")
        }
    }

    private fun extraerYBuscarPalabras(resultadoJson: String) {
        val texto = try {
            val obj = JSONObject(resultadoJson)
            // Vosk devuelve {"text": "..."} para final o {"partial": "..."} para parcial
            obj.optString("text").takeIf { it.isNotEmpty() }
                ?: obj.optString("partial")
        } catch (e: Exception) {
            return
        }

        if (texto.isNullOrBlank()) return

        val textoNormalizado = normalizar(texto)
        val ahora = System.currentTimeMillis()

        // Limpiar detecciones viejas (>10s)
        ultimasDetecciones.removeAll { ahora - it.timestampMs > 10_000 }

        palabrasNormalizadas.forEach { palabra ->
            if (textoNormalizado.contains(palabra)) {
                // Evitar duplicados muy recientes (mismo término en <2s)
                val recienteMismaPalabra = ultimasDetecciones.any {
                    it.palabra == palabra && ahora - it.timestampMs < 2_000
                }
                if (!recienteMismaPalabra) {
                    ultimasDetecciones.add(DeteccionPalabra(palabra, ahora))
                    Log.w(TAG, "★ PALABRA CLAVE DETECTADA: '$palabra' (en texto: '$texto')")
                }
            }
        }
    }

    /**
     * Devuelve las palabras detectadas en los últimos 10 segundos.
     * Lo lee AudioMonitor para incluir en su snapshot.
     */
    fun obtenerPalabrasRecientes(): List<String> {
        val ahora = System.currentTimeMillis()
        return ultimasDetecciones
            .filter { ahora - it.timestampMs <= 10_000 }
            .map { it.palabra }
            .distinct()
    }

    /** Quita acentos y baja a minúsculas para matching tolerante. */
    private fun normalizar(s: String): String {
        val sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinAcentos.lowercase().trim()
    }

    @Synchronized
    fun cerrar() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error cerrando recognizer: ${e.message}")
        }
        ultimasDetecciones.clear()
    }

    private data class DeteccionPalabra(val palabra: String, val timestampMs: Long)

    companion object {
        private const val TAG = "KeywordSpotter"
    }
}
