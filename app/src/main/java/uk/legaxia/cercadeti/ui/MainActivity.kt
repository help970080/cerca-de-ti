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

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepo
    private lateinit var contactos: ContactsRepo

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

        // Si no completó onboarding, redirigir
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
    }

    private fun actualizarEstado(tvEstado: TextView, btn: Button) {
        if (settings.servicioActivado) {
            tvEstado.text = getString(R.string.estado_activo)
            btn.text = getString(R.string.boton_desactivar)
        } else {
            tvEstado.text = getString(R.string.estado_inactivo)
            btn.text = getString(R.string.boton_activar)
        }
    }

    private fun solicitarPermisosYArrancar() {
        if (contactos.obtenerContactos().isEmpty()) {
            // No se puede activar sin al menos un contacto
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
