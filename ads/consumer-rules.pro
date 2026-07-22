# Third-party ad and analytics SDKs package their own consumer rules. Duplicating
# package-wide rules here prevents the consuming app from shrinking those SDKs.

# This view is inflated by its fully qualified name from the ads module XML.
-keep,allowoptimization class com.asianmobile.privatebrower.ads.customview.BorderImageView {
    public <init>(android.content.Context, android.util.AttributeSet);
}
