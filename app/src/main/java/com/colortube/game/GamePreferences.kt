package com.colortube.game

import android.content.Context
import android.content.SharedPreferences

/**
 * GamePreferences
 *
 * Saves and loads game progress, level unlocks, currency, and settings locally.
 * Fully offline, no login required.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("colortube_prefs", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_LEVEL, value).apply()

    var highestUnlockedLevel: Int
        get() = prefs.getInt(KEY_HIGHEST_UNLOCKED_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_UNLOCKED_LEVEL, value).apply()

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 250) // Starting balance
        set(value) = prefs.edit().putInt(KEY_COINS, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var isTutorialCompleted: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, value).apply()

    var selectedTubeSkin: TubeSkin
        get() {
            val id = prefs.getString(KEY_SELECTED_TUBE_SKIN, TubeSkin.CLASSIC_TUBE.id) ?: TubeSkin.CLASSIC_TUBE.id
            return TubeSkin.fromId(id)
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_TUBE_SKIN, value.id).apply()

    var selectedBackgroundTheme: BackgroundTheme
        get() {
            val id = prefs.getString(KEY_SELECTED_BG_THEME, BackgroundTheme.DARK_LAB.id) ?: BackgroundTheme.DARK_LAB.id
            return BackgroundTheme.fromId(id)
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_BG_THEME, value.id).apply()

    var selectedColorPalette: ColorPalette
        get() {
            val id = prefs.getString(KEY_SELECTED_COLOR_PALETTE, ColorPalette.VIBRANT_NEON.id) ?: ColorPalette.VIBRANT_NEON.id
            return ColorPalette.fromId(id)
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_COLOR_PALETTE, value.id).apply()

    fun isTubeSkinUnlocked(skin: TubeSkin): Boolean {
        return highestUnlockedLevel >= skin.unlockLevel || isItemPurchased("tube_" + skin.id)
    }

    fun isBackgroundThemeUnlocked(theme: BackgroundTheme): Boolean {
        return highestUnlockedLevel >= theme.unlockLevel || isItemPurchased("theme_" + theme.id)
    }

    fun isColorPaletteUnlocked(palette: ColorPalette): Boolean {
        return highestUnlockedLevel >= palette.unlockLevel || isItemPurchased("color_" + palette.id)
    }

    fun isItemPurchased(purchaseKey: String): Boolean {
        return prefs.getBoolean("purchased_$purchaseKey", false)
    }

    fun markItemPurchased(purchaseKey: String) {
        prefs.edit().putBoolean("purchased_$purchaseKey", true).apply()
    }

    /**
     * Checks if exact level reward was already granted to prevent double-claiming on reopen.
     */
    fun isLevelRewardClaimed(levelNumber: Int): Boolean {
        return prefs.getBoolean(KEY_LEVEL_REWARD_CLAIMED_PREFIX + levelNumber, false)
    }

    fun markLevelRewardClaimed(levelNumber: Int) {
        prefs.edit().putBoolean(KEY_LEVEL_REWARD_CLAIMED_PREFIX + levelNumber, true).apply()
    }

    fun getLevelStars(levelNumber: Int): Int {
        return prefs.getInt(KEY_LEVEL_STARS_PREFIX + levelNumber, 0)
    }

    fun setLevelStars(levelNumber: Int, stars: Int) {
        val current = getLevelStars(levelNumber)
        if (stars > current) {
            prefs.edit().putInt(KEY_LEVEL_STARS_PREFIX + levelNumber, stars).apply()
        }
    }

    fun getTotalStars(): Int {
        var total = 0
        for (i in 1..highestUnlockedLevel) {
            total += getLevelStars(i)
        }
        return total
    }

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_HIGHEST_UNLOCKED_LEVEL = "highest_unlocked_level"
        private const val KEY_COINS = "coins"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
        private const val KEY_SELECTED_TUBE_SKIN = "selected_tube_skin"
        private const val KEY_SELECTED_BG_THEME = "selected_bg_theme"
        private const val KEY_SELECTED_COLOR_PALETTE = "selected_color_palette"
        private const val KEY_LEVEL_REWARD_CLAIMED_PREFIX = "level_reward_claimed_"
        private const val KEY_LEVEL_STARS_PREFIX = "level_stars_"
    }
}
