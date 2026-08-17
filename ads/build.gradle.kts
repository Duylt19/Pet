import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.asianmobile.emojibattery.shimeji.ads"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "NullSafeMutableLiveData"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Compose
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)
    debugApi(libs.compose.ui.tooling)
    api(libs.activity.compose)

    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.shimmer)
    implementation(libs.infer.annotation)

    implementation(libs.cardview)
    api(libs.lifecycle.extensions)
    api(libs.lifecycle.runtime.ktx)
    annotationProcessor(libs.lifecycle.compiler)

    api(platform(libs.firebase.bom))
    api(libs.firebase.config.ktx)
    api(libs.ads.mobile.sdk)
    api(libs.multidex)

    api(libs.firebase.analytics.ktx)
    api(libs.firebase.crashlytics)
    api(libs.firebase.analytics)
    implementation(libs.lottie)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Sdp
    implementation(libs.sdp.android)

    // Tracking
    api(libs.adjust.android)
    api(libs.installreferrer)
    api(libs.play.services.ads.identifier)
    api(libs.adjust.android.webbridge)
    api(libs.play.services.appset)
    api(libs.facebook.login)

    api(libs.facebook)
    api(libs.pangle)
    api(libs.vungle)
    api(libs.unity.ads)
    api(libs.unity)
    api(libs.ironsource.adapter)
    api(libs.mintegral)
    api(libs.applovin.sdk)
    implementation(libs.applovin)
    api(libs.inmobi)

    api(libs.picasso)
    api(libs.recyclerview)
    implementation(libs.glide)
    api(libs.blurry)

    api(libs.sdp.android)
    api(libs.ssp.android)
    api(libs.lifecycle.viewmodel.compose)

    configurations.configureEach {
        exclude(group = "com.google.android.gms", module = "play-services-ads")
        exclude(group = "com.google.android.gms", module = "play-services-ads-api")
        exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    }

    // Core library desugaring — required for java.time.* on API < 26
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
