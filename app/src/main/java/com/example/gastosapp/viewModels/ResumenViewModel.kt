package com.example.gastosapp.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastosapp.Models.Resumen
import com.example.gastosapp.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ResumenViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val uid: String
        get() = FirebaseUtils.uid() ?: ""

    fun actualizarResumenActual(
        totalGastado: Double,
        montoInicial: Double
    ) {
        viewModelScope.launch {
            try {
                val totalGeneral = montoInicial - totalGastado

                val resumen = Resumen(
                    id = "actual",
                    id_usuario = uid,
                    total_general = totalGeneral,
                    monto_inicial = montoInicial,
                    total_gastado = totalGastado,
                    fecha_actualizacion = Date()
                )

                db.collection("resumenes")
                    .document(uid)
                    .collection("mis_resumenes")
                    .document("actual")
                    .set(resumen)
                    .await()

                Log.d("ResumenVM", "Resumen actual actualizado | Gastado: $$totalGastado | Inicial: $$montoInicial")
            } catch (e: Exception) {
                Log.e("ResumenVM", "Error al actualizar resumen actual", e)
            }
        }
    }

    suspend fun getResumenActual(): Resumen? {
        return try {
            val doc = db.collection("resumenes")
                .document(uid)
                .collection("mis_resumenes")
                .document("actual")
                .get()
                .await()

            doc.toObject(Resumen::class.java)
        } catch (e: Exception) {
            Log.e("ResumenVM", "Error obteniendo resumen actual", e)
            null
        }
    }
}