package com.colortube.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.max
import kotlin.math.min

/**
 * GlassTubeView
 *
 * Implements the exact Google Stitch ColorTube glass tube design.
 *
 * Visual Source of Truth: stitch_colortube_game_ui_ux/gameplay_screen/code.html
 *
 * Characteristics:
 * - Single-layer, ultra-clean transparent laboratory glass vessel
 * - Exact rounded-b-full, rounded-t-sm cylinder geometry
 * - Single crisp 1px glass border matching Stitch specs (rgba(255,255,255,0.7))
 * - Perfectly aligned lab glass lip / rim collar with inner opening ellipse
 * - Liquid physically contained inside inner rounded bowl
 * - Clean specular reflections directly over liquid (left key streak, right bounce, bottom caustic)
 * - Zero duplicated outlines, zero halos, zero rectangular background artifacts
 * - Subtle tactile lift & scale on selection with no outer glow
 * - Dynamic pouring animation support (draining & incoming liquid transfer)
 */
class GlassTubeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var tube: Tube? = null
        set(value) {
            field = value
            invalidate()
        }

    var tubeSkin: TubeSkin = TubeSkin.CLASSIC_TUBE
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var colorPalette: ColorPalette = ColorPalette.VIBRANT_NEON
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isSelectedTube: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                animateSelection(value)
            }
        }

    var isHintSource: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isHintTarget: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Interactive animation offsets
    var liftOffset: Float = 0f
        private set
    private var selectionScale: Float = 1.0f

    // Tube completion celebration animation properties
    var completionScale: Float = 1.0f
        private set
    var capAnimProgress: Float = 1.0f
        private set
    var capFlashAlpha: Float = 0f
        private set
    var glowPulseAlpha: Float = 0f
        private set

    // Pour animation dynamic properties
    var pourTranslateX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var pourTranslateY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var pourTiltAngle: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var drainingFraction: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var drainingCount: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var incomingColor: LiquidColor? = null
        set(value) {
            field = value
            invalidate()
        }
    var incomingFraction: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var incomingCount: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    // Reusable Paints
    private val glassBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(178, 255, 255, 255) // rgba(255,255,255,0.7)
    }
    private val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val lipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.argb(240, 255, 255, 255)
    }
    private val lipInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(35, 0, 60, 70)
    }
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val liquidDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(30, 0, 0, 0)
    }
    private val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(128, 255, 255, 255)
    }
    private val specularLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val specularRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bottomOpticalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bottomBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(220, 255, 255, 255)
    }
    private val corkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val starPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFAC32")
    }
    private val starTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val capRimRect = RectF()
    private val starBadgeRect = RectF()
    private val starBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#FFE082")
    }
    private val capGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Reusable Rects & Paths
    private val tubeBounds = RectF()
    private val innerLiquidBounds = RectF()
    private val tubePath = Path()
    private val liquidClipPath = Path()
    private val lipRect = RectF()
    private val lipInner = RectF()
    private val leftHighlight = RectF()
    private val rightHighlight = RectF()
    private val bottomOpticalCurve = RectF()
    private val layerRect = RectF()
    private val corkRect = RectF()
    private val starPill = RectF()

    fun getOpeningCenter(): Pair<Float, Float> {
        return Pair(tubeBounds.centerX(), tubeBounds.top)
    }

    fun getNeckWidth(): Float {
        val tubeW = tubeBounds.width()
        return when (tubeSkin) {
            TubeSkin.FLASK_BOTTLE -> tubeW * 0.42f
            TubeSkin.BULB_POTION -> tubeW * 0.40f
            TubeSkin.NARROW_DROPOP -> tubeW * 0.30f
            TubeSkin.SQUARE_CARAFE -> tubeW * 0.44f
            TubeSkin.FANTASY_ELIXIR -> tubeW * 0.38f
            TubeSkin.ROYAL_DECANTER -> tubeW * 0.34f
            TubeSkin.CELESTIAL_VIAL -> tubeW * 0.38f
            TubeSkin.ANCIENT_AMPHORA -> tubeW * 0.40f
            TubeSkin.MYSTIC_RELIC -> tubeW * 0.44f
            TubeSkin.SLENDER_VIAL -> tubeW * 0.50f
            TubeSkin.WIDE_TUMBLER -> tubeW * 0.80f
            TubeSkin.OCTAGON_PRISM -> tubeW * 0.65f
            else -> tubeW
        }
    }

    fun playCompletionAnimation(onCapSeated: () -> Unit) {
        capAnimProgress = 0f
        completionScale = 1.0f
        capFlashAlpha = 0f
        glowPulseAlpha = 0f

        var hasTriggeredCapSeated = false

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 580L
            addUpdateListener { va ->
                val p = va.animatedValue as Float

                // 1. Scale pop: 1.0 -> 1.045 -> 1.0 over first 300ms
                completionScale = when {
                    p < 0.28f -> 1.0f + (p / 0.28f) * 0.045f
                    p < 0.65f -> 1.045f - ((p - 0.28f) / 0.37f) * 0.045f
                    else -> 1.0f
                }

                // 2. Halo glow pulse: 0 -> 0.85 -> 0 over 500ms
                glowPulseAlpha = when {
                    p < 0.28f -> (p / 0.28f) * 0.85f
                    p < 0.85f -> 0.85f - ((p - 0.28f) / 0.57f) * 0.85f
                    else -> 0f
                }

                // 3. Cap drops down smoothly: 0 to 1 over first 300ms with overshoot
                capAnimProgress = when {
                    p < 0.40f -> {
                        val t = p / 0.40f
                        val c1 = 1.70158f
                        val c3 = c1 + 1f
                        (1f + c3 * Math.pow((t - 1f).toDouble(), 3.0) + c1 * Math.pow((t - 1f).toDouble(), 2.0)).toFloat().coerceIn(0f, 1.15f)
                    }
                    p < 0.55f -> 1.0f + 0.15f * (1f - (p - 0.40f) / 0.15f)
                    else -> 1.0f
                }

                // 4. Cap flash burst peaks right when cap seats (p ~ 0.35)
                if (p >= 0.34f && !hasTriggeredCapSeated) {
                    hasTriggeredCapSeated = true
                    onCapSeated()
                }

                capFlashAlpha = when {
                    p < 0.30f -> 0f
                    p < 0.40f -> (p - 0.30f) / 0.10f
                    p < 0.70f -> 1.0f - ((p - 0.40f) / 0.30f)
                    else -> 0f
                }

                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    completionScale = 1.0f
                    capAnimProgress = 1.0f
                    capFlashAlpha = 0f
                    glowPulseAlpha = 0f
                    if (!hasTriggeredCapSeated) {
                        hasTriggeredCapSeated = true
                        onCapSeated()
                    }
                    invalidate()
                }
            })
        }
        anim.start()
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val targetW = layoutParams?.width ?: 0
        val targetH = layoutParams?.height ?: 0

        val w = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> if (targetW > 0) min(widthSize, targetW) else widthSize
            else -> if (targetW > 0) targetW else widthSize
        }

        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> if (targetH > 0) min(heightSize, targetH) else heightSize
            else -> if (targetH > 0) targetH else heightSize
        }

        setMeasuredDimension(w, h)
    }

    private fun animateSelection(selected: Boolean) {
        val targetLift = if (selected) -20f else 0f
        val targetScale = if (selected) 1.03f else 1.0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.2f)
            val startLift = liftOffset
            val startScale = selectionScale
            addUpdateListener {
                val f = it.animatedValue as Float
                liftOffset = startLift + (targetLift - startLift) * f
                selectionScale = startScale + (targetScale - startScale) * f
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = tube ?: return
        if (width <= 0 || height <= 0) return

        canvas.save()
        // 1. Subtle tactile lift & scale on selection + completion scale pop
        canvas.translate(pourTranslateX, liftOffset + pourTranslateY)
        val totalScale = selectionScale * completionScale
        if (totalScale != 1.0f) {
            canvas.scale(totalScale, totalScale, width / 2f, height / 2f)
        }

        // Completion halo glow pulse around the tube
        if (glowPulseAlpha > 0.01f) {
            val haloRadius = tubeBounds.height() * 0.55f
            haloGlowPaint.shader = RadialGradient(
                tubeBounds.centerX(), tubeBounds.centerY(), haloRadius,
                intArrayOf(Color.argb((glowPulseAlpha * 85).toInt(), 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(tubeBounds.centerX(), tubeBounds.centerY(), haloRadius, haloGlowPaint)
        }
        if (pourTiltAngle != 0f) {
            val pivotX = if (pourTiltAngle >= 0f) tubeBounds.right else tubeBounds.left
            val pivotY = tubeBounds.top + 4f
            canvas.rotate(pourTiltAngle, pivotX, pivotY)
        }

        val paddingX = width * 0.16f
        val topY = height * 0.08f
        val bottomY = height * 0.90f
        val tubeWidth = width - (paddingX * 2f)
        val tubeHeight = bottomY - topY
        val cornerRadius = tubeWidth / 2f

        tubeBounds.set(paddingX, topY, paddingX + tubeWidth, bottomY)

        // 2. Dynamic Container Geometry according to selected TubeSkin
        tubePath.reset()
        when (tubeSkin) {
            TubeSkin.CLASSIC_TUBE -> {
                tubePath.addRoundRect(
                    tubeBounds,
                    floatArrayOf(
                        3f, 3f, // top-left
                        3f, 3f, // top-right
                        cornerRadius, cornerRadius, // bottom-right
                        cornerRadius, cornerRadius  // bottom-left
                    ),
                    Path.Direction.CW
                )
            }
            TubeSkin.FLASK_BOTTLE -> {
                // Erlenmeyer / conical flask: narrow neck opening out to a wide beveled base
                val neckW = tubeWidth * 0.42f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val neckBottom = topY + tubeHeight * 0.28f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, neckBottom)
                tubePath.lineTo(tubeBounds.right, bottomY - 14f)
                tubePath.quadTo(tubeBounds.right, bottomY, tubeBounds.right - 14f, bottomY)
                tubePath.lineTo(tubeBounds.left + 14f, bottomY)
                tubePath.quadTo(tubeBounds.left, bottomY, tubeBounds.left, bottomY - 14f)
                tubePath.lineTo(neckLeft, neckBottom)
                tubePath.close()
            }
            TubeSkin.SLENDER_VIAL -> {
                // Hourglass waist curved vial
                val waistY = topY + tubeHeight * 0.46f
                val waistInset = tubeWidth * 0.16f
                tubePath.moveTo(tubeBounds.left + 3f, topY)
                tubePath.lineTo(tubeBounds.right - 3f, topY)
                tubePath.cubicTo(
                    tubeBounds.right, topY + 20f,
                    tubeBounds.right - waistInset, waistY - 15f,
                    tubeBounds.right - waistInset, waistY
                )
                tubePath.cubicTo(
                    tubeBounds.right - waistInset, waistY + 15f,
                    tubeBounds.right, bottomY - 30f,
                    tubeBounds.right, bottomY - cornerRadius
                )
                tubePath.arcTo(
                    RectF(tubeBounds.left, bottomY - tubeWidth, tubeBounds.right, bottomY),
                    0f, 180f, false
                )
                tubePath.cubicTo(
                    tubeBounds.left, bottomY - 30f,
                    tubeBounds.left + waistInset, waistY + 15f,
                    tubeBounds.left + waistInset, waistY
                )
                tubePath.cubicTo(
                    tubeBounds.left + waistInset, waistY - 15f,
                    tubeBounds.left, topY + 20f,
                    tubeBounds.left + 3f, topY
                )
                tubePath.close()
            }
            TubeSkin.CHAMPAGNE_FLUTE -> {
                // Tapered crystal flute: flared top rim, gently narrowing downward with curved bottom
                tubePath.moveTo(tubeBounds.left - 4f, topY)
                tubePath.lineTo(tubeBounds.right + 4f, topY)
                tubePath.lineTo(tubeBounds.right - 4f, bottomY - 18f)
                tubePath.quadTo(tubeBounds.right - 4f, bottomY, tubeBounds.centerX(), bottomY)
                tubePath.quadTo(tubeBounds.left + 4f, bottomY, tubeBounds.left + 4f, bottomY - 18f)
                tubePath.close()
            }
            TubeSkin.BULB_POTION -> {
                // Spherical bottom alchemist potion bottle
                val neckW = tubeWidth * 0.48f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val neckBottom = topY + tubeHeight * 0.32f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, neckBottom)
                val bulbRect = RectF(tubeBounds.left, neckBottom - 10f, tubeBounds.right, bottomY)
                tubePath.arcTo(bulbRect, -45f, 270f, false)
                tubePath.lineTo(neckLeft, neckBottom)
                tubePath.close()
            }
            TubeSkin.SQUARE_CARAFE -> {
                // Beveled geometric apothecary container
                tubePath.addRoundRect(
                    tubeBounds,
                    floatArrayOf(
                        4f, 4f,
                        4f, 4f,
                        10f, 10f,
                        10f, 10f
                    ),
                    Path.Direction.CW
                )
            }
            TubeSkin.NARROW_DROPOP -> {
                // Slender raindrop tapered bottle
                tubePath.moveTo(tubeBounds.centerX() - 10f, topY)
                tubePath.lineTo(tubeBounds.centerX() + 10f, topY)
                tubePath.lineTo(tubeBounds.right - 2f, bottomY - cornerRadius)
                tubePath.arcTo(RectF(tubeBounds.left + 2f, bottomY - tubeWidth + 4f, tubeBounds.right - 2f, bottomY), 0f, 180f, false)
                tubePath.lineTo(tubeBounds.centerX() - 10f, topY)
                tubePath.close()
            }
            TubeSkin.WIDE_TUMBLER -> {
                // 8. Apothecary Jar (Level 160)
                val neckW = tubeWidth * 0.78f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val stepY = topY + tubeHeight * 0.14f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, stepY)
                tubePath.lineTo(tubeBounds.right - 2f, stepY + 4f)
                tubePath.lineTo(tubeBounds.right - 2f, bottomY - 8f)
                tubePath.quadTo(tubeBounds.right - 2f, bottomY, tubeBounds.right - 8f, bottomY)
                tubePath.lineTo(tubeBounds.left + 8f, bottomY)
                tubePath.quadTo(tubeBounds.left + 2f, bottomY, tubeBounds.left + 2f, bottomY - 8f)
                tubePath.lineTo(tubeBounds.left + 2f, stepY + 4f)
                tubePath.lineTo(neckLeft, stepY)
                tubePath.close()
            }
            TubeSkin.HOURGLASS_VASE -> {
                // 9. Hourglass Vase (Level 210)
                val rimW = tubeWidth * 0.86f
                val rimLeft = tubeBounds.centerX() - rimW / 2f
                val rimRight = tubeBounds.centerX() + rimW / 2f
                val waistY = topY + tubeHeight * 0.50f
                val waistW = tubeWidth * 0.38f
                val waistLeft = tubeBounds.centerX() - waistW / 2f
                val waistRight = tubeBounds.centerX() + waistW / 2f

                tubePath.moveTo(rimLeft, topY)
                tubePath.lineTo(rimRight, topY)
                tubePath.quadTo(waistRight, waistY - 10f, waistRight, waistY)
                tubePath.quadTo(waistRight, waistY + 10f, tubeBounds.right - 3f, bottomY - 10f)
                tubePath.quadTo(tubeBounds.right, bottomY, tubeBounds.right - 8f, bottomY)
                tubePath.lineTo(tubeBounds.left + 8f, bottomY)
                tubePath.quadTo(tubeBounds.left, bottomY, tubeBounds.left + 3f, bottomY - 10f)
                tubePath.quadTo(waistLeft, waistY + 10f, waistLeft, waistY)
                tubePath.quadTo(waistLeft, waistY - 10f, rimLeft, topY)
                tubePath.close()
            }
            TubeSkin.FANTASY_ELIXIR -> {
                // 10. Fantasy Elixir (Level 280)
                val neckW = tubeWidth * 0.36f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val diamondY = topY + tubeHeight * 0.40f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, topY + tubeHeight * 0.18f)
                tubePath.lineTo(tubeBounds.right - 1f, diamondY)
                tubePath.lineTo(tubeBounds.centerX() + tubeWidth * 0.20f, bottomY - 10f)
                tubePath.lineTo(tubeBounds.centerX(), bottomY)
                tubePath.lineTo(tubeBounds.centerX() - tubeWidth * 0.20f, bottomY - 10f)
                tubePath.lineTo(tubeBounds.left + 1f, diamondY)
                tubePath.lineTo(neckLeft, topY + tubeHeight * 0.18f)
                tubePath.close()
            }
            TubeSkin.OCTAGON_PRISM -> {
                // 11. Octagon Prism (Level 360)
                val cornerCut = tubeWidth * 0.22f
                val topCutY = topY + 4f
                val botCutY = bottomY - 2f

                tubePath.moveTo(tubeBounds.left + cornerCut, topCutY)
                tubePath.lineTo(tubeBounds.right - cornerCut, topCutY)
                tubePath.lineTo(tubeBounds.right - 2f, topCutY + cornerCut)
                tubePath.lineTo(tubeBounds.right - 2f, botCutY - cornerCut)
                tubePath.lineTo(tubeBounds.right - cornerCut, botCutY)
                tubePath.lineTo(tubeBounds.left + cornerCut, botCutY)
                tubePath.lineTo(tubeBounds.left + 2f, botCutY - cornerCut)
                tubePath.lineTo(tubeBounds.left + 2f, topCutY + cornerCut)
                tubePath.close()
            }
            TubeSkin.TALL_CHALICE -> {
                // 12. Tall Chalice (Level 450)
                val rimW = tubeWidth * 0.88f
                val rimLeft = tubeBounds.centerX() - rimW / 2f
                val rimRight = tubeBounds.centerX() + rimW / 2f
                val bowlBottom = topY + tubeHeight * 0.54f
                val stemW = tubeWidth * 0.22f
                val stemLeft = tubeBounds.centerX() - stemW / 2f
                val stemRight = tubeBounds.centerX() + stemW / 2f
                val footW = tubeWidth * 0.78f

                tubePath.moveTo(rimLeft, topY)
                tubePath.lineTo(rimRight, topY)
                tubePath.cubicTo(rimRight, topY + tubeHeight * 0.35f, stemRight + 4f, bowlBottom - 6f, stemRight, bowlBottom)
                tubePath.lineTo(stemRight, bottomY - 8f)
                tubePath.lineTo(tubeBounds.centerX() + footW / 2f, bottomY)
                tubePath.lineTo(tubeBounds.centerX() - footW / 2f, bottomY)
                tubePath.lineTo(stemLeft, bottomY - 8f)
                tubePath.lineTo(stemLeft, bowlBottom)
                tubePath.cubicTo(stemLeft - 4f, bowlBottom - 6f, rimLeft, topY + tubeHeight * 0.35f, rimLeft, topY)
                tubePath.close()
            }
            TubeSkin.ANCIENT_AMPHORA -> {
                // 13. Ancient Amphora (Level 550)
                val neckW = tubeWidth * 0.38f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val shoulderY = topY + tubeHeight * 0.28f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, shoulderY - 6f)
                tubePath.cubicTo(neckRight + 4f, shoulderY, tubeBounds.right - 3f, shoulderY + 4f, tubeBounds.right - 4f, shoulderY + 12f)
                tubePath.lineTo(tubeBounds.centerX() + 8f, bottomY - 8f)
                tubePath.lineTo(tubeBounds.centerX() + 12f, bottomY)
                tubePath.lineTo(tubeBounds.centerX() - 12f, bottomY)
                tubePath.lineTo(tubeBounds.centerX() - 8f, bottomY - 8f)
                tubePath.lineTo(tubeBounds.left + 4f, shoulderY + 12f)
                tubePath.cubicTo(tubeBounds.left + 3f, shoulderY + 4f, neckLeft - 4f, shoulderY, neckLeft, shoulderY - 6f)
                tubePath.close()
            }
            TubeSkin.CELESTIAL_VIAL -> {
                // 14. Celestial Vial (Level 650)
                val neckW = tubeWidth * 0.36f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.cubicTo(neckRight + 6f, topY + tubeHeight * 0.25f, tubeBounds.right, topY + tubeHeight * 0.55f, tubeBounds.right - 3f, bottomY - 20f)
                tubePath.quadTo(tubeBounds.centerX() + tubeWidth * 0.20f, bottomY, tubeBounds.centerX(), bottomY)
                tubePath.quadTo(tubeBounds.left + 4f, bottomY, tubeBounds.left + 4f, bottomY - 22f)
                tubePath.cubicTo(tubeBounds.left + 2f, topY + tubeHeight * 0.50f, neckLeft - 4f, topY + tubeHeight * 0.25f, neckLeft, topY)
                tubePath.close()
            }
            TubeSkin.ROYAL_DECANTER -> {
                // 15. Royal Decanter (Level 740)
                val neckW = tubeWidth * 0.30f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val neckBottom = topY + tubeHeight * 0.40f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, neckBottom)
                tubePath.cubicTo(neckRight + 6f, neckBottom + 6f, tubeBounds.right, bottomY - 24f, tubeBounds.right - 2f, bottomY - 10f)
                tubePath.quadTo(tubeBounds.right, bottomY, tubeBounds.right - 10f, bottomY)
                tubePath.lineTo(tubeBounds.left + 10f, bottomY)
                tubePath.quadTo(tubeBounds.left, bottomY, tubeBounds.left + 2f, bottomY - 10f)
                tubePath.cubicTo(tubeBounds.left, bottomY - 24f, neckLeft - 6f, neckBottom + 6f, neckLeft, neckBottom)
                tubePath.close()
            }
            TubeSkin.MYSTIC_RELIC -> {
                // 16. Mystic Relic (Level 820)
                val neckW = tubeWidth * 0.42f
                val neckLeft = tubeBounds.centerX() - neckW / 2f
                val neckRight = tubeBounds.centerX() + neckW / 2f
                val archY = topY + tubeHeight * 0.26f

                tubePath.moveTo(neckLeft, topY)
                tubePath.lineTo(neckRight, topY)
                tubePath.lineTo(neckRight, archY - 6f)
                tubePath.lineTo(tubeBounds.right - 2f, archY + 4f)
                tubePath.lineTo(tubeBounds.right - 4f, bottomY - 12f)
                tubePath.lineTo(tubeBounds.right, bottomY)
                tubePath.lineTo(tubeBounds.left, bottomY)
                tubePath.lineTo(tubeBounds.left + 4f, bottomY - 12f)
                tubePath.lineTo(tubeBounds.left + 2f, archY + 4f)
                tubePath.lineTo(neckLeft, archY - 6f)
                tubePath.close()
            }
            else -> {
                tubePath.addRoundRect(
                    tubeBounds,
                    floatArrayOf(3f, 3f, 3f, 3f, cornerRadius, cornerRadius, cornerRadius, cornerRadius),
                    Path.Direction.CW
                )
            }
        }

        // Fill single glass body
        val bodyAlpha = if (isSelectedTube) 65 else 50
        glassBodyPaint.shader = LinearGradient(
            tubeBounds.left, tubeBounds.top,
            tubeBounds.right, tubeBounds.bottom,
            intArrayOf(
                Color.argb(bodyAlpha, 255, 255, 255),
                Color.argb(20, 240, 245, 255),
                Color.argb(25, 220, 245, 250),
                Color.argb(bodyAlpha - 5, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(tubePath, glassBodyPaint)

        // 3. Liquid Layers Inside Glass (Clipped precisely to inner container shape)
        val wallThickness = 2.5f
        val innerLeft = tubeBounds.left + wallThickness
        val innerRight = tubeBounds.right - wallThickness
        val innerWidth = innerRight - innerLeft
        val innerRadius = innerWidth / 2f
        val innerBottom = tubeBounds.bottom - wallThickness
        val innerCenterY = innerBottom - innerRadius
        val innerTop = tubeBounds.top + 3f

        innerLiquidBounds.set(innerLeft, innerTop, innerRight, innerBottom)

        // Create inner liquid clip path by transforming or inset clipping
        liquidClipPath.reset()
        if (tubeSkin == TubeSkin.CLASSIC_TUBE) {
            liquidClipPath.moveTo(innerLeft, innerTop)
            liquidClipPath.lineTo(innerRight, innerTop)
            liquidClipPath.lineTo(innerRight, innerCenterY)
            val arcRect = RectF(innerLeft, innerBottom - (2 * innerRadius), innerRight, innerBottom)
            liquidClipPath.arcTo(arcRect, 0f, 180f, false)
            liquidClipPath.lineTo(innerLeft, innerTop)
            liquidClipPath.close()
        } else {
            // For specialized silhouettes, scaled inset of tubePath
            val scaleMatrix = android.graphics.Matrix()
            val sx = (innerWidth) / tubeWidth
            val sy = (innerBottom - innerTop) / tubeHeight
            scaleMatrix.setScale(sx, sy, tubeBounds.centerX(), innerCenterY)
            tubePath.transform(scaleMatrix, liquidClipPath)
        }

        canvas.save()
        canvas.clipPath(liquidClipPath)

        val capacity = t.capacity
        val totalLiquidSpace = (innerBottom - innerTop) - 10f
        val unitHeight = totalLiquidSpace / capacity
        val numLayers = t.layers.size

        // Build raw liquid segments from undisturbed base, draining, and incoming liquid
        data class LiquidBlock(val color: LiquidColor, val units: Float)
        val rawBlocks = mutableListOf<LiquidBlock>()

        val fullLayersCount = if (drainingCount > 0) max(0, numLayers - drainingCount) else numLayers
        for (i in 0 until fullLayersCount) {
            rawBlocks.add(LiquidBlock(t.layers[i], 1.0f))
        }

        if (drainingCount > 0 && drainingFraction < 1f && numLayers > 0) {
            val remainingUnits = drainingCount * (1f - drainingFraction)
            if (remainingUnits > 0.001f) {
                rawBlocks.add(LiquidBlock(t.layers.last(), remainingUnits))
            }
        }

        if (incomingCount > 0 && incomingFraction > 0f && incomingColor != null) {
            val inUnits = incomingCount * incomingFraction
            if (inUnits > 0.001f) {
                rawBlocks.add(LiquidBlock(incomingColor!!, inUnits))
            }
        }

        // Merge contiguous segments of the SAME color into ONE continuous fluid volume
        val mergedBlocks = mutableListOf<LiquidBlock>()
        for (block in rawBlocks) {
            if (block.units <= 0.001f) continue
            val last = mergedBlocks.lastOrNull()
            if (last != null && last.color == block.color) {
                mergedBlocks[mergedBlocks.lastIndex] = LiquidBlock(last.color, last.units + block.units)
            } else {
                mergedBlocks.add(block)
            }
        }

        // Render each continuous volume from bottom to top
        var currentBottomY = innerBottom
        var overallTopY = innerBottom

        for (bIndex in mergedBlocks.indices) {
            val block = mergedBlocks[bIndex]
            val blockHeight = block.units * unitHeight
            val blockTopY = currentBottomY - blockHeight

            val (topCol, bottomCol) = if (colorPalette == ColorPalette.VIBRANT_NEON) {
                Pair(block.color.topColor, block.color.bottomColor)
            } else {
                colorPalette.mapColor(block.color)
            }

            liquidPaint.shader = LinearGradient(
                innerLeft, blockTopY,
                innerLeft, currentBottomY,
                topCol,
                bottomCol,
                Shader.TileMode.CLAMP
            )

            // Fill full internal width (extending 1px into clipping boundary to eliminate any subpixel gap)
            layerRect.set(
                innerLeft - 1f,
                blockTopY,
                innerRight + 1f,
                currentBottomY
            )
            canvas.drawRect(layerRect, liquidPaint)

            // Subtle interface line between DIFFERENT immiscible color layers
            if (bIndex < mergedBlocks.lastIndex) {
                canvas.drawRect(
                    innerLeft,
                    blockTopY - 0.75f,
                    innerRight,
                    blockTopY + 0.75f,
                    liquidDividerPaint
                )
            }

            currentBottomY = blockTopY
            overallTopY = blockTopY
        }

        // Clean horizontal top surface meniscus sheen
        if (mergedBlocks.isNotEmpty() && overallTopY < innerBottom) {
            meniscusPaint.shader = LinearGradient(
                innerLeft, overallTopY,
                innerRight, overallTopY,
                intArrayOf(
                    Color.argb(70, 255, 255, 255),
                    Color.argb(220, 255, 255, 255),
                    Color.argb(70, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(innerLeft, overallTopY, innerRight, overallTopY + 2f, meniscusPaint)
        }

        canvas.restore()

        // 4. Exact Stitch Single Glass Outer Border (1px solid rgba(255,255,255,0.7))
        canvas.drawPath(tubePath, glassBorderPaint)

        // 5. Specular Highlights Layer (Directly over liquid, clipped to tubePath)
        canvas.save()
        canvas.clipPath(tubePath)

        // Stitch Left Light Reflection (inset-y-1 left-1.5 w-1.5)
        leftHighlight.set(
            tubeBounds.left + 3f,
            tubeBounds.top + 5f,
            tubeBounds.left + 6.5f,
            tubeBounds.bottom - 12f
        )
        specularLeftPaint.shader = LinearGradient(
            leftHighlight.left, leftHighlight.top,
            leftHighlight.left, leftHighlight.bottom,
            intArrayOf(
                Color.argb(240, 255, 255, 255), // 95%
                Color.argb(165, 255, 255, 255), // 65%
                Color.argb(50, 255, 255, 255),  // 20%
                Color.argb(178, 255, 255, 255)  // 70%
            ),
            floatArrayOf(0f, 0.45f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(leftHighlight, 1.75f, 1.75f, specularLeftPaint)

        // Stitch Right Ambient Bounce Reflection (inset-y-2 right-1.5 w-0.5 opacity-70)
        rightHighlight.set(
            tubeBounds.right - 4.5f,
            tubeBounds.top + 8f,
            tubeBounds.right - 3f,
            tubeBounds.bottom - 16f
        )
        specularRightPaint.shader = LinearGradient(
            rightHighlight.left, rightHighlight.top,
            rightHighlight.left, rightHighlight.bottom,
            intArrayOf(
                Color.argb(153, 255, 255, 255), // 60%
                Color.argb(38, 255, 255, 255),  // 15%
                Color.argb(128, 255, 255, 255)  // 50%
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rightHighlight, 0.75f, 0.75f, specularRightPaint)

        // Stitch Glass Bottom Thick Solid Optical Curve (bottom-0 inset-x-0 h-3)
        bottomOpticalCurve.set(
            tubeBounds.left + 2f,
            tubeBounds.bottom - 8f,
            tubeBounds.right - 2f,
            tubeBounds.bottom - 1f
        )
        bottomOpticalPaint.shader = LinearGradient(
            bottomOpticalCurve.left, bottomOpticalCurve.bottom,
            bottomOpticalCurve.left, bottomOpticalCurve.top,
            intArrayOf(
                Color.argb(153, 255, 255, 255), // 60%
                Color.argb(25, 255, 255, 255),  // 10%
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        val bottomArc = RectF(
            tubeBounds.left + 2f,
            tubeBounds.bottom - (2 * cornerRadius) + 2f,
            tubeBounds.right - 2f,
            tubeBounds.bottom - 1.5f
        )
        canvas.drawArc(bottomArc, 25f, 130f, false, bottomBorderPaint)
        canvas.restore()

        // 6. Realistic Lab Glass Lip / Rim Collar precisely matching neck width
        val neckWidth = when (tubeSkin) {
            TubeSkin.FLASK_BOTTLE -> tubeWidth * 0.42f
            TubeSkin.BULB_POTION -> tubeWidth * 0.40f
            TubeSkin.NARROW_DROPOP -> tubeWidth * 0.30f
            TubeSkin.SQUARE_CARAFE -> tubeWidth * 0.44f
            TubeSkin.FANTASY_ELIXIR -> tubeWidth * 0.38f
            TubeSkin.ROYAL_DECANTER -> tubeWidth * 0.34f
            TubeSkin.CELESTIAL_VIAL -> tubeWidth * 0.38f
            TubeSkin.ANCIENT_AMPHORA -> tubeWidth * 0.40f
            TubeSkin.MYSTIC_RELIC -> tubeWidth * 0.44f
            TubeSkin.SLENDER_VIAL -> tubeWidth * 0.50f
            TubeSkin.WIDE_TUMBLER -> tubeWidth * 0.80f
            TubeSkin.OCTAGON_PRISM -> tubeWidth * 0.65f
            else -> tubeWidth
        }
        val lipLeft = tubeBounds.centerX() - neckWidth / 2f - 2.5f
        val lipRight = tubeBounds.centerX() + neckWidth / 2f + 2.5f
        lipRect.set(
            lipLeft,
            tubeBounds.top - 3f,
            lipRight,
            tubeBounds.top + 3.5f
        )
        lipPaint.shader = LinearGradient(
            lipRect.left, lipRect.top,
            lipRect.left, lipRect.bottom,
            intArrayOf(
                Color.argb(242, 255, 255, 255), // 95%
                Color.argb(128, 240, 249, 255), // 50%
                Color.argb(153, 180, 215, 225), // 60%
                Color.argb(230, 255, 255, 255)  // 90%
            ),
            floatArrayOf(0f, 0.45f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(lipRect, 3.5f, 3.5f, lipPaint)
        canvas.drawRoundRect(lipRect, 3.5f, 3.5f, lipBorderPaint)

        // Inner mouth opening ellipse
        lipInner.set(
            lipRect.left + 4f,
            lipRect.top + 1.5f,
            lipRect.right - 4f,
            lipRect.bottom - 1.5f
        )
        canvas.drawOval(lipInner, lipInnerPaint)

        // 7. Premium Solved Cork/Crystal Stopper & Polished Star Badge
        if (t.isCompleted() && drainingCount == 0) {
            val nw = getNeckWidth()
            val capOffsetY = (1f - capAnimProgress) * -22f
            val capAlpha = (capAnimProgress.coerceIn(0f, 1f) * 255).toInt()

            val capCenterX = tubeBounds.centerX()
            val capTopY = tubeBounds.top + capOffsetY

            // Soft light burst behind the cap when locking into place
            if (capFlashAlpha > 0.01f) {
                val burstRadius = nw * 1.15f
                capGlowPaint.shader = RadialGradient(
                    capCenterX, capTopY + 4f, burstRadius,
                    intArrayOf(
                        Color.argb((capFlashAlpha * 220).toInt(), 255, 255, 255),
                        Color.argb((capFlashAlpha * 120).toInt(), 255, 220, 100),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(capCenterX, capTopY + 4f, burstRadius, capGlowPaint)
            }

            // Cap Stopper Body (fits inside the neck opening)
            val stopperW = nw * 0.70f
            corkRect.set(
                capCenterX - stopperW / 2f,
                capTopY + 1f,
                capCenterX + stopperW / 2f,
                capTopY + 13f
            )
            corkPaint.shader = LinearGradient(
                corkRect.left, corkRect.top,
                corkRect.left, corkRect.bottom,
                intArrayOf(
                    Color.argb(capAlpha, 212, 175, 55),  // Gold/cork top
                    Color.argb(capAlpha, 170, 119, 28),  // Amber woodgrain mid
                    Color.argb(capAlpha, 101, 62, 0)     // Deep rich base
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(corkRect, 3f, 3f, corkPaint)

            // Cap Stopper Top Beveled Ridge (wider collar sealing the rim)
            val ridgeW = nw * 0.88f
            capRimRect.set(
                capCenterX - ridgeW / 2f,
                capTopY - 3f,
                capCenterX + ridgeW / 2f,
                capTopY + 1.5f
            )
            corkPaint.shader = LinearGradient(
                capRimRect.left, capRimRect.top,
                capRimRect.left, capRimRect.bottom,
                intArrayOf(
                    Color.argb(capAlpha, 255, 235, 150),
                    Color.argb(capAlpha, 212, 175, 55),
                    Color.argb(capAlpha, 150, 100, 20)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(capRimRect, 2.5f, 2.5f, corkPaint)

            // Polished Star Badge: Elegant golden medallion on top of the cap
            val badgeSize = 20f
            starBadgeRect.set(
                capCenterX - badgeSize / 2f,
                capTopY - badgeSize - 1f,
                capCenterX + badgeSize / 2f,
                capTopY - 1f
            )
            starPillPaint.shader = LinearGradient(
                starBadgeRect.left, starBadgeRect.top,
                starBadgeRect.right, starBadgeRect.bottom,
                intArrayOf(
                    Color.argb(capAlpha, 30, 41, 59),
                    Color.argb(capAlpha, 15, 23, 42)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(starBadgeRect, 10f, 10f, starPillPaint)
            starBadgeBorderPaint.color = Color.argb(capAlpha, 255, 215, 0)
            canvas.drawRoundRect(starBadgeRect, 10f, 10f, starBadgeBorderPaint)

            starTextPaint.color = Color.argb(capAlpha, 255, 215, 0)
            canvas.drawText("★", capCenterX, capTopY - 5f, starTextPaint)
        }

        canvas.restore()
    }
}
