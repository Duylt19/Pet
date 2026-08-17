package com.asianmobile.emojibattery.shimeji.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageUtil {

    fun contextWithCachedAppLocale(context: Context): Context {
        val preferences = context.getSharedPreferences(
            LANGUAGE_CACHE_NAME,
            Context.MODE_PRIVATE,
        )
        return updateBaseContextLocale(
            context = context,
            keyLanguage = preferences.getString(KEY_LANGUAGE, "").orEmpty(),
            country = preferences.getString(COUNTRY_LANGUAGE, "").orEmpty(),
        )
    }

    fun updateBaseContextLocale(context: Context, keyLanguage: String, country: String): Context {
        val locale = Locale.Builder()
            .setLanguage(keyLanguage)
            .setRegion(country)
            .build()
        Locale.setDefault(locale)

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else {
            updateResourcesLocaleLegacy(context, locale)
        }
    }

    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    private const val LANGUAGE_CACHE_NAME = "language_cache"
    private const val KEY_LANGUAGE = "key_language"
    private const val COUNTRY_LANGUAGE = "country_language"
}

