package com.colortube.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * ThemePreviewView
 *
 * Custom View dedicated to rendering premium rounded glass theme preview cards.
 * Ensures:
 * - Perfect rounded clipping with zero pixel bleed or square container artifacts
 * - Multi-layer rich dark gradients
 * - Subtle low-opacity decorative vector line-art (tubes, bubbles, stars, geometric shapes, lab flasks)
 * - Soft inner highlight, diagonal reflection arc, and glass borders
 */
class ThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var theme: BackgroundTheme = BackgroundTheme.DARK_LAB
        set(value) {
            field = value
            invalidate()
        }

    private val bounds = RectF()
    private val cornerRadiusDp = 18f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        bounds.set(0f, 0f, w, h)
        val cornerRadius = cornerRadiusDp * resources.displayMetrics.density

        ThemeVisualRenderer.renderTheme(
            canvas = canvas,
            bounds = bounds,
            theme = theme,
            isPreview = true,
            cornerRadius = cornerRadius
        )
    }
}
