package com.colortube.game

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * CelebrationParticleType
 *
 * Diverse particle types for the completed tube celebration burst:
 * - Tiny 5-point stars
 * - 4-point diamond glint sparkles
 * - Confetti ribbon strips with 3D rotation
 * - Tiny glowing circular beads
 * - Soft volumetric glow orbs
 */
enum class CelebrationParticleType {
    STAR,
    DIAMOND_SPARKLE,
    CONFETTI_STRIP,
    CIRCLE_BEAD,
    SOFT_GLOW_ORB
}

/**
 * CelebrationParticle
 *
 * Physics-driven particle originating from the completed tube opening.
 */
class CelebrationParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val gravity: Float,
    val drag: Float,
    val size: Float,
    val type: CelebrationParticleType,
    val color: Int,
    var rotation: Float,
    val rotationSpeed: Float,
    val delayMs: Long,
    val lifeMs: Long
) {
    var elapsedMs: Long = 0L
    var isAlive: Boolean = true
    var alpha: Float = 0f
    var scale: Float = 0f

    fun update(deltaMs: Long) {
        elapsedMs += deltaMs
        if (elapsedMs < delayMs) {
            alpha = 0f
            scale = 0f
            return
        }

        val activeMs = elapsedMs - delayMs
        if (activeMs >= lifeMs) {
            isAlive = false
            alpha = 0f
            return
        }

        val dt = deltaMs / 1000f
        x += vx * dt
        y += vy * dt
        vy += gravity * dt
        vx *= drag

        rotation += rotationSpeed * dt

        val progress = activeMs.toFloat() / lifeMs

        // Rapid smooth pop-in, gentle drift, shrink at tail
        scale = when {
            progress < 0.12f -> (progress / 0.12f) * 1.15f
            progress < 0.22f -> 1.15f - ((progress - 0.12f) / 0.10f) * 0.15f
            progress > 0.70f -> 1.0f - ((progress - 0.70f) / 0.30f) * 0.45f
            else -> 1.0f
        }

        // High opacity early, smooth fade-out in final 40%
        alpha = when {
            progress < 0.08f -> progress / 0.08f
            progress > 0.55f -> 1.0f - ((progress - 0.55f) / 0.45f)
            else -> 1.0f
        }.coerceIn(0f, 1f)
    }
}

/**
 * TubeCelebrationRenderer
 *
 * Manages physics updates and high-performance vector rendering of celebration bursts.
 */
class TubeCelebrationRenderer {

    private val particles = mutableListOf<CelebrationParticle>()
    private val starPath = Path()
    private val sparklePath = Path()
    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    val isActive: Boolean
        get() = particles.any { it.isAlive }

    /**
     * Spawns a celebration burst anchored precisely inside the tube mouth.
     */
    fun spawnBurst(
        mouthCenterX: Float,
        mouthTopY: Float,
        neckWidth: Float,
        liquidColor: LiquidColor,
        density: Float = 2.5f
    ) {
        particles.clear()

        val primaryCol = liquidColor.topColor
        val secondaryCol = liquidColor.bottomColor
        val goldCol1 = Color.parseColor("#FFD700")
        val goldCol2 = Color.parseColor("#F59E0B")
        val whiteCol = Color.parseColor("#FFFFFF")
        val cyanGlint = Color.parseColor("#E0F7FA")

        val colorPool = listOf(
            primaryCol, primaryCol,
            secondaryCol,
            goldCol1, goldCol2,
            whiteCol, cyanGlint
        )

        val particleCount = 38
        val halfNeck = (neckWidth * 0.34f).coerceAtLeast(8f)

        for (i in 0 until particleCount) {
            // Anchor origin strictly inside the mouth opening
            val startX = mouthCenterX + (Random.nextFloat() * 2f - 1f) * halfNeck
            val startY = mouthTopY + Random.nextFloat() * 4f

            // Velocity: strong upward thrust, gentle fan spread
            // vy: -380dp to -760dp per second
            val vyBase = -(380f + Random.nextFloat() * 380f) * density
            // vx: -120dp to +120dp per second (fan arc ~ -24° to +24°)
            val spreadFactor = (startX - mouthCenterX) / halfNeck // bias slightly outwards based on spawn side
            val vxBase = (spreadFactor * 60f + (Random.nextFloat() * 2f - 1f) * 70f) * density

            val gravity = 480f * density // soft floating gravity
            val drag = 0.972f

            val size = (3f + Random.nextFloat() * 4f) * density
            val type = when (i % 5) {
                0 -> CelebrationParticleType.STAR
                1 -> CelebrationParticleType.DIAMOND_SPARKLE
                2 -> CelebrationParticleType.CONFETTI_STRIP
                3 -> CelebrationParticleType.CIRCLE_BEAD
                else -> CelebrationParticleType.SOFT_GLOW_ORB
            }

            val color = colorPool[Random.nextInt(colorPool.size)]
            val rotation = Random.nextFloat() * 360f
            val rotationSpeed = (Random.nextFloat() * 2f - 1f) * 360f

            // Stagger emergence (0 to 120ms) so particles erupt as a fountain
            val delayMs = (Random.nextFloat() * 110f).toLong()
            val lifeMs = 850L + (Random.nextFloat() * 320f).toLong()

            particles.add(
                CelebrationParticle(
                    x = startX,
                    y = startY,
                    vx = vxBase,
                    vy = vyBase,
                    gravity = gravity,
                    drag = drag,
                    size = size,
                    type = type,
                    color = color,
                    rotation = rotation,
                    rotationSpeed = rotationSpeed,
                    delayMs = delayMs,
                    lifeMs = lifeMs
                )
            )
        }
    }

