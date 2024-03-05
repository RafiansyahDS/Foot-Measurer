package com.rafiansyah.cameradegree.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View


class PixelGridView(context: Context, GCount: Int, attrs: AttributeSet? = null) :
    View(context, attrs) {
    //number of row and column
    var horizontalGridCount = GCount
    private val horiz: Drawable
    private val vert: Drawable
    private val width: Float

    constructor(context: Context, GCount: Int) : this(context, GCount,null) {}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        horiz.setBounds(left, 0, right, width.toInt())
        vert.setBounds(0, top, width.toInt(), bottom)
    }

    private fun getLinePosition(lineNumber: Int): Float {
        val lineCount = horizontalGridCount
        return 1f / (lineCount + 1) * (lineNumber + 1f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // drawTask.start();
        val count = horizontalGridCount
        for (n in 0 until count) {
            val pos = getLinePosition(n)

            // Draw horizontal line
            canvas.translate(0f, pos * getHeight())
            horiz.draw(canvas)
            canvas.translate(0f, -pos * getHeight())

            // Draw vertical line
            canvas.translate(pos * getWidth(), 0f)
            vert.draw(canvas)
            canvas.translate(-pos * getWidth(), 0f)
        }
        //drawTask.end(count);
    }

    init {
        horiz = ColorDrawable(Color.WHITE)
        horiz.alpha = 160
        vert = ColorDrawable(Color.WHITE)
        vert.alpha = 160
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            0.9f,
            context.getResources().getDisplayMetrics()
        )
    }
}