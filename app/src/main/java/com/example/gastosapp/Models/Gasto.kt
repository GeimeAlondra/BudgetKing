package com.example.gastosapp.Models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Gasto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val categoriaNombre: String = "Otros",
    val fecha: Date = Date(),
    @ServerTimestamp val timestamp: Date? = null
)