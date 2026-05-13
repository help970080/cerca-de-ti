package uk.legaxia.cercadeti.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Repositorio de configuración del usuario.
 *
 * Datos no sensibles van a SharedPreferences. Datos potencialmente sensibles
 * (palabras clave que podrían revelar contexto personal) van cifrados via
 * EncryptedSharedPreferences en una versión futura.
 */
class SettingsRepo(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Si el usuario completó el onboarding y consintió el monitoreo.
     */
    var consentimientoOtorgado: Boolean
        get() = prefs.getBoolean(KEY_CONSENTIMIENTO, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENTIMIENTO, value).apply()

    var servicioActivado: Boolean
        get() = prefs.getBoolean(KEY_SERVICIO_ACTIVO, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICIO_ACTIVO, value).apply()

    /**
     * Palabras clave personalizadas que disparan alerta directa.
     * El usuario las elige en onboarding; deben ser palabras o frases que
     * NO use en conversación cotidiana.
     */
    var palabrasClave: Set<String>
        get() = prefs.getStringSet(KEY_PALABRAS_CLAVE, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_PALABRAS_CLAVE, value).apply()

    /**
     * Si el usuario habilitó captura de foto frontal silenciosa al disparar alerta.
     */
    var capturarFotoFrontal: Boolean
        get() = prefs.getBoolean(KEY_FOTO_FRONTAL, false)
        set(value) = prefs.edit().putBoolean(KEY_FOTO_FRONTAL, value).apply()

    /**
     * Si el usuario habilitó SMS directo como fallback (consume saldo del usuario).
     */
    var smsDirectoActivo: Boolean
        get() = prefs.getBoolean(KEY_SMS_DIRECTO, true)
        set(value) = prefs.edit().putBoolean(KEY_SMS_DIRECTO, value).apply()

    /**
     * Si el usuario está actualmente en un trayecto esperado (vacaciones, viaje declarado).
     * Cuando es true, la señal "ubicación atípica" no contribuye al score.
     */
    fun estaEnTransitoEsperado(): Boolean = prefs.getBoolean(KEY_TRANSITO, false)

    companion object {
        private const val PREFS = "cdt_settings"
        private const val KEY_CONSENTIMIENTO = "consentimiento"
        private const val KEY_SERVICIO_ACTIVO = "servicio_activo"
        private const val KEY_PALABRAS_CLAVE = "palabras_clave"
        private const val KEY_FOTO_FRONTAL = "foto_frontal"
        private const val KEY_SMS_DIRECTO = "sms_directo"
        private const val KEY_TRANSITO = "transito"
    }
}
