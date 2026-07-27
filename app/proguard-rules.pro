# Keep retraceable line numbers for Crashlytics without exposing source filenames.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# JavaMail discovers providers and constructs SMTPTransport through reflection.
# Keep only the provider entry points and the transport used by rate feedback.
-keep,allowoptimization class com.sun.mail.**Provider {
    public <init>();
}
-keep,allowoptimization class com.sun.mail.smtp.SMTPTransport {
    public <init>(javax.mail.Session, javax.mail.URLName);
}
