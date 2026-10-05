package com.colortube.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class CollectionItem {
    data class TubeItem(val skin: TubeSkin) : CollectionItem()
    data class BackgroundItem(val theme: BackgroundTheme) : CollectionItem()
    data class ColorItem(val palette: ColorPalette) : CollectionItem()

    val title: String
        get() = when (this) {
            is TubeItem -> skin.displayName
            is BackgroundItem -> theme.displayName
            is ColorItem -> palette.displayName
        }

    val unlockLevel: Int
        get() = when (this) {
            is TubeItem -> skin.unlockLevel
            is BackgroundItem -> theme.unlockLevel
            is ColorItem -> palette.unlockLevel
        }

    val coinPrice: Int
        get() = when (this) {
            is TubeItem -> skin.coinPrice
            is BackgroundItem -> theme.coinPrice
            is ColorItem -> palette.coinPrice
        }

    val purchaseKey: String
        get() = when (this) {
            is TubeItem -> "tube_" + skin.id
            is BackgroundItem -> "theme_" + theme.id
            is ColorItem -> "color_" + palette.id
        }
}

class CollectionAdapter(
    private val prefs: GamePreferences,
    private val sound: SoundManager,
    private val onItemClick: (CollectionItem) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    private val items = mutableListOf<CollectionItem>()

    fun setItems(newItems: List<CollectionItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card, parent, false)
        view.background = CollectionCardDrawable()
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tubePreview: TubePreviewView = itemView.findViewById(R.id.tubePreviewView)
        private val themePreview: ThemePreviewView = itemView.findViewById(R.id.themePreviewView)
        private val colorPalettePreview: LinearLayout = itemView.findViewById(R.id.colorPalettePreview)
        private val paletteTube1: GlassTubeView = itemView.findViewById(R.id.paletteTube1)
        private val paletteTube2: GlassTubeView = itemView.findViewById(R.id.paletteTube2)
        private val paletteTube3: GlassTubeView = itemView.findViewById(R.id.paletteTube3)

        private val badgeEquipped: TextView = itemView.findViewById(R.id.badgeEquipped)
        private val lockOverlay: LinearLayout = itemView.findViewById(R.id.lockOverlay)
        private val tvLockLevel: TextView = itemView.findViewById(R.id.tvLockLevel)
        private val tvItemTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val btnItemAction: LinearLayout = itemView.findViewById(R.id.btnItemAction)
        private val tvActionText: TextView = itemView.findViewById(R.id.tvActionText)

        fun bind(item: CollectionItem) {
            when (item) {
                is CollectionItem.TubeItem -> {
                    val skin = item.skin
                    tvItemTitle.text = skin.displayName
                    tubePreview.visibility = View.VISIBLE
                    themePreview.visibility = View.GONE
                    colorPalettePreview.visibility = View.GONE

                    tubePreview.skin = skin
                    tubePreview.colorPalette = prefs.selectedColorPalette

                    val isEquipped = prefs.selectedTubeSkin == skin
                    val isUnlocked = prefs.isTubeSkinUnlocked(skin)
                    tubePreview.isLocked = !isUnlocked

                    setupState(
                        isEquipped = isEquipped,
                        isUnlocked = isUnlocked,
                        unlockLevel = skin.unlockLevel,
                        coinPrice = skin.coinPrice
                    )
                }
                is CollectionItem.BackgroundItem -> {
                    val theme = item.theme
                    tvItemTitle.text = theme.displayName
                    tubePreview.visibility = View.GONE
                    themePreview.visibility = View.VISIBLE
                    colorPalettePreview.visibility = View.GONE

                    themePreview.theme = theme

                    val isEquipped = prefs.selectedBackgroundTheme == theme
                    val isUnlocked = prefs.isBackgroundThemeUnlocked(theme)
                    themePreview.alpha = if (isUnlocked) 1.0f else 0.40f

                    setupState(
                        isEquipped = isEquipped,
                        isUnlocked = isUnlocked,
                        unlockLevel = theme.unlockLevel,
                        coinPrice = theme.coinPrice
                    )
                }
                is CollectionItem.ColorItem -> {
                    val palette = item.palette
                    tvItemTitle.text = palette.displayName
                    tubePreview.visibility = View.GONE
                    themePreview.visibility = View.GONE
                    colorPalettePreview.visibility = View.VISIBLE

                    // Set up 3 mini tubes demonstrating palette shades matching reference
                    val t1 = Tube(id = 1, capacity = 4, layers = mutableListOf(LiquidColor.AMBER, LiquidColor.CORAL, LiquidColor.CYAN, LiquidColor.LIME))
                    val t2 = Tube(id = 2, capacity = 4, layers = mutableListOf(LiquidColor.VIOLET, LiquidColor.GOLD, LiquidColor.VIOLET, LiquidColor.AQUA))
                    val t3 = Tube(id = 3, capacity = 4, layers = mutableListOf(LiquidColor.ROSE, LiquidColor.CYAN, LiquidColor.ROSE, LiquidColor.CORAL))

                    paletteTube1.tube = t1
                    paletteTube1.colorPalette = palette
                    paletteTube2.tube = t2
                    paletteTube2.colorPalette = palette
                    paletteTube3.tube = t3
                    paletteTube3.colorPalette = palette

                    val isEquipped = prefs.selectedColorPalette == palette
                    val isUnlocked = prefs.isColorPaletteUnlocked(palette)
                    colorPalettePreview.alpha = if (isUnlocked) 1.0f else 0.40f

                    setupState(
                        isEquipped = isEquipped,
                        isUnlocked = isUnlocked,
                        unlockLevel = palette.unlockLevel,
                        coinPrice = palette.coinPrice
                    )
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
            btnItemAction.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun setupState(
            isEquipped: Boolean,
            isUnlocked: Boolean,
            unlockLevel: Int,
            coinPrice: Int
        ) {
            if (isEquipped) {
                badgeEquipped.visibility = View.VISIBLE
                lockOverlay.visibility = View.GONE
                tvActionText.text = "EQUIPPED"
                tvActionText.setTextColor(itemView.context.getColor(R.color.primary))
                btnItemAction.isEnabled = false
                btnItemAction.alpha = 0.85f
            } else if (isUnlocked) {
                badgeEquipped.visibility = View.GONE
                lockOverlay.visibility = View.GONE
                tvActionText.text = "EQUIP"
                tvActionText.setTextColor(itemView.context.getColor(R.color.on_surface))
                btnItemAction.isEnabled = true
                btnItemAction.alpha = 1.0f
            } else {
                // Locked by level milestone: clean floating lock with level badge
                badgeEquipped.visibility = View.GONE
                lockOverlay.visibility = View.VISIBLE
                tvLockLevel.text = "LEVEL $unlockLevel"

                // Show Coin Unlock Price with golden amber color
                val formattedPrice = String.format(java.util.Locale.US, "%,d", coinPrice)
                tvActionText.text = "🪙 $formattedPrice"
                tvActionText.setTextColor(android.graphics.Color.parseColor("#FFC312"))
                btnItemAction.isEnabled = true
                btnItemAction.alpha = 1.0f
            }
        }
    }
}
