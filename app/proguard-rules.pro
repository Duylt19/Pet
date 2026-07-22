# Keep retraceable line numbers for Crashlytics without exposing source filenames.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# JavaScript calls these methods by name through WebView.addJavascriptInterface.
# The Android default rules already cover this annotation globally; keeping the
# app-scoped rule here makes this browser-specific runtime contract explicit.
-keepclassmembers class com.asianmobile.privatebrower.data.browser.** {
    @android.webkit.JavascriptInterface <methods>;
}

# JavaMail discovers providers and constructs SMTPTransport through reflection.
# Keep only the provider entry points and the transport used by rate feedback.
-keep,allowoptimization class com.sun.mail.**Provider {
    public <init>();
}
-keep,allowoptimization class com.sun.mail.smtp.SMTPTransport {
    public <init>(javax.mail.Session, javax.mail.URLName);
}
