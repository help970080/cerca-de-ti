package uk.legaxia.cercadeti.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Pantalla de diagnóstico de permisos.
 *
 * Muestra el estado de cada permiso crítico y permite al usuario otorgar
 * los faltantes uno por uno. Si Android ya marcó algún permiso como
 * "no volver a preguntar", da un botón para abrir Ajustes directamente.
 */
class PermisosActivity : AppCompatActivity() {

    private val solicitarPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        renderPermisos()
    }

    private val permisos = mutableListOf<PermisoInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.list_content)

        construirListaPermisos()
        renderPermisos()
    }

    override fun onResume() {
        super.onResume()
        renderPermisos()
    }

    private fun construirListaPermisos() {
        permisos.clear()
        permisos.add(PermisoInfo(
            permiso = Manifest.permission.RECORD_AUDIO,
            nombre = "Micrófono (RECORD_AUDIO)",
            descripcion = "Para detectar voz alterada y palabras clave"
        ))
        permisos.add(PermisoInfo(
            permiso = Manifest.permission.ACCESS_FINE_LOCATION,
            nombre = "Ubicación precisa (ACCESS_FINE_LOCATION)",
            descripcion = "Para enviar tu posición a tus contactos al disparar alerta"
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permisos.add(PermisoInfo(
                permiso = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                nombre = "Ubicación en segundo plano",
                descripcion = "Para que la detección funcione con la app cerrada"
            ))
        }
        permisos.add(PermisoInfo(
            permiso = Manifest.permission.SEND_SMS,
            nombre = "Enviar SMS (SEND_SMS)",
            descripcion = "Para avisar a tus contactos aunque no haya internet"
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(PermisoInfo(
                permiso = Manifest.permission.POST_NOTIFICATIONS,
                nombre = "Notificaciones (POST_NOTIFICATIONS)",
                descripcion = "Para mostrar el estado del servicio y alertas"
            ))
        }
    }

    private fun renderPermisos() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titulo = TextView(this).apply {
            text = "Diagnóstico de permisos"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        }
        root.addView(titulo)

        val subtitulo = TextView(this).apply {
            text = "Cerca de Ti necesita estos permisos para funcionar. Toca cada uno para otorgarlo."
            textSize = 14f
            setPadding(0, 0, 0, 32)
        }
        root.addView(subtitulo)

        permisos.forEach { p ->
            val granted = ContextCompat.checkSelfPermission(this, p.permiso) ==
                    PackageManager.PERMISSION_GRANTED

            val bloque = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(if (granted) 0xFFE8F5E9.toInt() else 0xFFFFEBEE.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }

            val nombre = TextView(this).apply {
                text = "${if (granted) "✓" else "✗"} ${p.nombre}"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            bloque.addView(nombre)

            val desc = TextView(this).apply {
                text = p.descripcion
                textSize = 13f
                setPadding(0, 4, 0, 8)
            }
            bloque.addView(desc)

            if (!granted) {
                val boton = Button(this).apply {
                    text = "Otorgar permiso"
                    setOnClickListener {
                        val deniedPermanently = !shouldShowRequestPermissionRationale(p.permiso)
                        if (deniedPermanently &&
                            ContextCompat.checkSelfPermission(this@PermisosActivity, p.permiso) !=
                            PackageManager.PERMISSION_GRANTED) {
                            // El usuario marcó "no preguntar de nuevo" o el sistema lo ocultó
                            abrirAjustesApp()
                        } else {
                            solicitarPermiso.launch(p.permiso)
                        }
                    }
                }
                bloque.addView(boton)
            }

            root.addView(bloque)
        }

        // Botón para abrir Ajustes directamente
        val btnAjustes = Button(this).apply {
            text = "Abrir ajustes del sistema"
            setOnClickListener { abrirAjustesApp() }
        }
        root.addView(btnAjustes)

        val btnVolver = Button(this).apply {
            text = "Volver"
            setOnClickListener { finish() }
        }
        root.addView(btnVolver)

        setContentView(root)
    }

    private fun abrirAjustesApp() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private data class PermisoInfo(
        val permiso: String,
        val nombre: String,
        val descripcion: String
    )
}
