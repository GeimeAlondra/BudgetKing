package com.example.gastosapp.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gastosapp.Models.toGasto
import com.example.gastosapp.Models.toPresupuesto
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.db.AppDatabase
import kotlinx.coroutines.tasks.await

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseUtils.uid() ?: return Result.failure()
        val db = AppDatabase.getInstance(applicationContext)
        val firestore = FirebaseUtils.db

        return try {
            // ── Sincronizar gastos pendientes ──────────────────
            val gastosPendientes = db.gastoDao().obtenerPendientes()
            gastosPendientes.forEach { entity ->
                val gasto = entity.toGasto()
                firestore.collection("gastos")
                    .document(uid)
                    .collection("mis_gastos")
                    .document(gasto.id)
                    .set(gasto)
                    .await()
                db.gastoDao().marcarSincronizado(entity.id)
                Log.d("SyncWorker", "Gasto sincronizado: ${entity.id}")
            }

            // Sincronizar presupuestos pendientes
            val presupuestosPendientes = db.presupuestoDao().obtenerPendientes()
            presupuestosPendientes.forEach { entity ->
                val presupuesto = entity.toPresupuesto()
                firestore.collection("presupuestos")
                    .document(uid)
                    .collection("activos")
                    .document(presupuesto.id)
                    .set(presupuesto)
                    .await()
                db.presupuestoDao().marcarSincronizado(entity.id)
                Log.d("SyncWorker", "Presupuesto sincronizado: ${entity.id}")
            }

            Log.d("SyncWorker", "Sincronización completada")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error en sincronización", e)
            Result.retry()  // WorkManager lo reintentará automáticamente
        }
    }
}