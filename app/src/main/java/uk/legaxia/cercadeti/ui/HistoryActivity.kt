package uk.legaxia.cercadeti.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.storage.EvidenceStore

class HistoryActivity : AppCompatActivity() {

    private lateinit var store: EvidenceStore
    private val eventos = mutableListOf<String>()
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        store = EvidenceStore(this)
        eventos.addAll(store.listarEventos().sortedDescending())

        val rv = findViewById<RecyclerView>(R.id.rvHistorial)
        val tvVacio = findViewById<TextView>(R.id.tvHistorialVacio)

        if (eventos.isEmpty()) {
            tvVacio.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvVacio.visibility = View.GONE
            rv.visibility = View.VISIBLE
            adapter = HistorialAdapter(eventos, ::mostrarDetalle, ::confirmarBorrar)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = adapter
        }
    }

    private fun mostrarDetalle(eventoId: String) {
        val evento = store.leerEvento(eventoId) ?: return
        val metadata = try { JSONObject(evento.metadataJson) } catch (e: Exception) { JSONObject() }
        val contribuciones = evento.contribucionesJson

        val msg = buildString {
            append("ID: $eventoId\n")
            append("Fecha: ${metadata.optString("timestamp_iso", "?")}\n")
            append("Nivel: ${metadata.optString("nivel_riesgo", "?")}\n")
            append("Score: ${metadata.optInt("score_total", 0)}\n")
            append("Audio: ${metadata.optInt("audio_bytes", 0)} bytes\n")
            append("Dispositivo: ${metadata.optString("dispositivo_modelo", "?")}\n\n")
            append("Razones:\n$contribuciones")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.historial_detalle_titulo)
            .setMessage(msg)
            .setPositiveButton(R.string.cerrar, null)
            .show()
    }

    private fun confirmarBorrar(eventoId: String, posicion: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.historial_borrar_titulo)
            .setMessage(R.string.historial_borrar_msg)
            .setPositiveButton(R.string.borrar) { _, _ ->
                if (store.borrarEvento(eventoId)) {
                    eventos.removeAt(posicion)
                    adapter.notifyItemRemoved(posicion)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private class HistorialAdapter(
        private val items: List<String>,
        private val onClick: (String) -> Unit,
        private val onBorrar: (String, Int) -> Unit
    ) : RecyclerView.Adapter<HistorialAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvId: TextView = v.findViewById(R.id.tvEventoId)
            val btnBorrar: Button = v.findViewById(R.id.btnBorrar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_historial, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val id = items[position]
            holder.tvId.text = id
            holder.itemView.setOnClickListener { onClick(id) }
            holder.btnBorrar.setOnClickListener { onBorrar(id, holder.bindingAdapterPosition) }
        }

        override fun getItemCount() = items.size
    }
}
