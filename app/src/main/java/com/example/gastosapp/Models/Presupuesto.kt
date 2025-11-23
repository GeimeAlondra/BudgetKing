package com.example.gastosapp.Models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Presupuesto(
    val id: String = "",
    val categoriaNombre: String = "Otros",
    val cantidad: Double = 0.0,
    val fechaInicio: Date = Date(),
    val fechaFinal: Date = Date(),
    var montoGastado: Double = 0.0,
    @ServerTimestamp val creadoEn: Date? = null
)