package com.example.gastosapp.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PresupuestoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _presupuestos = MutableLiveData<List<Presupuesto>>(emptyList())
    val presupuestos: LiveData<List<Presupuesto>> = _presupuestos

    // Callback que se llama cada vez que los presupuestos cambian
    var onPresupuestosActualizados: ((montoInicial: Double) -> Unit)? = null

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    init {
        if (FirebaseUtils.isLoggedIn()) {
            escucharPresupuestos()
        }
    }

    private fun escucharPresupuestos() {
        if (uid.isEmpty()) return

        db.collection("presupuestos").document(uid).collection("activos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PresupuestoVM", "Error escuchando presupuestos", error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Presupuesto::class.java)
                    ?.filter { !esExpirado(it) } ?: emptyList()
                _presupuestos.value = lista
                // Notificar con el monto inicial actualizado
                onPresupuestosActualizados?.invoke(lista.sumOf { it.cantidad })
            }
    }

    fun agregarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                val ref = db.collection("presupuestos").document(uid).collection("activos").document()
                ref.set(presupuesto.copy(id = ref.id)).await()
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al agregar presupuesto", e)
            }
        }
    }

    fun editarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                db.collection("presupuestos").document(uid).collection("activos")
                    .document(presupuesto.id).set(presupuesto).await()
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al editar presupuesto", e)
            }
        }
    }

    fun eliminarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                db.collection("presupuestos").document(uid).collection("activos")
                    .document(presupuesto.id).delete().await()
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al eliminar presupuesto", e)
            }
        }
    }

    private fun esExpirado(p: Presupuesto): Boolean {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return p.fechaFinal.before(hoy.time)
    }
}