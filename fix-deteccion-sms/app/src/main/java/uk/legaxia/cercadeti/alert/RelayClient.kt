package uk.legaxia.cercadeti.alert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uk.legaxia.cercadeti.storage.ContactsRepo
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente del backend relay.
 *
 * Estrategia:
 * 1. POST al backend relay (si está configurado)
 * 2. SMS directo desde el celular del usuario como respaldo (siempre)
 *
 * El SMS es la línea de vida — funciona offline, funciona sin backend,
 * funciona sin internet. Por eso siempre se envía aunque el relay falle.
 */
class RelayClient(private val context: Context) {

    private val contactsRepo = ContactsRepo(context)

    fun enviarAlerta(paquete: PaqueteEvidencia) {
        val contactos = contactsRepo.obtenerContactos()
        if (contactos.isEmpty()) {
            Log.e(TAG, "Sin contactos configurados; no se puede enviar alerta")
            return
        }

        Log.i(TAG, "Enviando alerta a ${contactos.size} contactos: ${contactos.map { it.nombre }}")

        // Backend relay (asíncrono, no-bloqueante)
        CoroutineScope(Dispatchers.IO).launch {
            enviarViaRelay(paquete, contactos)
        }

        // SMS directo (siempre, garantizado)
        enviarSmsDirecto(paquete, contactos)
    }

    private fun enviarViaRelay(paquete: PaqueteEvidencia, contactos: List<Contacto>) {
        try {
            val url = URL(RELAY_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Cerca-Token", BuildTimeToken.token())
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
            }
            val payload = JSONObject().apply {
                put("evento_id", paquete.eventoId)
                put("timestamp_ms", paquete.timestampMs)
                put("nivel", paquete.nivelRiesgo)
                put("lat", paquete.latitud ?: JSONObject.NULL)
                put("lon", paquete.longitud ?: JSONObject.NULL)
                put("token_acceso", paquete.tokenAcceso)
                put("contactos", JSONArray(contactos.map { it.telefono }))
                put("mensaje_usuario", contactsRepo.nombreUsuario())
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            Log.i(TAG, "Relay respondió código: ${conn.responseCode}")
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Relay no disponible (no fatal, hay SMS fallback): ${e.message}")
        }
    }

    /**
     * Envía SMS directamente desde el celular del usuario a cada contacto.
     *
     * Maneja:
     * - Validación del permiso en runtime
     * - Normalización del número al formato que acepta SmsManager
     * - Envío multipart si el mensaje es largo
     * - Logs detallados para debug
     */
    private fun enviarSmsDirecto(paquete: PaqueteEvidencia, contactos: List<Contacto>) {
        // Validar permiso
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SIN PERMISO SEND_SMS — la alerta NO se enviará. Otorgue el permiso desde Diagnóstico.")
            return
        }

        val mensaje = construirMensajeSms(paquete)
        Log.i(TAG, "Mensaje SMS a enviar (${mensaje.length} chars): $mensaje")

        val sm = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener SmsManager: ${e.message}")
            return
        }

        contactos.forEach { contacto ->
            val telefonoSms = normalizarTelefonoParaSms(contacto.telefono)
            Log.i(TAG, "Enviando SMS a ${contacto.nombre} (${contacto.telefono} → $telefonoSms)")

            try {
                val partes = sm.divideMessage(mensaje)
                Log.d(TAG, "SMS dividido en ${partes.size} parte(s)")

                if (partes.size == 1) {
                    sm.sendTextMessage(telefonoSms, null, mensaje, null, null)
                } else {
                    sm.sendMultipartTextMessage(telefonoSms, null, partes, null, null)
                }
                Log.i(TAG, "✓ SMS enviado a ${contacto.nombre}")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException enviando SMS a $telefonoSms: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Número inválido $telefonoSms: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando SMS a $telefonoSms: ${e.message}", e)
            }
        }
    }

    /**
     * Normaliza el número para SmsManager.
     *
     * SmsManager en México acepta:
     *   - 10 dígitos directos: 5512345678  (el más confiable)
     *   - Con prefijo +52: +525512345678
     *
     * NO acepta el prefijo "521..." de WhatsApp (eso es solo para WhatsApp).
     * Si el contacto está guardado como "521XXXXXXXXXX" (formato WhatsApp),
     * lo convertimos a "+52XXXXXXXXXX" para SMS.
     */
    private fun normalizarTelefonoParaSms(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            // Formato WhatsApp México (521XXXXXXXXXX, 13 dígitos) → +52XXXXXXXXXX
            digits.startsWith("521") && digits.length == 13 -> "+52${digits.drop(3)}"
            // Formato internacional sin "1" intermedio (52XXXXXXXXXX, 12 dígitos)
            digits.startsWith("52") && digits.length == 12 -> "+$digits"
            // 10 dígitos directos (mejor caso para SMS local)
            digits.length == 10 -> digits
            // Cualquier otro caso: devolver lo que hay, dejando que SmsManager decida
            else -> digits
        }
    }

    private fun construirMensajeSms(paquete: PaqueteEvidencia): String {
        val ubic = if (paquete.latitud != null && paquete.longitud != null) {
            "https://maps.google.com/?q=${paquete.latitud},${paquete.longitud}"
        } else "(sin ubicacion GPS)"

        val nombre = contactsRepo.nombreUsuario()
        // Mensaje conciso, sin acentos para evitar problemas de codificación SMS
        return "CERCA DE TI: $nombre puede estar en peligro. Ubicacion: $ubic"
    }

    data class Contacto(val nombre: String, val telefono: String)

    companion object {
        private const val TAG = "RelayClient"
        // Se cambia a producción al desplegar
        private const val RELAY_URL = "https://api.cerca.legaxia.uk/alerta"
    }
}

object BuildTimeToken {
    fun token(): String = "cdt_v0_compartido_temporal"
}
