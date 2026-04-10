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
    private val onEditarClick: (Presupuesto) -> Unit,
    private val onEliminarClick: (Presupuesto) -> Unit
) : RecyclerView.Adapter<PresupuestoAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var presupuestos = listOf<Presupuesto>()

    fun submitList(list: List<Presupuesto>) {
        presupuestos = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_presupuesto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(presupuestos[position])
    }

    override fun getItemCount(): Int = presupuestos.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePresupuesto)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        private val tvFechaInicio: TextView = itemView.findViewById(R.id.tvFechaInicio)
        private val tvFechaFinal: TextView = itemView.findViewById(R.id.tvFechaFinal)
        private val tvSaldo: TextView = itemView.findViewById(R.id.tvSaldoDisponible)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoPresupuesto)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarPresupuesto)
        private val btnEditar: View = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: View = itemView.findViewById(R.id.btnEliminar)

        fun bind(p: Presupuesto) {
            tvNombre.text = p.categoriaNombre
            tvCantidad.text = "$${String.format("%.2f", p.cantidad)}"
            tvCategoria.text = "Categoría: ${p.categoriaNombre}"
            tvFechaInicio.text = sdf.format(p.fechaInicio)
            tvFechaFinal.text = sdf.format(p.fechaFinal)

            val saldo = p.cantidad - p.montoGastado
            tvSaldo.text = "$${String.format("%.2f", saldo)}"

            val porcentaje = ((p.montoGastado / p.cantidad) * 100).toInt().coerceAtMost(100)
            progressBar.progress = porcentaje

            val expirado = p.fechaFinal.before(Date())
            tvEstado.text = if (expirado) "Expirado" else "Activo"
            tvEstado.setBackgroundResource(
                if (expirado) R.drawable.bg_estado_expirado else R.drawable.bg_estado_activo
            )

            when {
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

            btnEditar.setOnClickListener {
                (btnEditar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                onEditarClick(p)
            }

            btnEliminar.setOnClickListener {
                (btnEliminar as? com.airbnb.lottie.LottieAnimationView)?.playAnimation()
                onEliminarClick(p)
            }
        }
    }
}