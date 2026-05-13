package uk.legaxia.cercadeti.storage

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Almacenamiento cifrado de evidencia de eventos.
 *
 * Usa AndroidX Security `EncryptedFile` que envuelve los archivos con AES-256-GCM,
 * con llave maestra protegida por hardware (StrongBox/TEE) cuando el dispositivo lo soporta.
 *
 * Estructura en disco:
 *   filesDir/evidencia/
 *     evt_<timestamp>_<rand>/
 *       audio.wav.enc
 *       trayectoria.json.enc
 *       contribuciones.json.enc
 *       metadata.json.enc
 *
 * El backup automático del dispositivo está deshabilitado en el manifest
 * (`allowBackup="false"`) para que las evidencias no se sincronicen con Google Drive.
 */
class EvidenceStore(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val rootDir: File by lazy {
        File(context.filesDir, "evidencia").apply { mkdirs() }
    }

    fun guardarEvento(
        id: String,
        audioWav: ByteArray,
        trayectoriaJson: String,
        contribucionesJson: String,
        metadataJson: String
    ) {
        val dir = File(rootDir, id).apply { mkdirs() }

        try {
            escribirCifrado(File(dir, "audio.wav.enc"), audioWav)
            escribirCifrado(File(dir, "trayectoria.json.enc"), trayectoriaJson.toByteArray())
            escribirCifrado(File(dir, "contribuciones.json.enc"), contribucionesJson.toByteArray())
            escribirCifrado(File(dir, "metadata.json.enc"), metadataJson.toByteArray())
            Log.i(TAG, "Evento $id guardado en $dir")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando evento $id", e)
        }
    }

    fun leerEvento(id: String): EventoEvidencia? {
        val dir = File(rootDir, id)
        if (!dir.exists()) return null
        return try {
            EventoEvidencia(
                id = id,
                audioWav = leerCifrado(File(dir, "audio.wav.enc")),
                trayectoriaJson = leerCifrado(File(dir, "trayectoria.json.enc")).toString(Charsets.UTF_8),
                contribucionesJson = leerCifrado(File(dir, "contribuciones.json.enc")).toString(Charsets.UTF_8),
                metadataJson = leerCifrado(File(dir, "metadata.json.enc")).toString(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo evento $id", e)
            null
        }
    }

    fun listarEventos(): List<String> =
        rootDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

    fun borrarEvento(id: String): Boolean {
        val dir = File(rootDir, id)
        return dir.deleteRecursively()
    }

    fun borrarTodo() {
        rootDir.deleteRecursively()
        rootDir.mkdirs()
    }

    private fun escribirCifrado(archivo: File, bytes: ByteArray) {
        if (archivo.exists()) archivo.delete()  // EncryptedFile no permite reescribir
        val ef = EncryptedFile.Builder(
            context, archivo, masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        ef.openFileOutput().use { it.write(bytes) }
    }

    private fun leerCifrado(archivo: File): ByteArray {
        val ef = EncryptedFile.Builder(
            context, archivo, masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return ef.openFileInput().use { it.readBytes() }
    }

    data class EventoEvidencia(
        val id: String,
        val audioWav: ByteArray,
        val trayectoriaJson: String,
        val contribucionesJson: String,
        val metadataJson: String
    )

    companion object {
        private const val TAG = "EvidenceStore"
    }
}
