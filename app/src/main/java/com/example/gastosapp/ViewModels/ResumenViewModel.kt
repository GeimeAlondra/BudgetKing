package com.example.gastosapp.ViewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Resumen
import com.example.gastosapp.Models.ResumenEntity
import com.example.gastosapp.db.AppDatabase
import com.example.gastosapp.Utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResumenViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val resumenDao = AppDatabase.getInstance(application).resumenDao()

    // La UI puede observar el resumen directamente desde Room
    val resumen: LiveData<ResumenEntity?> = resumenDao.observar().asLiveData()

    fun recalcularYGuardar(totalGastado: Double, montoInicial: Double) {
        val uid = FirebaseUtils.uid()
        if (uid.isNullOrEmpty()) return

        val totalDisponible = montoInicial - totalGastado

        viewModelScope.launch {
            // Guardar en Room inmediatamente — funciona sin internet
            resumenDao.guardar(
                ResumenEntity(
                    montoInicial = montoInicial,
                    totalGastado = totalGastado,
                    totalDisponible = totalDisponible
                )
            )

            // Intentar sincronizar con Firestore
            try {
                db.collection("resumenes").document(uid).set(
                    Resumen(
                        id_usuario = uid,
                        monto_inicial = montoInicial,
                        total_gastado = totalGastado,
                        total_disponible = totalDisponible
                    )
                ).await()
                Log.d("ResumenVM", "Sincronizado con Firestore")
            } catch (e: Exception) {
                Log.w("ResumenVM", "Sin conexión, resumen guardado solo localmente")
            }
        }
    }
}