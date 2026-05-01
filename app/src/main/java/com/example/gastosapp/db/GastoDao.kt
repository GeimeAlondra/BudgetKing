package com.example.gastosapp.db

import androidx.room.*
import com.example.gastosapp.Models.GastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE pendienteSync = 1")
    suspend fun obtenerPendientes(): List<GastoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(gastos: List<GastoEntity>)

    @Update
    suspend fun actualizar(gasto: GastoEntity)

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query("DELETE FROM gastos WHERE categoriaNombre = :categoria")
    suspend fun eliminarPorCategoria(categoria: String)

    @Query("DELETE FROM gastos")
    suspend fun eliminarTodos()

    @Query("UPDATE gastos SET pendienteSync = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)
}