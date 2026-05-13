package uk.legaxia.cercadeti.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.service.GuardianService
import uk.legaxia.cercadeti.storage.ContactsRepo
import uk.legaxia.cercadeti.storage.SettingsRepo
import uk.legaxia.cercadeti.stt.VoskManager

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepo
    private lateinit var contactos: ContactsRepo
    private lateinit var voskManager: VoskManager

    private val solicitarPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todosOtorgados = resultados.values.all { it }
        if (todosOtorgados) {
            arrancarServicio()
        } else {
            mostrarMensajePermisos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsRepo(this)
        contactos = ContactsRepo(this)
        voskManager = VoskManager(this)

        if (!settings.consentimientoOtorgado) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        val btnActivar = findViewById<Button>(R.id.btnActivar)
        val btnContactos = findViewById<Button>(R.id.btnContactos)
        val btnHistorial = findViewById<Button>(R.id.btnHistorial)
        val btnPermisos = findViewById<Button>(R.id.btnPermisos)
        val btnModeloVoz = findViewById<Button>(R.id.btnModeloVoz)

        actualizarEstado(tvEstado, btnActivar)

        btnActivar.setOnClickListener {
            if (settings.servicioActivado) {
                detenerServicio()
            } else {
                solicitarPermisosYArrancar()
            }
            actualizarEstado(tvEstado, btnActivar)
        }

        btnContactos.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnPermisos.setOnClickListener {
            startActivity(Intent(this, PermisosActivity::class.java))
        }

        btnModeloVoz.setOnClickListener {
            startActivity(Intent(this, DescargaModeloActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        val btnActivar = findViewById<Button>(R.id.btnActivar)
        actualizarEstado(tvEstado, btnActivar)
    }

    private fun actualizarEstado(tvEstado: TextView, btn: Button) {
        val baseTexto = if (settings.servicioActivado) {
            getString(R.string.estado_activo)
        } else {
            getString(R.string.estado_inactivo)
        }
        val infoModelo = if (voskManager.modeloListoEnDisco()) {
            "\n✓ Modelo de voz listo (${settings.palabrasClave.size} palabras clave)"
        } else {
            "\n⚠ Modelo de voz no descargado — toca \"Modelo de voz\""
        }
        tvEstado.text = baseTexto + infoModelo
        btn.text = if (settings.servicioActivado) getString(R.string.boton_desactivar)
                   else getString(R.string.boton_activar)
    }

    private fun solicitarPermisosYArrancar() {
        if (contactos.obtenerContactos().isEmpty()) {
            startActivity(Intent(this, ContactsActivity::class.java))
            return
        }

        val permisos = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val faltantes = permisos.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltantes.isEmpty()) {
            arrancarServicio()
        } else {
            solicitarPermisos.launch(faltantes.toTypedArray())
        }
    }

    private fun arrancarServicio() {
        val intent = Intent(this, GuardianService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        settings.servicioActivado = true
    }

    private fun detenerServicio() {
        val intent = Intent(this, GuardianService::class.java).apply {
            action = GuardianService.ACTION_DETENER
        }
        startService(intent)
        settings.servicioActivado = false
    }

    private fun mostrarMensajePermisos() {
        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        tvEstado.text = getString(R.string.permisos_requeridos)
    }
}
