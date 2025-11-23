package com.example.gastosapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class PresupuestoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _presupuestos = MutableLiveData<List<Presupuesto>>(emptyList())
    val presupuestos: LiveData<List<Presupuesto>> = _presupuestos

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    init {
        if (FirebaseUtils.isLoggedIn()) {
            escucharPresupuestos()
        }
    }

    private fun escucharPresupuestos() {
        db.collection("presupuestos")
            .document(uid)
            .collection("activos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val lista = snapshot?.toObjects(Presupuesto::class.java) ?: emptyList()
                _presupuestos.value = lista.filter { !esExpirado(it) }
            }
    }

    fun agregarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            val ref = db.collection("presupuestos")
                .document(uid)
                .collection("activos")
                .document()
            ref.set(presupuesto.copy(id = ref.id)).await()
        }
    }

    fun editarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            db.collection("presupuestos")
                .document(uid)
                .collection("activos")
                .document(presupuesto.id)
                .set(presupuesto)
                .await()
        }
    }

    fun eliminarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            db.collection("presupuestos")
                .document(uid)
                .collection("activos")
                .document(presupuesto.id)
                .delete()
                .await()
        }
    }

    private fun esExpirado(p: Presupuesto): Boolean {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fechaFinal = p.fechaFinal
        return fechaFinal.before(hoy.time)
    }
}