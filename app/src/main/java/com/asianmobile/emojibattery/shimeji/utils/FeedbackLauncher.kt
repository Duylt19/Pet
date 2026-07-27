package com.asianmobile.emojibattery.shimeji.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.R

object FeedbackLauncher {
    fun launch(context: Context) {
        val email = context.getString(R.string.feedback_email_address)
        val subject = context.getString(
            R.string.feedback_subject_template,
            BuildConfig.VERSION_NAME,
            Build.VERSION.SDK_INT,
            Build.MANUFACTURER,
            Build.MODEL
        )
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_body_template))
        }

        try {
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.feedback_chooser_title))
            )
        } catch (_: ActivityNotFoundException) {
            ToastHelper.show(
                context = context,
                message = context.getString(R.string.feedback_no_email_app),
                duration = Toast.LENGTH_LONG
            )
        }
    }
}
