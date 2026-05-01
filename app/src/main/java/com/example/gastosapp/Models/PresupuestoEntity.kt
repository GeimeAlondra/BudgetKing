package com.example.gastosapp.Models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presupuestos")
data class PresupuestoEntity(
    @PrimaryKey val id: String,
    val categoriaNombre: String,
    val cantidad: Double,
    val fechaInicio: Long,
    val fechaFinal: Long,
    val montoGastado: Double,
    val pendienteSync: Boolean = false
)

fun PresupuestoEntity.toPresupuesto() = Presupuesto(
    id = id,
    categoriaNombre = categoriaNombre,
    cantidad = cantidad,
    fechaInicio = java.util.Date(fechaInicio),
    fechaFinal = java.util.Date(fechaFinal),
    montoGastado = montoGastado
)

fun Presupuesto.toEntity(pendienteSync: Boolean = false) = PresupuestoEntity(
    id = id.ifEmpty { java.util.UUID.randomUUID().toString() },
    categoriaNombre = categoriaNombre,
    cantidad = cantidad,
    fechaInicio = fechaInicio.time,
    fechaFinal = fechaFinal.time,
    montoGastado = montoGastado,
    pendienteSync = pendienteSync
)