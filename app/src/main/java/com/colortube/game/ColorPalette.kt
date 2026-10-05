package com.colortube.game

import android.graphics.Color

/**
 * ColorPalette
 *
 * Defines curated liquid color palettes for the COLOR customization tab.
 * Visual skins only: preserves 1-to-1 color slot mapping, gameplay rules and puzzle engine.
 */
enum class ColorPalette(
    val id: String,
    val displayName: String,
    val unlockLevel: Int,
    val paletteColors: List<Int>
) {
    VIBRANT_NEON(
        id = "vibrant_neon",
        displayName = "Vibrant Neon",
        unlockLevel = 1,
        paletteColors = listOf(
            Color.parseColor("#00D2D3"), // Cyan
            Color.parseColor("#FF4757"), // Coral
            Color.parseColor("#2ED573"), // Lime
            Color.parseColor("#5352ED"), // Violet
            Color.parseColor("#FFA502"), // Amber
            Color.parseColor("#FF6B81"), // Rose
            Color.parseColor("#26DCDD"), // Aqua
            Color.parseColor("#FFC312"), // Gold
            Color.parseColor("#1DD1A1"), // Emerald
            Color.parseColor("#8854D0")  // Deep Purple
        )
    ),
    PASTEL_DREAM(
        id = "pastel_dream",
        displayName = "Pastel Dream",
        unlockLevel = 25,
        paletteColors = listOf(
            Color.parseColor("#70A1FF"), // Soft Sky
            Color.parseColor("#FFA502"), // Soft Orange
            Color.parseColor("#7BED9F"), // Mint
            Color.parseColor("#9A89FF"), // Soft Lavender
            Color.parseColor("#F9CA24"), // Buttercup
            Color.parseColor("#FFB8B8"), // Pastel Pink
            Color.parseColor("#48DBFB"), // Baby Blue
            Color.parseColor("#F8C291"), // Pale Apricot
            Color.parseColor("#6AB04C"), // Sage Green
            Color.parseColor("#E056FD")  // Lilac
        )
    ),
    SUNSET_BREEZE(
        id = "sunset_breeze",
        displayName = "Sunset Glow",
        unlockLevel = 70,
        paletteColors = listOf(
            Color.parseColor("#0ABDE3"), // Deep Cyan
            Color.parseColor("#EE5253"), // Sunset Red
            Color.parseColor("#10AC84"), // Emerald
            Color.parseColor("#5F27CD"), // Twilight Purple
            Color.parseColor("#FF9F43"), // Golden Peach
            Color.parseColor("#FF6B6B"), // Coral Pink
            Color.parseColor("#54A0FF"), // Ocean Blue
            Color.parseColor("#FECA57"), // Sunburst
            Color.parseColor("#1DD1A1"), // Lime Splash
            Color.parseColor("#833471")  // Crimson Dusk
        )
    ),
    COSMIC_AURORA(
        id = "cosmic_aurora",
        displayName = "Cosmic Aurora",
        unlockLevel = 180,
        paletteColors = listOf(
            Color.parseColor("#3498DB"), // Cerulean
            Color.parseColor("#E74C3C"), // Ruby
            Color.parseColor("#1ABC9C"), // Turquoise
            Color.parseColor("#9B59B6"), // Amethyst
            Color.parseColor("#E67E22"), // Solar Flare
            Color.parseColor("#FD79A8"), // Nebula Pink
            Color.parseColor("#0984E3"), // Electric Blue
            Color.parseColor("#F1C40F"), // Starlight Gold
            Color.parseColor("#00B894"), // Alien Mint
            Color.parseColor("#6C5CE7")  // Deep Void
        )
    ),
    EARTH_NATURE(
        id = "earth_nature",
        displayName = "Earth Flora",
        unlockLevel = 380,
        paletteColors = listOf(
            Color.parseColor("#2980B9"), // Ocean
            Color.parseColor("#D35400"), // Terracotta
            Color.parseColor("#27AE60"), // Jade
            Color.parseColor("#8E44AD"), // Mountain Plum
            Color.parseColor("#F39C12"), // Amber
            Color.parseColor("#E17055"), // Desert Blossom
            Color.parseColor("#00CEC9"), // Glacier River
            Color.parseColor("#FDCB6E"), // Clay Sand
            Color.parseColor("#218C74"), // Forest Green
            Color.parseColor("#574B90")  // Midnight Bark
        )
    ),
    ROYAL_CRYSTAL(
        id = "royal_crystal",
        displayName = "Royal Gemstones",
        unlockLevel = 620,
        paletteColors = listOf(
            Color.parseColor("#2980B9"), // Sapphire
            Color.parseColor("#C0392B"), // Crimson Crown
            Color.parseColor("#16A085"), // Deep Teal
            Color.parseColor("#8E44AD"), // Royal Velvet
            Color.parseColor("#F1C40F"), // Imperial Gold
            Color.parseColor("#D980FA"), // Pink Spinel
            Color.parseColor("#12CBC4"), // Aquamarine
            Color.parseColor("#F79F1F"), // Topaz
            Color.parseColor("#009432"), // Emerald Jewel
            Color.parseColor("#0652DD")  // Tanzanite
        )
    );

    val coinPrice: Int
        get() = when (this) {
            VIBRANT_NEON -> 0
            PASTEL_DREAM -> 3500
            SUNSET_BREEZE -> 6500
            COSMIC_AURORA -> 10000
            EARTH_NATURE -> 18000
            ROYAL_CRYSTAL -> 26000
        }

    val previewColors: List<Int>
        get() = paletteColors.take(4)

    fun mapColor(originalColor: LiquidColor): Pair<Int, Int> {
        val idx = (originalColor.id - 1).coerceIn(0, paletteColors.size - 1)
        val top = paletteColors[idx]
        // Generate paired gradient bottom shade (28% darker)
        val r = (Color.red(top) * 0.72f).toInt()
        val g = (Color.green(top) * 0.72f).toInt()
        val b = (Color.blue(top) * 0.72f).toInt()
        val bottom = Color.rgb(r, g, b)
        return Pair(top, bottom)
    }

    companion object {
        fun fromId(id: String): ColorPalette {
            return entries.firstOrNull { it.id == id } ?: VIBRANT_NEON
        }
    }
}
