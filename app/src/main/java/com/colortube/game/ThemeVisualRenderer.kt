package com.colortube.game

import android.graphics.*

/**
 * MotifStyle
 *
 * Distinct decorative artwork motifs for each theme.
 */
enum class MotifStyle {
    LAB_BEAKERS,
    GEOMETRIC_GRID,
    NIGHT_FOREST,
    SUNSET_VALLEY,
    DESERT_DUSK,
    COSMIC_DEEP,
    MIDNIGHT_CITY,
    NEON_CIRCUITS,
    OCEAN_ABYSS,
    FANTASY_REALM,
    AURORA_SKY,
    CYBER_METROPOLIS,
    TWILIGHT_HORIZON
}

/**
 * ThemeVisualRenderer
 *
 * Dedicated high-performance vector renderer for theme previews and gameplay backgrounds.
 * Guarantees:
 * - 100% clipped rounded surfaces (zero square artifact leakage)
 * - Multi-layer rich dark gradients
 * - Subtle low-opacity decorative vector line-art (tubes, bubbles, stars, geometric shapes, lab symbols)
 * - Realistic glass depth, inner highlights, reflections, and borders
 */
object ThemeVisualRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clipPath = Path()
    private val shapePath = Path()
    private val starPath = Path()
    private val reflectionPath = Path()

    fun renderTheme(
        canvas: Canvas,
        bounds: RectF,
        theme: BackgroundTheme,
        isPreview: Boolean = true,
        cornerRadius: Float = 0f
    ) {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Strict Clipping to Rounded Rect / Container
        if (cornerRadius > 0f) {
            clipPath.reset()
            clipPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }

        // 2. Base Multi-Layer Dark Gradient
        val colors = theme.gradientColors
        val positions = floatArrayOf(0f, 0.52f, 1f)
        val baseGradient = LinearGradient(
            bounds.left, bounds.top,
            bounds.right, bounds.bottom,
            colors, positions,
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = baseGradient
        canvas.drawRect(bounds, bgPaint)

        // 3. Volumetric Ambient Radial Glow (Off-center soft lighting)
        val glowRadius = (w.coerceAtLeast(h) * 0.75f)
        val glowCenterX = bounds.left + w * 0.35f
        val glowCenterY = bounds.top + h * 0.30f
        val glowGradient = RadialGradient(
            glowCenterX, glowCenterY, glowRadius,
            intArrayOf(theme.accentGlow, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = glowGradient
        canvas.drawRect(bounds, glowPaint)

        // 4. Subtle Decorative Vector Line-Art Elements (Low opacity background motifs)
        val densityScale = (w / 120f).coerceIn(0.8f, 3.5f)
        linePaint.strokeWidth = (1.1f * densityScale).coerceAtLeast(1f)
        linePaint.color = Color.argb(28, 255, 255, 255) // ~11% opacity white

        accentLinePaint.strokeWidth = (1.0f * densityScale).coerceAtLeast(1f)
        accentLinePaint.color = Color.argb(38, Color.red(theme.accentGlow), Color.green(theme.accentGlow), Color.blue(theme.accentGlow))

        fillPaint.color = Color.argb(20, 255, 255, 255) // ~8% opacity white

        drawMotifs(canvas, bounds, theme.motifStyle, densityScale)

        // 5. Premium Glass Overlay Effects (for Card Preview)
        if (isPreview) {
            drawGlassEffects(canvas, bounds, cornerRadius)
        }

        canvas.restoreToCount(saveCount)
    }

    private fun drawMotifs(canvas: Canvas, b: RectF, style: MotifStyle, s: Float) {
        val left = b.left
        val top = b.top
        val w = b.width()
        val h = b.height()

        // Universal game motifs across all themes: subtle bubbles & twinkle stars
        drawBubble(canvas, left + w * 0.22f, top + h * 0.18f, 5.5f * s)
        drawBubble(canvas, left + w * 0.78f, top + h * 0.32f, 4.0f * s)
        drawBubble(canvas, left + w * 0.85f, top + h * 0.74f, 6.0f * s)
        drawBubble(canvas, left + w * 0.16f, top + h * 0.82f, 3.5f * s)

        drawSparkleStar(canvas, left + w * 0.72f, top + h * 0.14f, 7f * s)
        drawSparkleStar(canvas, left + w * 0.28f, top + h * 0.64f, 6f * s)
        drawSparkleStar(canvas, left + w * 0.82f, top + h * 0.52f, 5f * s)

        // Theme-specific line-art motifs
        when (style) {
            MotifStyle.LAB_BEAKERS -> {
                drawMiniTestTube(canvas, left + w * 0.38f, top + h * 0.26f, 14f * s, 34f * s, 0.45f)
                drawMiniErlenmeyer(canvas, left + w * 0.62f, top + h * 0.68f, 22f * s, 26f * s)
                drawAtomRings(canvas, left + w * 0.20f, top + h * 0.45f, 10f * s)
            }
            MotifStyle.GEOMETRIC_GRID -> {
                drawHexagon(canvas, left + w * 0.50f, top + h * 0.35f, 18f * s)
                drawHexagon(canvas, left + w * 0.32f, top + h * 0.72f, 13f * s)
                drawDiamond(canvas, left + w * 0.70f, top + h * 0.65f, 12f * s)
                drawGridSegment(canvas, left + w * 0.15f, top + h * 0.20f, left + w * 0.85f, top + h * 0.80f, s)
            }
            MotifStyle.NIGHT_FOREST -> {
                drawOrganicBranch(canvas, left + w * 0.20f, top + h * 0.75f, 24f * s)
                drawOrganicSpore(canvas, left + w * 0.55f, top + h * 0.40f, 9f * s)
                drawMiniTestTube(canvas, left + w * 0.70f, top + h * 0.25f, 12f * s, 28f * s, 0.6f)
            }
            MotifStyle.SUNSET_VALLEY -> {
                drawHorizonArcs(canvas, left + w * 0.50f, top + h * 0.55f, w * 0.40f, s)
                drawMiniTestTube(canvas, left + w * 0.32f, top + h * 0.30f, 13f * s, 30f * s, 0.5f)
                drawSparkleStar(canvas, left + w * 0.50f, top + h * 0.20f, 9f * s)
            }
            MotifStyle.DESERT_DUSK -> {
                drawDuneCurves(canvas, left, top + h * 0.45f, w, s)
                drawSunDisc(canvas, left + w * 0.65f, top + h * 0.25f, 12f * s)
                drawDiamond(canvas, left + w * 0.30f, top + h * 0.70f, 11f * s)
            }
            MotifStyle.COSMIC_DEEP -> {
                drawOrbitRing(canvas, left + w * 0.48f, top + h * 0.38f, 22f * s, 9f * s, -25f)
                drawConstellation(canvas, left + w * 0.30f, top + h * 0.65f, left + w * 0.70f, top + h * 0.82f, s)
                drawMiniTestTube(canvas, left + w * 0.75f, top + h * 0.60f, 11f * s, 26f * s, 0.4f)
            }
            MotifStyle.MIDNIGHT_CITY -> {
                drawSkylinePulses(canvas, left, top + h * 0.65f, w, s)
                drawHexagon(canvas, left + w * 0.60f, top + h * 0.25f, 15f * s)
                drawMiniTestTube(canvas, left + w * 0.30f, top + h * 0.40f, 12f * s, 30f * s, 0.7f)
            }
            MotifStyle.NEON_CIRCUITS -> {
                drawCircuitTracks(canvas, left, top, w, h, s)
                drawMiniTestTube(canvas, left + w * 0.50f, top + h * 0.32f, 14f * s, 32f * s, 0.5f)
                drawHexagon(canvas, left + w * 0.75f, top + h * 0.70f, 14f * s)
            }
            MotifStyle.OCEAN_ABYSS -> {
                drawBubble(canvas, left + w * 0.45f, top + h * 0.45f, 8f * s)
                drawBubble(canvas, left + w * 0.52f, top + h * 0.32f, 5f * s)
                drawWaveRipple(canvas, left + w * 0.20f, top + h * 0.62f, w * 0.6f, s)
                drawMiniTestTube(canvas, left + w * 0.68f, top + h * 0.68f, 12f * s, 28f * s, 0.35f)
            }
            MotifStyle.FANTASY_REALM -> {
                drawPotionVial(canvas, left + w * 0.45f, top + h * 0.40f, 16f * s, 28f * s)
                drawSparkleStar(canvas, left + w * 0.30f, top + h * 0.25f, 8f * s)
                drawOrbitRing(canvas, left + w * 0.65f, top + h * 0.70f, 16f * s, 7f * s, 35f)
            }
            MotifStyle.AURORA_SKY -> {
                drawAuroraCurtains(canvas, left, top + h * 0.30f, w, s)
                drawSparkleStar(canvas, left + w * 0.55f, top + h * 0.18f, 8f * s)
                drawMiniTestTube(canvas, left + w * 0.35f, top + h * 0.65f, 13f * s, 30f * s, 0.55f)
            }
            MotifStyle.CYBER_METROPOLIS -> {
                drawHexagon(canvas, left + w * 0.40f, top + h * 0.30f, 16f * s)
                drawHexagon(canvas, left + w * 0.62f, top + h * 0.42f, 11f * s)
                drawCircuitTracks(canvas, left, top + h * 0.5f, w, h * 0.5f, s)
            }
            MotifStyle.TWILIGHT_HORIZON -> {
                drawHorizonArcs(canvas, left + w * 0.50f, top + h * 0.50f, w * 0.45f, s)
                drawMiniTestTube(canvas, left + w * 0.65f, top + h * 0.35f, 13f * s, 32f * s, 0.5f)
                drawDiamond(canvas, left + w * 0.30f, top + h * 0.32f, 10f * s)
            }
        }
    }

    private fun drawMiniTestTube(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float, fluidLevel: Float) {
        shapePath.reset()
        val radius = width / 2f
        val top = cy - height / 2f
        val bottom = cy + height / 2f
        val left = cx - radius
        val right = cx + radius

        // Tube body with rounded bottom
        shapePath.moveTo(left, top)
        shapePath.lineTo(left, bottom - radius)
        shapePath.arcTo(left, bottom - 2 * radius, right, bottom, 180f, -180f, false)
        shapePath.lineTo(right, top)
        canvas.drawPath(shapePath, linePaint)

        // Tube lip / rim
        canvas.drawOval(left - 1.5f, top - 2.5f, right + 1.5f, top + 2.5f, linePaint)

        // Fluid meniscus line
        val fluidY = bottom - height * fluidLevel
        if (fluidY > top + 4f && fluidY < bottom - 4f) {
            canvas.drawLine(left + 2f, fluidY, right - 2f, fluidY, accentLinePaint)
            // Soft fluid fill
            val fluidPath = Path()
            fluidPath.moveTo(left + 1f, fluidY)
            fluidPath.lineTo(left + 1f, bottom - radius)
            fluidPath.arcTo(left + 1f, bottom - 2 * radius + 1f, right - 1f, bottom - 1f, 180f, -180f, false)
            fluidPath.lineTo(right - 1f, fluidY)
            fluidPath.close()
            canvas.drawPath(fluidPath, fillPaint)
        }
    }

    private fun drawMiniErlenmeyer(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        shapePath.reset()
        val top = cy - h / 2f
        val bottom = cy + h / 2f
        val neckW = w * 0.32f
        val neckBottom = top + h * 0.35f

        shapePath.moveTo(cx - neckW / 2, top)
        shapePath.lineTo(cx - neckW / 2, neckBottom)
        shapePath.lineTo(cx - w / 2, bottom - 3f)
        shapePath.quadTo(cx - w / 2, bottom, cx - w / 2 + 3f, bottom)
        shapePath.lineTo(cx + w / 2 - 3f, bottom)
        shapePath.quadTo(cx + w / 2, bottom, cx + w / 2, bottom - 3f)
        shapePath.lineTo(cx + neckW / 2, neckBottom)
        shapePath.lineTo(cx + neckW / 2, top)
        shapePath.close()
        canvas.drawPath(shapePath, linePaint)

        // Fluid fill line
        val fluidY = bottom - h * 0.35f
        canvas.drawLine(cx - w * 0.35f, fluidY, cx + w * 0.35f, fluidY, accentLinePaint)
    }

    private fun drawPotionVial(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        shapePath.reset()
        val top = cy - h / 2f
        val neckW = w * 0.35f
        val neckBottom = top + h * 0.30f
        val bulbRadius = w / 2f
        val bulbCenterY = top + h * 0.65f

        shapePath.moveTo(cx - neckW / 2, top)
        shapePath.lineTo(cx - neckW / 2, neckBottom)
        shapePath.addCircle(cx, bulbCenterY, bulbRadius, Path.Direction.CW)
        canvas.drawPath(shapePath, linePaint)
        canvas.drawOval(cx - neckW / 2 - 1f, top - 2f, cx + neckW / 2 + 1f, top + 2f, linePaint)
    }

    private fun drawBubble(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, linePaint)
        // Specular highlight dot
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, (r * 0.28f).coerceAtLeast(0.8f), fillPaint)
    }

    private fun drawSparkleStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        starPath.reset()
        val c = r * 0.18f
        starPath.moveTo(cx, cy - r)
        starPath.quadTo(cx, cy, cx + r, cy)
        starPath.quadTo(cx, cy, cx, cy + r)
        starPath.quadTo(cx, cy, cx - r, cy)
        starPath.quadTo(cx, cy, cx, cy - r)
        starPath.close()
        canvas.drawPath(starPath, fillPaint)
        canvas.drawPath(starPath, accentLinePaint)
        canvas.drawCircle(cx, cy, c, fillPaint)
    }

    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        shapePath.reset()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            val x = (cx + r * Math.cos(angle)).toFloat()
            val y = (cy + r * Math.sin(angle)).toFloat()
            if (i == 0) shapePath.moveTo(x, y) else shapePath.lineTo(x, y)
        }
        shapePath.close()
        canvas.drawPath(shapePath, linePaint)
    }

    private fun drawDiamond(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        shapePath.reset()
        shapePath.moveTo(cx, cy - r)
        shapePath.lineTo(cx + r * 0.7f, cy)
        shapePath.lineTo(cx, cy + r)
        shapePath.lineTo(cx - r * 0.7f, cy)
        shapePath.close()
        canvas.drawPath(shapePath, linePaint)
    }

    private fun drawAtomRings(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.save()
        canvas.rotate(35f, cx, cy)
        canvas.drawOval(cx - r, cy - r * 0.4f, cx + r, cy + r * 0.4f, linePaint)
        canvas.rotate(70f, cx, cy)
        canvas.drawOval(cx - r, cy - r * 0.4f, cx + r, cy + r * 0.4f, linePaint)
        canvas.restore()
        canvas.drawCircle(cx, cy, 1.8f, fillPaint)
    }

    private fun drawOrbitRing(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, angle: Float) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, linePaint)
        canvas.restore()
        canvas.drawCircle(cx, cy, 2.0f, fillPaint)
    }

    private fun drawConstellation(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, s: Float) {
        val mx = (x1 + x2) / 2f + 8f * s
        val my = (y1 + y2) / 2f - 10f * s
        canvas.drawLine(x1, y1, mx, my, linePaint)
        canvas.drawLine(mx, my, x2, y2, linePaint)
        canvas.drawCircle(x1, y1, 2f * s, fillPaint)
        canvas.drawCircle(mx, my, 2.5f * s, fillPaint)
        canvas.drawCircle(x2, y2, 2f * s, fillPaint)
    }

    private fun drawOrganicBranch(canvas: Canvas, x: Float, y: Float, len: Float) {
        shapePath.reset()
        shapePath.moveTo(x, y)
        shapePath.quadTo(x + len * 0.4f, y - len * 0.2f, x + len, y - len * 0.6f)
        canvas.drawPath(shapePath, linePaint)
        canvas.drawCircle(x + len, y - len * 0.6f, 2.2f, fillPaint)
    }

    private fun drawOrganicSpore(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, accentLinePaint)
        canvas.drawCircle(cx, cy, r * 0.35f, fillPaint)
    }

    private fun drawHorizonArcs(canvas: Canvas, cx: Float, cy: Float, radius: Float, s: Float) {
        canvas.drawArc(cx - radius, cy - radius * 0.4f, cx + radius, cy + radius * 0.4f, 190f, 160f, false, linePaint)
        canvas.drawArc(cx - radius * 0.7f, cy - radius * 0.25f, cx + radius * 0.7f, cy + radius * 0.25f, 190f, 160f, false, accentLinePaint)
    }

    private fun drawDuneCurves(canvas: Canvas, left: Float, y: Float, w: Float, s: Float) {
        shapePath.reset()
        shapePath.moveTo(left, y)
        shapePath.cubicTo(left + w * 0.35f, y - 12f * s, left + w * 0.65f, y + 14f * s, left + w, y)
        canvas.drawPath(shapePath, linePaint)
    }

    private fun drawSunDisc(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, accentLinePaint)
        canvas.drawCircle(cx, cy, r * 0.5f, fillPaint)
    }

    private fun drawSkylinePulses(canvas: Canvas, left: Float, y: Float, w: Float, s: Float) {
        shapePath.reset()
        val step = w / 5f
        for (i in 0 until 5) {
            val h = ((i % 3) + 1) * 8f * s
            canvas.drawLine(left + i * step + 4f, y, left + i * step + 4f, y - h, linePaint)
            canvas.drawCircle(left + i * step + 4f, y - h, 1.8f * s, fillPaint)
        }
    }

    private fun drawCircuitTracks(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, s: Float) {
        shapePath.reset()
        shapePath.moveTo(left + w * 0.1f, top + h * 0.2f)
        shapePath.lineTo(left + w * 0.4f, top + h * 0.2f)
        shapePath.lineTo(left + w * 0.6f, top + h * 0.4f)
        shapePath.lineTo(left + w * 0.9f, top + h * 0.4f)
        canvas.drawPath(shapePath, linePaint)
        canvas.drawCircle(left + w * 0.9f, top + h * 0.4f, 2.2f * s, fillPaint)
    }

    private fun drawWaveRipple(canvas: Canvas, left: Float, y: Float, w: Float, s: Float) {
        shapePath.reset()
        shapePath.moveTo(left, y)
        shapePath.quadTo(left + w * 0.25f, y - 6f * s, left + w * 0.5f, y)
        shapePath.quadTo(left + w * 0.75f, y + 6f * s, left + w, y)
        canvas.drawPath(shapePath, linePaint)
    }

    private fun drawAuroraCurtains(canvas: Canvas, left: Float, y: Float, w: Float, s: Float) {
        shapePath.reset()
        shapePath.moveTo(left, y)
        shapePath.cubicTo(left + w * 0.3f, y - 16f * s, left + w * 0.7f, y + 12f * s, left + w, y - 8f * s)
        canvas.drawPath(shapePath, accentLinePaint)
    }

    private fun drawGridSegment(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, s: Float) {
        canvas.drawLine(x1, y1, x2, y2, accentLinePaint)
    }

    /**
     * Premium Glass Overlays:
     * - Top-edge subtle white vignette
     * - Diagonal curved reflection beam
     * - Soft inner perimeter border stroke
     */
    private fun drawGlassEffects(canvas: Canvas, b: RectF, cornerRadius: Float) {
        val w = b.width()
        val h = b.height()

        // 1. Soft Top Inner Lighting / Highlight
        val topHighlight = LinearGradient(
            b.left, b.top,
            b.left, b.top + h * 0.35f,
            intArrayOf(Color.argb(45, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glassPaint.shader = topHighlight
        canvas.drawRect(b, glassPaint)

        // 2. Realistic Diagonal Glass Reflection Arc
        reflectionPath.reset()
        reflectionPath.moveTo(b.left, b.top)
        reflectionPath.lineTo(b.right, b.top)
        reflectionPath.lineTo(b.right, b.top + h * 0.18f)
        reflectionPath.cubicTo(
            b.left + w * 0.75f, b.top + h * 0.22f,
            b.left + w * 0.30f, b.top + h * 0.48f,
            b.left, b.top + h * 0.52f
        )
        reflectionPath.close()

        val reflectionGradient = LinearGradient(
            b.left, b.top,
            b.right, b.top + h * 0.40f,
            intArrayOf(Color.argb(32, 255, 255, 255), Color.argb(8, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        glassPaint.shader = reflectionGradient
        canvas.drawPath(reflectionPath, glassPaint)

        // 3. Subtle Glass Outer Border Stroke
        val strokeInset = 0.75f
        val strokeBounds = RectF(
            b.left + strokeInset,
            b.top + strokeInset,
            b.right - strokeInset,
            b.bottom - strokeInset
        )
        val borderGradient = LinearGradient(
            b.left, b.top,
            b.right, b.bottom,
            intArrayOf(
                Color.argb(75, 255, 255, 255), // Top-left catchlight
                Color.argb(25, 255, 255, 255),
                Color.argb(12, 255, 255, 255)  // Bottom-right softer edge
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        borderPaint.shader = borderGradient
        borderPaint.strokeWidth = 1.3f
        canvas.drawRoundRect(strokeBounds, cornerRadius, cornerRadius, borderPaint)
    }
}
