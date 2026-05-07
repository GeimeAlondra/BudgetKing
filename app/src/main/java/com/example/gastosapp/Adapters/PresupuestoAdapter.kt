package com.example.gastosapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import java.text.SimpleDateFormat
import java.util.*

class PresupuestoAdapter(
    private val onEditarClick: (Presupuesto, esAgotado: Boolean) -> Unit,
    private val onEliminarClick: (Presupuesto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_PRESUPUESTO = 0
        private const val TIPO_ENCABEZADO   = 1
    }

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Lista plana que el adapter renderiza: puede contener Presupuesto o String (encabezado)
    private var items: List<Any> = emptyList()

    // Estado de colapso de la sección agotados
    private var agotadosColapsados = true

    // Listas originales separadas
    private var activos  = listOf<Presupuesto>()
    private var agotados = listOf<Presupuesto>()

    fun submitList(list: List<Presupuesto>) {
        activos  = list.filter { !esAgotado(it) }
        agotados = list.filter {  esAgotado(it) }
        reconstruirItems()
    }

    private fun esAgotado(p: Presupuesto) = p.montoGastado >= p.cantidad && p.cantidad > 0

    private fun reconstruirItems() {
        val nueva = mutableListOf<Any>()
        nueva.addAll(activos)

        if (agotados.isNotEmpty()) {
            val label = if (agotadosColapsados)
                "▶  Agotados (${agotados.size})"
            else
                "▼  Agotados (${agotados.size})"
            nueva.add(label)
            if (!agotadosColapsados) nueva.addAll(agotados)
        }

        items = nueva
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is String) TIPO_ENCABEZADO else TIPO_PRESUPUESTO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENCABEZADO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_encabezado_seccion, parent, false)
            EncabezadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_presupuesto, parent, false)
            PresupuestoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EncabezadoViewHolder  -> holder.bind(items[position] as String)
            is PresupuestoViewHolder -> holder.bind(items[position] as Presupuesto)
        }
    }

    override fun getItemCount() = items.size

    // ── ViewHolder encabezado ──────────────────────────────────────────────
    inner class EncabezadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloSeccion)

        fun bind(label: String) {
            tvTitulo.text = label
            itemView.setOnClickListener {
                agotadosColapsados = !agotadosColapsados
                reconstruirItems()
            }
        }
    }

    // ── ViewHolder presupuesto ─────────────────────────────────────────────
    inner class PresupuestoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre:   TextView    = itemView.findViewById(R.id.tvNombrePresupuesto)
        private val tvCantidad: TextView    = itemView.findViewById(R.id.tvCantidad)
        private val tvCategoria:TextView    = itemView.findViewById(R.id.tvCategoria)
        private val tvFechaInicio: TextView = itemView.findViewById(R.id.tvFechaInicio)
        private val tvFechaFinal:  TextView = itemView.findViewById(R.id.tvFechaFinal)
        private val tvSaldo:    TextView    = itemView.findViewById(R.id.tvSaldoDisponible)
        private val tvEstado:   TextView    = itemView.findViewById(R.id.tvEstadoPresupuesto)
        private val progressBar: ProgressBar= itemView.findViewById(R.id.progressBarPresupuesto)
        private val btnEditar:  View        = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar:View        = itemView.findViewById(R.id.btnEliminar)

        fun bind(p: Presupuesto) {
            val agotado  = esAgotado(p)
            val expirado = p.fechaFinal.before(Date()) && !agotado

            tvNombre.text    = p.categoriaNombre
            tvCantidad.text  = "$${String.format("%.2f", p.cantidad)}"
            tvCategoria.text = "Categoría: ${p.categoriaNombre}"
            tvFechaInicio.text = sdf.format(p.fechaInicio)
            tvFechaFinal.text  = sdf.format(p.fechaFinal)

            // Saldo: si agotado mostrar gastado/total en lugar de número negativo
            val saldo = p.cantidad - p.montoGastado
            tvSaldo.text = if (agotado)
                "$${String.format("%.2f", p.montoGastado)} / $${String.format("%.2f", p.cantidad)}"
            else
                "$${String.format("%.2f", saldo)}"

            // Barra de progreso
            val porcentaje = ((p.montoGastado / p.cantidad) * 100).toInt().coerceAtMost(100)
            progressBar.progress = porcentaje

            // Badge de estado
            when {
                agotado  -> {
                    tvEstado.text = "Agotado"
                    tvEstado.setBackgroundResource(R.drawable.bg_estado_agotado)
                }
                expirado -> {
                    tvEstado.text = "Expirado"
                    tvEstado.setBackgroundResource(R.drawable.bg_estado_expirado)
                }
                else     -> {
                    tvEstado.text = "Activo"
                    tvEstado.setBackgroundResource(R.drawable.`bg_estado_activo_blue`)
                }
            }

            // Colores del saldo y barra
            when {
                agotado -> {
                    tvSaldo.setTextColor(ContextCompat.getColor(itemView.context, R.color.warning))
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.warning)
                }
                saldo < 0 -> {
                    tvSaldo.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.error)
                }
                saldo < p.cantidad * 0.2 -> {
                    tvSaldo.setTextColor(ContextCompat.getColor(itemView.context, R.color.warning))
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.warning)
                }
                else -> {
                    tvSaldo.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, R.color.primary)
                }
            }

            // Opacidad visual para agotados
            itemView.alpha = if (agotado) 0.75f else 1f

            btnEditar.setOnClickListener {
                (btnEditar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                onEditarClick(p, agotado)
            }
            btnEliminar.setOnClickListener {
                (btnEliminar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                onEliminarClick(p)
            }
        }
    }
}