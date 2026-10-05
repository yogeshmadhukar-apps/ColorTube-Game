package com.colortube.game

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * ThemeBackgroundDrawable
 *
 * Full-screen dynamic procedural drawable that renders equipped background themes
 * with multi-layer dark gradients and ambient subtle decorative motifs.
 */
class ThemeBackgroundDrawable(
    var theme: BackgroundTheme = BackgroundTheme.DARK_LAB
) : Drawable() {

    private val boundsF = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        boundsF.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        ThemeVisualRenderer.renderTheme(
            canvas = canvas,
            bounds = boundsF,
            theme = theme,
            isPreview = false,
            cornerRadius = 0f
        )
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
