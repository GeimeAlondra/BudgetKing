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
import com.example.gastosapp.views.BarChartView
import java.text.SimpleDateFormat
import java.util.*

class FragmentResumen : Fragment() {

    private var _binding: FragmentResumenBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private val sdfDia = SimpleDateFormat("EEE", Locale.getDefault())
    private val sdfMes = SimpleDateFormat("MMM", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResumenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            actualizarTodo(gastos)
        }

        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            calcularTotales(presupuestos)
        }
    }

    private fun actualizarTodo(gastos: List<Gasto>) {
        actualizarResumenSemanal(gastos)
        actualizarResumenCategorias(gastos)
        actualizarGraficas(gastos)
    }

    private fun actualizarResumenSemanal(gastos: List<Gasto>) {
        val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        val semana = gastos.filter { it.fecha.after(limite) || sameDay(it.fecha, limite) }
        binding.tvGastosSemana.text = String.format("$%.2f", semana.sumOf { it.monto })
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
            item.findViewById<TextView>(R.id.tvNombreCategoria).text = nombreCat  // ← String directo
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

    private fun calcularTotales(presupuestos: List<Presupuesto>) {
        val total = presupuestos.sumOf { it.cantidad }
        val gastado = presupuestos.sumOf { it.montoGastado }
        binding.tvPresupuestoTotal.text = String.format("$%.2f", total)
        binding.tvGastadoTotal.text = String.format("$%.2f", gastado)
        binding.tvTotalActivos.text = String.format("$%.2f", total - gastado)
    }

    private fun actualizarGraficas(gastos: List<Gasto>) {
        actualizarDiaria(gastos)
        actualizarSemanal(gastos)
        actualizarMensual(gastos)
    }

    private fun actualizarDiaria(gastos: List<Gasto>) {
        val hoy = Calendar.getInstance()
        val inicio = hoy.clone() as Calendar
        inicio.add(Calendar.DAY_OF_YEAR, -6)

        val datos = mutableListOf<BarChartView.BarData>()
        for (i in 0..6) {
            val dia = inicio.clone() as Calendar
            dia.add(Calendar.DAY_OF_YEAR, i)
            val montoDia = gastos.filter { sameDay(it.fecha, dia.time) }.sumOf { it.monto }
            val label = sdfDia.format(dia.time).take(3)
            datos.add(BarChartView.BarData(label, montoDia.toFloat()))
        }
        binding.chartDiario.setData(datos)
    }

    private fun actualizarSemanal(gastos: List<Gasto>) {
        val datos = mutableListOf<BarChartView.BarData>()
        val hoy = Calendar.getInstance()

        for (i in 0..15) {
            val cal = hoy.clone() as Calendar
            cal.add(Calendar.WEEK_OF_YEAR, -i)
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val inicioSemana = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val finSemana = cal.time

            val monto = gastos.filter { it.fecha in inicioSemana..finSemana }.sumOf { it.monto }
            datos.add(BarChartView.BarData("S${cal.get(Calendar.WEEK_OF_YEAR)}", monto.toFloat()))
        }
        binding.chartSemanal.setData(datos.reversed())
    }

    private fun actualizarMensual(gastos: List<Gasto>) {
        val meses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val cal = Calendar.getInstance()

        val datos = meses.mapIndexed { index, _ ->
            cal.set(Calendar.MONTH, index)
            val inicioMes = cal.clone() as Calendar
            inicioMes.set(Calendar.DAY_OF_MONTH, 1)
            val finMes = inicioMes.clone() as Calendar
            finMes.add(Calendar.MONTH, 1)
            finMes.add(Calendar.DAY_OF_YEAR, -1)

            val monto = gastos.filter { it.fecha in inicioMes.time..finMes.time }.sumOf { it.monto }
            BarChartView.BarData(meses[index], monto.toFloat())
        }
        binding.chartMensual.setData(datos)
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}