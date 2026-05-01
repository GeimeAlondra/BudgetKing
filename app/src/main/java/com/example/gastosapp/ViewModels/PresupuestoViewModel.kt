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
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.Models.toEntity
import com.example.gastosapp.Models.toPresupuesto
import com.example.gastosapp.db.AppDatabase
import com.example.gastosapp.sync.SyncWorker
import com.example.gastosapp.Utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PresupuestoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val localDb = AppDatabase.getInstance(application)
    private val presupuestoDao = localDb.presupuestoDao()

    // La UI siempre lee de Room
    val presupuestos: LiveData<List<Presupuesto>> = presupuestoDao.observarTodos()
        .map { entities ->
            entities
                .map { it.toPresupuesto() }
                .filter { presupuesto -> !esExpirado(presupuesto) }
        }
        .asLiveData()

    var onPresupuestosActualizados: ((montoInicial: Double) -> Unit)? = null

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    init {
        if (FirebaseUtils.isLoggedIn()) {
            escucharFirestore()
        }
    }

    private fun escucharFirestore() {
        if (uid.isEmpty()) return

        db.collection("presupuestos").document(uid).collection("activos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PresupuestoVM", "Error Firestore (posiblemente offline)", error)
                    return@addSnapshotListener
                }
                viewModelScope.launch {
                    val lista = snapshot?.toObjects(Presupuesto::class.java) ?: return@launch
                    presupuestoDao.insertarTodos(lista.map { it.toEntity(pendienteSync = false) })
                    val activos = lista.filter { presupuesto -> !esExpirado(presupuesto) }
                    onPresupuestosActualizados?.invoke(activos.sumOf { presupuesto -> presupuesto.cantidad })
                }
            }
    }

    fun agregarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                val entity = presupuesto.toEntity(pendienteSync = true)
                presupuestoDao.insertar(entity)

                try {
                    val ref = db.collection("presupuestos").document(uid)
                        .collection("activos").document(entity.id)
                    ref.set(presupuesto.copy(id = entity.id)).await()
                    presupuestoDao.marcarSincronizado(entity.id)
                } catch (e: Exception) {
                    Log.w("PresupuestoVM", "Sin conexión, presupuesto guardado localmente")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al agregar presupuesto", e)
            }
        }
    }

    fun editarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                presupuestoDao.insertar(presupuesto.toEntity(pendienteSync = true))

                try {
                    db.collection("presupuestos").document(uid).collection("activos")
                        .document(presupuesto.id).set(presupuesto).await()
                    presupuestoDao.marcarSincronizado(presupuesto.id)
                } catch (e: Exception) {
                    Log.w("PresupuestoVM", "Sin conexión, edición guardada localmente")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al editar presupuesto", e)
            }
        }
    }

    fun eliminarPresupuesto(presupuesto: Presupuesto) {
        viewModelScope.launch {
            try {
                // Eliminar gastos asociados de Room
                localDb.gastoDao().eliminarPorCategoria(presupuesto.categoriaNombre)

                // Eliminar presupuesto de Room
                presupuestoDao.eliminar(presupuesto.id)

                // Intentar eliminar en cascada en Firestore
                try {
                    val gastosSnapshot = db.collection("gastos")
                        .document(uid).collection("mis_gastos")
                        .whereEqualTo("categoriaNombre", presupuesto.categoriaNombre)
                        .get().await()

                    if (!gastosSnapshot.isEmpty) {
                        val batch = db.batch()
                        gastosSnapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                        batch.commit().await()
                    }

                    db.collection("presupuestos").document(uid).collection("activos")
                        .document(presupuesto.id).delete().await()

                } catch (e: Exception) {
                    Log.w("PresupuestoVM", "Sin conexión al eliminar, se eliminó localmente. Se sincronizará luego.")
                    programarSync()
                }
            } catch (e: Exception) {
                Log.e("PresupuestoVM", "Error al eliminar presupuesto", e)
            }
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

    private fun esExpirado(p: Presupuesto): Boolean {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return p.fechaFinal.before(hoy.time)
    }
}