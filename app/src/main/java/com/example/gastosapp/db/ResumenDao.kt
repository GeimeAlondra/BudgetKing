package com.example.gastosapp.db

import androidx.room.*
import com.example.gastosapp.Models.ResumenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumenDao {

    @Query("SELECT * FROM resumen WHERE id = 'local' LIMIT 1")
    fun observar(): Flow<ResumenEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(resumen: ResumenEntity)

    @Query("DELETE FROM resumen")
    suspend fun eliminarTodos()
}