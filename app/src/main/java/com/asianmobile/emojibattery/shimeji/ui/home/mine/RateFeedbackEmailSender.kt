package com.asianmobile.emojibattery.shimeji.ui.home.mine

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.APP_PASSWORD_MAIL
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

internal enum class RateFeedbackSubmitResult {
    SUCCESS,
    NO_NETWORK,
    CONFIGURATION_MISSING,
    FAILED
}

internal object RateFeedbackEmailSender {
    private const val TAG = "RateFeedbackSender"
    private const val GMAIL_APP_PASSWORD_LENGTH = 16

    fun send(
        context: Context,
        state: RateAppUiState
    ): RateFeedbackSubmitResult {
        if (!Utils.isNetworkAvailable(context)) {
            return RateFeedbackSubmitResult.NO_NETWORK
        }

        val selectedOptions = state.feedbackOptions.filter(FeedbackOption::isSelected)
        if (selectedOptions.isEmpty()) {
            return RateFeedbackSubmitResult.FAILED
        }

        val email = context.getString(R.string.feedback_email_address).trim()
        val appPassword = SafeRemoteConfig
            .getSensitiveString(APP_PASSWORD_MAIL)
            .filterNot(Char::isWhitespace)
        if (email.isBlank() || appPassword.length != GMAIL_APP_PASSWORD_LENGTH) {
            return RateFeedbackSubmitResult.CONFIGURATION_MISSING
        }

        return runCatching {
            val properties = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                put("mail.smtp.writetimeout", "10000")
                put("mail.mime.allowutf8", "true")
            }
            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(email, appPassword)
            })

            val message = MimeMessage(session).apply {
                setFrom(
                    InternetAddress(
                        email,
                        "${context.getString(R.string.app_name)} Feedback"
                    )
                )
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                setSubject(
                    "Feedback - ${context.getString(R.string.app_name)} - Version ${BuildConfig.VERSION_NAME}",
                    Charsets.UTF_8.name()
                )
                setText(buildFeedbackContent(context, state, selectedOptions), Charsets.UTF_8.name())
            }
            Transport.send(message)
        }.fold(
            onSuccess = { RateFeedbackSubmitResult.SUCCESS },
            onFailure = {
                Log.e(TAG, "Failed to send rate feedback: ${it.javaClass.simpleName}")
                RateFeedbackSubmitResult.FAILED
            }
        )
    }

    private fun buildFeedbackContent(
        context: Context,
        state: RateAppUiState,
        selectedOptions: List<FeedbackOption>
    ): String {
        val englishConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }
        val englishContext = context.createConfigurationContext(englishConfiguration)
        val locale = Locale.getDefault()
        val isOtherSelected = state.feedbackOptions.lastOrNull()?.isSelected == true

        return buildString {
            appendLine("--------- App info ---------")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("OS version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Locale: ${locale.toLanguageTag()}")
            appendLine()
            appendLine("--------- Feedback ---------")
            selectedOptions.forEach { option ->
                appendLine("- ${englishContext.getString(option.textResId)}")
            }
            if (isOtherSelected && state.otherFeedbackText.isNotBlank()) {
                appendLine()
                appendLine("Other details:")
                appendLine(state.otherFeedbackText.trim())
            }
        }
    }
}
