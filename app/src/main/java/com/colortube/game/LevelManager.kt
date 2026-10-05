package com.colortube.game

import java.util.ArrayDeque
import kotlin.random.Random

/**
 * LevelData
 *
 * Represents an intentionally generated, solvable level specification.
 */
data class LevelData(
    val levelNumber: Int,
    val numColors: Int,
    val numEmptyTubes: Int,
    val tubeConfigurations: List<List<Int>> // Each inner list contains color IDs from bottom to top
)

/**
 * LevelManager
 *
 * Provides 100+ progressively challenging, mathematically verified solvable levels.
 *
 * Difficulty progression:
 * - Levels 1–5: Tutorial / Gentle introduction (3 tubes, 2 colors, 1 empty)
 * - Levels 6–20: Beginner (4–5 tubes, 3 colors, 1–2 empty)
 * - Levels 21–50: Intermediate (6–8 tubes, 4–6 colors, 2 empty)
 * - Levels 51–80: Advanced (8–10 tubes, 6–8 colors, 2 empty)
 * - Levels 81–120: Master / Expert (10–12 tubes, 8–10 colors, 2 empty)
 *
 * Levels are constructed using reverse-scramble generation from solved states,
 * guaranteeing 100% solvability without brute force unreliability.
 */
object LevelManager {

    const val TOTAL_LEVELS = 1000

    private val cachedLevels = mutableMapOf<Int, LevelData>()

    fun getLevel(levelNumber: Int): LevelData {
        return cachedLevels.getOrPut(levelNumber) {
            generateSolvableLevel(levelNumber)
        }
    }

    private fun generateSolvableLevel(level: Int): LevelData {
        // Determine difficulty parameters based on level (1 to 1000)
        val (numColors, numEmptyTubes, scrambleSteps) = when {
            level in 1..3 -> Triple(2, 1, 10)
            level in 4..10 -> Triple(3, 1, 15)
            level in 11..25 -> Triple(4, 2, 25)
            level in 26..50 -> Triple(5, 2, 35)
            level in 51..80 -> Triple(6, 2, 45)
            level in 81..120 -> Triple(7, 2, 55)
            level in 121..170 -> Triple(8, 2, 65)
            level in 171..230 -> Triple(9, 2, 75)
            level in 231..290 -> Triple(9, 2, 85)
            level in 291..350 -> Triple(10, 2, 95)
            level in 351..420 -> Triple(9, 2, 105)
            level in 421..500 -> Triple(10, 2, 115)
            level in 501..590 -> Triple(10, 2, 130)
            level in 591..680 -> Triple(10, 2, 145)
            level in 681..770 -> Triple(10, 2, 160)
            level in 771..850 -> Triple(10, 2, 180)
            level in 851..920 -> Triple(10, 2, 195)
            level in 921..970 -> Triple(10, 2, 210)
            else -> Triple(10, 2, 225)
        }

        val totalTubes = numColors + numEmptyTubes
        val capacity = 4

        var seedOffset = 0L
        while (true) {
            // 2. Deterministic pseudo-random seed based on level number + seedOffset retry
            val seed = (level * 31337L) + seedOffset
            val rng = Random(seed)

            // 1. Start with a completely solved state
            val tubes = MutableList(totalTubes) { mutableListOf<Int>() }
            for (c in 1..numColors) {
                repeat(capacity) {
                    tubes[c - 1].add(c)
                }
            }

            // 3. Perform legal reverse-scramble pours
            var lastFrom = -1
            var lastTo = -1
            var steps = 0
            var attempts = 0

            while (steps < scrambleSteps && attempts < scrambleSteps * 6) {
                attempts++
                val from = rng.nextInt(totalTubes)
                val to = rng.nextInt(totalTubes)

                if (from == to) continue
                // Avoid immediately reversing the immediate previous scramble step
                if (from == lastTo && to == lastFrom) continue

                val src = tubes[from]
                val dst = tubes[to]

                if (src.isEmpty() || dst.size >= capacity) continue

                // Reverse pour 1 segment
                val color = src.removeAt(src.lastIndex)
                dst.add(color)

                lastFrom = from
                lastTo = to
                steps++
            }

            // Ensure there is at least 1 empty tube in initial state for player mobility
            val emptyCount = tubes.count { it.isEmpty() }
            if (emptyCount < 1) {
                // Find a tube to clear into others if possible or keep deterministic
                for (i in tubes.indices.reversed()) {
                    if (tubes[i].isNotEmpty() && tubes.any { it.size < capacity && it != tubes[i] }) {
                        while (tubes[i].isNotEmpty()) {
                            val target = tubes.firstOrNull { it != tubes[i] && it.size < capacity } ?: break
                            target.add(tubes[i].removeAt(tubes[i].lastIndex))
                        }
                        if (tubes[i].isEmpty()) break
                    }
                }
            }

            // Ensure puzzle is not already solved at start
            val coloredTubes = tubes.filter { it.isNotEmpty() }
            val alreadySolved = coloredTubes.size == numColors && coloredTubes.all { t -> t.size == capacity && t.all { it == t[0] } }
            if (alreadySolved) {
                seedOffset++
                continue
            }

            val configs = tubes.map { it.toList() }

            // Validate that the puzzle is solvable using BFS solver
            if (isSolvable(configs, numColors)) {
                return LevelData(
                    levelNumber = level,
                    numColors = numColors,
                    numEmptyTubes = tubes.count { it.isEmpty() },
                    tubeConfigurations = configs
                )
            }

            // Retry with seed + 1
            seedOffset++
        }
    }

