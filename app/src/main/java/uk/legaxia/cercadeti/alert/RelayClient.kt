package uk.legaxia.cercadeti.alert

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
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
 * Estrategia de envío en paralelo (multi-canal):
 * 1. POST al endpoint /alerta del relay (enrutará a SMS + WhatsApp + Push de forma centralizada)
 * 2. SMS directo desde el celular como fallback (por si el relay no responde o no hay datos)
 *
 * El relay NUNCA recibe el audio crudo. Solo recibe metadata mínima y la lista
 * de contactos a notificar, además del token de acceso a la evidencia local
 * (que solo abre desde el celular del usuario o vía link firmado).
 */
class RelayClient(private val context: Context) {

    private val contactsRepo = ContactsRepo(context)

    fun enviarAlerta(paquete: PaqueteEvidencia) {
        val contactos = contactsRepo.obtenerContactos()
        if (contactos.isEmpty()) {
            Log.e(TAG, "Sin contactos configurados; no se puede enviar alerta")
            return
        }

        // Envío vía relay (asíncrono)
        CoroutineScope(Dispatchers.IO).launch {
            enviarViaRelay(paquete, contactos)
        }

        // Envío SMS directo como respaldo (síncrono, garantizado)
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
                put("mensaje_usuario", contactsRepo.mensajePersonalizado())
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            Log.i(TAG, "Relay respondió $code")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando al relay (no fatal, hay fallback SMS)", e)
        }
    }

    private fun enviarSmsDirecto(paquete: PaqueteEvidencia, contactos: List<Contacto>) {
        val mensaje = construirMensajeSms(paquete)
        try {
            val sm = SmsManager.getDefault()
            contactos.forEach { contacto ->
                try {
                    val partes = sm.divideMessage(mensaje)
                    sm.sendMultipartTextMessage(
                        contacto.telefono, null, partes, null, null
                    )
                    Log.i(TAG, "SMS enviado a ${contacto.nombre}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando SMS a ${contacto.telefono}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo SmsManager", e)
        }
    }

    private fun construirMensajeSms(paquete: PaqueteEvidencia): String {
        val ubic = if (paquete.latitud != null && paquete.longitud != null) {
            "https://maps.google.com/?q=${paquete.latitud},${paquete.longitud}"
        } else "(sin GPS)"

        val nombre = contactsRepo.nombreUsuario()
        return "CERCA DE TI: $nombre puede estar en peligro. Ubicacion: $ubic"
    }

    data class Contacto(val nombre: String, val telefono: String)

    companion object {
        private const val TAG = "RelayClient"
        // Se cambia a producción al desplegar; por ahora apunta a desarrollo
        private const val RELAY_URL = "https://api.cerca.legaxia.uk/alerta"
    }
}

/**
 * Token compartido con el backend para auth básica.
 * En producción se reemplaza por un token por-instalación generado en onboarding.
 */
object BuildTimeToken {
    fun token(): String = "cdt_v0_compartido_temporal"
}
