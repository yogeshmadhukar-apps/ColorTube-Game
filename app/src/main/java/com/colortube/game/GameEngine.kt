package com.colortube.game

import android.graphics.Color
import java.util.ArrayDeque

/**
 * LiquidColor
 *
 * Defines vibrant, premium liquid colors used across game levels,
 * matching Stitch design specifications with top and bottom gradient shades.
 */
enum class LiquidColor(
    val id: Int,
    val displayName: String,
    val topColor: Int,
    val bottomColor: Int,
    val hexString: String
) {
    CYAN(1, "Radiant Cyan", Color.parseColor("#00D2D3"), Color.parseColor("#009B9C"), "#00D2D3"),
    CORAL(2, "Neon Coral", Color.parseColor("#DD2D42"), Color.parseColor("#B9082C"), "#FF4757"),
    LIME(3, "Electric Lime", Color.parseColor("#2ED573"), Color.parseColor("#10AC84"), "#2ED573"),
    VIOLET(4, "Royal Violet", Color.parseColor("#5352ED"), Color.parseColor("#3B3AC4"), "#5352ED"),
    AMBER(5, "Sunburst Amber", Color.parseColor("#FFAC32"), Color.parseColor("#855400"), "#FFA502"),
    ROSE(6, "Rose Quartz", Color.parseColor("#FF6B81"), Color.parseColor("#D63031"), "#FF6B81"),
    AQUA(7, "Inverse Aqua", Color.parseColor("#26DCDD"), Color.parseColor("#006A6A"), "#26DCDD"),
    GOLD(8, "Golden Honey", Color.parseColor("#FFC312"), Color.parseColor("#F79F1F"), "#FFC312"),
    EMERALD(9, "Emerald Green", Color.parseColor("#1DD1A1"), Color.parseColor("#10AC84"), "#1DD1A1"),
    DEEP_PURPLE(10, "Cosmic Purple", Color.parseColor("#8854D0"), Color.parseColor("#3867D6"), "#8854D0");

    companion object {
        fun fromId(id: Int): LiquidColor = entries.firstOrNull { it.id == id } ?: CYAN
    }
}

/**
 * Tube
 *
 * Represents an individual glass vessel containing up to [capacity] liquid segments.
 * Stored as a stack from bottom (index 0) to top (last index).
 */
data class Tube(
    val id: Int,
    val capacity: Int = 4,
    val layers: MutableList<LiquidColor> = mutableListOf(),
    var isExtraTube: Boolean = false
) {
    fun isFull(): Boolean = layers.size >= capacity
    fun isEmpty(): Boolean = layers.isEmpty()
    fun isNotEmpty(): Boolean = layers.isNotEmpty()
    fun topColor(): LiquidColor? = layers.lastOrNull()

    /**
     * Number of consecutive segments at the top of this tube having the same color.
     */
    fun topCount(): Int {
        if (isEmpty()) return 0
        val top = layers.last()
        var count = 0
        for (i in layers.indices.reversed()) {
            if (layers[i] == top) count++ else break
        }
        return count
    }

    /**
     * Tube is considered completed/solved if it is full and contains only one color.
     */
    fun isCompleted(): Boolean {
        if (layers.size != capacity) return false
        val first = layers.first()
        return layers.all { it == first }
    }

    fun copyTube(): Tube {
        return Tube(
            id = id,
            capacity = capacity,
            layers = ArrayList(layers),
            isExtraTube = isExtraTube
        )
    }
}

/**
 * MoveRecord
 *
 * Stored in undo history stack.
 */
data class MoveRecord(
    val fromTubeId: Int,
    val toTubeId: Int,
    val color: LiquidColor,
    val count: Int
)

/**
 * GameEngine
 *
 * Robust state management and rule validation for ColorTube puzzle gameplay.
 */
