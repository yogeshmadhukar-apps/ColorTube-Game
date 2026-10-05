package com.colortube.game

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * LevelAdapter
 *
 * Renders levels 1 to 100+ with lock/unlock states, star ratings, and click handling.
 */
class LevelAdapter(
    private val totalLevels: Int = LevelManager.TOTAL_LEVELS,
    private val highestUnlocked: Int,
    private val prefs: GamePreferences,
    private val onLevelClick: (levelNumber: Int) -> Unit
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    class LevelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardLevel: FrameLayout = view.findViewById(R.id.cardLevel)
        val tvLevelNumber: TextView = view.findViewById(R.id.tvLevelNumber)
        val imgLock: ImageView = view.findViewById(R.id.imgLock)
        val tvLevelStars: TextView = view.findViewById(R.id.tvLevelStars)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level_grid, parent, false)
        return LevelViewHolder(view)
    }

    override fun getItemCount(): Int = totalLevels

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val levelNum = position + 1
        val isUnlocked = levelNum <= highestUnlocked
        val stars = prefs.getLevelStars(levelNum)

        holder.tvLevelNumber.text = levelNum.toString()

        if (isUnlocked) {
            holder.tvLevelNumber.visibility = View.VISIBLE
            holder.imgLock.visibility = View.GONE
            holder.cardLevel.alpha = 1.0f

            val starsStr = buildString {
                repeat(stars) { append("★") }
                repeat(3 - stars) { append("☆") }
            }
            holder.tvLevelStars.text = starsStr
            holder.tvLevelStars.visibility = View.VISIBLE

            holder.cardLevel.setOnClickListener {
                onLevelClick(levelNum)
            }
        } else {
            holder.tvLevelNumber.visibility = View.GONE
            holder.imgLock.visibility = View.VISIBLE
            holder.cardLevel.alpha = 0.45f
            holder.tvLevelStars.visibility = View.INVISIBLE
            holder.cardLevel.setOnClickListener(null)
        }
    }
}
