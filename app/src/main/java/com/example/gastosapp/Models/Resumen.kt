package com.example.gastosapp.Models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Resumen(
    val id: String = "actual", // ID fijo
    val id_usuario: String = "",
    val total_general: Double = 0.0,
    val monto_inicial: Double = 0.0,
    val total_gastado: Double = 0.0,
    val fecha_actualizacion: Date = Date(),

    @ServerTimestamp
    val timestamp: Date? = null
)