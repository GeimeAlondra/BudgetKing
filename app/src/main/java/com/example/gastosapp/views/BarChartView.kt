package com.example.gastosapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pintura para las barras
    private val barPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")  // Verde
        style = Paint.Style.FILL
    }

    // Pintura para el texto
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    // Pintura para los ejes
    private val axisPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    // Datos del gráfico: Lista de (etiqueta, valor)
    private var data: List<Pair<String, Float>> = emptyList()
    private var maxValue: Float = 0f

    // Variables para controlar el ancho y espaciado de barras
    private var barWidthFactor: Float = 0.9f
    private var barSpacing: Float = 0.1f // 10% de espacio entre barras

    /**
     * Establece los datos para el gráfico
     * @param newData Lista de pares (etiqueta, valor)
     */
    /**
     * Establece los datos para el gráfico
     * @param newData Lista de pares (etiqueta, valor)
     */
    fun setData(newData: List<Pair<String, Float>>) {
        data = newData
        // Calcular el valor máximo para escalar las barras
        maxValue = if (data.isNotEmpty()) {
            val rawMax = data.maxOf { it.second }
            // Redondear hacia arriba para una escala más limpia
            when {
                rawMax == 0f -> 1f
                rawMax < 10f -> rawMax * 1.5f
                rawMax < 100f -> Math.ceil((rawMax * 1.2).toDouble()).toFloat()
                else -> Math.ceil((rawMax * 1.1).toDouble()).toFloat()
            }
        } else {
            1f
        }
        // Redibujar el view
        invalidate()
    }

    /**
     * Establece el ancho de las barras (0.1f a 1.0f)
     */
    fun setBarWidthFactor(factor: Float) {
        barWidthFactor = factor.coerceIn(0.1f, 1.0f)
        invalidate()
    }

    /**
     * Establece el espacio entre barras
     * @param spacing Valor entre 0.0f (sin espacio) y 0.5f (50% de espacio)
     */
    fun setBarSpacing(spacing: Float) {
        barSpacing = spacing.coerceIn(0.0f, 0.5f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Si no hay datos, mostrar mensaje
        if (data.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        // Dibujar los elementos del gráfico
        drawBars(canvas)
        drawLabels(canvas)
        drawAxes(canvas)
    }

    /**
     * Dibuja mensaje cuando no hay datos
     */
    private fun drawNoDataMessage(canvas: Canvas) {
        val text = "No hay datos"
        val x = width / 2f
        val y = height / 2f

        textPaint.textSize = 48f
        textPaint.color = Color.GRAY
        canvas.drawText(text, x, y, textPaint)
    }

    /**
     * Dibuja los ejes X e Y CON MÁS ESPACIO SUPERIOR
     */
    private fun drawAxes(canvas: Canvas) {
        // AUMENTAR márgenes superiores e inferiores
        val topMargin = 80f  // Más espacio arriba
        val bottomMargin = 100f  // Más espacio abajo
        val leftMargin = 50f
        val rightMargin = 40f

        val chartHeight = height - topMargin - bottomMargin
        val chartWidth = width - leftMargin - rightMargin

        // Eje Y (vertical)
        canvas.drawLine(leftMargin, height - bottomMargin, leftMargin, topMargin, axisPaint)

        // Eje X (horizontal)
        canvas.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin, axisPaint)

        // Marcas en el eje Y
        if (maxValue > 0) {
            textPaint.textSize = 18f
            textPaint.color = Color.GRAY
            textPaint.textAlign = Paint.Align.RIGHT

            val valorMaximoReal = maxValue / 1.1f

            // Dibujar marcas en el eje Y
            for (i in 0..3) {
                val porcentaje = i * 0.25f
                val valor = valorMaximoReal * porcentaje
                val yPos = height - bottomMargin - (chartHeight * porcentaje)

                // Línea horizontal de guía
                axisPaint.color = Color.LTGRAY
                axisPaint.strokeWidth = 1f
                canvas.drawLine(leftMargin, yPos, width - rightMargin, yPos, axisPaint)
                axisPaint.color = Color.GRAY
                axisPaint.strokeWidth = 2f

                // Texto del valor - CON MÁS MARGEN IZQUIERDO
                if (i > 0) {
                    canvas.drawText("$${String.format("%.0f", valor)}", leftMargin - 10f, yPos + 6f, textPaint)
                }
            }

            // Valor máximo en la parte superior - CON MÁS ESPACIO
            canvas.drawText("$${String.format("%.0f", valorMaximoReal)}", leftMargin - 10f, topMargin - 10f, textPaint)

            textPaint.textAlign = Paint.Align.CENTER
        }
    }

    /**
     * Dibuja las barras del gráfico CON NUEVOS MÁRGENES
     */
    private fun drawBars(canvas: Canvas) {
        val topMargin = 80f
        val bottomMargin = 100f
        val leftMargin = 50f
        val rightMargin = 40f

        val chartWidth = width - leftMargin - rightMargin
        val chartHeight = height - topMargin - bottomMargin

        val barWidth = chartWidth / data.size.toFloat()
        val actualBarWidth = barWidth * (1f - barSpacing)
        val spacingWidth = barWidth * barSpacing

        data.forEachIndexed { index, (label, value) ->
            // Calcular altura de la barra
            val barHeight = (value / (maxValue / 1.1f)) * chartHeight

            // Calcular posición de la barra CON NUEVOS MÁRGENES
            val left = leftMargin + (index * barWidth) + (spacingWidth / 2)
            val top = height - bottomMargin - barHeight
            val right = left + actualBarWidth * barWidthFactor
            val bottom = height - bottomMargin

            // Dibujar barra
            canvas.drawRect(left, top, right, bottom, barPaint)

            // Dibujar valor encima de la barra
            if (value > 0 && barHeight > 40f) {
                textPaint.textSize = 20f
                textPaint.color = Color.BLACK
                canvas.drawText(
                    "$${String.format("%.0f", value)}",
                    (left + right) / 2,
                    top - 10f,
                    textPaint
                )
            }
        }
    }

    /**
     * Dibuja las etiquetas en el eje X CON NUEVOS MÁRGENES
     */
    private fun drawLabels(canvas: Canvas) {
        val topMargin = 80f
        val bottomMargin = 100f
        val leftMargin = 50f
        val rightMargin = 40f

        val chartWidth = width - leftMargin - rightMargin
        val barWidth = chartWidth / data.size.toFloat()

        data.forEachIndexed { index, (label, value) ->
            val x = leftMargin + (index * barWidth) + (barWidth / 2)
            val y = height - (bottomMargin / 2)  // Centrado en el espacio inferior

            textPaint.textSize = 22f  // Texto un poco más grande
            textPaint.color = Color.BLACK
            canvas.drawText(label, x, y, textPaint)
        }
    }

    /**
     * Cambia el color de las barras
     */
    fun setBarColor(color: Int) {
        barPaint.color = color
        invalidate()
    }

    /**
     * Establece el tamaño del texto
     */
    fun setTextSize(size: Float) {
        textPaint.textSize = size
        invalidate()
    }
}