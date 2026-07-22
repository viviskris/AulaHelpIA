package com.tuusuario.aulahelpia.home.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AdClickManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_click_prefs", Context.MODE_PRIVATE)
    private val CLICK_COUNT_KEY = "click_count"
    private val CLICKS_FOR_AD = 5  // ✅ 5 CLICS

    fun registerClick(): Boolean {
        val currentCount = prefs.getInt(CLICK_COUNT_KEY, 0) + 1
        prefs.edit().putInt(CLICK_COUNT_KEY, currentCount).apply()

        val shouldShowAd = currentCount >= CLICKS_FOR_AD

        if (shouldShowAd) {
            prefs.edit().putInt(CLICK_COUNT_KEY, 0).apply()
            Log.d("AD_CLICKS", "🎯 ¡8 CLICS ALCANZADOS! Mostrar interstitial")
        }

        Log.d("AD_CLICKS", "🖱️ Click $currentCount/$CLICKS_FOR_AD")
        return shouldShowAd
    }

    fun resetCounter() {
        prefs.edit().putInt(CLICK_COUNT_KEY, 0).apply()
        Log.d("AD_CLICKS", "🔄 Contador reiniciado")
    }

    fun getCurrentCount(): Int {
        return prefs.getInt(CLICK_COUNT_KEY, 0)
    }
}