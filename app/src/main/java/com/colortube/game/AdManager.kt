package com.colortube.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.lang.ref.WeakReference
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdManager
 *
 * Centralized, production-quality ad orchestration layer.
 * Architecture:
 * Game/UI -> AdManager -> AdMob Mediation (AdMob, Meta Audience Network, Unity Ads)
 *
 * Enforces:
 * - Single MobileAds.initialize()
 * - Production AdMob IDs (Banner, Rewarded, Interstitial)
 * - Configurable frequency control and cooldowns for Interstitials
 * - Strict verification callback for Rewarded ads (never reward without callback)
 * - Safe lifecycle and memory management with WeakReferences
 */
object AdManager {

    private const val TAG = "ColorTubeAdManager"

    // ---- PRODUCTION AD UNIT IDS ----
    var bannerAdUnitId: String = "ca-app-pub-4958842931036358/1616454057"
    var rewardedAdUnitId: String = "ca-app-pub-4958842931036358/1648992076"
    var interstitialAdUnitId: String = "ca-app-pub-4958842931036358/9850167297"

    // Optional mediation configuration hooks (Meta, Unity)
    var metaPlacementId: String = ""
    var unityGameId: String = ""

    // ---- FREQUENCY & COOLDOWN SETTINGS ----
    var interstitialCooldownMs: Long = 45_000L // 45 seconds cooldown between interstitials
    var levelsBetweenInterstitials: Int = 3     // Show interstitial every 3 levels completed

    // ---- STATE TRACKING ----
    private var initialized = false
    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    @Volatile
    private var isShowingAd = false

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerAdView: AdView? = null

    private var lastInterstitialShownTime: Long = 0
    private var levelsCompletedSinceLastAd: Int = 0

    // Lifecycle safe reference
    private var currentActivityRef: WeakReference<Activity>? = null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        try {
            Log.d(TAG, "Initializing MobileAds with mediation (AdMob, Meta, Unity)...")
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "MobileAds initialized: ${initializationStatus.adapterStatusMap.keys}")
                preloadInterstitial(context)
                preloadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun updateCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    // =========================================================================
    // ANCHORED ADAPTIVE BANNER AD
    // =========================================================================

    /**
     * Loads an anchored adaptive banner into [container] and shows it once the ad is received.
     * Uses the full available [Activity] width to let AdMob pick the best height automatically.
     * If loading fails, the container stays GONE so no empty black area is shown.
     *
     * Call this once in Activity.onCreate() / onResume() after MobileAds is initialized.
     */
    fun loadBanner(activity: Activity, container: FrameLayout) {
        // Destroy any previous banner to avoid leaks
        bannerAdView?.destroy()
        bannerAdView = null

        val adView = AdView(activity)
        bannerAdView = adView

        // Anchored adaptive banner — uses device width in dp
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels.takeIf { it > 0 } ?: 360
        val adWidthDp = (adWidthPixels / displayMetrics.density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)

        adView.adUnitId = bannerAdUnitId
        adView.setAdSize(adSize)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded (height=${adSize.height}dp).")
                // Only show the container AFTER the ad has loaded successfully
                container.removeAllViews()
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER_HORIZONTAL
                )
                container.addView(adView, lp)
                container.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner failed to load: ${error.message}")
                // Keep container GONE so layout is not disrupted
                container.visibility = View.GONE
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    fun pauseBanner() {
        bannerAdView?.pause()
    }

    fun resumeBanner() {
        bannerAdView?.resume()
    }

    fun destroyBanner() {
        bannerAdView?.destroy()
        bannerAdView = null
    }

    // =========================================================================
    // REWARDED ADS
    // =========================================================================

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null

    /**
     * Shows a rewarded ad. User is only granted a reward if onUserEarnedReward fires!
     */
    fun showRewardedAd(
        activity: Activity,
        rewardType: String,
        onRewardEarned: (rewardType: String, amount: Int) -> Unit,
        onAdClosed: () -> Unit = {},
        onAdNotReady: () -> Unit = {},
        onAdSkipped: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewardedAd requested but ad not ready. Preloading...")
            preloadRewarded(activity)
            onAdNotReady()
            onAdClosed()
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                rewardedAd = null
                Log.d(TAG, "Rewarded ad displayed.")
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                Log.d(TAG, "Rewarded ad dismissed. Reward granted: $rewardGranted")
                preloadRewarded(activity)
                if (!rewardGranted) {
                    onAdSkipped()
                }
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd = false
                rewardedAd = null
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                preloadRewarded(activity)
                onAdNotReady()
                onAdClosed()
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            val amount = if (rewardItem.amount > 0) rewardItem.amount else 1
            Log.d(TAG, "User earned reward: $rewardType (amount: $amount)")
            onRewardEarned(rewardType, amount)
        }
    }

    // =========================================================================
    // INTERSTITIAL ADS
    // =========================================================================

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Check frequency & cooldown conditions when a level finishes.
     */
    fun onLevelCompleted(activity: Activity, onComplete: () -> Unit) {
        levelsCompletedSinceLastAd++
        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastInterstitialShownTime

        if (levelsCompletedSinceLastAd >= levelsBetweenInterstitials && timeSinceLast >= interstitialCooldownMs) {
            showInterstitial(activity) {
                levelsCompletedSinceLastAd = 0
                lastInterstitialShownTime = System.currentTimeMillis()
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    private fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity)
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                interstitialAd = null
                Log.d(TAG, "Interstitial ad displayed.")
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                Log.d(TAG, "Interstitial ad dismissed.")
                preloadInterstitial(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd = false
                interstitialAd = null
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                preloadInterstitial(activity)
                onAdDismissed()
            }
        }

        ad.show(activity)
    }
}
