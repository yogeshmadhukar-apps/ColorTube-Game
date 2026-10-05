package com.colortube.game

import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * CollectionCardDrawable
 *
 * Procedural premium glass card background for Collection items.
 * Features:
 * - 20dp smooth rounded card shape
 * - Rich subtle navy/indigo gradient
 * - Ambient soft radial glow behind the floating collectible (zero hard square edges)
 * - Low-opacity decorative background sparkles & line-art
 * - Delicate specular glass reflection arc
 * - Refined semi-transparent glass border stroke
 */
class CollectionCardDrawable : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(12, 255, 255, 255)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private val clipPath = Path()
    private val starPath = Path()
    private val reflectionPath = Path()
    private val boundsF = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        boundsF.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        val cornerRadius = 20f * (canvas.density / 160f).coerceAtLeast(1f)

        val saveCount = canvas.save()

        // 1. Strict rounded container clipping
        clipPath.reset()
        clipPath.addRoundRect(boundsF, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // 2. Rich subtle navy/indigo base gradient
        bgPaint.shader = LinearGradient(
            boundsF.left, boundsF.top,
            boundsF.right, boundsF.bottom,
            intArrayOf(
                Color.parseColor("#1A2438"), // Deep indigo navy
                Color.parseColor("#131B2C"), // Mid dark indigo
                Color.parseColor("#0C121E")  // Shadow base
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(boundsF, bgPaint)

        // 3. Soft ambient radial glow behind the collectible (centered in upper showcase area)
        val glowCenterX = boundsF.centerX()
        val glowCenterY = boundsF.top + boundsF.height() * 0.38f
        val glowRadius = boundsF.width() * 0.58f

        glowPaint.shader = RadialGradient(
            glowCenterX, glowCenterY, glowRadius,
            intArrayOf(
                Color.argb(32, 0, 210, 211), // Brand cyan ambient glow
                Color.argb(14, 79, 70, 229), // Indigo hue
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(glowCenterX, glowCenterY, glowRadius, glowPaint)

        // 4. Low-opacity decorative background elements (sparkles & geometric lines)
        val sparkleX1 = boundsF.left + boundsF.width() * 0.82f
        val sparkleY1 = boundsF.top + boundsF.height() * 0.16f
        drawSparkle(canvas, sparkleX1, sparkleY1, 5f)

        val sparkleX2 = boundsF.left + boundsF.width() * 0.18f
        val sparkleY2 = boundsF.top + boundsF.height() * 0.55f
        drawSparkle(canvas, sparkleX2, sparkleY2, 3.5f)

        // Faint ambient orbital rings
        canvas.drawCircle(boundsF.centerX(), boundsF.top + boundsF.height() * 0.40f, boundsF.width() * 0.42f, linePaint)

        // 5. Delicate specular glass reflection arc across top corner
        reflectionPath.reset()
        reflectionPath.moveTo(boundsF.left, boundsF.top + boundsF.height() * 0.32f)
        reflectionPath.cubicTo(
            boundsF.left + boundsF.width() * 0.25f, boundsF.top + boundsF.height() * 0.08f,
            boundsF.left + boundsF.width() * 0.65f, boundsF.top + 4f,
            boundsF.right, boundsF.top + 14f
        )
        reflectionPath.lineTo(boundsF.right, boundsF.top)
        reflectionPath.lineTo(boundsF.left, boundsF.top)
        reflectionPath.close()

        highlightPaint.shader = LinearGradient(
            boundsF.left, boundsF.top,
            boundsF.left, boundsF.top + boundsF.height() * 0.32f,
            intArrayOf(
                Color.argb(22, 255, 255, 255),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(reflectionPath, highlightPaint)

        // 6. Premium Glass Border Stroke
        borderPaint.shader = LinearGradient(
            boundsF.left, boundsF.top,
            boundsF.right, boundsF.bottom,
            intArrayOf(
                Color.argb(70, 255, 255, 255), // Top bright reflection
                Color.argb(28, 255, 255, 255),
                Color.argb(16, 255, 255, 255)  // Bottom soft border
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(boundsF, cornerRadius, cornerRadius, borderPaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        starPath.reset()
        starPath.moveTo(cx, cy - size)
        starPath.quadTo(cx, cy, cx + size, cy)
        starPath.quadTo(cx, cy, cx, cy + size)
        starPath.quadTo(cx, cy, cx - size, cy)
        starPath.quadTo(cx, cy, cx, cy - size)
        canvas.drawPath(starPath, linePaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
