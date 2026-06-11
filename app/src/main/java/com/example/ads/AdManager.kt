package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"

    // Given IDs from the user request
    const val BANNER_AD_ID = "ca-app-pub-3214717672189600/9086824587"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-3214717672189600/9821851983"
    const val PREMIUM_REWARDED_AD_ID = "ca-app-pub-3214717672189600/8317198623"
    const val AUDIOS_REWARDED_AD_ID = "ca-app-pub-3214717672189600/5499463599"

    // 15 minutes in milliseconds = 15 * 60 * 1000 = 900,000 ms
    private const val AD_INTERVAL_LIMIT = 15 * 60 * 1000L 

    private val isInitialized = AtomicBoolean(false)
    private var mInterstitialAd: InterstitialAd? = null
    private var lastAdShowTime: Long = 0L
    private var isAdLoading = false

    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
                loadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadInterstitial(context: Context) {
        if (isAdLoading || mInterstitialAd != null) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, "Ad failed to load: ${adError.message}")
                        mInterstitialAd = null
                        isAdLoading = false
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded successfully.")
                        mInterstitialAd = interstitialAd
                        isAdLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading interstitial", e)
            isAdLoading = false
        }
    }

    /**
     * Shows the interstitial ad if the 15-minute interval has passed and an ad is available.
     * Returns true if shown, false otherwise.
     */
    fun showInterstitial(activity: Activity, force: Boolean = false, onAdClosed: (() -> Unit)? = null): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastAdShowTime

        if (!force && timeSinceLastAd < AD_INTERVAL_LIMIT) {
            val remainMin = ((AD_INTERVAL_LIMIT - timeSinceLastAd) / 60000) + 1
            Log.d(TAG, "Ad skipped: 15min cooldown in progress. Remaining minutes: $remainMin")
            onAdClosed?.invoke()
            return false
        }

        val ad = mInterstitialAd
        if (ad != null) {
            try {
                activity.runOnUiThread {
                    ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            onAdClosed?.invoke()
                        }
                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            onAdClosed?.invoke()
                        }
                    }
                    ad.show(activity)
                    lastAdShowTime = currentTime
                    mInterstitialAd = null
                    // Preload the next ad for future use
                    loadInterstitial(activity.applicationContext)
                }
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error showing interstitial", e)
                onAdClosed?.invoke()
                return false
            }
        } else {
            Log.d(TAG, "Ad was not loaded yet. Retrying to load...")
            loadInterstitial(activity.applicationContext)
            onAdClosed?.invoke()
            return false
        }
    }

    /**
     * Utility to check if cooldown is over
     */
    fun getSecondsUntilNextAd(): Long {
        val nextTime = lastAdShowTime + AD_INTERVAL_LIMIT
        val remainMs = nextTime - System.currentTimeMillis()
        return if (remainMs <= 0) 0 else remainMs / 1000
    }

    /**
     * Helper to load and format a Google AdMob AdView programmatically for Compose integration.
     */
    fun createBannerAdView(context: Context): AdView {
        val adView = AdView(context)
        adView.adUnitId = BANNER_AD_ID
        adView.setAdSize(AdSize.BANNER)
        
        try {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner", e)
        }
        return adView
    }
}
