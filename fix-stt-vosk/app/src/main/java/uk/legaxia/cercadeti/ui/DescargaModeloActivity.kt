package uk.legaxia.cercadeti.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.legaxia.cercadeti.stt.VoskManager

/**
 * Pantalla que descarga el modelo Vosk al primer arranque (o cuando el usuario
 * pide reintentarlo). Muestra progreso, permite reintentar si falla, y permite
 * saltar (en cuyo caso las palabras clave no funcionarán hasta que se descargue).
 */
class DescargaModeloActivity : AppCompatActivity() {

    private lateinit var voskManager: VoskManager
    private lateinit var tvEstado: TextView
    private lateinit var tvDetalle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnReintentar: Button
    private lateinit var btnSaltar: Button
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voskManager = VoskManager(this)

        construirUI()

        if (voskManager.modeloListoEnDisco()) {
            mostrarYaListo()
        } else {
            iniciarDescarga()
        }
    }

    private fun construirUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val titulo = TextView(this).apply {
            text = "Descarga del modelo de voz"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        }
        root.addView(titulo)

        val descripcion = TextView(this).apply {
            text = "Cerca de Ti necesita un modelo de reconocimiento de voz para detectar tus palabras de auxilio. Es un archivo de unos 39 MB que se descarga UNA SOLA VEZ. Después funcionará sin internet.\n\nMejor conéctate a WiFi antes de continuar."
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }
        root.addView(descripcion)

        tvEstado = TextView(this).apply {
            text = "Preparando..."
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        root.addView(tvEstado)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        root.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        tvDetalle = TextView(this).apply {
            text = ""
            textSize = 13f
            setPadding(0, 8, 0, 32)
        }
        root.addView(tvDetalle)

        btnReintentar = Button(this).apply {
            text = "Reintentar"
            visibility = View.GONE
            setOnClickListener { iniciarDescarga() }
        }
        root.addView(btnReintentar)

        btnSaltar = Button(this).apply {
            text = "Saltar (palabras clave no funcionarán)"
            visibility = View.GONE
            setOnClickListener { irAMain() }
        }
        root.addView(btnSaltar)

        btnContinuar = Button(this).apply {
            text = "Continuar"
            visibility = View.GONE
            setOnClickListener { irAMain() }
        }
        root.addView(btnContinuar)

        setContentView(root)
    }

    private fun iniciarDescarga() {
        btnReintentar.visibility = View.GONE
        btnSaltar.visibility = View.GONE
        btnContinuar.visibility = View.GONE
        progressBar.progress = 0

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                voskManager.descargarYCargar { estado, pct ->
                    runOnUiThread {
                        tvEstado.text = estado
                        if (pct >= 0) progressBar.progress = pct
                    }
                }
            }
            if (ok) {
                tvEstado.text = "✓ Modelo descargado y listo"
                tvDetalle.text = "Las palabras clave ya están activas."
                btnContinuar.visibility = View.VISIBLE
            } else {
                tvEstado.text = "✗ No se pudo descargar"
                tvDetalle.text = "Verifica tu conexión a internet e intenta de nuevo. Puedes saltar este paso pero las palabras clave no funcionarán hasta descargar el modelo."
                btnReintentar.visibility = View.VISIBLE
                btnSaltar.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarYaListo() {
        tvEstado.text = "✓ Modelo ya está descargado"
        tvDetalle.text = "Puedes continuar."
        progressBar.progress = 100
        btnContinuar.visibility = View.VISIBLE
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