    /**
     * Lightweight BFS Water Sort Solvability Validator.
     * Guarantees 100% mathematical solvability without modifying level difficulty or tube rules.
     */
    fun isSolvable(initialConfig: List<List<Int>>, numColors: Int): Boolean {
        val initialTubes = initialConfig.map { ArrayList(it) }

        fun canonicalState(tubes: List<List<Int>>): String {
            return tubes.map { it.joinToString(",") }.sorted().joinToString("|")
        }

        fun isSolved(tubes: List<List<Int>>): Boolean {
            val colored = tubes.filter { it.isNotEmpty() }
            return colored.size == numColors && colored.all { t -> t.size == 4 && t.all { it == t[0] } }
        }

        val visited = HashSet<String>()
        val queue = ArrayDeque<List<List<Int>>>()

        queue.add(initialTubes)
        visited.add(canonicalState(initialTubes))

        var iterations = 0
        val maxIterations = 25000

        while (queue.isNotEmpty() && iterations < maxIterations) {
            iterations++
            val current = queue.poll() ?: break

            if (isSolved(current)) {
                return true
            }

            for (i in current.indices) {
                val src = current[i]
                if (src.isEmpty()) continue
                // Skip completed tube
                if (src.size == 4 && src.all { it == src[0] }) continue

                val color = src.last()
                var count = 0
                for (idx in src.indices.reversed()) {
                    if (src[idx] == color) count++ else break
                }

                for (j in current.indices) {
                    if (i == j) continue
                    val dst = current[j]
                    if (dst.size >= 4) continue

                    // Can only pour if empty or matching color
                    if (dst.isNotEmpty() && dst.last() != color) continue

                    // Prune redundant move: pouring already uniform stack into an empty tube
                    if (dst.isEmpty() && src.all { it == src[0] }) continue

                    val space = 4 - dst.size
                    val pourCount = minOf(count, space)

                    val nextTubes = current.map { ArrayList(it) }
                    val nextSrc = nextTubes[i]
                    val nextDst = nextTubes[j]

                    for (k in 0 until pourCount) {
                        nextSrc.removeAt(nextSrc.lastIndex)
                        nextDst.add(color)
                    }

                    val key = canonicalState(nextTubes)
                    if (visited.add(key)) {
                        queue.add(nextTubes)
                    }
                }
            }
        }

        return false
    }

    fun buildInitialEngine(levelNumber: Int): GameEngine {
        val levelData = getLevel(levelNumber)
        val initialTubes = levelData.tubeConfigurations.mapIndexed { index, list ->
            Tube(
                id = index + 1,
                capacity = 4,
                layers = list.map { LiquidColor.fromId(it) }.toMutableList()
            )
        }
        return GameEngine(levelNumber, initialTubes)
    }
}
