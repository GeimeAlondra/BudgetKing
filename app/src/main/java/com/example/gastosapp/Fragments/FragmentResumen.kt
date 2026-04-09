package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentResumenBinding
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentResumen : Fragment() {

    private var _binding: FragmentResumenBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private val sdfDia = SimpleDateFormat("EEE", Locale.getDefault())

    private var gastosActuales = emptyList<Gasto>()
    private var presupuestosActuales = emptyList<Presupuesto>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResumenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observar gastos
        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            gastosActuales = gastos
            actualizarTodo(gastos)
            calcularTotales()
        }

        // Observar presupuestos
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            presupuestosActuales = presupuestos
            calcularTotales()
        }

        setupChartButtons()
    }

    private fun actualizarTodo(gastos: List<Gasto>) {
        actualizarResumenSemanal(gastos)
        actualizarResumenCategorias(gastos)
    }

    private fun actualizarResumenSemanal(gastos: List<Gasto>) {
        val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        val semana = gastos.filter { it.fecha.after(limite) || sameDay(it.fecha, limite) }
        val totalSemana = semana.sumOf { it.monto }
        binding.tvGastosSemana.text = String.format("$%.2f", totalSemana)
    }

    private fun actualizarResumenCategorias(gastos: List<Gasto>) {
        binding.containerCategorias.removeAllViews()

        val porCategoria = gastos.groupBy { it.categoriaNombre }.mapValues { it.value.sumOf { g -> g.monto } }
        val total = gastos.sumOf { it.monto }.coerceAtLeast(1.0)

        if (porCategoria.isEmpty()) {
            binding.containerCategorias.addView(TextView(requireContext()).apply {
                text = "No hay gastos registrados"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 100)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            })
            return
        }

        porCategoria.entries.sortedByDescending { it.value }.forEach { (nombreCat, monto) ->
            val item = layoutInflater.inflate(R.layout.item_categorias, binding.containerCategorias, false)
            item.findViewById<TextView>(R.id.tvNombreCategoria).text = nombreCat
            item.findViewById<TextView>(R.id.tvMontoCategoria).text = String.format("$%.2f", monto)

            val progress = item.findViewById<View>(R.id.progressBarCategoria)
            val params = progress.layoutParams as LinearLayout.LayoutParams
            params.weight = (monto / total * 100).toFloat()
            progress.layoutParams = params
            progress.setBackgroundColor(obtenerColorPorNombre(nombreCat))

            binding.containerCategorias.addView(item)
        }
    }

    private fun obtenerColorPorNombre(catNombre: String): Int {
        val cat = Categoria.fromNombre(catNombre)
        return when (cat) {
            Categoria.ALIMENTACION -> 0xFFE57373.toInt()
            Categoria.TRANSPORTE -> 0xFF7986CB.toInt()
            Categoria.ENTRETENIMIENTO -> 0xFFFFB74D.toInt()
            Categoria.SALUD -> 0xFF81C784.toInt()
            Categoria.EDUCACION -> 0xFF9575CD.toInt()
            Categoria.COMPRAS -> 0xFFFF8A65.toInt()
            Categoria.HOGAR -> 0xFFA1887F.toInt()
            Categoria.OTROS -> 0xFF90A4AE.toInt()
        }
    }

    private fun calcularTotales() {
        // Total de presupuesto (suma de todas las cantidades asignadas)
        val presupuestoTotal = presupuestosActuales.sumOf { it.cantidad }

        // Total gastado (suma de todos los gastos registrados)
        val gastadoTotal = gastosActuales.sumOf { it.monto }

        // Saldo disponible
        val saldoDisponible = presupuestoTotal - gastadoTotal

        // Actualizar UI
        binding.tvPresupuestoTotal.text = String.format("$%.2f", presupuestoTotal)
        binding.tvGastadoTotal.text = String.format("$%.2f", gastadoTotal)
        binding.tvTotalActivos.text = String.format("$%.2f", saldoDisponible)

        // Cambiar color del saldo según sea positivo o negativo
        if (saldoDisponible < 0) {
            binding.tvTotalActivos.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        } else {
            binding.tvTotalActivos.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        }
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    private fun setupChartButtons() {
        binding.btnGraficaDiaria.setOnClickListener {
            if (gastosActuales.isNotEmpty()) {
                ChartDialogFragment.newInstance("diario", gastosActuales)
                    .show(childFragmentManager, "ChartDialog")
            }
        }

        binding.btnGraficaSemanal.setOnClickListener {
            if (gastosActuales.isNotEmpty()) {
                ChartDialogFragment.newInstance("semanal", gastosActuales)
                    .show(childFragmentManager, "ChartDialog")
            }
        }

        binding.btnGraficaMensual.setOnClickListener {
            if (gastosActuales.isNotEmpty()) {
                ChartDialogFragment.newInstance("mensual", gastosActuales)
                    .show(childFragmentManager, "ChartDialog")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}