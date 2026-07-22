package com.asianmobile.privatebrower.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Singleton helper to prevent toast spam.
 * Cancels the previous toast before showing a new one,
 * so rapid taps don't queue up multiple toasts.
 */
object ToastHelper {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentToast: Toast? = null

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val appContext = context.applicationContext
        mainHandler.post {
            currentToast?.cancel()
            currentToast = Toast.makeText(appContext, message, duration).also { it.show() }
        }
    }
}

