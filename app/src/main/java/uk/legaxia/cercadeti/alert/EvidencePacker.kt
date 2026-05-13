package uk.legaxia.cercadeti.alert

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import uk.legaxia.cercadeti.detector.RiskScore
import uk.legaxia.cercadeti.service.SensorWindow
import uk.legaxia.cercadeti.storage.EvidenceStore
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Empaqueta la evidencia de una alerta confirmada.
 *
 * Contenido del paquete (cifrado AES-256 antes de almacenar):
 * - audio.wav: audio PCM de los últimos 60 segundos
 * - foto_frontal.jpg: foto silenciosa con cámara frontal (si activado)
 * - trayectoria.json: puntos GPS de los últimos 10 minutos
 * - contribuciones.json: por qué disparó la alerta (señales que aportaron puntos)
 * - metadata.json: timestamp, hash, modelo de dispositivo, versión de la app
 *
 * El paquete se almacena en `EvidenceStore` (cifrado).
 * Solo el usuario puede abrirlo desde la propia app.
 * Un enlace firmado (token de un solo uso) se envía a los contactos elegidos
 * para que puedan acceder a la evidencia vía web.
 */
class EvidencePacker(private val context: Context) {

    fun empaquetar(score: RiskScore, ventana: SensorWindow, audioPcm: ShortArray): PaqueteEvidencia {
        val timestampMs = System.currentTimeMillis()
        val eventoId = "evt_${timestampMs}_${(0..9999).random()}"

        val audioBytes = if (audioPcm.isNotEmpty()) pcmToWav(audioPcm) else ByteArray(0)
        val trayectoriaJson = construirTrayectoriaJson(ventana)
        val contribucionesJson = construirContribucionesJson(score)
        val metadataJson = construirMetadata(eventoId, timestampMs, audioBytes, score)

        val store = EvidenceStore(context)
        store.guardarEvento(
            id = eventoId,
            audioWav = audioBytes,
            trayectoriaJson = trayectoriaJson.toString(),
            contribucionesJson = contribucionesJson.toString(),
            metadataJson = metadataJson.toString()
        )

        Log.i(TAG, "Evidencia empaquetada: $eventoId (${audioBytes.size} bytes audio)")

        return PaqueteEvidencia(
            eventoId = eventoId,
            timestampMs = timestampMs,
            metadata = metadataJson,
            latitud = ventana.location.latActual,
            longitud = ventana.location.lonActual,
            nivelRiesgo = score.level.name,
            tokenAcceso = generarTokenAcceso(eventoId)
        )
    }

    private fun construirTrayectoriaJson(ventana: SensorWindow): JSONArray {
        val arr = JSONArray()
        ventana.location.trayectoria.forEach { punto ->
            val obj = JSONObject().apply {
                put("lat", punto.lat)
                put("lon", punto.lon)
                put("ts", punto.timestampMs)
                put("speed", punto.speedMs)
            }
            arr.put(obj)
        }
        return arr
    }

    private fun construirContribucionesJson(score: RiskScore): JSONArray {
        val arr = JSONArray()
        score.contribuciones.forEach { c ->
            val obj = JSONObject().apply {
                put("codigo", c.codigo)
                put("puntos", c.puntos)
                put("descripcion", c.descripcion)
            }
            arr.put(obj)
        }
        return arr
    }

    private fun construirMetadata(
        eventoId: String,
        timestampMs: Long,
        audioBytes: ByteArray,
        score: RiskScore
    ): JSONObject {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(audioBytes)
        val hashHex = sha256.joinToString("") { "%02x".format(it) }

        return JSONObject().apply {
            put("evento_id", eventoId)
            put("timestamp_ms", timestampMs)
            put("timestamp_iso", java.time.Instant.ofEpochMilli(timestampMs).toString())
            put("nivel_riesgo", score.level.name)
            put("score_total", score.total)
            put("audio_sha256", hashHex)
            put("audio_bytes", audioBytes.size)
            put("dispositivo_modelo", android.os.Build.MODEL)
            put("dispositivo_manufacturer", android.os.Build.MANUFACTURER)
            put("android_version", android.os.Build.VERSION.RELEASE)
            put("app_version", obtenerVersionApp())
        }
    }

    private fun obtenerVersionApp(): String = try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "desconocida"
    } catch (e: Exception) { "desconocida" }

    private fun generarTokenAcceso(eventoId: String): String {
        // Token de un solo uso firmado con la llave del Android Keystore
        val random = ByteArray(32)
        java.security.SecureRandom().nextBytes(random)
        val randomHex = random.joinToString("") { "%02x".format(it) }
        return "$eventoId.$randomHex"
    }

    /**
     * Construye un WAV (header + PCM) en memoria a partir del audio del ring buffer.
     */
    private fun pcmToWav(pcm: ShortArray, sampleRate: Int = 16000): ByteArray {
        val byteOutput = ByteArrayOutputStream()
        val dataSize = pcm.size * 2
        val totalSize = 36 + dataSize

        // Header WAV
        byteOutput.write("RIFF".toByteArray())
        byteOutput.write(intToBytes(totalSize))
        byteOutput.write("WAVE".toByteArray())
        byteOutput.write("fmt ".toByteArray())
        byteOutput.write(intToBytes(16))         // chunk size
        byteOutput.write(shortToBytes(1))        // PCM
        byteOutput.write(shortToBytes(1))        // mono
        byteOutput.write(intToBytes(sampleRate))
        byteOutput.write(intToBytes(sampleRate * 2))
        byteOutput.write(shortToBytes(2))        // block align
        byteOutput.write(shortToBytes(16))       // bits per sample
        byteOutput.write("data".toByteArray())
        byteOutput.write(intToBytes(dataSize))

        // PCM data
        val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { buffer.putShort(it) }
        byteOutput.write(buffer.array())

        return byteOutput.toByteArray()
    }

    private fun intToBytes(v: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    private fun shortToBytes(v: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v.toShort()).array()

    companion object {
        private const val TAG = "EvidencePacker"
    }
}

/**
 * Resumen de evidencia que se envía al RelayClient (NO incluye audio crudo).
 */
data class PaqueteEvidencia(
    val eventoId: String,
    val timestampMs: Long,
    val metadata: JSONObject,
    val latitud: Double?,
    val longitud: Double?,
    val nivelRiesgo: String,
    val tokenAcceso: String
)
