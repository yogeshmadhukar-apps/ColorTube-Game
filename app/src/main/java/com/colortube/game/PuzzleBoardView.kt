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
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * ActivePour
 *
 * Tracks the state of an active physical pouring animation between two tubes.
 */
data class ActivePour(
    val fromIndex: Int,
    val toIndex: Int,
    val color: LiquidColor,
    val count: Int,
    var progress: Float = 0f
)

/**
 * PuzzleBoardView
 *
 * Responsive puzzle board supporting 3 to 14+ tubes dynamically.
 * Features synchronized physical pouring animations with fluid stream rendering,
 * source liquid draining, destination liquid rising, and seamless state commits.
 */
class PuzzleBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val tubeViews = mutableListOf<GlassTubeView>()
    private var currentTubes: List<Tube> = emptyList()
    private var currentSelectedIndex: Int? = null
    private var currentHintPair: Pair<Int, Int>? = null

    var isPouring: Boolean = false
        private set
    private var activePour: ActivePour? = null

    // Completed tube celebration burst animation
    private val celebrationRenderer = TubeCelebrationRenderer()
    private var celebrationAnimator: ValueAnimator? = null
    var isCelebrating: Boolean = false
        private set

    var currentTubeSkin: TubeSkin = TubeSkin.CLASSIC_TUBE
        set(value) {
            field = value
            for (tv in tubeViews) {
                tv.tubeSkin = value
            }
            invalidate()
        }

    var currentColorPalette: ColorPalette = ColorPalette.VIBRANT_NEON
        set(value) {
            field = value
            for (tv in tubeViews) {
                tv.colorPalette = value
            }
            invalidate()
        }

    // Layout calculation caching
    private var computedCols = 4
    private var computedRows = 1
    private var computedTubeW = 0
    private var computedTubeH = 0

    // Reusable stream rendering objects
    private val streamPath = Path()
    private val streamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val streamGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#90FFFFFF")
    }
    private val splashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val splashRect = RectF()

    var onTubeClickListener: ((tubeIndex: Int) -> Unit)? = null

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    fun setTubes(tubes: List<Tube>, selectedIndex: Int? = null, hintPair: Pair<Int, Int>? = null) {
        val total = tubes.size
        currentTubes = ArrayList(tubes)
        currentSelectedIndex = selectedIndex
        currentHintPair = hintPair

        if (total == 0) {
            removeAllViews()
            tubeViews.clear()
            requestLayout()
            invalidate()
            return
        }

        // Adjust child count to match total
        while (tubeViews.size < total) {
            val tv = GlassTubeView(context).apply {
                visibility = View.VISIBLE
            }
            tv.setOnClickListener {
                if (!isPouring && !isCelebrating) {
                    val curIdx = tubeViews.indexOf(tv)
                    if (curIdx != -1) onTubeClickListener?.invoke(curIdx)
                }
            }
            tubeViews.add(tv)
            addView(tv)
        }
        while (tubeViews.size > total) {
            val tv = tubeViews.removeAt(tubeViews.lastIndex)
            removeView(tv)
        }

        // Bind models and states
        for (i in 0 until total) {
            val tv = tubeViews[i]
            tv.tube = currentTubes[i]
            tv.tubeSkin = currentTubeSkin
            tv.colorPalette = currentColorPalette
            tv.isSelectedTube = (selectedIndex == i)
            tv.isHintSource = (hintPair?.first == i)
            tv.isHintTarget = (hintPair?.second == i)
            tv.visibility = View.VISIBLE
        }

        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val total = currentTubes.size
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(w, h)
        if (total == 0 || w <= 0 || h <= 0) return

        val optimalCols = when {
            total <= 4 -> total
            total in 5..6 -> 3
            total in 7..8 -> 4
            total in 9..10 -> 5
            total in 11..12 -> 4
            else -> 5
        }
        val optimalRows = ceil(total.toDouble() / optimalCols).toInt()

        computedCols = optimalCols
        computedRows = optimalRows

        val usableW = max(100, w - paddingLeft - paddingRight)
        val usableH = max(200, h - paddingTop - paddingBottom)

        val availWidthPerCell = usableW / optimalCols
        val availHeightPerCell = usableH / optimalRows

        val tubeW = min(availWidthPerCell * 0.85f, (availHeightPerCell * 0.85f) / 3.0f).toInt().coerceAtLeast(40)
        val tubeH = (tubeW * 3.0f).toInt().coerceAtLeast(120)

        computedTubeW = tubeW
        computedTubeH = tubeH

        val childWidthSpec = MeasureSpec.makeMeasureSpec(tubeW, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(tubeH, MeasureSpec.EXACTLY)

        for (i in 0 until total) {
            val tv = tubeViews.getOrNull(i) ?: continue
            tv.measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val total = currentTubes.size
        if (total == 0) return

        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return

        val cols = computedCols
        val rows = computedRows
        val tubeW = computedTubeW
        val tubeH = computedTubeH
        if (cols <= 0 || rows <= 0 || tubeW <= 0 || tubeH <= 0) return

        val usableW = w - paddingLeft - paddingRight
        val usableH = h - paddingTop - paddingBottom

        val cellW = usableW / cols
        val cellH = usableH / rows

        for (i in 0 until total) {
            val tv = tubeViews.getOrNull(i) ?: continue
            val row = i / cols
            val colInRow = i % cols

            val tubesInThisRow = if (row == rows - 1) total - (row * cols) else cols
            val rowStartX = paddingLeft + (usableW - (tubesInThisRow * cellW)) / 2

            val childX = rowStartX + (colInRow * cellW) + (cellW - tubeW) / 2
            val childY = paddingTop + (row * cellH) + (cellH - tubeH) / 2

            tv.layout(childX, childY, childX + tubeW, childY + tubeH)
            tv.visibility = View.VISIBLE
        }
    }

    /**
     * Executes the premium physically believable pouring animation.
     * Moves and tilts the source tube, draws the flowing stream, drains the source liquid,
     * rises the destination liquid, triggers [onStreamStart] when liquid emerges from spout,
     * [onStreamEnd] when stream finishes flowing, and triggers [onFinished] upon completion to commit state.
     * Duration scales proportionally with liquid volume (1 unit: 580ms, 2 units: 840ms, 3 units: 1100ms, 4 units: 1360ms).
     */
    fun startPourAnimation(
        fromIndex: Int,
        toIndex: Int,
        color: LiquidColor,
        count: Int,
        onStreamStart: ((flowDurationMs: Long) -> Unit)? = null,
        onStreamEnd: (() -> Unit)? = null,
        onFinished: () -> Unit
    ) {
        val srcView = tubeViews.getOrNull(fromIndex) ?: run {
            onFinished()
            return
        }
        val destView = tubeViews.getOrNull(toIndex) ?: run {
            onFinished()
            return
        }

        isPouring = true
        srcView.bringToFront()
        srcView.elevation = 40f
        srcView.translationZ = 40f

        // Compute tilt direction and natural arc
        val isTiltingRight = fromIndex <= toIndex || srcView.left <= destView.left
        val targetTiltAngle = if (isTiltingRight) 56f else -56f

        val destMouthX = destView.left + destView.width * 0.5f
        val destMouthY = destView.top + destView.height * 0.08f

        val localLipX = if (isTiltingRight) srcView.width * 0.88f else srcView.width * 0.12f
        val targetTranslateX = if (isTiltingRight) {
            destMouthX - (srcView.left + localLipX) - 16f
        } else {
            destMouthX - (srcView.left + localLipX) + 16f
        }
        val targetTranslateY = destMouthY - (srcView.top + srcView.height * 0.08f) - 36f

        srcView.drainingCount = count
        srcView.drainingFraction = 0f

        destView.incomingColor = color
        destView.incomingCount = count
        destView.incomingFraction = 0f

        // Volume-based duration scaling
        val approachDurationMs = 180L
        val flowDurationMs = 260L * count + 60L
        val returnDurationMs = 190L
        val totalDurationMs = approachDurationMs + flowDurationMs + returnDurationMs

        val pStreamStart = approachDurationMs.toFloat() / totalDurationMs
        val pStreamEnd = (approachDurationMs + flowDurationMs).toFloat() / totalDurationMs

        val pour = ActivePour(fromIndex, toIndex, color, count)
        activePour = pour

        var hasTriggeredStreamStart = false
        var hasTriggeredStreamEnd = false

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalDurationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val p = va.animatedValue as Float
                pour.progress = p

                when {
                    p < pStreamStart -> {
                        // Phase 1: Smooth approach glide and tilt
                        val t = (p / pStreamStart).coerceIn(0f, 1f)
                        srcView.pourTranslateX = targetTranslateX * t
                        srcView.pourTranslateY = targetTranslateY * t
                        srcView.pourTiltAngle = targetTiltAngle * t
                        srcView.drainingFraction = 0f
                        destView.incomingFraction = 0f
                    }
                    p in pStreamStart..pStreamEnd -> {
                        // Phase 2: Active continuous liquid flow
                        if (!hasTriggeredStreamStart) {
                            hasTriggeredStreamStart = true
                            onStreamStart?.invoke(flowDurationMs)
                        }
                        val t = ((p - pStreamStart) / (pStreamEnd - pStreamStart)).coerceIn(0f, 1f)
                        srcView.pourTranslateX = targetTranslateX
                        srcView.pourTranslateY = targetTranslateY
                        srcView.pourTiltAngle = targetTiltAngle
                        srcView.drainingFraction = t
                        destView.incomingFraction = t
                    }
                    else -> {
                        // Phase 3: Liquid stops, tube returns to upright position and settles
                        if (!hasTriggeredStreamEnd) {
                            hasTriggeredStreamEnd = true
                            onStreamEnd?.invoke()
                        }
                        val t = ((p - pStreamEnd) / (1f - pStreamEnd)).coerceIn(0f, 1f)
                        srcView.pourTranslateX = targetTranslateX * (1f - t)
                        srcView.pourTranslateY = targetTranslateY * (1f - t)
                        srcView.pourTiltAngle = targetTiltAngle * (1f - t)
                        srcView.drainingFraction = 1f
                        destView.incomingFraction = 1f
                    }
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!hasTriggeredStreamEnd) {
                        hasTriggeredStreamEnd = true
                        onStreamEnd?.invoke()
                    }

                    srcView.pourTranslateX = 0f
                    srcView.pourTranslateY = 0f
                    srcView.pourTiltAngle = 0f
                    srcView.drainingFraction = 0f
                    srcView.drainingCount = 0
                    srcView.elevation = 0f
                    srcView.translationZ = 0f

                    destView.incomingFraction = 0f
                    destView.incomingCount = 0
                    destView.incomingColor = null

                    activePour = null
                    isPouring = false
                    invalidate()

                    onFinished()
                }
            })
        }
        animator.start()
    }

    /**
     * Executes the premium celebration burst when a tube becomes 100% completed.
     * Animates tube pop scale, cap seating, light burst, and a sparkling fountain
     * particle burst originating from inside the tube mouth.
     */
    fun playTubeCompletionCelebration(
        tubeIndex: Int,
        completedColor: LiquidColor,
        onCelebrationDone: () -> Unit
    ) {
        val targetView = tubeViews.getOrNull(tubeIndex) ?: run {
            onCelebrationDone()
            return
        }

        isCelebrating = true

        // 1. Finalize target incoming state cleanly
        targetView.incomingFraction = 0f
        targetView.incomingCount = 0
        targetView.incomingColor = null

        // 2. Play cap lock-in, tube pop, and light flash
        targetView.playCompletionAnimation {
            // At the exact moment the cap locks into place, spawn celebratory burst!
            val (localX, localY) = targetView.getOpeningCenter()
            val openingX = targetView.left + localX
            val openingY = targetView.top + localY
            val neckWidth = targetView.getNeckWidth()

            celebrationRenderer.spawnBurst(
                mouthCenterX = openingX,
                mouthTopY = openingY,
                neckWidth = neckWidth,
                liquidColor = completedColor,
                density = resources.displayMetrics.density
            )
            invalidate()
        }

        // 3. Drive celebration particle animation over 1200ms
        celebrationAnimator?.cancel()
        var lastTime = System.currentTimeMillis()

        celebrationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200L
            addUpdateListener {
                val now = System.currentTimeMillis()
                val delta = (now - lastTime).coerceIn(8L, 40L)
                lastTime = now

                celebrationRenderer.update(delta)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isCelebrating = false
                    celebrationAnimator = null
                    onCelebrationDone()
                    invalidate()
                }
            })
        }
        celebrationAnimator?.start()
    }

    fun stopCelebration() {
        celebrationAnimator?.cancel()
        celebrationAnimator = null
        celebrationRenderer.clear()
        isCelebrating = false
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        activePour?.let { pour ->
            drawPourStream(canvas, pour)
        }
        if (celebrationRenderer.isActive) {
            celebrationRenderer.draw(canvas)
        }
    }

    private fun drawPourStream(canvas: Canvas, pour: ActivePour) {
        // Stream only exists when liquid is actively flowing
        val srcView = tubeViews.getOrNull(pour.fromIndex) ?: return
        val destView = tubeViews.getOrNull(pour.toIndex) ?: return

        // Draining must be active
        if (srcView.drainingFraction <= 0.01f || srcView.drainingFraction >= 0.99f) return

        val isTiltingRight = srcView.pourTiltAngle >= 0f

        val localSpoutX = if (isTiltingRight) srcView.width * (1f - 0.12f) else srcView.width * 0.12f
        val localSpoutY = srcView.height * 0.08f + 6f

        val pivotX = if (isTiltingRight) srcView.width * (1f - 0.12f) else srcView.width * 0.12f
        val pivotY = srcView.height * 0.08f + 6f

        val rad = Math.toRadians(srcView.pourTiltAngle.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()

        val relX = localSpoutX - pivotX
        val relY = localSpoutY - pivotY

        val rotX = pivotX + (relX * cos - relY * sin)
        val rotY = pivotY + (relX * sin + relY * cos)

        val spoutX = srcView.left + srcView.pourTranslateX + rotX
        val spoutY = srcView.top + srcView.liftOffset + srcView.pourTranslateY + rotY

        // Destination mouth center
        val destX = destView.left + destView.width * 0.5f
        val destY = destView.top + destView.height * 0.08f + 4f

        streamPath.reset()
        streamPath.moveTo(spoutX, spoutY)

        // Believable fluid trajectory: arcs out of tilted mouth, drops downward into dest tube mouth
        val ctrlX1 = spoutX + (if (isTiltingRight) 22f else -22f)
        val ctrlY1 = spoutY + 40f
        val ctrlX2 = destX + (if (isTiltingRight) -6f else 6f)
        val ctrlY2 = destY - 32f

        streamPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, destX, destY)

        // Stream outer body (meniscus width 9dp)
        streamPaint.strokeWidth = 9f
        streamPaint.shader = LinearGradient(
            spoutX, spoutY,
            destX, destY,
            pour.color.topColor,
            pour.color.bottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(streamPath, streamPaint)

        // Stream specular fluid glint
        streamGlintPaint.strokeWidth = 2.4f
        canvas.drawPath(streamPath, streamGlintPaint)

        // Landing entry splash meniscus at destination mouth
        splashRect.set(destX - 8.5f, destY - 3.5f, destX + 8.5f, destY + 3.5f)
        splashPaint.color = pour.color.topColor
        canvas.drawOval(splashRect, splashPaint)

        // Occasional micro-droplets around entry point
        splashPaint.alpha = 200
        canvas.drawCircle(destX - (if (isTiltingRight) 4f else -4f), destY - 5f, 1.8f, splashPaint)
        canvas.drawCircle(destX + (if (isTiltingRight) 3f else -3f), destY - 3f, 1.4f, splashPaint)
        splashPaint.alpha = 255
    }

    fun getTubeView(index: Int): GlassTubeView? {
        return tubeViews.getOrNull(index)
    }
}
