// com.example.gastosapp.viewModels.PresupuestoViewModel
package com.example.gastosapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gastosapp.Models.Presupuesto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class PresupuestoViewModel : ViewModel() {

    private val _listaPresupuestos = MutableLiveData<List<Presupuesto>>(emptyList())
    val presupuestos: LiveData<List<Presupuesto>> get() = _listaPresupuestos

    private val auth = FirebaseAuth.getInstance()
    private var databaseRef: DatabaseReference? = null

    init {
        mostrarPresupuestoUsuario()
    }


    private fun mostrarPresupuestoUsuario() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        databaseRef = FirebaseDatabase.getInstance().reference.child("presupuestos").child(uid)

        databaseRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Presupuesto>()
                for (child in snapshot.children) {
                    val presupuesto = child.getValue(Presupuesto::class.java)
                    presupuesto?.id = child.key
                    presupuesto?.let {
                        // CALCULAR EL SALDO DISPONIBLE CON EL MONTO GASTADO
                        val saldoDisponible = it.cantidad - it.montoGastado
                        lista.add(it)
                    }
                }
                _listaPresupuestos.value = lista
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error Firebase: ${error.message}")
            }
        })
    }
    fun agregarPresupuesto(presupuesto: Presupuesto) {
        val uid = auth.currentUser?.uid ?: return
        val nuevoId = databaseRef?.push()?.key ?: return

        val presupuestoConId = presupuesto.apply { id = nuevoId }
        val nuevaLista = (_listaPresupuestos.value?.toMutableList() ?: mutableListOf()).apply {
            add(presupuestoConId)
        }
        _listaPresupuestos.value = nuevaLista

        FirebaseDatabase.getInstance()
            .reference.child("presupuestos").child(uid).child(nuevoId)
            .setValue(presupuestoConId)
            .addOnFailureListener {
                nuevaLista.remove(presupuestoConId)
                _listaPresupuestos.value = nuevaLista
            }
    }

    fun editarPresupuesto(presupuestoActualizado: Presupuesto, position: Int) {
        val uid = auth.currentUser?.uid ?: return
        val id = presupuestoActualizado.id ?: return

        val lista = _listaPresupuestos.value?.toMutableList() ?: return
        if (position !in lista.indices) return

        val original = lista[position].copy()
        lista[position] = presupuestoActualizado
        _listaPresupuestos.value = lista

        FirebaseDatabase.getInstance().reference.child("presupuestos").child(uid).child(id)
            .setValue(presupuestoActualizado)
            .addOnFailureListener {
                lista[position] = original
                _listaPresupuestos.value = lista
            }
    }

    fun eliminarPresupuesto(position: Int) {
        val lista = _listaPresupuestos.value?.toMutableList() ?: return
        if (position !in lista.indices) return

        val presupuesto = lista.removeAt(position)
        _listaPresupuestos.value = lista

        val uid = auth.currentUser?.uid ?: return
        presupuesto.id?.let { id ->
            FirebaseDatabase.getInstance()
                .reference.child("presupuestos").child(uid).child(id)
                .removeValue()
                .addOnFailureListener {
                    lista.add(position, presupuesto)
                    _listaPresupuestos.value = lista
                }
        }
    }

    fun getPositionById(id: String): Int {
        return _listaPresupuestos.value?.indexOfFirst { it.id == id } ?: -1
    }

    override fun onCleared() {
        //databaseRef?.removeEventListener { }
        super.onCleared()
    }

    fun getPresupuestoPorCategoria(categoriaId: Int): Presupuesto? {
        return _listaPresupuestos.value?.find { it.categoriaId == categoriaId }
    }

    private fun haExpirado(fechaFinal: String?): Boolean {
        if (fechaFinal.isNullOrEmpty()) return false

        return try {
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaLimite = formatoFecha.parse(fechaFinal)
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            fechaLimite?.before(hoy) ?: false

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Obtiene el estado del presupuesto (Activo/Expirado)
     */
    fun obtenerEstadoPresupuesto(presupuesto: Presupuesto): String {
        return if (haExpirado(presupuesto.fechaFinal)) {
            "Expirado"
        } else {
            "  Activo"
        }
    }

    /**
     * Elimina automáticamente los presupuestos expirados
     */
    fun limpiarPresupuestosExpirados() {
        val uid = auth.currentUser?.uid ?: return
        val listaActual = _listaPresupuestos.value ?: emptyList()

        val presupuestosExpirados = listaActual.filter { haExpirado(it.fechaFinal) }
        val idsExpirados = presupuestosExpirados.mapNotNull { it.id }

        if (idsExpirados.isNotEmpty()) {
            // Eliminar de Firebase
            idsExpirados.forEach { id ->
                FirebaseDatabase.getInstance()
                    .reference.child("presupuestos").child(uid).child(id)
                    .removeValue()
            }

            // Actualizar lista local
            val nuevaLista = listaActual.filterNot { haExpirado(it.fechaFinal) }
            _listaPresupuestos.value = nuevaLista
        }
    }

}