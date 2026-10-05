package com.colortube.game

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * MainActivity
 *
 * Coordinates screens, state, input flow, sound/haptics, and AdMob mediation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: GamePreferences
    private lateinit var sound: SoundManager

    // Screens & Modals
    private lateinit var rootContainer: FrameLayout
    private lateinit var screenSplash: LinearLayout
    private lateinit var screenHome: LinearLayout
    private lateinit var screenGameplay: LinearLayout
    private lateinit var screenLevelSelect: LinearLayout
    private lateinit var screenCollection: LinearLayout
    private lateinit var modalSettings: FrameLayout
    private lateinit var modalVictory: FrameLayout
    private lateinit var adBannerContainer: FrameLayout

    // Home UI
    private lateinit var tvHomeCoins: TextView
    private lateinit var tvHomeLevelBadge: TextView
    private lateinit var tvHomeStars: TextView
    private lateinit var btnPlayContinue: Button
    private lateinit var btnLevelSelect: Button
    private lateinit var cardHomeCustomize: LinearLayout
    private lateinit var btnHomeSettings: ImageView

    // Collection UI
    private lateinit var btnCollectionBack: ImageView
    private lateinit var tvCollectionCoins: TextView
    private lateinit var btnTabTubes: Button
    private lateinit var btnTabTheme: Button
    private lateinit var btnTabColor: Button
    private lateinit var recyclerCollection: RecyclerView
    private lateinit var btnCollectionFreeCoins: LinearLayout
    private var collectionAdapter: CollectionAdapter? = null
    private var currentCollectionTab: String = "tubes" // "tubes", "theme", or "color"

    // Gameplay UI
    private lateinit var tvGameLevel: TextView
    private lateinit var tvGameMoves: TextView
    private lateinit var tvGameCoins: TextView
    private lateinit var tvGameStatus: TextView
    private lateinit var puzzleBoardView: PuzzleBoardView
    private lateinit var btnUndo: Button
    private lateinit var btnRestart: Button
    private lateinit var btnHint: Button
    private lateinit var btnExtraTube: Button
    private lateinit var btnGameBack: ImageView

    // Level Select UI
    private lateinit var recyclerLevels: RecyclerView
    private lateinit var btnLevelSelectBack: ImageView

    // Settings UI
    private lateinit var switchSound: SwitchCompat
    private lateinit var switchMusic: SwitchCompat
    private lateinit var switchHaptics: SwitchCompat
    private lateinit var btnSettingsClose: Button

    // Victory UI
    private lateinit var tvVictoryTitle: TextView
    private lateinit var tvVictoryStats: TextView
    private lateinit var btnVictoryDoubleCoins: Button
    private lateinit var btnVictoryNext: Button

    // Unlock with Coins Modal UI
    private lateinit var modalUnlock: FrameLayout
    private lateinit var tvUnlockTitle: TextView
    private lateinit var tvUnlockSubtitle: TextView
    private lateinit var tvUnlockBalance: TextView
    private lateinit var tvUnlockPrice: TextView
    private lateinit var tvUnlockShortage: TextView
    private lateinit var btnUnlockConfirm: Button
    private lateinit var btnUnlockWatchAd: Button
    private lateinit var btnUnlockCancel: Button
    private var pendingUnlockItem: CollectionItem? = null

    // Game Runtime State
    private var engine: GameEngine? = null
    private var selectedTubeIndex: Int? = null
    private var hintPair: Pair<Int, Int>? = null
    private var currentActiveScreen: String = "splash"

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = GamePreferences(this)
        sound = SoundManager(this, prefs)
        AdManager.updateCurrentActivity(this)

        initViews()
        setupWindowInsets()
        setupListeners()
        setupBackNavigation()

        // Load anchored adaptive banner ad
        AdManager.loadBanner(this, adBannerContainer)

        // Splash sequence
        if (intent?.hasExtra("screen") == true) {
            handleIntentScreen(intent)
        } else {
            handler.postDelayed({
                if (currentActiveScreen == "splash") {
                    showScreen("home")
                }
            }, 1200)
        }
    }

    override fun onPause() {
        AdManager.pauseBanner()
        super.onPause()
    }

    override fun onDestroy() {
        AdManager.destroyBanner()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        AdManager.updateCurrentActivity(this)
        AdManager.resumeBanner()
        updateCoinsDisplay()
        if (currentActiveScreen == "gameplay" && engine != null) {
            puzzleBoardView.requestLayout()
            puzzleBoardView.invalidate()
        }
        handleIntentScreen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentScreen(intent)
    }

    private fun handleIntentScreen(intent: Intent?) {
        if (intent == null) return
        val target = intent.getStringExtra("screen")
        if (target != null) {
            showScreen(target)
            intent.getStringExtra("tab")?.let { tab ->
                switchCollectionTab(tab)
            }
            intent.removeExtra("screen")
            intent.removeExtra("tab")
        }
        val targetLevel = intent.getIntExtra("level", -1)
        if (targetLevel in 1..LevelManager.TOTAL_LEVELS) {
            startLevel(targetLevel)
            intent.removeExtra("level")
        }
    }

    private fun initViews() {
        rootContainer = findViewById(R.id.rootContainer)
        screenSplash = findViewById(R.id.screenSplash)
        screenHome = findViewById(R.id.screenHome)
        screenGameplay = findViewById(R.id.screenGameplay)
        screenLevelSelect = findViewById(R.id.screenLevelSelect)
        screenCollection = findViewById(R.id.screenCollection)
        modalSettings = findViewById(R.id.modalSettings)
        modalVictory = findViewById(R.id.modalVictory)

        // Home
        tvHomeCoins = findViewById(R.id.tvHomeCoins)
        tvHomeLevelBadge = findViewById(R.id.tvHomeLevelBadge)
        tvHomeStars = findViewById(R.id.tvHomeStars)
        btnPlayContinue = findViewById(R.id.btnPlayContinue)
        btnLevelSelect = findViewById(R.id.btnLevelSelect)
        cardHomeCustomize = findViewById(R.id.cardHomeCustomize)
        btnHomeSettings = findViewById(R.id.btnHomeSettings)

        // Collection
        btnCollectionBack = findViewById(R.id.btnCollectionBack)
        tvCollectionCoins = findViewById(R.id.tvCollectionCoins)
        btnTabTubes = findViewById(R.id.btnTabTubes)
        btnTabTheme = findViewById(R.id.btnTabTheme)
        btnTabColor = findViewById(R.id.btnTabColor)
        recyclerCollection = findViewById(R.id.recyclerCollection)
        btnCollectionFreeCoins = findViewById(R.id.btnCollectionFreeCoins)

        // Gameplay
        tvGameLevel = findViewById(R.id.tvGameLevel)
        tvGameMoves = findViewById(R.id.tvGameMoves)
        tvGameCoins = findViewById(R.id.tvGameCoins)
        tvGameStatus = findViewById(R.id.tvGameStatus)
        puzzleBoardView = findViewById(R.id.puzzleBoardView)
        btnUndo = findViewById(R.id.btnUndo)
        btnRestart = findViewById(R.id.btnRestart)
        btnHint = findViewById(R.id.btnHint)
        btnExtraTube = findViewById(R.id.btnExtraTube)
        btnGameBack = findViewById(R.id.btnGameBack)

        // Level Select
        recyclerLevels = findViewById(R.id.recyclerLevels)
        btnLevelSelectBack = findViewById(R.id.btnLevelSelectBack)

        // Settings
        switchSound = findViewById(R.id.switchSound)
        switchMusic = findViewById(R.id.switchMusic)
        switchHaptics = findViewById(R.id.switchHaptics)
        btnSettingsClose = findViewById(R.id.btnSettingsClose)

        // Victory
        tvVictoryTitle = findViewById(R.id.tvVictoryTitle)
        tvVictoryStats = findViewById(R.id.tvVictoryStats)
        btnVictoryDoubleCoins = findViewById(R.id.btnVictoryDoubleCoins)
        btnVictoryNext = findViewById(R.id.btnVictoryNext)

        // Banner Ad Container
        adBannerContainer = findViewById(R.id.adBannerContainer)

        // Modal Unlock
        modalUnlock = findViewById(R.id.modalUnlock)
        tvUnlockTitle = findViewById(R.id.tvUnlockTitle)
        tvUnlockSubtitle = findViewById(R.id.tvUnlockSubtitle)
        tvUnlockBalance = findViewById(R.id.tvUnlockBalance)
        tvUnlockPrice = findViewById(R.id.tvUnlockPrice)
        tvUnlockShortage = findViewById(R.id.tvUnlockShortage)
        btnUnlockConfirm = findViewById(R.id.btnUnlockConfirm)
        btnUnlockWatchAd = findViewById(R.id.btnUnlockWatchAd)
        btnUnlockCancel = findViewById(R.id.btnUnlockCancel)
    }

    private fun setupListeners() {
        // Home
        btnPlayContinue.setOnClickListener {
            sound.playClick()
            startLevel(prefs.currentLevel)
        }
        btnLevelSelect.setOnClickListener {
            sound.playClick()
            showScreen("level_select")
        }
        btnHomeSettings.setOnClickListener {
            sound.playClick()
            openSettings()
        }
        val openCollection = View.OnClickListener {
            sound.playClick()
            showScreen("collection")
        }
        cardHomeCustomize.setOnClickListener(openCollection)
        for (i in 0 until cardHomeCustomize.childCount) {
            cardHomeCustomize.getChildAt(i).setOnClickListener(openCollection)
        }

        // Collection Listeners
        btnCollectionBack.setOnClickListener {
            sound.playClick()
            showScreen("home")
        }
        btnTabTubes.setOnClickListener {
            sound.playClick()
            switchCollectionTab("tubes")
        }
        btnTabTheme.setOnClickListener {
            sound.playClick()
            switchCollectionTab("theme")
        }
        btnTabColor.setOnClickListener {
            sound.playClick()
            switchCollectionTab("color")
        }
        btnCollectionFreeCoins.setOnClickListener {
            sound.playClick()
            var rewarded = false
            AdManager.showRewardedAd(
                activity = this,
                rewardType = "collection_coins",
                onRewardEarned = { _, _ ->
                    if (!rewarded) {
                        rewarded = true
                        prefs.coins += 25
                        updateCoinsDisplay()
                        sound.playReward()
                        Toast.makeText(this, "+25 Coins added to balance!", Toast.LENGTH_SHORT).show()
                        collectionAdapter?.notifyDataSetChanged()
                    }
                }
            )
        }

        // Gameplay Back
        btnGameBack.setOnClickListener {
            sound.playClick()
            showScreen("home")
        }

        // Puzzle Board Tube Click
        puzzleBoardView.onTubeClickListener = { clickedIndex ->
            handleTubeClick(clickedIndex)
        }

        // Controls
        btnUndo.setOnClickListener {
            if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return@setOnClickListener
            sound.playClick()
            handleUndo()
        }
        btnRestart.setOnClickListener {
            if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return@setOnClickListener
            sound.playClick()
            engine?.let { startLevel(it.levelNumber) }
        }
        btnHint.setOnClickListener {
            if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return@setOnClickListener
            sound.playClick()
            handleHint()
        }
        btnExtraTube.setOnClickListener {
            if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return@setOnClickListener
            sound.playClick()
            handleExtraTube()
        }

        // Level Select
        btnLevelSelectBack.setOnClickListener {
            sound.playClick()
            showScreen("home")
        }

        // Settings
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
        }
        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            prefs.musicEnabled = isChecked
        }
        switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            prefs.hapticsEnabled = isChecked
        }
        btnSettingsClose.setOnClickListener {
            sound.playClick()
            modalSettings.visibility = View.GONE
        }

        // Victory
        btnVictoryNext.setOnClickListener {
            sound.playClick()
            modalVictory.visibility = View.GONE
            val nextLevel = (engine?.levelNumber ?: 1) + 1
            AdManager.onLevelCompleted(this) {
                startLevel(nextLevel)
            }
        }
        btnVictoryDoubleCoins.setOnClickListener {
            sound.playClick()
            var rewarded = false
            AdManager.showRewardedAd(
                activity = this,
                rewardType = "double_coins",
                onRewardEarned = { _, _ ->
                    if (!rewarded) {
                        rewarded = true
                        prefs.coins += 25
                        updateCoinsDisplay()
                        sound.playReward()
                        Toast.makeText(this, "Bonus earned! +25 Coins", Toast.LENGTH_SHORT).show()
                        btnVictoryDoubleCoins.isEnabled = false
                        btnVictoryDoubleCoins.alpha = 0.5f
                    }
                }
            )
        }

        // Modal Unlock
        modalUnlock.setOnClickListener {
            modalUnlock.visibility = View.GONE
        }
        btnUnlockCancel.setOnClickListener {
            sound.playClick()
            modalUnlock.visibility = View.GONE
        }
        btnUnlockConfirm.setOnClickListener {
            sound.playClick()
            handleConfirmUnlock()
        }
        btnUnlockWatchAd.setOnClickListener {
            sound.playClick()
            handleWatchAdForUnlock()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    modalUnlock.visibility == View.VISIBLE -> modalUnlock.visibility = View.GONE
                    modalSettings.visibility == View.VISIBLE -> modalSettings.visibility = View.GONE
                    modalVictory.visibility == View.VISIBLE -> modalVictory.visibility = View.GONE
                    currentActiveScreen == "gameplay" -> showScreen("home")
                    currentActiveScreen == "level_select" -> showScreen("home")
                    currentActiveScreen == "collection" -> showScreen("home")
                    else -> finish()
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val bannerReservePx = (66 * resources.displayMetrics.density).toInt()

            // Banner container sits right above the system navigation bar with zero extra top padding
            adBannerContainer.setPadding(0, 0, 0, navBars.bottom)

            // Content screens have status bar padding at the top and navigation bar + banner reserve at the bottom
            val contentScreens = listOf(screenHome, screenGameplay, screenLevelSelect, screenCollection)
            for (scr in contentScreens) {
                scr.setPadding(0, statusBars.top, 0, navBars.bottom + bannerReservePx)
            }

            insets
        }
        ViewCompat.requestApplyInsets(rootContainer)
    }

    private fun showScreen(screen: String) {
        currentActiveScreen = screen
        screenSplash.visibility = if (screen == "splash") View.VISIBLE else View.GONE
        screenHome.visibility = if (screen == "home") View.VISIBLE else View.GONE
        screenGameplay.visibility = if (screen == "gameplay") View.VISIBLE else View.GONE
        screenLevelSelect.visibility = if (screen == "level_select") View.VISIBLE else View.GONE
        screenCollection.visibility = if (screen == "collection") View.VISIBLE else View.GONE

        // Keep banner hidden on splash screen
        if (screen == "splash") {
            adBannerContainer.visibility = View.GONE
        }

        // Apply active theme background & tube skin
        applyCurrentTheme()

        if (screen == "home") {
            updateHomeDisplay()
        } else if (screen == "level_select") {
            setupLevelSelectRecycler()
        } else if (screen == "collection") {
            setupCollectionScreen()
        }
    }

    private fun applyCurrentTheme() {
        // 1. Background Theme (Procedural vector background with subtle line-art & rich dark gradients)
        val theme = prefs.selectedBackgroundTheme
        rootContainer.background = ThemeBackgroundDrawable(theme)

        // 2. Tube Skin
        puzzleBoardView.currentTubeSkin = prefs.selectedTubeSkin

        // 3. Color Palette
        puzzleBoardView.currentColorPalette = prefs.selectedColorPalette
    }

    private fun setupCollectionScreen() {
        tvCollectionCoins.text = prefs.coins.toString()
        recyclerCollection.layoutManager = GridLayoutManager(this, 2)
        if (collectionAdapter == null) {
            collectionAdapter = CollectionAdapter(
                prefs = prefs,
                sound = sound,
                onItemClick = { item ->
                    handleCollectionItemClick(item)
                }
            )
            recyclerCollection.adapter = collectionAdapter
        }
        refreshCollectionItems()
    }

    private fun switchCollectionTab(tab: String) {
        currentCollectionTab = tab

        btnTabTubes.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        btnTabTubes.setTextColor(getColor(R.color.on_surface_variant))
        btnTabTheme.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        btnTabTheme.setTextColor(getColor(R.color.on_surface_variant))
        btnTabColor.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        btnTabColor.setTextColor(getColor(R.color.on_surface_variant))

        when (tab) {
            "tubes" -> {
                btnTabTubes.setBackgroundResource(R.drawable.bg_btn_primary_3d)
                btnTabTubes.setTextColor(getColor(R.color.on_primary))
            }
            "theme" -> {
                btnTabTheme.setBackgroundResource(R.drawable.bg_btn_primary_3d)
                btnTabTheme.setTextColor(getColor(R.color.on_primary))
            }
            "color" -> {
                btnTabColor.setBackgroundResource(R.drawable.bg_btn_primary_3d)
                btnTabColor.setTextColor(getColor(R.color.on_primary))
            }
        }
        refreshCollectionItems()
    }

    private fun refreshCollectionItems() {
        val items: List<CollectionItem> = when (currentCollectionTab) {
            "tubes" -> TubeSkin.entries.map { CollectionItem.TubeItem(it) }
            "theme" -> BackgroundTheme.entries.map { CollectionItem.BackgroundItem(it) }
            "color" -> ColorPalette.entries.map { CollectionItem.ColorItem(it) }
            else -> emptyList()
        }
        collectionAdapter?.setItems(items)
    }

    private fun handleCollectionItemClick(item: CollectionItem) {
        val isUnlocked = when (item) {
            is CollectionItem.TubeItem -> prefs.isTubeSkinUnlocked(item.skin)
            is CollectionItem.BackgroundItem -> prefs.isBackgroundThemeUnlocked(item.theme)
            is CollectionItem.ColorItem -> prefs.isColorPaletteUnlocked(item.palette)
        }

        if (isUnlocked) {
            when (item) {
                is CollectionItem.TubeItem -> prefs.selectedTubeSkin = item.skin
                is CollectionItem.BackgroundItem -> prefs.selectedBackgroundTheme = item.theme
                is CollectionItem.ColorItem -> prefs.selectedColorPalette = item.palette
            }
            applyCurrentTheme()
            sound.playTubeSelect()
            Toast.makeText(this, "${item.title} Equipped!", Toast.LENGTH_SHORT).show()
            collectionAdapter?.notifyDataSetChanged()
        } else {
            showUnlockConfirmDialog(item)
        }
    }

    private fun showUnlockConfirmDialog(item: CollectionItem) {
        pendingUnlockItem = item
        val formattedPrice = String.format(java.util.Locale.US, "%,d", item.coinPrice)
        val formattedBalance = String.format(java.util.Locale.US, "%,d", prefs.coins)

        tvUnlockTitle.text = "Unlock ${item.title}"
        tvUnlockSubtitle.text = "Requires Level ${item.unlockLevel} or unlock now with coins"
        tvUnlockBalance.text = "Your Coins: 🪙 $formattedBalance"
        tvUnlockPrice.text = "Cost: 🪙 $formattedPrice"

        val canAfford = prefs.coins >= item.coinPrice
        if (canAfford) {
            tvUnlockShortage.visibility = View.GONE
            btnUnlockConfirm.isEnabled = true
            btnUnlockConfirm.alpha = 1.0f
            btnUnlockConfirm.text = "UNLOCK FOR $formattedPrice 🪙"
            btnUnlockWatchAd.visibility = View.GONE
        } else {
            val shortage = item.coinPrice - prefs.coins
            val formattedShortage = String.format(java.util.Locale.US, "%,d", shortage)
            tvUnlockShortage.visibility = View.VISIBLE
            tvUnlockShortage.text = "Need $formattedShortage more coins"
            btnUnlockConfirm.isEnabled = false
            btnUnlockConfirm.alpha = 0.5f
            btnUnlockConfirm.text = "NOT ENOUGH COINS"
            btnUnlockWatchAd.visibility = View.VISIBLE
        }

        modalUnlock.visibility = View.VISIBLE
    }

    private fun handleConfirmUnlock() {
        val item = pendingUnlockItem ?: return
        if (prefs.coins < item.coinPrice) {
            sound.playInvalidMove()
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.coins -= item.coinPrice
        prefs.markItemPurchased(item.purchaseKey)

        when (item) {
            is CollectionItem.TubeItem -> prefs.selectedTubeSkin = item.skin
            is CollectionItem.BackgroundItem -> prefs.selectedBackgroundTheme = item.theme
            is CollectionItem.ColorItem -> prefs.selectedColorPalette = item.palette
        }
        applyCurrentTheme()
        sound.playReward()

        updateCoinsDisplay()
        collectionAdapter?.notifyDataSetChanged()
        modalUnlock.visibility = View.GONE

        Toast.makeText(this, "🎉 ${item.title} Unlocked & Equipped!", Toast.LENGTH_SHORT).show()
    }

    private fun handleWatchAdForUnlock() {
        var rewarded = false
        AdManager.showRewardedAd(
            activity = this,
            rewardType = "unlock_dialog_coins",
            onRewardEarned = { _, _ ->
                if (!rewarded) {
                    rewarded = true
                    prefs.coins += 25
                    updateCoinsDisplay()
                    sound.playReward()
                    Toast.makeText(this, "+25 Coins added to balance!", Toast.LENGTH_SHORT).show()
                    collectionAdapter?.notifyDataSetChanged()
                    pendingUnlockItem?.let { showUnlockConfirmDialog(it) }
                }
            }
        )
    }

    private fun updateHomeDisplay() {
        tvHomeCoins.text = prefs.coins.toString()
        val cur = prefs.currentLevel
        tvHomeLevelBadge.text = "Level $cur • Puzzle Flow"
        val totalStars = prefs.getTotalStars()
        tvHomeStars.text = "★ $totalStars Stars"
    }

    private fun updateCoinsDisplay() {
        tvHomeCoins.text = prefs.coins.toString()
        tvGameCoins.text = prefs.coins.toString()
        tvCollectionCoins.text = prefs.coins.toString()
    }

    private fun setupLevelSelectRecycler() {
        recyclerLevels.layoutManager = GridLayoutManager(this, 4)
        recyclerLevels.adapter = LevelAdapter(
            totalLevels = LevelManager.TOTAL_LEVELS,
            highestUnlocked = prefs.highestUnlockedLevel,
            prefs = prefs,
            onLevelClick = { levelNum ->
                sound.playClick()
                startLevel(levelNum)
            }
        )
    }

    private fun openSettings() {
        switchSound.isChecked = prefs.soundEnabled
        switchMusic.isChecked = prefs.musicEnabled
        switchHaptics.isChecked = prefs.hapticsEnabled
        modalSettings.visibility = View.VISIBLE
    }

    private fun startLevel(levelNum: Int) {
        puzzleBoardView.stopCelebration()
        val eng = LevelManager.buildInitialEngine(levelNum)
        engine = eng
        selectedTubeIndex = null
        hintPair = null

        prefs.currentLevel = levelNum
        if (levelNum > prefs.highestUnlockedLevel) {
            prefs.highestUnlockedLevel = levelNum
        }

        tvGameLevel.text = "Level $levelNum"
        tvGameMoves.text = "Moves: 0"
        tvGameStatus.text = "Tap source tube, then destination tube"
        updateCoinsDisplay()

        puzzleBoardView.setTubes(eng.tubes, selectedIndex = null)
        showScreen("gameplay")
    }

    private fun handleTubeClick(clickedIndex: Int) {
        if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return
        val eng = engine ?: return
        if (eng.isLevelComplete) return

        val prevSelected = selectedTubeIndex

        if (prevSelected == null) {
            // Select source
            val tube = eng.tubes[clickedIndex]
            if (tube.isEmpty()) {
                tvGameStatus.text = "Empty tube! Tap a tube with liquid first."
                sound.playInvalidMove()
                return
            }
            if (tube.isCompleted()) {
                tvGameStatus.text = "This tube is already completed!"
                sound.playInvalidMove()
                return
            }

            selectedTubeIndex = clickedIndex
            sound.playTubeSelect()
            tvGameStatus.text = "Now tap destination tube"
            puzzleBoardView.setTubes(eng.tubes, selectedIndex = clickedIndex, hintPair = hintPair)
        } else {
            // Deselect if same tube
            if (prevSelected == clickedIndex) {
                selectedTubeIndex = null
                sound.playTubeSelect()
                tvGameStatus.text = "Selection cancelled"
                puzzleBoardView.setTubes(eng.tubes, selectedIndex = null, hintPair = hintPair)
                return
            }

            // Attempt pour
            if (eng.canPour(prevSelected, clickedIndex)) {
                val srcTube = eng.tubes[prevSelected]
                val destTube = eng.tubes[clickedIndex]
                val color = srcTube.topColor()!!
                val countToPour = minOf(srcTube.topCount(), destTube.capacity - destTube.layers.size)

                tvGameStatus.text = "Pouring ${color.displayName}..."

                // Trigger synchronized physical pouring animation with volume-based audio and stream
                puzzleBoardView.startPourAnimation(
                    fromIndex = prevSelected,
                    toIndex = clickedIndex,
                    color = color,
                    count = countToPour,
                    onStreamStart = { flowDurationMs ->
                        sound.playPourStream(volumeUnits = countToPour, flowDurationMs = flowDurationMs)
                    },
                    onStreamEnd = {
                        sound.playPourLanding()
                    },
                    onFinished = {
                        // Update state in puzzle engine
                        val pouredCount = eng.pour(prevSelected, clickedIndex)

                        selectedTubeIndex = null
                        hintPair = null
                        tvGameMoves.text = "Moves: ${eng.moveCount}"

                        val updatedDest = eng.tubes[clickedIndex]
                        // Update views with actual transferred liquid FIRST
                        puzzleBoardView.setTubes(eng.tubes, selectedIndex = null)

                        if (updatedDest.isCompleted()) {
                            sound.playTubeComplete()
                            tvGameStatus.text = "Tube completed! 🎉"
                            puzzleBoardView.playTubeCompletionCelebration(
                                tubeIndex = clickedIndex,
                                completedColor = updatedDest.topColor() ?: color,
                                onCelebrationDone = {
                                    if (eng.isLevelComplete) {
                                        handleVictory()
                                    }
                                }
                            )
                        } else {
                            tvGameStatus.text = "Liquid poured successfully!"
                            if (eng.isLevelComplete) {
                                handleVictory()
                            }
                        }
                    }
                )
            } else {
                // Illegal pour
                sound.playInvalidMove()
                tvGameStatus.text = "Invalid move! Match color or find empty space."
                selectedTubeIndex = null
                puzzleBoardView.setTubes(eng.tubes, selectedIndex = null, hintPair = hintPair)
            }
        }
    }

    private fun handleUndo() {
        if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return
        val eng = engine ?: return
        if (eng.undo()) {
            sound.playClick()
            tvGameMoves.text = "Moves: ${eng.moveCount}"
            tvGameStatus.text = "Move undone"
            selectedTubeIndex = null
            puzzleBoardView.setTubes(eng.tubes, selectedIndex = null)
        } else {
            tvGameStatus.text = "No moves to undo"
        }
    }

    private fun handleHint() {
        if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return
        val eng = engine ?: return
        val hint = eng.findHint()
        if (hint != null) {
            hintPair = hint
            sound.playTubeSelect()
            tvGameStatus.text = "Hint: Pour from Tube ${hint.first + 1} to Tube ${hint.second + 1}"
            puzzleBoardView.setTubes(eng.tubes, selectedIndex = selectedTubeIndex, hintPair = hint)
        } else {
            tvGameStatus.text = "No moves available! Try +Tube or Restart."
            sound.playInvalidMove()
        }
    }

    private fun handleExtraTube() {
        if (puzzleBoardView.isPouring || puzzleBoardView.isCelebrating) return
        val eng = engine ?: return
        if (eng.isLevelComplete) return

        val maxAllowed = GameEngine.getMaxExtraTubes(eng.levelNumber)
        val currentExtraCount = eng.tubes.count { it.isExtraTube }

        if (currentExtraCount >= maxAllowed) {
            val message = if (maxAllowed == 1) {
                "Extra tube already added for Level ${eng.levelNumber} (Max $maxAllowed)."
            } else {
                "Maximum extra tubes ($maxAllowed) already added for Level ${eng.levelNumber}."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            tvGameStatus.text = message
            return
        }

        val nextTubeNumber = currentExtraCount + 1
        val dialogMessage = if (maxAllowed > 1) {
            "Watch a short video ad to unlock Extra Tube $nextTubeNumber of $maxAllowed for Level ${eng.levelNumber}."
        } else {
            "Watch a short video ad to unlock an extra empty glass tube for Level ${eng.levelNumber}."
        }

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_ColorTube_Dialog)
            .setTitle("Watch Ad to Get an Extra Tube")
            .setMessage(dialogMessage)
            .setPositiveButton("Watch Ad 📺") { dialog, _ ->
                dialog.dismiss()
                sound.playClick()
                tvGameStatus.text = "Loading video ad..."

                var rewardEarned = false

                AdManager.showRewardedAd(
                    activity = this,
                    rewardType = "extra_tube",
                    onRewardEarned = { _, _ ->
                        rewardEarned = true
                    },
                    onAdClosed = {
                        if (rewardEarned) {
                            applyExtraTubeReward()
                        }
                    },
                    onAdNotReady = {
                        tvGameStatus.text = "Ad is loading, please try again in a moment."
                        Toast.makeText(this, "Ad is loading. Please try again in a few seconds.", Toast.LENGTH_SHORT).show()
                    },
                    onAdSkipped = {
                        tvGameStatus.text = "Ad skipped. Extra tube was not added."
                        Toast.makeText(this, "Ad closed before completion. No extra tube added.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                sound.playClick()
            }
            .show()
    }

    private fun applyExtraTubeReward() {
        val eng = engine ?: return
        if (eng.isLevelComplete) return

        // 1. Add strictly ONE new empty tube to actual game state
        val added = eng.addExtraTube()
        if (!added) return

        selectedTubeIndex = null
        hintPair = null

        val maxAllowed = GameEngine.getMaxExtraTubes(eng.levelNumber)
        val currentExtraCount = eng.tubes.count { it.isExtraTube }

        // 2. Re-render board immediately with updated tubes
        puzzleBoardView.setTubes(eng.tubes, selectedIndex = null)

        // 3. Confirm visually and audibly AFTER the tube is inserted and laid out
        puzzleBoardView.post {
            sound.playTubeSelect()
            val statusMsg = if (maxAllowed > 1) {
                "Extra tube ($currentExtraCount/$maxAllowed) added! Ready to pour."
            } else {
                "Extra tube added! Ready to pour."
            }
            tvGameStatus.text = statusMsg
            Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVictory() {
        val eng = engine ?: return
        sound.playLevelWin()

        // Calculate stars based on efficiency
        val moves = eng.moveCount
        val stars = when {
            moves <= 16 -> 3
            moves <= 26 -> 2
            else -> 1
        }
        prefs.setLevelStars(eng.levelNumber, stars)

        // 5. Coin Economy - Exactly 10 Coins per completed level, never duplicated
        val baseCoins = 10
        val isFirstTimeClaim = !prefs.isLevelRewardClaimed(eng.levelNumber)
        if (isFirstTimeClaim) {
            prefs.coins += baseCoins
            prefs.markLevelRewardClaimed(eng.levelNumber)
            updateCoinsDisplay()
        }

        // Unlock next level
        if (eng.levelNumber >= prefs.highestUnlockedLevel) {
            prefs.highestUnlockedLevel = eng.levelNumber + 1
        }

        tvVictoryTitle.text = "STAGE ${eng.levelNumber} MASTERED!"
        tvVictoryStats.text = if (isFirstTimeClaim) {
            "Solved in $moves moves • $stars Stars • +$baseCoins Coins"
        } else {
            "Solved in $moves moves • $stars Stars • Completed"
        }
        btnVictoryDoubleCoins.isEnabled = isFirstTimeClaim
        btnVictoryDoubleCoins.alpha = if (isFirstTimeClaim) 1.0f else 0.5f

        handler.postDelayed({
            modalVictory.visibility = View.VISIBLE
        }, 500)
    }
}
