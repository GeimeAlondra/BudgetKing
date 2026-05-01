package com.example.gastosapp.Views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes
import com.example.gastosapp.R

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // === Paints mejorados ===
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#2C3E50")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 26f
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#7F8C8D")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 22f
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#ECF0F1")
        strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val axisPaint = Paint().apply {
        color = Color.parseColor("#BDC3C7")
        strokeWidth = 2.5f
    }

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#FAFBFC")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D000000")
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // === Datos ===
    private var chartData: List<BarData> = emptyList()
    private var maxValue: Float = 1f
    private var animationProgress = 0f

    // === Configuración (MISMAS DIMENSIONES) ===
    private var barCornerRadius = 12f
    private var showGrid = true
    private var labelTextSize = 24f
    private var valueTextSize = 22f
    private var gridLinesCount = 4

    // Paleta de colores moderna
    private val colorPalette = listOf(
        Color.parseColor("#FF6B6B"), // Coral
        Color.parseColor("#4ECDC4"), // Turquesa
        Color.parseColor("#45B7D1"), // Azul cielo
        Color.parseColor("#96CEB4"), // Verde menta
        Color.parseColor("#FFEAA7"), // Amarillo pastel
        Color.parseColor("#DDA0DD"), // Lavanda
        Color.parseColor("#98D8C8"), // Verde agua
        Color.parseColor("#F7DC6F"), // Amarillo
        Color.parseColor("#BB8FCE"), // Púrpura
        Color.parseColor("#85C1E9"), // Azul claro
        Color.parseColor("#FF8A80"), // Salmón
        Color.parseColor("#80DEEA"), // Cyan
        Color.parseColor("#A5D6A7"), // Verde
        Color.parseColor("#FFCC80"), // Naranja
        Color.parseColor("#CE93D8")  // Lila
    )

    init {
        context.withStyledAttributes(attrs, R.styleable.BarChartView) {
            barCornerRadius = getDimension(R.styleable.BarChartView_barCornerRadius, 12f)
            showGrid = getBoolean(R.styleable.BarChartView_showGrid, true)
            labelTextSize = getDimensionPixelSize(R.styleable.BarChartView_labelTextSize, 24).toFloat()
            valueTextSize = getDimensionPixelSize(R.styleable.BarChartView_valueTextSize, 22).toFloat()
            gridLinesCount = getInt(R.styleable.BarChartView_gridLinesCount, 4)
        }
        setBackgroundColor(Color.WHITE)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    data class BarData(val label: String, val value: Float, val color: Int = Color.parseColor("#4CAF50"))

    fun setData(data: List<BarData>) {
        this.chartData = data.mapIndexed { index, barData ->
            BarData(barData.label, barData.value, colorPalette[index % colorPalette.size])
        }
        calculateMaxValue()
        startAnimation()
        invalidate()
    }

    private fun startAnimation() {
        animationProgress = 0f
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            animationProgress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
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
        val desiredHeight = 400 + paddingTop + paddingBottom  // MISMA DIMENSIÓN
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        if (chartData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val usableWidth = width - paddingLeft - paddingRight.toFloat()
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat() + 20f
        val right = width - paddingRight.toFloat()
        val bottom = height - paddingBottom.toFloat() - 20f
        val chartTop = top + 30f
        val chartBottom = bottom - 50f
        val chartHeight = chartBottom - chartTop

        // Fondo con sombra
        canvas.drawRoundRect(left + 3, top + 3, right - 3, bottom - 3, 12f, 12f, shadowPaint)
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, bgPaint)

        if (showGrid) drawGrid(canvas, chartTop, chartBottom, chartHeight)
        drawXAxis(canvas, chartBottom, left, right)
        drawBars(canvas, chartTop, chartBottom, chartHeight, usableWidth)
        drawLabels(canvas, chartBottom + 35f, usableWidth)
    }

    private fun drawGrid(canvas: Canvas, chartTop: Float, chartBottom: Float, chartHeight: Float) {
        for (i in 1..gridLinesCount) {
            val y = chartBottom - (chartHeight * i / gridLinesCount)
            canvas.drawLine(paddingLeft.toFloat() + 5, y, width - paddingRight.toFloat() - 5, y, gridPaint)
        }
    }

    private fun drawXAxis(canvas: Canvas, y: Float, left: Float, right: Float) {
        canvas.drawLine(left + 5, y, right - 5, y, axisPaint)
    }

    private fun drawBars(canvas: Canvas, chartTop: Float, chartBottom: Float, chartHeight: Float, usableWidth: Float) {
        val barWidth = usableWidth / chartData.size * 0.55f
        val spacing = usableWidth / chartData.size * 0.45f

        chartData.forEachIndexed { index, bar ->
            val barLeft = paddingLeft + index * (barWidth + spacing) + spacing / 2
            val barRight = barLeft + barWidth
            val barHeight = chartHeight * (bar.value / maxValue) * animationProgress
            val barTop = chartBottom - barHeight

            // Gradiente para cada barra
            val gradient = LinearGradient(
                barLeft, barTop,
                barRight, chartBottom,
                intArrayOf(lightenColor(bar.color), bar.color),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            barPaint.shader = gradient

            // Dibujar barra
            canvas.drawRoundRect(barLeft, barTop, barRight, chartBottom, barCornerRadius, barCornerRadius, barPaint)

            // Brillo superior
            if (barHeight > 10) {
                highlightPaint.shader = LinearGradient(
                    barLeft, barTop,
                    barRight, barTop + 15,
                    intArrayOf(Color.parseColor("#40FFFFFF"), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(barLeft + 2, barTop, barRight - 2, barTop + 12, barCornerRadius/2, barCornerRadius/2, highlightPaint)
            }

            // Valor encima de la barra
            if (bar.value > 0 && animationProgress > 0.9f) {
                valuePaint.textSize = valueTextSize
                valuePaint.color = Color.parseColor("#7F8C8D")
                canvas.drawText("$${bar.value.toInt()}", (barLeft + barRight) / 2, barTop - 6f, valuePaint)
            }
        }
        barPaint.shader = null
        highlightPaint.shader = null
    }

    private fun drawLabels(canvas: Canvas, y: Float, usableWidth: Float) {
        textPaint.textSize = labelTextSize
        textPaint.color = Color.parseColor("#95A5A6")
        val barWidth = usableWidth / chartData.size * 0.55f
        val spacing = usableWidth / chartData.size * 0.45f

        chartData.forEachIndexed { index, bar ->
            val x = paddingLeft + index * (barWidth + spacing) + spacing / 2 + barWidth / 2
            canvas.drawText(bar.label, x, y, textPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        canvas.drawRoundRect(paddingLeft.toFloat() + 3, paddingTop.toFloat() + 3,
            width - paddingRight.toFloat() - 3, height - paddingBottom.toFloat() - 3, 12f, 12f, shadowPaint)
        canvas.drawRoundRect(paddingLeft.toFloat(), paddingTop.toFloat(),
            width - paddingRight.toFloat(), height - paddingBottom.toFloat(), 12f, 12f, bgPaint)

        textPaint.textSize = 32f
        textPaint.color = Color.parseColor("#BDC3C7")
        canvas.drawText(" Sin datos", width / 2f, height / 2f, textPaint)
    }

    private fun lightenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1] * 0.6f
        hsv[2] = hsv[2] * 1.15f
        return Color.HSVToColor(hsv)
    }
}