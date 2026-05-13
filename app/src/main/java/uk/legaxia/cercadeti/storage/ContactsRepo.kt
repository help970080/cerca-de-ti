package uk.legaxia.cercadeti.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import uk.legaxia.cercadeti.alert.RelayClient

/**
 * Repositorio de contactos de confianza.
 *
 * Los contactos se almacenan localmente en SharedPreferences como JSON.
 * Mínimo 1, máximo 5 contactos.
 *
 * Los contactos NUNCA se suben a un servidor central. Solo se envían
 * al backend relay en el momento de disparar una alerta, para que
 * el relay los notifique.
 */
class ContactsRepo(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun obtenerContactos(): List<RelayClient.Contacto> {
        val json = prefs.getString(KEY_CONTACTOS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                RelayClient.Contacto(
                    nombre = obj.getString("nombre"),
                    telefono = obj.getString("telefono")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardarContactos(contactos: List<RelayClient.Contacto>) {
        require(contactos.size in 1..5) { "Mínimo 1, máximo 5 contactos" }
        val arr = JSONArray()
        contactos.forEach { c ->
            val obj = JSONObject().apply {
                put("nombre", c.nombre)
                put("telefono", normalizarTelefono(c.telefono))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CONTACTOS, arr.toString()).apply()
    }

    fun nombreUsuario(): String =
        prefs.getString(KEY_NOMBRE_USUARIO, "Una persona cercana") ?: "Una persona cercana"

    fun setNombreUsuario(nombre: String) {
        prefs.edit().putString(KEY_NOMBRE_USUARIO, nombre).apply()
    }

    fun mensajePersonalizado(): String =
        prefs.getString(KEY_MENSAJE, "") ?: ""

    fun setMensajePersonalizado(mensaje: String) {
        prefs.edit().putString(KEY_MENSAJE, mensaje).apply()
    }

    /**
     * Normaliza números mexicanos a formato internacional 521XXXXXXXXXX para WhatsApp.
     * SMS se envía al número tal cual.
     */
    private fun normalizarTelefono(raw: String): String {
        val limpio = raw.filter { it.isDigit() }
        return when {
            limpio.startsWith("521") && limpio.length == 13 -> limpio
            limpio.startsWith("52") && limpio.length == 12 -> "521${limpio.drop(2)}"
            limpio.length == 10 -> "521$limpio"
            else -> limpio
        }
    }

    companion object {
        private const val PREFS = "cdt_contactos"
        private const val KEY_CONTACTOS = "contactos"
        private const val KEY_NOMBRE_USUARIO = "nombre_usuario"
        private const val KEY_MENSAJE = "mensaje"
    }
}
