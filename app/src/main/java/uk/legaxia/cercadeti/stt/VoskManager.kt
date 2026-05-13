package uk.legaxia.cercadeti.stt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Gestor del modelo de Vosk STT.
 *
 * Se encarga de:
 * 1. Descargar el modelo desde GitHub Releases si no existe localmente
 * 2. Descomprimir el zip a /data/data/<app>/files/vosk-model/
 * 3. Cargar el modelo en memoria y exponerlo al KeywordSpotter
 *
 * Modelo: vosk-model-small-es-0.42 (~39 MB, español)
 * Fuente: alphacephei.com (espejo en GitHub Releases del propio repo)
 */
class VoskManager(private val context: Context) {

    @Volatile private var modelo: Model? = null

    /** True si el modelo ya está descargado y descomprimido en disco. */
    fun modeloListoEnDisco(): Boolean {
        val configFile = File(directorioModelo(), "conf/model.conf")
        return configFile.exists()
    }

    /** Devuelve el modelo cargado (puede ser null si aún no se cargó). */
    fun modelo(): Model? = modelo

    /**
     * Descarga el modelo (si no existe), lo descomprime y lo carga.
     * Reporta progreso vía callback.
     *
     * @return true si todo salió bien, false si hubo error
     */
    suspend fun descargarYCargar(progreso: (estado: String, pct: Int) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!modeloListoEnDisco()) {
                    progreso("Descargando modelo de voz...", 0)
                    val zip = descargarZip(progreso)
                    progreso("Descomprimiendo...", 90)
                    descomprimir(zip)
                    zip.delete()
                }

                progreso("Cargando modelo en memoria...", 95)
                val modeloPath = directorioModelo().absolutePath
                modelo = Model(modeloPath)
                progreso("Listo", 100)
                Log.i(TAG, "Modelo Vosk cargado desde $modeloPath")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error preparando modelo Vosk", e)
                progreso("Error: ${e.message}", -1)
                false
            }
        }

    private fun directorioModelo(): File =
        File(context.filesDir, "vosk-model")

    private suspend fun descargarZip(progreso: (String, Int) -> Unit): File {
        val destino = File(context.cacheDir, "vosk-model.zip")
        if (destino.exists()) destino.delete()

        val url = URL(URL_MODELO)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true

        try {
            conn.connect()
            val total = conn.contentLength.takeIf { it > 0 } ?: TAMANO_ESTIMADO

            conn.inputStream.use { input ->
                FileOutputStream(destino).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var leidoTotal = 0L
                    var ultimoReporte = 0

                    while (true) {
                        val leido = input.read(buffer)
                        if (leido <= 0) break
                        output.write(buffer, 0, leido)
                        leidoTotal += leido

                        val pct = (leidoTotal * 85 / total).toInt().coerceIn(0, 85)
                        if (pct - ultimoReporte >= 2) {
                            ultimoReporte = pct
                            val mbDescargado = leidoTotal / 1_048_576
                            val mbTotal = total / 1_048_576
                            progreso("Descargando modelo: ${mbDescargado}/${mbTotal} MB", pct)
                        }
                    }
                }
            }
            Log.i(TAG, "Descarga completada: ${destino.length() / 1_048_576} MB")
            return destino
        } finally {
            conn.disconnect()
        }
    }

    private fun descomprimir(zip: File) {
        val destino = directorioModelo()
        if (destino.exists()) destino.deleteRecursively()
        destino.mkdirs()

        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // El zip de vosk típicamente trae "vosk-model-small-es-0.42/conf/..."
                // Queremos guardarlo plano en destino/conf/...
                val nombreSimple = entry.name.substringAfter('/', entry.name)
                if (nombreSimple.isEmpty()) {
                    entry = zis.nextEntry
                    continue
                }
                val outFile = File(destino, nombreSimple)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
        Log.i(TAG, "Modelo descomprimido en ${destino.absolutePath}")
    }

    /** Libera recursos cuando se detiene el servicio. */
    fun cerrar() {
        try {
            modelo?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error cerrando modelo: ${e.message}")
        }
        modelo = null
    }

    companion object {
        private const val TAG = "VoskManager"

        // Modelo hospedado en el GitHub Release del propio repo (gratis, ilimitado)
        // Si el release no existe todavía, fallback al espejo oficial de alphacephei.
        private const val URL_MODELO =
            "https://github.com/help970080/cerca-de-ti/releases/download/v0.2.0-model/vosk-model-small-es-0.42.zip"

        private const val TAMANO_ESTIMADO = 41_000_000  // ~39 MB
    }
}
