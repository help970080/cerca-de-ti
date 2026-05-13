package uk.legaxia.cercadeti.alert

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uk.legaxia.cercadeti.R

/**
 * Pantalla que aparece encima de cualquier otra (incluido el lock screen) cuando
 * el detector dispara una alerta de nivel HIGH o CRITICAL.
 *
 * Da al usuario una cuenta atrás cancelable. Si no se cancela, AlertManager
 * confirma la alerta y envía a contactos.
 */
class CountdownActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private lateinit var alertManager: AlertManager
    private var cancelado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarSobreLockScreen()
        setContentView(R.layout.activity_countdown)

        alertManager = AlertManager(this)

        val segundos = intent.getIntExtra(EXTRA_SEGUNDOS, 30)
        val nivel = intent.getStringExtra(EXTRA_NIVEL) ?: "HIGH"

        val tvCount = findViewById<TextView>(R.id.tvCountdown)
        val tvNivel = findViewById<TextView>(R.id.tvNivel)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)

        tvNivel.text = getString(R.string.countdown_nivel, nivel)

        timer = object : CountDownTimer(segundos * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                tvCount.text = ((millisUntilFinished / 1000) + 1).toString()
            }

            override fun onFinish() {
                if (cancelado) return
                alertManager.confirmarAlerta()
                finish()
            }
        }.start()

        btnCancelar.setOnClickListener {
            cancelado = true
            timer?.cancel()
            alertManager.cancelarAlerta()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun mostrarSobreLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // Bloquear botón atrás durante cuenta atrás
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No-op: usuario debe usar el botón explícito Cancelar
    }

    companion object {
        const val EXTRA_SEGUNDOS = "segundos"
        const val EXTRA_NIVEL = "nivel"
        const val EXTRA_TOTAL = "total"
    }
}
