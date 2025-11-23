package com.example.gastosapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.gastosapp.R
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // === Paints (optimizados y configurables) ===
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 2f
    }
    private val axisPaint = Paint().apply {
        color = Color.parseColor("#666666")
        strokeWidth = 4f
    }

    // === Datos ===
    private var chartData: List<BarData> = emptyList()
    private var maxValue: Float = 1f

    // === Configuración personalizable desde XML ===
    private var barCornerRadius = 16f
    private var showGrid = true
    private var labelTextSize = 36f
    private var valueTextSize = 32f
    private var gridLinesCount = 4

    init {
        // Leer atributos del XML
        context.withStyledAttributes(attrs, R.styleable.BarChartView) {
            barCornerRadius = getDimension(R.styleable.BarChartView_barCornerRadius, 16f)
            showGrid = getBoolean(R.styleable.BarChartView_showGrid, true)
            labelTextSize = getDimensionPixelSize(R.styleable.BarChartView_labelTextSize, 36).toFloat()
            valueTextSize = getDimensionPixelSize(R.styleable.BarChartView_valueTextSize, 32).toFloat()
            gridLinesCount = getInt(R.styleable.BarChartView_gridLinesCount, 4)
        }
    }

    data class BarData(
        val label: String,
        val value: Float,
        val color: Int = Color.parseColor("#4CAF50")
    )

    fun setData(data: List<BarData>) {
        this.chartData = data
        calculateMaxValue()
        requestLayout()
        invalidate()
    }

    private fun calculateMaxValue() {
        maxValue = if (chartData.isEmpty()) 1f else {
            val rawMax = chartData.maxOf { it.value }
            when {
                rawMax <= 0f -> 1f
                rawMax < 10f -> rawMax * 1.5f
                rawMax < 100f -> (rawMax * 1.2f).toInt().toFloat()
                else -> (rawMax * 1.1f).toInt().toFloat()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = 600 + paddingTop + paddingBottom  // altura decente por defecto
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chartData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val usableWidth = width - paddingLeft - paddingRight.toFloat()
        val usableHeight = height - paddingTop - paddingBottom.toFloat()

        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = width - paddingRight.toFloat()
        val bottom = height - paddingBottom.toFloat()

        val chartTop = top + 60f
        val chartBottom = bottom - 80f
        val chartHeight = chartBottom - chartTop

        if (showGrid) drawGrid(canvas, chartTop, chartBottom, chartHeight)
        drawXAxis(canvas, chartBottom, left, right)
        drawBars(canvas, chartTop, chartBottom, chartHeight, usableWidth)
        drawLabels(canvas, chartBottom + 50f, usableWidth)
    }

    private fun drawGrid(canvas: Canvas, chartTop: Float, chartBottom: Float, chartHeight: Float) {
        for (i in 1..gridLinesCount) {
            val y = chartBottom - (chartHeight * i / gridLinesCount)
            canvas.drawLine(paddingLeft.toFloat(), y, width - paddingRight.toFloat(), y, gridPaint)
        }
    }

    private fun drawXAxis(canvas: Canvas, y: Float, left: Float, right: Float) {
        canvas.drawLine(left, y, right, y, axisPaint)
    }

    private fun drawBars(
        canvas: Canvas,
        chartTop: Float,
        chartBottom: Float,
        chartHeight: Float,
        usableWidth: Float
    ) {
        val barWidth = usableWidth / chartData.size * 0.7f
        val spacing = usableWidth / chartData.size * 0.3f

        chartData.forEachIndexed { index, bar ->
            val barLeft = paddingLeft + index * (barWidth + spacing) + spacing / 2
            val barRight = barLeft + barWidth
            val ratio = bar.value / maxValue
            val barHeight = chartHeight * ratio
            val barTop = chartBottom - barHeight
            val barBottom = chartBottom

            barPaint.color = bar.color
            canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, barCornerRadius, barCornerRadius, barPaint)

            // Valor encima de la barra
            if (bar.value > maxValue * 0.05f) {  // solo si es visible
                textPaint.textSize = valueTextSize
                textPaint.color = Color.BLACK
                canvas.drawText(
                    "$${bar.value.toInt()}",
                    (barLeft + barRight) / 2,
                    barTop - 10f,
                    textPaint
                )
            }
        }
    }

    private fun drawLabels(canvas: Canvas, y: Float, usableWidth: Float) {
        textPaint.textSize = labelTextSize
        textPaint.color = Color.DKGRAY

        val barWidth = usableWidth / chartData.size * 0.7f
        val spacing = usableWidth / chartData.size * 0.3f

        chartData.forEachIndexed { index, bar ->
            val x = paddingLeft + index * (barWidth + spacing) + spacing / 2 + barWidth / 2
            canvas.drawText(bar.label, x, y, textPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = 48f
        textPaint.color = Color.LTGRAY
        canvas.drawText(
            "Sin datos",
            width / 2f,
            height / 2f,
            textPaint
        )
    }
}