class GameEngine(
    val levelNumber: Int,
    initialTubes: List<Tube>
) {
    val tubes: MutableList<Tube> = initialTubes.map { it.copyTube() }.toMutableList()
    val undoStack: ArrayDeque<MoveRecord> = ArrayDeque()
    var moveCount: Int = 0
    var isLevelComplete: Boolean = false

    /**
     * Validates whether pouring from [fromIndex] to [toIndex] is a legal move.
     */
    fun canPour(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex == toIndex) return false
        if (fromIndex !in tubes.indices || toIndex !in tubes.indices) return false

        val source = tubes[fromIndex]
        val dest = tubes[toIndex]

        if (source.isEmpty()) return false
        if (dest.isFull()) return false

        // Don't pour out of an already completed tube
        if (source.isCompleted()) return false

        // Destination must be either empty or have matching top color
        val sourceTop = source.topColor()!!
        val destTop = dest.topColor()

        if (destTop != null && destTop != sourceTop) {
            return false
        }

        // Must have room for at least 1 segment
        val spaceLeft = dest.capacity - dest.layers.size
        return spaceLeft > 0
    }

    /**
     * Executes the pour. Returns the number of segments poured, or 0 if illegal.
     */
    fun pour(fromIndex: Int, toIndex: Int): Int {
        if (!canPour(fromIndex, toIndex)) return 0

        val source = tubes[fromIndex]
        val dest = tubes[toIndex]
        val color = source.topColor()!!

        val sourceTopCount = source.topCount()
        val spaceLeft = dest.capacity - dest.layers.size
        val countToPour = minOf(sourceTopCount, spaceLeft)

        for (i in 0 until countToPour) {
            source.layers.removeAt(source.layers.lastIndex)
            dest.layers.add(color)
        }

        undoStack.push(MoveRecord(source.id, dest.id, color, countToPour))
        moveCount++

        checkCompletion()
        return countToPour
    }

    /**
     * Undoes the last move.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false

        val record = undoStack.pop()
        val fromTube = tubes.firstOrNull { it.id == record.fromTubeId }
        val toTube = tubes.firstOrNull { it.id == record.toTubeId }

        if (fromTube == null || toTube == null) return false

        for (i in 0 until record.count) {
            if (toTube.isNotEmpty()) {
                toTube.layers.removeAt(toTube.layers.lastIndex)
                fromTube.layers.add(record.color)
            }
        }

        if (moveCount > 0) moveCount--
        checkCompletion()
        return true
    }

    fun canAddExtraTube(): Boolean {
        val currentExtra = tubes.count { it.isExtraTube }
        return currentExtra < getMaxExtraTubes(levelNumber)
    }

    /**
     * Adds an extra empty helper tube to the board.
     * Guaranteed to add strictly ONE empty tube and respect level-based maximums.
     */
    fun addExtraTube(): Boolean {
        if (!canAddExtraTube()) return false
        val nextId = (tubes.maxOfOrNull { it.id } ?: 0) + 1
        tubes.add(Tube(id = nextId, capacity = 4, layers = mutableListOf(), isExtraTube = true))
        return true
    }

    companion object {
        /**
         * Level-based Extra Tube assistance rules:
         * - Levels 1–49: Max 1 Extra Tube (1 ad = 1 tube)
         * - Levels 50–79: Max 2 Extra Tubes via separate ads (1 ad = 1 tube)
         * - Levels 80+: Max 3 Extra Tubes via separate ads (1 ad = 1 tube)
         */
        fun getMaxExtraTubes(level: Int): Int = when {
            level in 1..49 -> 1
            level in 50..79 -> 2
            else -> 3
        }
    }

    /**
     * Checks if all colored liquids are sorted.
     * Level is complete when every non-empty tube is completely uniform and full.
     */
    fun checkCompletion(): Boolean {
        val coloredTubes = tubes.filter { it.isNotEmpty() }
        val allSolved = coloredTubes.all { it.isCompleted() }
        isLevelComplete = allSolved && coloredTubes.isNotEmpty()
        return isLevelComplete
    }

    /**
     * Computes a legal hint move (fromIndex -> toIndex), if one exists.
     */
    fun findHint(): Pair<Int, Int>? {
        for (i in tubes.indices) {
            if (tubes[i].isCompleted() || tubes[i].isEmpty()) continue
            for (j in tubes.indices) {
                if (i == j) continue
                if (canPour(i, j)) {
                    // Avoid pouring an entire uniform tube into an empty tube uselessly
                    val src = tubes[i]
                    val dst = tubes[j]
                    if (dst.isEmpty() && src.layers.all { it == src.layers.first() }) {
                        continue
                    }
                    return Pair(i, j)
                }
            }
        }
        return null
    }
}
