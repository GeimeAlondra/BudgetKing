package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.example.gastosapp.Views.BarChartView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class ChartDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_CHART_TYPE = "chart_type"
        private const val ARG_GASTOS = "gastos"

        fun newInstance(chartType: String, gastos: List<Gasto>): ChartDialogFragment {
            val fragment = ChartDialogFragment()
            val args = Bundle()
            args.putString(ARG_CHART_TYPE, chartType)
            args.putSerializable(ARG_GASTOS, ArrayList(gastos))
            fragment.arguments = args
            return fragment
        }
    }

    private val sdfDia = SimpleDateFormat("EEE", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chartType = arguments?.getString(ARG_CHART_TYPE) ?: "diario"
        @Suppress("UNCHECKED_CAST")
        val gastos = (arguments?.getSerializable(ARG_GASTOS) as? ArrayList<Gasto>)?.toList() ?: emptyList()

        val chartView = view.findViewById<BarChartView>(R.id.chartView)
        val tvTitle = view.findViewById<TextView>(R.id.tvChartTitle)
        val btnCerrar = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCerrar)
        val scrollView = view.findViewById<HorizontalScrollView>(R.id.scrollView)


        val anchoPorBarra = 90  // dp por barra
        val layoutParams = chartView.layoutParams

        layoutParams.width = when (chartType) {
            "diario" -> 7 * anchoPorBarra      // 630dp
            "semanal" -> 16 * anchoPorBarra    // 1440dp
            "mensual" -> 12 * anchoPorBarra    // 1080dp
            else -> 800
        }
        chartView.layoutParams = layoutParams

        // También ajustar altura para hacerlo más compacto
        val alturaLayout = scrollView.layoutParams
        alturaLayout.height = 320  // Más compacto
        scrollView.layoutParams = alturaLayout

        // Configurar título y gráfica
        when (chartType) {
            "diario" -> {
                tvTitle.text = " Gastos Diarios (7 días)"
                configurarGraficaDiaria(chartView, gastos)
            }
            "semanal" -> {
                tvTitle.text = " Gastos Semanales (16 sem)"
                configurarGraficaSemanal(chartView, gastos)
                scrollView.post {
                    scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
                }
            }
            "mensual" -> {
                tvTitle.text = " Gastos Mensuales (12 meses)"
                configurarGraficaMensual(chartView, gastos)
            }
        }

        btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    private fun configurarGraficaDiaria(chart: BarChartView, gastos: List<Gasto>) {
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
        chart.setData(datos)
    }

    private fun configurarGraficaSemanal(chart: BarChartView, gastos: List<Gasto>) {
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
            val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
            datos.add(BarChartView.BarData("S$weekNumber", monto.toFloat()))
        }
        chart.setData(datos.reversed())
    }

    private fun configurarGraficaMensual(chart: BarChartView, gastos: List<Gasto>) {
        val meses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val cal = Calendar.getInstance()

        val datos = meses.mapIndexed { index, mes ->
            cal.set(Calendar.MONTH, index)
            val inicioMes = cal.clone() as Calendar
            inicioMes.set(Calendar.DAY_OF_MONTH, 1)
            val finMes = inicioMes.clone() as Calendar
            finMes.add(Calendar.MONTH, 1)
            finMes.add(Calendar.DAY_OF_YEAR, -1)

            val monto = gastos.filter { it.fecha in inicioMes.time..finMes.time }.sumOf { it.monto }
            BarChartView.BarData(mes, monto.toFloat())
        }
        chart.setData(datos)
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }
}