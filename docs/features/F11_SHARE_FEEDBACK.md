# F11 — Share App / Feedback / Privacy Policy

3 actions trong Settings > Other Settings.

---

## 1. Share App

Trigger `Intent.ACTION_SEND` với Play Store URL.

```kotlin
fun shareApp(context: Context) {
    val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val shareText = context.getString(R.string.share_app_message, playStoreUrl)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_app_chooser_title)))
}
```

### Strings

```xml
<string name="share_app_subject">Try Private Browser</string>
<string name="share_app_message">Check out Private Browser — Safe &amp; Secure mobile browsing.\n\nDownload: %1$s</string>
<string name="share_app_chooser_title">Share via</string>
```

---

## 2. Send Feedback (mailto)

```kotlin
fun sendFeedback(context: Context) {
    val email = context.getString(R.string.feedback_email_address)
    val subject = context.getString(R.string.feedback_subject_template,
        BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT, Build.MANUFACTURER, Build.MODEL
    )
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_body_template))
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.feedback_chooser_title)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.feedback_no_email_app, Toast.LENGTH_LONG).show()
    }
}
```

### Strings

```xml
<string name="feedback_email_address">feedback@asianmobile.ltd</string>
<string name="feedback_subject_template">Private Browser feedback v%1$s (Android API %2$d, %3$s %4$s)</string>
<string name="feedback_body_template">\n\n---\nDevice info auto-filled. Please describe your issue or feedback above.</string>
<string name="feedback_chooser_title">Send feedback via</string>
<string name="feedback_no_email_app">No email app installed</string>
```

---

## 3. Privacy Policy

Mở internal `BrowserWebView` (không phải external Chrome) với URL config.

```kotlin
fun openPrivacyPolicy(navController: NavController, remoteConfig: SafeRemoteConfig) {
    val url = remoteConfig.getString("privacy_policy_url").ifBlank {
        "https://privatebrowser.example.com/privacy"   // TODO confirm URL chính thức với user
    }
    navController.safeNavigate(Routes.PRIVACY_POLICY + "?url=${Uri.encode(url)}")
}
```

Route `PRIVACY_POLICY`:
```kotlin
composable(
    route = "${Routes.PRIVACY_POLICY}?url={url}",
    arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" })
) { entry ->
    val url = entry.arguments?.getString("url").orEmpty()
    PrivacyPolicyScreen(url = url, onBack = { navController.safePopBackStack() })
}
```

`PrivacyPolicyScreen` = WebView fullscreen, không có URL bar (chỉ back button top), không có ads. Xem [S14_PRIVACY_POLICY.md](../screens/S14_PRIVACY_POLICY.md).

---

## 4. UI Integration

Settings rows:

```kotlin
SettingsSection(title = stringResource(R.string.settings_section_other)) {
    SettingsRow(
        iconRes = R.drawable.ic_lang_globe,
        title = stringResource(R.string.settings_language_title),
        onClick = { navigateWithAd(context) { navController.safeNavigate(Routes.LANGUAGE_SETTINGS) } },
    )
    Divider()
    SettingsRow(
        iconRes = R.drawable.ic_chat_bubble,
        title = stringResource(R.string.settings_feedback_title),
        onClick = { sendFeedback(context) },
    )
    Divider()
    SettingsRow(
        iconRes = R.drawable.ic_share,
        title = stringResource(R.string.settings_share_title),
        onClick = { shareApp(context) },
    )
    Divider()
    SettingsRow(
        iconRes = R.drawable.ic_shield_warning,
        title = stringResource(R.string.settings_privacy_title),
        onClick = { openPrivacyPolicy(navController, remoteConfig) },
    )
}
```

---

## 5. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| Không có email app | Catch ActivityNotFoundException + toast |
| Không có app share text | Hệ thống ALWAYS có share sheet — không cần fallback |
| Privacy URL không reachable (no internet) | WebView báo error page; user thấy được status |
| Privacy URL trả 404 | Vẫn load page error của host, không crash |
| Remote config chưa load | Dùng fallback URL hardcode |

---

## 6. Strings Cho Settings

```xml
<string name="settings_section_general">General</string>
<string name="settings_section_other">Other Settings</string>
<string name="settings_language_title">Language</string>
<string name="settings_feedback_title">Send Feedback</string>
<string name="settings_share_title">Share app</string>
<string name="settings_privacy_title">Privacy Policy</string>
<string name="settings_set_default_title">Set As Default Browser</string>
<string name="settings_search_engine_title">Google</string>     <!-- dynamic theo engine current -->
<string name="settings_clear_history_title">Clear History</string>
<string name="settings_version_label">Version %1$s</string>
```

---

## 7. Liên Quan

- [S09_SETTINGS.md](../screens/S09_SETTINGS.md)
- [S14_PRIVACY_POLICY.md](../screens/S14_PRIVACY_POLICY.md)
- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md) — WebView engine cho Privacy Policy
