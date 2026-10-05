package com.colortube.game

import android.graphics.Color

/**
 * TubeSkin
 *
 * Defines visual container styles inspired by reference designs.
 * Unlocks strictly through progressive level milestones up to Level 350.
 * Affects ONLY visual rendering. Game engine, capacities, pouring logic, and collision are completely unaffected.
 */
enum class TubeSkin(
    val id: String,
    val displayName: String,
    val unlockLevel: Int
) {
    CLASSIC_TUBE(
        id = "classic_tube",
        displayName = "Classic Glass",
        unlockLevel = 1
    ),
    FLASK_BOTTLE(
        id = "flask_bottle",
        displayName = "Chemist Flask",
        unlockLevel = 10
    ),
    SLENDER_VIAL(
        id = "slender_vial",
        displayName = "Slender Vial",
        unlockLevel = 30
    ),
    CHAMPAGNE_FLUTE(
        id = "champagne_flute",
        displayName = "Crystal Flute",
        unlockLevel = 50
    ),
    BULB_POTION(
        id = "bulb_potion",
        displayName = "Alchemist Bulb",
        unlockLevel = 70
    ),
    SQUARE_CARAFE(
        id = "square_carafe",
        displayName = "Square Decanter",
        unlockLevel = 90
    ),
    NARROW_DROPOP(
        id = "narrow_dropop",
        displayName = "Narrow Droplet",
        unlockLevel = 120
    ),
    WIDE_TUMBLER(
        id = "wide_tumbler",
        displayName = "Apothecary Jar",
        unlockLevel = 160
    ),
    HOURGLASS_VASE(
        id = "hourglass_vase",
        displayName = "Hourglass Vase",
        unlockLevel = 210
    ),
    FANTASY_ELIXIR(
        id = "fantasy_elixir",
        displayName = "Fantasy Elixir",
        unlockLevel = 280
    ),
    OCTAGON_PRISM(
        id = "octagon_prism",
        displayName = "Octagon Prism",
        unlockLevel = 360
    ),
    TALL_CHALICE(
        id = "tall_chalice",
        displayName = "Tall Chalice",
        unlockLevel = 450
    ),
    ANCIENT_AMPHORA(
        id = "ancient_amphora",
        displayName = "Ancient Amphora",
        unlockLevel = 550
    ),
    CELESTIAL_VIAL(
        id = "celestial_vial",
        displayName = "Celestial Vial",
        unlockLevel = 650
    ),
    ROYAL_DECANTER(
        id = "royal_decanter",
        displayName = "Royal Decanter",
        unlockLevel = 740
    ),
    MYSTIC_RELIC(
        id = "mystic_relic",
        displayName = "Mystic Relic",
        unlockLevel = 820
    );

    val coinPrice: Int
        get() = when (this) {
            CLASSIC_TUBE -> 0
            FLASK_BOTTLE -> 3500
            SLENDER_VIAL -> 4500
            CHAMPAGNE_FLUTE -> 5500
            BULB_POTION -> 6500
            SQUARE_CARAFE -> 8000
            NARROW_DROPOP -> 9000
            WIDE_TUMBLER -> 10000
            HOURGLASS_VASE -> 12500
            FANTASY_ELIXIR -> 15000
            OCTAGON_PRISM -> 18000
            TALL_CHALICE -> 22000
            ANCIENT_AMPHORA -> 26000
            CELESTIAL_VIAL -> 30000
            ROYAL_DECANTER -> 35000
            MYSTIC_RELIC -> 40000
        }

    companion object {
        fun fromId(id: String): TubeSkin {
            return entries.firstOrNull { it.id == id } ?: CLASSIC_TUBE
        }
    }
}

/**
 * BackgroundTheme
 *
 * Visual backdrop collections that unlock progressively through level progression up to Level 850.
 */
