package com.example.gastosapp.Models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Resumen(
    val id_usuario: String = "",
    val monto_inicial: Double = 0.0,
    val total_gastado: Double = 0.0,
    val total_disponible: Double = 0.0,
    @ServerTimestamp
    val fecha_actualizacion: Date? = null
)