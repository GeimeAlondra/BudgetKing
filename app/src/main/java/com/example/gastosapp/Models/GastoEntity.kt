package com.example.gastosapp.Models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos")
data class GastoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val monto: Double,
    val categoriaNombre: String,
    val fecha: Long, // Date guardado como timestamp Long
    val pendienteSync: Boolean = false // true = aún no subido a Firestore
)

// Conversiones entre el model de Firestore y la entidad de Room
fun GastoEntity.toGasto() = Gasto(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    monto = monto,
    categoriaNombre = categoriaNombre,
    fecha = java.util.Date(fecha)
)

fun Gasto.toEntity(pendienteSync: Boolean = false) = GastoEntity(
    id = id.ifEmpty { java.util.UUID.randomUUID().toString() },
    nombre = nombre,
    descripcion = descripcion,
    monto = monto,
    categoriaNombre = categoriaNombre,
    fecha = fecha.time,
    pendienteSync = pendienteSync
)