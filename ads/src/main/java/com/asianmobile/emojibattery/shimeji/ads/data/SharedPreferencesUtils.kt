package com.asianmobile.emojibattery.shimeji.ads.data

import android.content.Context
import androidx.core.content.edit
import com.asianmobile.emojibattery.shimeji.ads.config.CANCEL_SHOW_OPEN_ONE_TIME
import com.asianmobile.emojibattery.shimeji.ads.config.DO_NOT_SHOW_OPEN
import com.asianmobile.emojibattery.shimeji.ads.config.IS_ENABLE_AD
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_OPEN_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.IS_USER_PREMIUM
import com.asianmobile.emojibattery.shimeji.ads.config.LIMIT_AD_COUNT
import com.asianmobile.emojibattery.shimeji.ads.config.MY_PREFERENCES
import com.asianmobile.emojibattery.shimeji.ads.config.NUMBER_CLICK_ADS_TO_LIMIT

object SharedPreferencesUtils {
    fun setIsShowOpenAds(context: Context, isShow: Boolean) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putBoolean(IS_SHOW_OPEN_ADS, isShow) }
    }

    fun increaseAdClicked(context: Context) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        val timeClick = sharedPreferences.getInt(LIMIT_AD_COUNT, 0)
        sharedPreferences.edit { putInt(LIMIT_AD_COUNT, timeClick + 1) }
    }

    fun resetAdClicked(context: Context) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putInt(NUMBER_CLICK_ADS_TO_LIMIT, 0) }
    }

    fun showOpenInNextTime(context: Context, boolean: Boolean) {
        context
            .getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(DO_NOT_SHOW_OPEN, boolean)
            }
    }

    fun showOpenInNextTime(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(DO_NOT_SHOW_OPEN, false)
    }

    fun setClicksAdsLimit(context: Context) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putInt(LIMIT_AD_COUNT, 0) }
    }

    fun getTimeAdClicked(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(LIMIT_AD_COUNT, 0)
    }

    fun getIsShowOpenAds(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(IS_SHOW_OPEN_ADS, false)
    }

    fun setIsEnableAds(context: Context, isEnable: Boolean) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putBoolean(IS_ENABLE_AD, isEnable) }
    }

    fun cancelOpenAdOneTime(context: Context) {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putBoolean(CANCEL_SHOW_OPEN_ONE_TIME, true) }
    }

    fun getIsEnableAds(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(IS_ENABLE_AD, true)
    }

    var isUserPremium = false

    fun setIsPremium(context: Context, isPremium: Boolean) {
        isUserPremium = isPremium
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit { putBoolean(IS_USER_PREMIUM, isPremium) }
    }

    fun getIsPremium(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(IS_USER_PREMIUM, false)
    }
}
