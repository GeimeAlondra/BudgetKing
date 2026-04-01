package com.example.gastosapp.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Resumen
import com.example.gastosapp.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResumenViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    fun recalcularYGuardar(totalGastado: Double, montoInicial: Double) {
        val uid = FirebaseUtils.uid()
        if (uid.isNullOrEmpty()) {
            Log.w("ResumenVM", "UID vacío, se omite el guardado")
            return
        }

        val resumen = Resumen(
            id_usuario = uid,
            monto_inicial = montoInicial,
            total_gastado = totalGastado,
            total_disponible = montoInicial - totalGastado
        )

        viewModelScope.launch {
            try {
                db.collection("resumenes")
                    .document(uid)
                    .set(resumen)
                    .await()
                Log.d("ResumenVM", "Guardado | Inicial: $montoInicial | Gastado: $totalGastado")
            } catch (e: Exception) {
                Log.e("ResumenVM", "Error al guardar", e)
            }
        }
    }
}