package com.example.gastosapp.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GastoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _gastos = MutableLiveData<List<Gasto>>(emptyList())
    val gastos: LiveData<List<Gasto>> = _gastos

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    init {
        escucharGastos()
    }

    private fun escucharGastos() {
        db.collection("gastos").document(uid).collection("mis_gastos")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val lista = snapshot?.toObjects(Gasto::class.java) ?: emptyList()
                _gastos.value = lista
            }
    }

    fun agregarGasto(gasto: Gasto) {
        viewModelScope.launch {
            try {
                val ref = db.collection("gastos").document(uid).collection("mis_gastos").document()
                val gastoConId = gasto.copy(id = ref.id)
                ref.set(gastoConId).await()
                actualizarPresupuesto(gasto.categoriaNombre, gasto.monto, true)
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al agregar gasto: ", e)

            }
        }
    }

    fun editarGasto(gastoEditado: Gasto, gastoOriginal: Gasto) {
        viewModelScope.launch {
            try {
                val ref = db.collection("gastos").document(uid).collection("mis_gastos").document(gastoEditado.id)
                ref.set(gastoEditado).await()

                // Revertir gasto original
                actualizarPresupuesto(gastoOriginal.categoriaNombre, gastoOriginal.monto, false)
                // Aplicar nuevo
                actualizarPresupuesto(gastoEditado.categoriaNombre, gastoEditado.monto, true)
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al editar gasto: ", e)

            }
        }
    }

    fun eliminarGasto(gasto: Gasto) {
        viewModelScope.launch {
            try {
                db.collection("gastos").document(uid).collection("mis_gastos").document(gasto.id).delete().await()
                actualizarPresupuesto(gasto.categoriaNombre, gasto.monto, false)
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al eliminar gasto: ", e)
            }
        }
    }

    private suspend fun actualizarPresupuesto(categoriaNombre: String, monto: Double, esGasto: Boolean) {
        try {
            val snapshot = db.collection("presupuestos")
                .document(uid)
                .collection("activos")
                .whereEqualTo("categoriaNombre", categoriaNombre)
                .get()
                .await()

            if (snapshot.isEmpty) return

            val doc = snapshot.documents.first()
            db.runTransaction { transaction ->
                val presupuesto = transaction.get(doc.reference).toObject(Presupuesto::class.java)!!
                val nuevoGastado = if (esGasto) {
                    presupuesto.montoGastado + monto
                } else {
                    presupuesto.montoGastado - monto
                }.coerceAtLeast(0.0)
                transaction.update(doc.reference, "montoGastado", nuevoGastado)
            }.await()
        } catch (e: Exception) {
            Log.e("GastoVM", "Error actualizando presupuesto", e)
        }
    }
}