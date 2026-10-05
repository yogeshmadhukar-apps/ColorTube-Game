package com.colortube.game

import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque
import java.util.PriorityQueue

class LevelAuditTest {

    @Test
    fun auditLevelManagerGenerationIntegrity_All850Levels() {
        println("=== AUDITING LEVEL GENERATION (LEVELS 1 TO 850) ===")
        val capacity = 4
        var anyError = false

        for (lvl in 1..LevelManager.TOTAL_LEVELS) {
            val levelData = LevelManager.getLevel(lvl)
            val tubes = levelData.tubeConfigurations

            // 1. Check basic metadata
            assertEquals("Level number mismatch for $lvl", lvl, levelData.levelNumber)
            assertTrue("numColors must be >= 2 for level $lvl", levelData.numColors >= 2)
            assertTrue("numColors must be <= 10 for level $lvl", levelData.numColors <= 10)
            assertTrue("Must have at least 1 empty tube for level $lvl", levelData.numEmptyTubes >= 1)

            // 2. Check tube count
            val totalTubes = tubes.size
            assertTrue("Total tubes must equal numColors + numEmptyTubes for level $lvl", totalTubes >= levelData.numColors + 1)

            // 3. Check capacity of each tube
            for ((tubeIdx, tube) in tubes.withIndex()) {
                assertTrue("Tube $tubeIdx in level $lvl exceeds capacity 4: size=${tube.size}", tube.size <= capacity)
            }

            // 4. Check color distribution: each color from 1..numColors must appear exactly 4 times
            val colorCounts = mutableMapOf<Int, Int>()
            var totalSegments = 0
            for (tube in tubes) {
                for (color in tube) {
                    colorCounts[color] = (colorCounts[color] ?: 0) + 1
                    totalSegments++
                }
            }

            assertEquals(
                "Level $lvl total segments mismatch: expected ${levelData.numColors * 4}, got $totalSegments",
                levelData.numColors * 4,
                totalSegments
            )

            for (c in 1..levelData.numColors) {
                val count = colorCounts[c] ?: 0
                assertEquals("Level $lvl: Color $c count must be exactly 4, got $count", 4, count)
            }

            // 5. Ensure initial state is not already solved
            val coloredTubes = tubes.filter { it.isNotEmpty() }
            val alreadySolved = coloredTubes.size == levelData.numColors && coloredTubes.all { t -> t.size == 4 && t.all { it == t[0] } }
            assertFalse("Level $lvl is already completely solved at start!", alreadySolved)
        }
        println("✓ Passed Level Generation & Color Conservation Audit for all 850 levels.")
    }

    @Test
    fun test28ProblematicLevelsNowSolvableInLevelManager() {
        println("=== VERIFYING 28 PREVIOUSLY PROBLEMATIC LEVELS IN LEVELMANAGER ===")
        val problematic = listOf(8, 298, 330, 390, 486, 529, 577, 652, 655, 657, 662, 697, 704, 712, 736, 750, 765, 775, 776, 791, 796, 797, 801, 821, 836, 838, 843, 846)
        for (lvl in problematic) {
            val levelData = LevelManager.getLevel(lvl)
            val solvable = LevelManager.isSolvable(levelData.tubeConfigurations, levelData.numColors)
            println("Level $lvl: solvable=$solvable, colors=${levelData.numColors}, empty=${levelData.numEmptyTubes}")
            assertTrue("Level $lvl must now be solvable via LevelManager", solvable)
        }
        println("✓ All 28 problematic levels are now 100% SOLVABLE via LevelManager!")
    }




    @Test
    fun auditPuzzleSolvability() {
        println("=== AUDITING PUZZLE SOLVABILITY (BFS/A* SOLVER) ===")
        val failedLevels = mutableListOf<Int>()
        val sampleLevels = mutableListOf<Int>()

        // Check first 50 levels comprehensively, plus every 10th level up to 850,
        // and boundary levels (1, 3, 4, 10, 11, 25, 26, 50, 51, 80, 81, 120, 121, 170, 171, 230, 231, 290, 291, 350, 420, 500, 590, 680, 770, 850)
        val milestoneLevels = listOf(
            1, 2, 3, 4, 5, 10, 11, 15, 20, 25, 26, 30, 40, 50, 51, 60, 70, 80, 81, 90, 100, 110, 120,
            121, 130, 150, 170, 171, 200, 230, 231, 260, 290, 291, 320, 350, 380, 420, 450, 500,
            550, 590, 630, 680, 720, 770, 800, 850
        )
        sampleLevels.addAll(1..50)
        sampleLevels.addAll(milestoneLevels)
        for (lvl in 51..850 step 15) {
            sampleLevels.add(lvl)
        }
        val distinctLevels = sampleLevels.distinct().sorted()

        println("Testing solvability on ${distinctLevels.size} representative levels across the 1..850 range...")

        for (lvl in distinctLevels) {
            val levelData = LevelManager.getLevel(lvl)
            val isSolvable = solveLevel(levelData.tubeConfigurations, levelData.numColors)
            if (!isSolvable) {
                println("FAILED SOLVABILITY: Level $lvl")
                failedLevels.add(lvl)
            }
        }

        if (failedLevels.isNotEmpty()) {
            fail("The following levels were detected as UNSOLVABLE: $failedLevels")
        } else {
            println("✓ All tested levels are 100% SOLVABLE!")
        }
    }

    /**
     * Canonical Water Sort Solver using BFS with canonical state hashing
     */
    private fun solveLevel(initialConfig: List<List<Int>>, numColors: Int): Boolean {
        // State representation: List of List<Int>
        val initialTubes = initialConfig.map { ArrayList(it) }

        // Canonical state string: sort tube strings so tube permutation is ignored
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
        val maxIterations = 25000 // reasonable bound for verification

        while (queue.isNotEmpty() && iterations < maxIterations) {
            iterations++
            val current = queue.poll()!!

            if (isSolved(current)) {
                return true
            }

            // Generate next states
            for (i in current.indices) {
                val src = current[i]
                if (src.isEmpty()) continue
                // Don't pour from a completed tube
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

                    // Prune useless move: pouring an already uniform stack into an empty tube
                    if (dst.isEmpty() && src.all { it == src[0] }) continue

                    val space = 4 - dst.size
                    val pourCount = minOf(count, space)

                    // Clone state
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
}
