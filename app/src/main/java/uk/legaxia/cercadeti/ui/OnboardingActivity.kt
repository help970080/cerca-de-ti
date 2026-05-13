package uk.legaxia.cercadeti.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.storage.ContactsRepo
import uk.legaxia.cercadeti.storage.SettingsRepo

/**
 * Onboarding paso a paso:
 *  1. Pantalla de bienvenida + explicación honesta de qué hace y qué NO hace
 *  2. Aceptación expresa de la política de privacidad
 *  3. Captura de nombre del usuario (aparece en el SMS de alerta)
 *  4. Captura de palabras clave personalizadas (opcional pero recomendado)
 *  5. Configuración de contactos (delegada a ContactsActivity)
 *
 * El consentimiento se marca otorgado SOLO al final del flujo.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepo
    private lateinit var contactos: ContactsRepo

    private var paso = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        settings = SettingsRepo(this)
        contactos = ContactsRepo(this)

        mostrarPaso(0)
    }

    private fun mostrarPaso(n: Int) {
        paso = n
        val tvTitulo = findViewById<TextView>(R.id.tvOnbTitulo)
        val tvTexto = findViewById<TextView>(R.id.tvOnbTexto)
        val etCampo = findViewById<EditText>(R.id.etOnbCampo)
        val chkConsent = findViewById<CheckBox>(R.id.chkConsent)
        val btnSig = findViewById<Button>(R.id.btnOnbSiguiente)

        etCampo.visibility = android.view.View.GONE
        chkConsent.visibility = android.view.View.GONE

        when (n) {
            0 -> {
                tvTitulo.text = getString(R.string.onb_bienvenida_titulo)
                tvTexto.text = getString(R.string.onb_bienvenida_texto)
                btnSig.text = getString(R.string.onb_btn_entendido)
                btnSig.setOnClickListener { mostrarPaso(1) }
            }
            1 -> {
                tvTitulo.text = getString(R.string.onb_privacidad_titulo)
                tvTexto.text = getString(R.string.onb_privacidad_texto)
                chkConsent.visibility = android.view.View.VISIBLE
                btnSig.text = getString(R.string.onb_btn_acepto)
                btnSig.setOnClickListener {
                    if (!chkConsent.isChecked) {
                        Toast.makeText(this, R.string.onb_debe_aceptar, Toast.LENGTH_LONG).show()
                    } else {
                        mostrarPaso(2)
                    }
                }
            }
            2 -> {
                tvTitulo.text = getString(R.string.onb_nombre_titulo)
                tvTexto.text = getString(R.string.onb_nombre_texto)
                etCampo.visibility = android.view.View.VISIBLE
                etCampo.hint = getString(R.string.onb_nombre_hint)
                etCampo.setText(contactos.nombreUsuario().takeIf { it != "Una persona cercana" } ?: "")
                btnSig.text = getString(R.string.onb_btn_siguiente)
                btnSig.setOnClickListener {
                    val nombre = etCampo.text.toString().trim()
                    if (nombre.isEmpty()) {
                        Toast.makeText(this, R.string.onb_nombre_vacio, Toast.LENGTH_SHORT).show()
                    } else {
                        contactos.setNombreUsuario(nombre)
                        mostrarPaso(3)
                    }
                }
            }
            3 -> {
                tvTitulo.text = getString(R.string.onb_palabras_titulo)
                tvTexto.text = getString(R.string.onb_palabras_texto)
                etCampo.visibility = android.view.View.VISIBLE
                etCampo.hint = getString(R.string.onb_palabras_hint)
                etCampo.setText(settings.palabrasClave.joinToString(", "))
                btnSig.text = getString(R.string.onb_btn_siguiente)
                btnSig.setOnClickListener {
                    val palabras = etCampo.text.toString()
                        .split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    settings.palabrasClave = palabras
                    mostrarPaso(4)
                }
            }
            4 -> {
                tvTitulo.text = getString(R.string.onb_contactos_titulo)
                tvTexto.text = getString(R.string.onb_contactos_texto)
                btnSig.text = getString(R.string.onb_btn_configurar_contactos)
                btnSig.setOnClickListener {
                    settings.consentimientoOtorgado = true
                    startActivity(Intent(this, ContactsActivity::class.java).apply {
                        putExtra(ContactsActivity.EXTRA_DESDE_ONBOARDING, true)
                    })
                    finish()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (paso > 0) mostrarPaso(paso - 1)
        else super.onBackPressed()
    }
}
