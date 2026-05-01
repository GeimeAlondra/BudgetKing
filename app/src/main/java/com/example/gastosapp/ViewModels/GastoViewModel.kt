package com.example.gastosapp.ViewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.toEntity
import com.example.gastosapp.Models.toGasto
import com.example.gastosapp.db.AppDatabase
import com.example.gastosapp.sync.SyncWorker
import com.example.gastosapp.Utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GastoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val localDb = AppDatabase.getInstance(application)
    private val gastoDao = localDb.gastoDao()

    // La fuente de verdad ahora es Room, la UI siempre lee de local
    val gastos: LiveData<List<Gasto>> = gastoDao.observarTodos()
        .map { entities -> entities.map { it.toGasto() } }
        .asLiveData()

    var onGastosActualizados: ((totalGastado: Double) -> Unit)? = null

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    init {
        if (FirebaseUtils.uid() != null) {
            escucharFirestore()
        }
    }

    // Firestore sigue escuchando en tiempo real cuando hay conexión y actualiza Room con los datos más recientes
    private fun escucharFirestore() {
        if (uid.isEmpty()) return

        db.collection("gastos").document(uid).collection("mis_gastos")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GastoVM", "Error escuchando Firestore (posiblemente offline)", error)
                    return@addSnapshotListener
                }
                viewModelScope.launch {
                    val lista = snapshot?.toObjects(Gasto::class.java) ?: return@launch
                    // Actualizar Room con lo que viene de Firestore
                    gastoDao.insertarTodos(lista.map { it.toEntity(pendienteSync = false) })
                    onGastosActualizados?.invoke(lista.sumOf { gasto -> gasto.monto })
                }
            }
    }

    fun agregarGasto(gasto: Gasto) {
        viewModelScope.launch {
            try {
                // Guardar en Room inmediatamente (visible sin internet)
                val entity = gasto.toEntity(pendienteSync = true)
                gastoDao.insertar(entity)

                // Intentar subir a Firestore
                try {
                    val ref = db.collection("gastos").document(uid)
                        .collection("mis_gastos").document(entity.id)
                    ref.set(gasto.copy(id = entity.id)).await()
                    gastoDao.marcarSincronizado(entity.id)
                    actualizarPresupuesto(gasto.categoriaNombre, gasto.monto, true)
                } catch (e: Exception) {
                    // Sin internet: queda pendienteSync = true, el Worker lo subirá después
                    Log.w("GastoVM", "Sin conexión, gasto guardado localmente: ${entity.id}")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al agregar gasto", e)
            }
        }
    }

    fun editarGasto(gastoEditado: Gasto, gastoOriginal: Gasto) {
        viewModelScope.launch {
            try {
                gastoDao.insertar(gastoEditado.toEntity(pendienteSync = true))

                try {
                    db.collection("gastos").document(uid).collection("mis_gastos")
                        .document(gastoEditado.id).set(gastoEditado).await()
                    gastoDao.marcarSincronizado(gastoEditado.id)
                    actualizarPresupuesto(gastoOriginal.categoriaNombre, gastoOriginal.monto, false)
                    actualizarPresupuesto(gastoEditado.categoriaNombre, gastoEditado.monto, true)
                } catch (e: Exception) {
                    Log.w("GastoVM", "Sin conexión, edición guardada localmente")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al editar gasto", e)
            }
        }
    }

    fun eliminarGasto(gasto: Gasto) {
        viewModelScope.launch {
            try {
                // Eliminar de Room inmediatamente
                gastoDao.eliminar(gasto.id)

                // Intentar eliminar de Firestore
                try {
                    db.collection("gastos").document(uid).collection("mis_gastos")
                        .document(gasto.id).delete().await()
                    actualizarPresupuesto(gasto.categoriaNombre, gasto.monto, false)
                } catch (e: Exception) {
                    Log.w("GastoVM", "Sin conexión al eliminar, se eliminó solo localmente")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("GastoVM", "Error al eliminar gasto", e)
            }
        }
    }

    // Llamado desde PresupuestoViewModel al hacer cascade delete
    fun eliminarGastosPorCategoria(categoria: String) {
        viewModelScope.launch {
            gastoDao.eliminarPorCategoria(categoria)
        }
    }

    private fun programarSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    private suspend fun actualizarPresupuesto(categoriaNombre: String, monto: Double, esGasto: Boolean) {
        try {
            val snapshot = db.collection("presupuestos")
                .document(uid).collection("activos")
                .whereEqualTo("categoriaNombre", categoriaNombre)
                .get().await()

            if (snapshot.isEmpty) return

            val doc = snapshot.documents.first()
            db.runTransaction { transaction ->
                val presupuesto = transaction.get(doc.reference)
                    .toObject(com.example.gastosapp.Models.Presupuesto::class.java)!!
                val nuevoGastado = if (esGasto) presupuesto.montoGastado + monto
                else presupuesto.montoGastado - monto
                transaction.update(doc.reference, "montoGastado", nuevoGastado.coerceAtLeast(0.0))
            }.await()
        } catch (e: Exception) {
            Log.w("GastoVM", "No se pudo actualizar presupuesto en Firestore (offline)", e)
        }
    }
}