    fun clear() {
        particles.clear()
    }

    fun update(deltaMs: Long) {
        for (p in particles) {
            if (p.isAlive) {
                p.update(deltaMs)
            }
        }
    }

    fun draw(canvas: Canvas) {
        for (p in particles) {
            if (!p.isAlive || p.alpha <= 0.01f || p.scale <= 0.01f) continue

            val saveCount = canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.scale(p.scale, p.scale)

            val a = (p.alpha * 255).toInt().coerceIn(0, 255)
            val baseColor = p.color
            val drawColor = Color.argb(a, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            drawPaint.color = drawColor

            val s = p.size

            when (p.type) {
                CelebrationParticleType.STAR -> {
                    buildStar(starPath, 0f, 0f, s)
                    drawPaint.style = Paint.Style.FILL
                    canvas.drawPath(starPath, drawPaint)
                }

                CelebrationParticleType.DIAMOND_SPARKLE -> {
                    buildDiamond(sparklePath, 0f, 0f, s * 1.25f, s * 0.65f)
                    drawPaint.style = Paint.Style.FILL
                    canvas.drawPath(sparklePath, drawPaint)
                }

                CelebrationParticleType.CONFETTI_STRIP -> {
                    // 3D flutter rotation effect
                    val flutterScaleX = cos(Math.toRadians(p.rotation.toDouble() * 2.0)).toFloat().coerceIn(-1f, 1f)
                    canvas.scale(flutterScaleX, 1f)
                    drawPaint.style = Paint.Style.FILL
                    canvas.drawRoundRect(-s * 0.45f, -s * 0.95f, s * 0.45f, s * 0.95f, 1.5f, 1.5f, drawPaint)
                }

                CelebrationParticleType.CIRCLE_BEAD -> {
                    drawPaint.style = Paint.Style.FILL
                    canvas.drawCircle(0f, 0f, s * 0.5f, drawPaint)
                    // Specular glint
                    drawPaint.color = Color.argb((a * 0.75f).toInt(), 255, 255, 255)
                    canvas.drawCircle(-s * 0.15f, -s * 0.15f, s * 0.2f, drawPaint)
                }

                CelebrationParticleType.SOFT_GLOW_ORB -> {
                    val radius = s * 0.85f
                    glowPaint.shader = RadialGradient(
                        0f, 0f, radius,
                        intArrayOf(drawColor, Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(0f, 0f, radius, glowPaint)
                }
            }

            canvas.restoreToCount(saveCount)
        }
    }

    private fun buildStar(path: Path, cx: Float, cy: Float, radius: Float) {
        path.reset()
        val innerRadius = radius * 0.42f
        val points = 5
        var angle = -Math.PI / 2.0
        val step = Math.PI / points

        path.moveTo((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat())
        for (i in 0 until points * 2) {
            angle += step
            val r = if (i % 2 == 0) innerRadius else radius
            path.lineTo((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())
        }
        path.close()
    }

    private fun buildDiamond(path: Path, cx: Float, cy: Float, height: Float, width: Float) {
        path.reset()
        path.moveTo(cx, cy - height)
        path.quadTo(cx, cy, cx + width, cy)
        path.quadTo(cx, cy, cx, cy + height)
        path.quadTo(cx, cy, cx - width, cy)
        path.quadTo(cx, cy, cx, cy - height)
        path.close()
    }
}
