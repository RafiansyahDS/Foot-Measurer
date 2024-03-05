package com.rafiansyah.cameradegree

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class RoundMeterView : View {
    private var progress: Double = 0.0
    private var max: Double = 100.0
    private var progressString : String = ""
    private var progressExt: String = ""
    private val circlePaint: Paint = Paint()
    private val progressPaint: Paint = Paint()
    private val textPaint: Paint = Paint()
    private val secondaryTextPaint: Paint = Paint()
    private val textBounds: Rect = Rect()
    private var secondaryText: String = ""
    private var secondaryTextMargin: Float = 0f

    private lateinit var valueAnimator: ValueAnimator

    private var circleColor: Int = Color.LTGRAY
    private var progressColor: Int = Color.BLUE
    private var textColor: Int = Color.GRAY
    private var secondaryTextColor: Int = Color.GRAY

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeWidth = 8f

        // Progress paint settings
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = 8f
        progressPaint.strokeCap = Paint.Cap.ROUND

        // Text paint settings
        textPaint.textAlign = Paint.Align.CENTER

        // Secondary text paint settings
        secondaryTextPaint.textAlign = Paint.Align.CENTER

        // Create the ValueAnimator
        valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000 // Animation duration in milliseconds
        valueAnimator.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Float
            progress = animatedValue.toDouble() * max
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2
        val centerY = height / 2
        val radius = width / 2 - 16f
        circlePaint.color = circleColor
        // Draw the outer circle
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius, circlePaint)

        // Calculate the sweep angle based on the progress
        val sweepAngle = (progress.toFloat() / max) * 360
        progressPaint.color = progressColor

        // Draw the progress arc
        canvas.drawArc(
            16f,
            16f,
            (width - 16f),
            (height - 16f),
            -90f,
            sweepAngle.toFloat(),
            false,
            progressPaint
        )

        // Set the text size as 1/3 of the view's dimensions
        textPaint.color = textColor
        secondaryTextPaint.color = secondaryTextColor
        var textSize = Math.min(width, height) / 4f
        var secondSize = textSize / 2
        // Draw the progress text
        val progressText = progressString.ifEmpty {
            if (progressExt.isNotEmpty()){
                textSize = Math.min(width, height) / 6f
                secondSize = textSize / 1.2f
                "%.2f $progressExt".format(progress)
            }else{
                "%.2f".format(progress)
            }
        }
        textPaint.textSize = textSize
        secondaryTextPaint.textSize = secondSize

        textPaint.getTextBounds(progressText, 0, progressText.length, textBounds)
        val x = centerX.toFloat()
        val y = centerY.toFloat() + textBounds.height() / 2
        canvas.drawText(progressText, x, y, textPaint)

        // Calculate the position of the secondary text
        val secondaryTextY = y - textBounds.height() / 2 - textSize/2f - secondaryTextMargin

        // Draw the secondary text
        canvas.drawText(secondaryText, x, secondaryTextY, secondaryTextPaint)
    }

    fun setProgress(progress: Double,progressExt: String, text: String, margin: Float, color: Int) {
        if (!valueAnimator.isRunning) {
            valueAnimator.setFloatValues((this.progress / max).toFloat(), (progress / max).toFloat())
            valueAnimator.start()
        }
        this.circleColor = color
        this.progressColor = color
        this.textColor = color
        this.secondaryTextColor = color
        this.progressExt = progressExt
        this.secondaryText = text
        this.secondaryTextMargin = margin
        invalidate()
    }

    fun setProgress(progress: String, text: String, margin: Float, color: Int) {
        if (!valueAnimator.isRunning) {
            valueAnimator.setFloatValues((this.progress / max).toFloat(), (100 / max).toFloat())
            valueAnimator.start()
        }
        this.circleColor = color
        this.progressColor = color
        this.textColor = color
        this.secondaryTextColor = color
        this.progressString = progress
        this.secondaryText = text
        this.secondaryTextMargin = margin
        invalidate()
    }
}