enum class BackgroundTheme(
    val id: String,
    val displayName: String,
    val unlockLevel: Int,
    val drawableResName: String,
    val gradientColors: IntArray,
    val accentGlow: Int,
    val motifStyle: MotifStyle
) {
    DARK_LAB(
        id = "dark_lab",
        displayName = "Dark Laboratory",
        unlockLevel = 1,
        drawableResName = "bg_game_pattern",
        gradientColors = intArrayOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#090D16"),
            Color.parseColor("#04060A")
        ),
        accentGlow = Color.parseColor("#2800D2D3"),
        motifStyle = MotifStyle.LAB_BEAKERS
    ),
    GEOMETRIC_MATRIX(
        id = "geometric_matrix",
        displayName = "Geometric Grid",
        unlockLevel = 4,
        drawableResName = "bg_theme_geometric",
        gradientColors = intArrayOf(
            Color.parseColor("#0D1B2A"),
            Color.parseColor("#1B263B"),
            Color.parseColor("#070D18")
        ),
        accentGlow = Color.parseColor("#28415A77"),
        motifStyle = MotifStyle.GEOMETRIC_GRID
    ),
    NIGHT_FOREST(
        id = "night_forest",
        displayName = "Night Forest",
        unlockLevel = 20,
        drawableResName = "bg_theme_nature",
        gradientColors = intArrayOf(
            Color.parseColor("#06231A"),
            Color.parseColor("#0B3B2B"),
            Color.parseColor("#03130D")
        ),
        accentGlow = Color.parseColor("#2810AC84"),
        motifStyle = MotifStyle.NIGHT_FOREST
    ),
    SUNSET_VALLEY(
        id = "sunset_valley",
        displayName = "Sunset Valley",
        unlockLevel = 40,
        drawableResName = "bg_theme_crimson",
        gradientColors = intArrayOf(
            Color.parseColor("#2B0C18"),
            Color.parseColor("#1F0A1F"),
            Color.parseColor("#0B040D")
        ),
        accentGlow = Color.parseColor("#28FF6B81"),
        motifStyle = MotifStyle.SUNSET_VALLEY
    ),
    DESERT_DUSK(
        id = "desert_dusk",
        displayName = "Desert Dusk",
        unlockLevel = 75,
        drawableResName = "bg_theme_crimson",
        gradientColors = intArrayOf(
            Color.parseColor("#241208"),
            Color.parseColor("#1A090A"),
            Color.parseColor("#0A0305")
        ),
        accentGlow = Color.parseColor("#28FFA502"),
        motifStyle = MotifStyle.DESERT_DUSK
    ),
    COSMIC_DEEP(
        id = "cosmic_deep",
        displayName = "Cosmic Deep",
        unlockLevel = 110,
        drawableResName = "bg_theme_cosmic",
        gradientColors = intArrayOf(
            Color.parseColor("#1E0F33"),
            Color.parseColor("#120722"),
            Color.parseColor("#06020D")
        ),
        accentGlow = Color.parseColor("#288854D0"),
        motifStyle = MotifStyle.COSMIC_DEEP
    ),
    MIDNIGHT_CITY(
        id = "midnight_city",
        displayName = "Midnight City",
        unlockLevel = 160,
        drawableResName = "bg_theme_geometric",
        gradientColors = intArrayOf(
            Color.parseColor("#0C1636"),
            Color.parseColor("#121E47"),
            Color.parseColor("#050917")
        ),
        accentGlow = Color.parseColor("#283867D6"),
        motifStyle = MotifStyle.MIDNIGHT_CITY
    ),
    NEON_LAB(
        id = "neon_lab",
        displayName = "Neon Laboratory",
        unlockLevel = 220,
        drawableResName = "bg_game_pattern",
        gradientColors = intArrayOf(
            Color.parseColor("#0A192F"),
            Color.parseColor("#0F2C42"),
            Color.parseColor("#040D18")
        ),
        accentGlow = Color.parseColor("#2800D2D3"),
        motifStyle = MotifStyle.NEON_CIRCUITS
    ),
    OCEAN_ABYSS(
        id = "ocean_abyss",
        displayName = "Ocean Abyss",
        unlockLevel = 300,
        drawableResName = "bg_theme_nature",
        gradientColors = intArrayOf(
            Color.parseColor("#041C2C"),
            Color.parseColor("#0A2E3D"),
            Color.parseColor("#020E17")
        ),
        accentGlow = Color.parseColor("#280ABDE3"),
        motifStyle = MotifStyle.OCEAN_ABYSS
    ),
    FANTASY_REALM(
        id = "fantasy_realm",
        displayName = "Fantasy Realm",
        unlockLevel = 400,
        drawableResName = "bg_theme_cosmic",
        gradientColors = intArrayOf(
            Color.parseColor("#230E30"),
            Color.parseColor("#170821"),
            Color.parseColor("#08020D")
        ),
        accentGlow = Color.parseColor("#289B59B6"),
        motifStyle = MotifStyle.FANTASY_REALM
    ),
    AURORA_SKY(
        id = "aurora_sky",
        displayName = "Aurora Sky",
        unlockLevel = 520,
        drawableResName = "bg_theme_nature",
        gradientColors = intArrayOf(
            Color.parseColor("#082424"),
            Color.parseColor("#0F3B35"),
            Color.parseColor("#031212")
        ),
        accentGlow = Color.parseColor("#282ED573"),
        motifStyle = MotifStyle.AURORA_SKY
    ),
    CYBER_METROPOLIS(
        id = "cyber_metropolis",
        displayName = "Cyber Metropolis",
        unlockLevel = 660,
        drawableResName = "bg_theme_geometric",
        gradientColors = intArrayOf(
            Color.parseColor("#14141F"),
            Color.parseColor("#1F1A30"),
            Color.parseColor("#0B0A12")
        ),
        accentGlow = Color.parseColor("#285F27CD"),
        motifStyle = MotifStyle.CYBER_METROPOLIS
    ),
    TWILIGHT_HORIZON(
        id = "twilight_horizon",
        displayName = "Twilight Horizon",
        unlockLevel = 800,
        drawableResName = "bg_theme_cosmic",
        gradientColors = intArrayOf(
            Color.parseColor("#211224"),
            Color.parseColor("#140A18"),
            Color.parseColor("#08030B")
        ),
        accentGlow = Color.parseColor("#28EE5253"),
        motifStyle = MotifStyle.TWILIGHT_HORIZON
    );

    val coinPrice: Int
        get() = when (this) {
            DARK_LAB -> 0
            GEOMETRIC_MATRIX -> 3500
            NIGHT_FOREST -> 4500
            SUNSET_VALLEY -> 5500
            DESERT_DUSK -> 6500
            COSMIC_DEEP -> 8000
            MIDNIGHT_CITY -> 10000
            NEON_LAB -> 12500
            OCEAN_ABYSS -> 15000
            FANTASY_REALM -> 18000
            AURORA_SKY -> 22000
            CYBER_METROPOLIS -> 30000
            TWILIGHT_HORIZON -> 35000
        }

    companion object {
        fun fromId(id: String): BackgroundTheme {
            return entries.firstOrNull { it.id == id } ?: DARK_LAB
        }
    }
}
