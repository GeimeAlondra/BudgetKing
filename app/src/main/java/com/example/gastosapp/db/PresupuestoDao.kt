package com.example.gastosapp.db

import androidx.room.*
import com.example.gastosapp.Models.PresupuestoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresupuestoDao {

    @Query("SELECT * FROM presupuestos ORDER BY fechaInicio DESC")
    fun observarTodos(): Flow<List<PresupuestoEntity>>

    @Query("SELECT * FROM presupuestos WHERE pendienteSync = 1")
    suspend fun obtenerPendientes(): List<PresupuestoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(presupuesto: PresupuestoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(presupuestos: List<PresupuestoEntity>)

    @Update
    suspend fun actualizar(presupuesto: PresupuestoEntity)

    @Query("DELETE FROM presupuestos WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query("DELETE FROM presupuestos")
    suspend fun eliminarTodos()

    @Query("UPDATE presupuestos SET pendienteSync = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)
}