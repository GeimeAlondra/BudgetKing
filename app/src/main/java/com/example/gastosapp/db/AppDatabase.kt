package com.example.gastosapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gastosapp.Models.GastoEntity
import com.example.gastosapp.Models.PresupuestoEntity
import com.example.gastosapp.Models.ResumenEntity

@Database(
    entities = [GastoEntity::class, PresupuestoEntity::class, ResumenEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gastoDao(): GastoDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun resumenDao(): ResumenDao

    // Limpia todas las tablas al cerrar sesión
    suspend fun clearAllUserData() {
        gastoDao().eliminarTodos()
        presupuestoDao().eliminarTodos()
        resumenDao().eliminarTodos()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetking_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}