package uk.legaxia.cercadeti.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.legaxia.cercadeti.R
import uk.legaxia.cercadeti.alert.RelayClient
import uk.legaxia.cercadeti.storage.ContactsRepo

class ContactsActivity : AppCompatActivity() {

    private lateinit var repo: ContactsRepo
    private lateinit var adapter: ContactosAdapter

    private val lista = mutableListOf<RelayClient.Contacto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        repo = ContactsRepo(this)
        lista.addAll(repo.obtenerContactos())

        val rv = findViewById<RecyclerView>(R.id.rvContactos)
        adapter = ContactosAdapter(lista) { posicion ->
            confirmarBorrar(posicion)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAgregarContacto).setOnClickListener {
            if (lista.size >= 5) {
                Toast.makeText(this, R.string.contactos_max_alcanzado, Toast.LENGTH_LONG).show()
            } else {
                dialogoAgregar()
            }
        }

        findViewById<Button>(R.id.btnGuardarContactos).setOnClickListener {
            if (lista.isEmpty()) {
                Toast.makeText(this, R.string.contactos_min_uno, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            repo.guardarContactos(lista)
            Toast.makeText(this, R.string.contactos_guardados, Toast.LENGTH_SHORT).show()

            // Si vino desde onboarding, regresar a Main
            if (intent.getBooleanExtra(EXTRA_DESDE_ONBOARDING, false)) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            finish()
        }
    }

    private fun dialogoAgregar() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_contacto, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etTel = view.findViewById<EditText>(R.id.etTelefono)

        AlertDialog.Builder(this)
            .setTitle(R.string.contactos_agregar_titulo)
            .setView(view)
            .setPositiveButton(R.string.agregar) { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val tel = etTel.text.toString().trim()
                if (nombre.isEmpty() || tel.length < 10) {
                    Toast.makeText(this, R.string.contactos_invalido, Toast.LENGTH_SHORT).show()
                } else {
                    lista.add(RelayClient.Contacto(nombre, tel))
                    adapter.notifyItemInserted(lista.size - 1)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun confirmarBorrar(posicion: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.contactos_borrar_titulo)
            .setMessage(getString(R.string.contactos_borrar_msg, lista[posicion].nombre))
            .setPositiveButton(R.string.borrar) { _, _ ->
                lista.removeAt(posicion)
                adapter.notifyItemRemoved(posicion)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private class ContactosAdapter(
        private val items: List<RelayClient.Contacto>,
        private val onBorrar: (Int) -> Unit
    ) : RecyclerView.Adapter<ContactosAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombre)
            val tvTel: TextView = v.findViewById(R.id.tvTel)
            val btnBorrar: Button = v.findViewById(R.id.btnBorrar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contacto, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.tvNombre.text = c.nombre
            holder.tvTel.text = c.telefono
            holder.btnBorrar.setOnClickListener { onBorrar(holder.bindingAdapterPosition) }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        const val EXTRA_DESDE_ONBOARDING = "desde_onboarding"
    }
}
