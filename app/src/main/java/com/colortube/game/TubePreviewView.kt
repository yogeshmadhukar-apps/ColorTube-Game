package com.colortube.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * TubePreviewView
 *
 * Dedicated showcase view for collectible tubes in the Collection screen.
 * Implements:
 * - 100% transparent background (zero square box or rectangular container artifacts)
 * - Properly scaled and centered collectible container proportions (never stretched or bloated)
 * - Genuinely unique outer silhouettes, necks, rims, bodies, and bases for all 16 skins
 * - Soft localized circular ambient glow behind the tube
 * - Multi-layer liquid fill clipped inside the custom glass container
 * - Realistic glass specular highlights (key streak, ambient bounce, bottom caustic)
 * - Dimmed/desaturated alpha support for locked milestone items
 */
class TubePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var skin: TubeSkin = TubeSkin.CLASSIC_TUBE
        set(value) {
            field = value
            invalidate()
        }

    var colorPalette: ColorPalette = ColorPalette.VIBRANT_NEON
        set(value) {
            field = value
            invalidate()
        }

    var isLocked: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val tubeBounds = RectF()
    private val tubePath = Path()
    private val liquidClipPath = Path()
    private val leftHighlight = RectF()
    private val rightHighlight = RectF()
    private val lipRect = RectF()
    private val extraDetailPath = Path()

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        color = Color.argb(190, 255, 255, 255)
    }
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val liquidDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 0, 0, 0)
    }
    private val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specularLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specularRightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(160, 255, 255, 255)
    }
    private val detailLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(120, 255, 255, 255)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Proportions: Maintain slender, elegant collectible proportions centered in the card
        val targetHeight = h * 0.84f
        val maxTargetWidth = min(w * 0.48f, targetHeight * 0.42f)
        val tubeWidth = maxTargetWidth
        val left = (w - tubeWidth) / 2f
        val right = left + tubeWidth
        val top = (h - targetHeight) / 2f + (h * 0.04f)
        val bottom = top + targetHeight

        tubeBounds.set(left, top, right, bottom)

        val saveCount = canvas.save()

        // Dim if locked
        if (isLocked) {
            canvas.saveLayerAlpha(0f, 0f, w, h, 115) // ~45% opacity
        }

        // 2. Soft Ambient Glow Behind the Actual Tube Only (Zero square box edges)
        val glowRadius = tubeBounds.height() * 0.46f
        val glowCenterY = tubeBounds.top + tubeBounds.height() * 0.52f
        glowPaint.shader = RadialGradient(
            tubeBounds.centerX(), glowCenterY, glowRadius,
            intArrayOf(
                Color.argb(48, 0, 210, 211), // Cyan core
                Color.argb(20, 79, 70, 229), // Indigo fringe
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(tubeBounds.centerX(), glowCenterY, glowRadius, glowPaint)

        // 3. Build Unique Outer Silhouette and Inner Liquid Clip Path for all 16 Skins
        buildPathsForSkin(skin, tubeBounds)

        // 4. Glass Vessel Body Fill
        glassBodyPaint.shader = LinearGradient(
            tubeBounds.left, tubeBounds.top,
            tubeBounds.right, tubeBounds.bottom,
            intArrayOf(
                Color.argb(55, 255, 255, 255),
                Color.argb(16, 230, 245, 255),
                Color.argb(22, 210, 240, 250),
                Color.argb(50, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.35f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(tubePath, glassBodyPaint)

        // 5. Liquid Layers Inside Vessel (Clipped strictly to liquidClipPath)
        val liquidSave = canvas.save()
        canvas.clipPath(liquidClipPath)

        val demoColors = listOf(
            LiquidColor.AMBER,
            LiquidColor.CORAL,
            LiquidColor.CYAN,
            LiquidColor.LIME
        )

        val layerHeight = tubeBounds.height() * 0.20f
        var currentBottom = tubeBounds.bottom - 2.5f

        for (i in 0 until 4) {
            val colorEnum = demoColors[i]
            val layerTop = currentBottom - layerHeight

            val (topCol, bottomCol) = if (colorPalette == ColorPalette.VIBRANT_NEON) {
                Pair(colorEnum.topColor, colorEnum.bottomColor)
            } else {
                colorPalette.mapColor(colorEnum)
            }

            liquidPaint.shader = LinearGradient(
                tubeBounds.left, layerTop,
                tubeBounds.left, currentBottom,
                topCol, bottomCol,
                Shader.TileMode.CLAMP
            )

            canvas.drawRect(
                tubeBounds.left - 4f, layerTop,
                tubeBounds.right + 4f, currentBottom,
                liquidPaint
            )

            // Divider line between layers
            if (i < 3) {
                canvas.drawRect(
                    tubeBounds.left - 4f, layerTop - 0.75f,
                    tubeBounds.right + 4f, layerTop + 0.75f,
                    liquidDividerPaint
                )
            }

            currentBottom = layerTop
        }

        // Top surface meniscus sheen
        val meniscusTop = currentBottom
        meniscusPaint.shader = LinearGradient(
            tubeBounds.left, meniscusTop,
            tubeBounds.right, meniscusTop,
            intArrayOf(
                Color.argb(60, 255, 255, 255),
                Color.argb(210, 255, 255, 255),
                Color.argb(60, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(tubeBounds.left - 4f, meniscusTop, tubeBounds.right + 4f, meniscusTop + 2f, meniscusPaint)

        canvas.restoreToCount(liquidSave)

        // 6. Crisp Outer Glass Border
        canvas.drawPath(tubePath, glassBorderPaint)

        // Extra details (handles, facet lines) if present
        if (!extraDetailPath.isEmpty) {
            canvas.drawPath(extraDetailPath, detailLinePaint)
        }

        // 7. Specular Glass Highlights (Left key streak & right bounce)
        val highlightSave = canvas.save()
        canvas.clipPath(tubePath)

        leftHighlight.set(
            tubeBounds.left + 2.5f,
            tubeBounds.top + 5f,
            tubeBounds.left + 5.5f,
            tubeBounds.bottom - 8f
        )
        specularLeftPaint.shader = LinearGradient(
            leftHighlight.left, leftHighlight.top,
            leftHighlight.left, leftHighlight.bottom,
            intArrayOf(
                Color.argb(220, 255, 255, 255),
                Color.argb(140, 255, 255, 255),
                Color.argb(35, 255, 255, 255),
                Color.argb(160, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.45f, 0.80f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(leftHighlight, 1.5f, 1.5f, specularLeftPaint)

        rightHighlight.set(
            tubeBounds.right - 4.5f,
            tubeBounds.top + 8f,
            tubeBounds.right - 2.5f,
            tubeBounds.bottom - 12f
        )
        specularRightPaint.shader = LinearGradient(
            rightHighlight.left, rightHighlight.top,
            rightHighlight.left, rightHighlight.bottom,
            intArrayOf(
                Color.argb(130, 255, 255, 255),
                Color.argb(28, 255, 255, 255),
                Color.argb(110, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rightHighlight, 0.75f, 0.75f, specularRightPaint)

        canvas.restoreToCount(highlightSave)

        // 8. Matching Glass Lip / Rim Collar precisely sized to the neck
        lipPaint.shader = LinearGradient(
            lipRect.left, lipRect.top,
            lipRect.left, lipRect.bottom,
            intArrayOf(
                Color.argb(240, 255, 255, 255),
                Color.argb(130, 240, 250, 255),
                Color.argb(150, 190, 220, 230),
                Color.argb(230, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.45f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(lipRect, 3f, 3f, lipPaint)
        canvas.drawRoundRect(lipRect, 3f, 3f, lipBorderPaint)

        if (isLocked) {
            canvas.restoreToCount(saveCount) // Restore alpha layer
        }
    }

    /**
     * Builds distinct outer silhouette and inner liquid cavity for each of the 16 skins.
     */
    private fun buildPathsForSkin(skin: TubeSkin, b: RectF) {
        val l = b.left
        val r = b.right
        val t = b.top
        val bot = b.bottom
        val w = b.width()
        val h = b.height()
        val cx = b.centerX()

        tubePath.reset()
        liquidClipPath.reset()
        extraDetailPath.reset()

        when (skin) {
            TubeSkin.CLASSIC_TUBE -> {
                // 1. Classic Glass (Level 1): Slender cylindrical lab tube with hemispherical base
                val radius = w / 2f
                tubePath.moveTo(l, t)
                tubePath.lineTo(r, t)
                tubePath.lineTo(r, bot - radius)
                tubePath.arcTo(RectF(l, bot - w, r, bot), 0f, 180f, false)
                tubePath.lineTo(l, t)
                tubePath.close()

                lipRect.set(l - 2.5f, t - 2.5f, r + 2.5f, t + 3.5f)

                val iw = w - 5f
                val il = l + 2.5f
                val ir = r - 2.5f
                val ibot = bot - 2.5f
                val iradius = iw / 2f
                liquidClipPath.moveTo(il, t + 2f)
                liquidClipPath.lineTo(ir, t + 2f)
                liquidClipPath.lineTo(ir, ibot - iradius)
                liquidClipPath.arcTo(RectF(il, ibot - iw, ir, ibot), 0f, 180f, false)
                liquidClipPath.lineTo(il, t + 2f)
                liquidClipPath.close()
            }

            TubeSkin.FLASK_BOTTLE -> {
                // 2. Chemist Flask (Level 10): Erlenmeyer flask with narrow neck & flared triangular base
                val neckW = w * 0.38f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val neckBottom = t + h * 0.30f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, neckBottom)
                tubePath.lineTo(r - 1f, bot - 8f)
                tubePath.quadTo(r, bot, r - 8f, bot)
                tubePath.lineTo(l + 8f, bot)
                tubePath.quadTo(l, bot, l + 1f, bot - 8f)
                tubePath.lineTo(neckLeft, neckBottom)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                val inNeckLeft = neckLeft + 2.5f
                val inNeckRight = neckRight - 2.5f
                liquidClipPath.moveTo(inNeckLeft, t + 2f)
                liquidClipPath.lineTo(inNeckRight, t + 2f)
                liquidClipPath.lineTo(inNeckRight, neckBottom)
                liquidClipPath.lineTo(r - 3f, bot - 8f)
                liquidClipPath.quadTo(r - 2.5f, bot - 2.5f, r - 8f, bot - 2.5f)
                liquidClipPath.lineTo(l + 8f, bot - 2.5f)
                liquidClipPath.quadTo(l + 2.5f, bot - 2.5f, l + 3f, bot - 8f)
                liquidClipPath.lineTo(inNeckLeft, neckBottom)
                liquidClipPath.close()
            }

            TubeSkin.SLENDER_VIAL -> {
                // 3. Slender Vial (Level 30): Perfume vial with curved cinched waist
                val neckW = w * 0.46f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val waistY = t + h * 0.48f
                val waistInset = w * 0.16f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.cubicTo(neckRight + 4f, t + h * 0.15f, cx + w * 0.36f - waistInset, waistY - 8f, cx + w * 0.36f - waistInset, waistY)
                tubePath.cubicTo(cx + w * 0.36f - waistInset, waistY + 8f, r, bot - 18f, r, bot - w * 0.35f)
                tubePath.arcTo(RectF(l, bot - w * 0.7f, r, bot), 0f, 180f, false)
                tubePath.cubicTo(l, bot - 18f, cx - w * 0.36f + waistInset, waistY + 8f, cx - w * 0.36f + waistInset, waistY)
                tubePath.cubicTo(cx - w * 0.36f + waistInset, waistY - 8f, neckLeft - 4f, t + h * 0.15f, neckLeft, t)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.CHAMPAGNE_FLUTE -> {
                // 4. Crystal Flute (Level 50): Flared rim, conical bowl tapering to crystal pedestal
                val rimW = w * 0.90f
                val rimLeft = cx - rimW / 2f
                val rimRight = cx + rimW / 2f
                val stemY = t + h * 0.78f
                val stemW = w * 0.30f
                val stemLeft = cx - stemW / 2f
                val stemRight = cx + stemW / 2f

                tubePath.moveTo(rimLeft, t)
                tubePath.lineTo(rimRight, t)
                tubePath.lineTo(stemRight, stemY)
                tubePath.lineTo(r - 2f, bot - 4f)
                tubePath.lineTo(r - 2f, bot)
                tubePath.lineTo(l + 2f, bot)
                tubePath.lineTo(l + 2f, bot - 4f)
                tubePath.lineTo(stemLeft, stemY)
                tubePath.lineTo(rimLeft, t)
                tubePath.close()

                lipRect.set(rimLeft - 2f, t - 2f, rimRight + 2f, t + 3.5f)

                // Crystal facet line
                extraDetailPath.moveTo(stemLeft, stemY)
                extraDetailPath.lineTo(stemRight, stemY)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.BULB_POTION -> {
                // 5. Alchemist Bulb (Level 70): Slender neck opening into a round spherical bulb
                val neckW = w * 0.34f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val neckBottom = t + h * 0.34f
                val bulbRadius = w / 2f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, neckBottom)
                val bulbRect = RectF(l, bot - w, r, bot)
                tubePath.arcTo(bulbRect, -50f, 280f, false)
                tubePath.lineTo(neckLeft, neckBottom)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.SQUARE_CARAFE -> {
                // 6. Square Decanter (Level 90): Sharp horizontal beveled shoulders & thick heavy base
                val neckW = w * 0.40f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val shoulderY = t + h * 0.22f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, shoulderY - 4f)
                tubePath.lineTo(r - 3f, shoulderY)
                tubePath.lineTo(r - 1f, bot - 6f)
                tubePath.quadTo(r, bot, r - 6f, bot)
                tubePath.lineTo(l + 6f, bot)
                tubePath.quadTo(l, bot, l + 1f, bot - 6f)
                tubePath.lineTo(l + 3f, shoulderY)
                tubePath.lineTo(neckLeft, shoulderY - 4f)
                tubePath.close()

                lipRect.set(neckLeft - 3f, t - 2.5f, neckRight + 3f, t + 3.5f)

                // Thick base horizontal line
                extraDetailPath.moveTo(l + 4f, bot - 8f)
                extraDetailPath.lineTo(r - 4f, bot - 8f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.NARROW_DROPOP -> {
                // 7. Narrow Droplet (Level 120): Slender top expanding into sweeping teardrop bulb
                val neckW = w * 0.28f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val dropRadius = w / 2f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.cubicTo(neckRight + 2f, t + h * 0.35f, r, bot - dropRadius - 10f, r, bot - dropRadius)
                tubePath.arcTo(RectF(l, bot - w, r, bot), 0f, 180f, false)
                tubePath.cubicTo(l, bot - dropRadius - 10f, neckLeft - 2f, t + h * 0.35f, neckLeft, t)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.WIDE_TUMBLER -> {
                // 8. Apothecary Jar (Level 160): Stepped shoulder collar, straight wide jar, flat heavy base
                val neckW = w * 0.74f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val stepY = t + h * 0.14f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, stepY)
                tubePath.lineTo(r - 2f, stepY + 4f)
                tubePath.lineTo(r - 2f, bot - 8f)
                tubePath.quadTo(r - 2f, bot, r - 8f, bot)
                tubePath.lineTo(l + 8f, bot)
                tubePath.quadTo(l + 2f, bot, l + 2f, bot - 8f)
                tubePath.lineTo(l + 2f, stepY + 4f)
                tubePath.lineTo(neckLeft, stepY)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                extraDetailPath.moveTo(l + 3f, stepY + 4f)
                extraDetailPath.lineTo(r - 3f, stepY + 4f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.HOURGLASS_VASE -> {
                // 9. Hourglass Vase (Level 210): Flared top, narrow cinched waist, matching flared base
                val rimW = w * 0.84f
                val rimLeft = cx - rimW / 2f
                val rimRight = cx + rimW / 2f
                val waistY = t + h * 0.50f
                val waistW = w * 0.38f
                val waistLeft = cx - waistW / 2f
                val waistRight = cx + waistW / 2f

                tubePath.moveTo(rimLeft, t)
                tubePath.lineTo(rimRight, t)
                tubePath.quadTo(waistRight, waistY - 10f, waistRight, waistY)
                tubePath.quadTo(waistRight, waistY + 10f, r - 3f, bot - 10f)
                tubePath.quadTo(r, bot, r - 8f, bot)
                tubePath.lineTo(l + 8f, bot)
                tubePath.quadTo(l, bot, l + 3f, bot - 10f)
                tubePath.quadTo(waistLeft, waistY + 10f, waistLeft, waistY)
                tubePath.quadTo(waistLeft, waistY - 10f, rimLeft, t)
                tubePath.close()

                lipRect.set(rimLeft - 2f, t - 2.5f, rimRight + 2f, t + 3.5f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.FANTASY_ELIXIR -> {
                // 10. Fantasy Elixir (Level 280): Sharp diamond faceted shoulders & jewel cut bottom
                val neckW = w * 0.36f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val diamondY = t + h * 0.40f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, t + h * 0.20f)
                tubePath.lineTo(r - 1f, diamondY)
                tubePath.lineTo(cx + w * 0.20f, bot - 10f)
                tubePath.lineTo(cx, bot)
                tubePath.lineTo(cx - w * 0.20f, bot - 10f)
                tubePath.lineTo(l + 1f, diamondY)
                tubePath.lineTo(neckLeft, t + h * 0.20f)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                // Diamond facet lines
                extraDetailPath.moveTo(neckLeft, t + h * 0.20f)
                extraDetailPath.lineTo(cx, diamondY)
                extraDetailPath.lineTo(neckRight, t + h * 0.20f)
                extraDetailPath.moveTo(cx, diamondY)
                extraDetailPath.lineTo(cx, bot)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.OCTAGON_PRISM -> {
                // 11. Octagon Prism (Level 360): Chamfered diagonal corners & vertical facet prism
                val cornerCut = w * 0.22f
                val topY = t + 4f
                val botY = bot - 2f

                tubePath.moveTo(l + cornerCut, topY)
                tubePath.lineTo(r - cornerCut, topY)
                tubePath.lineTo(r - 2f, topY + cornerCut)
                tubePath.lineTo(r - 2f, botY - cornerCut)
                tubePath.lineTo(r - cornerCut, botY)
                tubePath.lineTo(l + cornerCut, botY)
                tubePath.lineTo(l + 2f, botY - cornerCut)
                tubePath.lineTo(l + 2f, topY + cornerCut)
                tubePath.close()

                lipRect.set(l + cornerCut - 2f, t - 2f, r - cornerCut + 2f, t + 4f)

                // Vertical prism lines
                extraDetailPath.moveTo(l + cornerCut, topY)
                extraDetailPath.lineTo(l + cornerCut, botY)
                extraDetailPath.moveTo(r - cornerCut, topY)
                extraDetailPath.lineTo(r - cornerCut, botY)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.TALL_CHALICE -> {
                // 12. Tall Chalice (Level 450): Goblet bowl tapering into slender stem & wide circular foot
                val rimW = w * 0.86f
                val rimLeft = cx - rimW / 2f
                val rimRight = cx + rimW / 2f
                val bowlBottom = t + h * 0.54f
                val stemW = w * 0.22f
                val stemLeft = cx - stemW / 2f
                val stemRight = cx + stemW / 2f
                val footW = w * 0.78f

                tubePath.moveTo(rimLeft, t)
                tubePath.lineTo(rimRight, t)
                tubePath.cubicTo(rimRight, t + h * 0.35f, stemRight + 4f, bowlBottom - 6f, stemRight, bowlBottom)
                tubePath.lineTo(stemRight, bot - 8f)
                tubePath.lineTo(cx + footW / 2f, bot)
                tubePath.lineTo(cx - footW / 2f, bot)
                tubePath.lineTo(stemLeft, bot - 8f)
                tubePath.lineTo(stemLeft, bowlBottom)
                tubePath.cubicTo(stemLeft - 4f, bowlBottom - 6f, rimLeft, t + h * 0.35f, rimLeft, t)
                tubePath.close()

                lipRect.set(rimLeft - 2f, t - 2.5f, rimRight + 2f, t + 3.5f)

                // Stem ornament node
                extraDetailPath.addCircle(cx, t + h * 0.68f, 3.5f, Path.Direction.CW)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.ANCIENT_AMPHORA -> {
                // 13. Ancient Amphora (Level 550): Greco-Roman vessel with side loop handles & pointed base
                val neckW = w * 0.38f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val shoulderY = t + h * 0.28f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, shoulderY - 6f)
                tubePath.cubicTo(neckRight + 4f, shoulderY, r - 3f, shoulderY + 4f, r - 4f, shoulderY + 12f)
                tubePath.lineTo(cx + 8f, bot - 8f)
                tubePath.lineTo(cx + 12f, bot)
                tubePath.lineTo(cx - 12f, bot)
                tubePath.lineTo(cx - 8f, bot - 8f)
                tubePath.lineTo(l + 4f, shoulderY + 12f)
                tubePath.cubicTo(l + 3f, shoulderY + 4f, neckLeft - 4f, shoulderY, neckLeft, shoulderY - 6f)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                // Twin amphora loop handles on sides
                extraDetailPath.moveTo(neckLeft - 1f, shoulderY - 4f)
                extraDetailPath.cubicTo(l - 2f, shoulderY - 4f, l - 2f, shoulderY + 16f, l + 4f, shoulderY + 16f)
                extraDetailPath.moveTo(neckRight + 1f, shoulderY - 4f)
                extraDetailPath.cubicTo(r + 2f, shoulderY - 4f, r + 2f, shoulderY + 16f, r - 4f, shoulderY + 16f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.CELESTIAL_VIAL -> {
                // 14. Celestial Vial (Level 650): Crescent curved vial with celestial star highlight
                val neckW = w * 0.36f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.cubicTo(neckRight + 6f, t + h * 0.25f, r, t + h * 0.55f, r - 3f, bot - 20f)
                tubePath.quadTo(cx + w * 0.20f, bot, cx, bot)
                tubePath.quadTo(l + 4f, bot, l + 4f, bot - 22f)
                tubePath.cubicTo(l + 2f, t + h * 0.50f, neckLeft - 4f, t + h * 0.25f, neckLeft, t)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                // Celestial star highlight
                drawStar(extraDetailPath, cx + w * 0.16f, t + h * 0.32f, 4.5f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.ROYAL_DECANTER -> {
                // 15. Royal Decanter (Level 740): Regal slender neck with triple rings & fluted bulb base
                val neckW = w * 0.30f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val neckBottom = t + h * 0.40f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, neckBottom)
                tubePath.cubicTo(neckRight + 6f, neckBottom + 6f, r, bot - 24f, r - 2f, bot - 10f)
                tubePath.quadTo(r, bot, r - 10f, bot)
                tubePath.lineTo(l + 10f, bot)
                tubePath.quadTo(l, bot, l + 2f, bot - 10f)
                tubePath.cubicTo(l, bot - 24f, neckLeft - 6f, neckBottom + 6f, neckLeft, neckBottom)
                tubePath.close()

                lipRect.set(neckLeft - 3f, t - 2.5f, neckRight + 3f, t + 3.5f)

                // Triple decorative rings on neck
                extraDetailPath.moveTo(neckLeft - 1f, t + h * 0.14f)
                extraDetailPath.lineTo(neckRight + 1f, t + h * 0.14f)
                extraDetailPath.moveTo(neckLeft - 1f, t + h * 0.22f)
                extraDetailPath.lineTo(neckRight + 1f, t + h * 0.22f)
                extraDetailPath.moveTo(neckLeft - 1f, t + h * 0.30f)
                extraDetailPath.lineTo(neckRight + 1f, t + h * 0.30f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }

            TubeSkin.MYSTIC_RELIC -> {
                // 16. Mystic Relic (Level 820): Arched gothic spire shoulders & runic relic pedestal
                val neckW = w * 0.42f
                val neckLeft = cx - neckW / 2f
                val neckRight = cx + neckW / 2f
                val archY = t + h * 0.26f

                tubePath.moveTo(neckLeft, t)
                tubePath.lineTo(neckRight, t)
                tubePath.lineTo(neckRight, archY - 6f)
                tubePath.lineTo(r - 2f, archY + 4f)
                tubePath.lineTo(r - 4f, bot - 12f)
                tubePath.lineTo(r, bot)
                tubePath.lineTo(l, bot)
                tubePath.lineTo(l + 4f, bot - 12f)
                tubePath.lineTo(l + 2f, archY + 4f)
                tubePath.lineTo(neckLeft, archY - 6f)
                tubePath.close()

                lipRect.set(neckLeft - 2.5f, t - 2.5f, neckRight + 2.5f, t + 3.5f)

                // Runic glyph accent
                extraDetailPath.moveTo(cx, archY + 8f)
                extraDetailPath.lineTo(cx, bot - 14f)
                extraDetailPath.moveTo(cx - 6f, archY + 18f)
                extraDetailPath.lineTo(cx + 6f, archY + 18f)

                buildInsetClip(tubePath, liquidClipPath, 2.5f)
            }
        }
    }

    private fun buildInsetClip(outerPath: Path, innerClip: Path, inset: Float) {
        val matrix = Matrix()
        val bounds = RectF()
        outerPath.computeBounds(bounds, true)
        val sx = (bounds.width() - (inset * 2f)) / bounds.width().coerceAtLeast(1f)
        val sy = (bounds.height() - (inset * 2f)) / bounds.height().coerceAtLeast(1f)
        matrix.setScale(sx, sy, bounds.centerX(), bounds.centerY())
        outerPath.transform(matrix, innerClip)
    }

    private fun drawStar(path: Path, cx: Float, cy: Float, size: Float) {
        path.moveTo(cx, cy - size)
        path.lineTo(cx + size * 0.25f, cy - size * 0.25f)
        path.lineTo(cx + size, cy)
        path.lineTo(cx + size * 0.25f, cy + size * 0.25f)
        path.lineTo(cx, cy + size)
        path.lineTo(cx - size * 0.25f, cy + size * 0.25f)
        path.lineTo(cx - size, cy)
        path.lineTo(cx - size * 0.25f, cy - size * 0.25f)
        path.close()
    }
}
