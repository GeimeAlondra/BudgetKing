package com.example.gastosapp.Models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resumen")
data class ResumenEntity(
    @PrimaryKey val id: String = "local",
    val montoInicial: Double = 0.0,
    val totalGastado: Double = 0.0,
    val totalDisponible: Double = 0.0,
    val ultimaActualizacion: Long = System.currentTimeMillis()
)