package com.example.gastosapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import java.text.SimpleDateFormat
import java.util.*

class GastoAgrupadoAdapter(
    private var onEditarClick: (Gasto) -> Unit,
    private var onEliminarClick: (Gasto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val items = mutableListOf<Any>()
    private val expandedCategories = mutableSetOf<String>()  // ← EMPIEZA VACÍO
    private var todosLosGastos = listOf<Gasto>()
    private var presupuestos = listOf<Presupuesto>()

    fun submitList(gastos: List<Gasto>) {
        todosLosGastos = gastos

        if (gastos.isEmpty()) {
            items.clear()
            notifyDataSetChanged()
            return
        }

        // ELIMINAR ESTA LÍNEA - No expandir automáticamente
        // val categoriasConGastos = gastos.map { it.categoriaNombre }.distinct()
        // expandedCategories.addAll(categoriasConGastos)

        reconstruirLista()
    }

    fun updatePresupuestos(presupuestos: List<Presupuesto>) {
        this.presupuestos = presupuestos
        reconstruirLista()
    }

    private fun reconstruirLista() {
        items.clear()

        if (todosLosGastos.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        val gastosPorCategoria = todosLosGastos.groupBy { it.categoriaNombre }
        val categoriasOrdenadas = gastosPorCategoria.keys.sorted()

        categoriasOrdenadas.forEach { categoria ->
            items.add(categoria)  // Agregar header

            // SOLO agregar gastos si la categoría está expandida
            if (expandedCategories.contains(categoria)) {
                val gastosCategoria = gastosPorCategoria[categoria] ?: emptyList()
                val gastosOrdenados = gastosCategoria.sortedByDescending { it.fecha }
                items.addAll(gastosOrdenados)
            }
        }

        notifyDataSetChanged()
    }

    fun toggleCategory(categoria: String) {
        if (expandedCategories.contains(categoria)) {
            expandedCategories.remove(categoria)
        } else {
            expandedCategories.add(categoria)
        }
        reconstruirLista()
    }

    fun updateCategoriasDisponibles(categorias: List<String>) {
        // ELIMINAR ESTO - No expandir automáticamente
        // categorias.forEach { categoria ->
        //     if (!expandedCategories.contains(categoria)) {
        //         expandedCategories.add(categoria)
        //     }
        // }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> TYPE_HEADER
            is Gasto -> TYPE_ITEM
            else -> TYPE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_categoria_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_gasto, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("ViewType desconocido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val categoria = items[position] as String
                val gastosCategoria = todosLosGastos.filter { it.categoriaNombre == categoria }
                val presupuestoCategoria = presupuestos.find { it.categoriaNombre == categoria }
                holder.bind(categoria, gastosCategoria, presupuestoCategoria)
            }
            is ItemViewHolder -> {
                val gasto = items[position] as Gasto
                holder.bind(gasto)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoriaHeader)
        private val tvTotalGastado: TextView = itemView.findViewById(R.id.tvTotalCategoria)
        private val tvExpandIcon: TextView = itemView.findViewById(R.id.tvExpandIcon)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarPresupuesto)
        private val tvPresupuestoInfo: TextView = itemView.findViewById(R.id.tvPresupuestoInfo)

        fun bind(categoria: String, gastos: List<Gasto>, presupuesto: Presupuesto?) {
            tvCategoria.text = categoria  // ← Quité .uppercase() para que se vea normal
            val totalGastado = gastos.sumOf { it.monto }
            tvTotalGastado.text = "-$${String.format("%.2f", totalGastado)}"

            // Configurar barra de progreso si hay presupuesto
            if (presupuesto != null && presupuesto.cantidad > 0) {
                val porcentaje = ((totalGastado / presupuesto.cantidad) * 100).toInt().coerceAtMost(100)
                progressBar.progress = porcentaje
                progressBar.visibility = View.VISIBLE
                tvPresupuestoInfo.visibility = View.VISIBLE

                // Mostrar info del presupuesto
                tvPresupuestoInfo.text = "$${String.format("%.2f", totalGastado)} de $${String.format("%.2f", presupuesto.cantidad)}"

                // Cambiar color si excede el presupuesto
                if (totalGastado > presupuesto.cantidad) {
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.error)
                    tvTotalGastado.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
                    tvPresupuestoInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
                } else {
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.primary)
                    tvTotalGastado.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
                    tvPresupuestoInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            } else {
                // Sin presupuesto definido
                progressBar.visibility = View.GONE
                tvPresupuestoInfo.visibility = View.GONE
            }

            val isExpanded = expandedCategories.contains(categoria)
            tvExpandIcon.text = if (isExpanded) "▼" else "▶"

            itemView.setOnClickListener {
                toggleCategory(categoria)
            }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreGasto: TextView = itemView.findViewById(R.id.tvNombreGasto)
        private val tvCantidadGasto: TextView = itemView.findViewById(R.id.tvCantidadGasto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        private val tvFechaGasto: TextView = itemView.findViewById(R.id.tvFechaGasto)
        private val btnEditar: View = itemView.findViewById(R.id.btnEditarGasto)
        private val btnEliminar: View = itemView.findViewById(R.id.btnEliminarGasto)

        fun bind(gasto: Gasto) {
            tvNombreGasto.text = gasto.nombre
            tvCantidadGasto.text = "-$${String.format("%.2f", gasto.monto)}"
            tvDescripcion.text = if (gasto.descripcion.isNotEmpty()) gasto.descripcion else "Sin descripción"
            tvFechaGasto.text = sdf.format(gasto.fecha)

            btnEditar.setOnClickListener {
                try {
                    (btnEditar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                } catch (e: Exception) {}
                onEditarClick(gasto)
            }

            btnEliminar.setOnClickListener {
                try {
                    (btnEliminar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                } catch (e: Exception) {}
                onEliminarClick(gasto)
            }
        }
    }
}