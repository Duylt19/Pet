import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.asianmobile.emojibattery.shimeji"
    compileSdk = 36
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.asianmobile.emojibattery.shimeji"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "BATTERY_STATUS_ENABLED", "true")
        }
        release {
            buildConfigField("boolean", "BATTERY_STATUS_ENABLED", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        resources {
            // Keep service/provider files used by JavaMail and other SDKs.
            merges += "META-INF/services/**"
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/maven/**"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "NullSafeMutableLiveData"
    }

    sourceSets {
        getByName("debug").assets.srcDir(
            layout.buildDirectory.dir("generated/batteryCatalogAssets/debug")
        )
        getByName("debug").res.srcDir(
            layout.buildDirectory.dir("generated/batteryResources/debug")
        )
    }
}

val batterySnapshotDirectory = rootProject.layout.projectDirectory.dir(
    "private_data/battery-apk-1.0.2/battery-data"
)
val batteryRuntimeDirectory = rootProject.layout.projectDirectory.dir(
    "private_data/battery-apk-1.0.2/battery-runtime"
)
val batteryCatalogFile = batteryRuntimeDirectory.file("catalog.json")
val batteryAuditFile = batteryRuntimeDirectory.file("audit.json")
val generatedBatteryAssets = layout.buildDirectory.dir(
    "generated/batteryCatalogAssets/debug"
)
val generatedBatteryResources = layout.buildDirectory.dir(
    "generated/batteryResources/debug"
)

val auditDebugBatterySnapshot by tasks.registering(Exec::class) {
    group = "battery data"
    description = "Audits the private Battery snapshot and creates the debug catalog."
    inputs.dir(batterySnapshotDirectory)
    inputs.file(rootProject.layout.projectDirectory.file("tools/battery_data_snapshot.py"))
    outputs.files(batteryCatalogFile, batteryAuditFile)
    onlyIf { batterySnapshotDirectory.asFile.isDirectory }
    commandLine(
        "python3",
        rootProject.layout.projectDirectory.file("tools/battery_data_snapshot.py").asFile,
        batterySnapshotDirectory.asFile,
        "--catalog",
        batteryCatalogFile.asFile,
        "--report",
        batteryAuditFile.asFile
    )
}

val prepareDebugBatteryAssets by tasks.registering(Sync::class) {
    group = "battery data"
    description = "Packages the audited Battery catalog into debug APK assets."
    dependsOn(auditDebugBatterySnapshot)
    onlyIf {
        batterySnapshotDirectory.asFile.isDirectory && batteryCatalogFile.asFile.isFile
    }
    into(generatedBatteryAssets)
    from(batteryCatalogFile) {
        into("battery_catalog")
    }
    from(batterySnapshotDirectory.dir("remote/thumbnails")) {
        into("battery_catalog/thumb")
    }
    from(batterySnapshotDirectory.dir("remote/batteries")) {
        into("battery_catalog/battery")
    }
    from(batterySnapshotDirectory.dir("remote/emojis")) {
        into("battery_catalog/emoji")
    }
    from(batterySnapshotDirectory.dir("bundled/assets/background_template")) {
        into("battery_catalog/background")
    }
    from(batterySnapshotDirectory.dir("bundled/assets/cute_animation")) {
        into("battery_catalog/animation")
    }
}

tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(prepareDebugBatteryAssets)
}

tasks.matching {
    it.name.contains("Debug", ignoreCase = true) &&
        it.name.contains("Lint", ignoreCase = true)
}.configureEach {
    dependsOn(prepareDebugBatteryAssets)
}

// Layoutlib cannot load adquality-sdk 9.1.1 because its generated R classes have
// incompatible InnerClasses metadata. Keep the SDK in normal debug/release builds and
// remove it only while the host-side Compose screenshot tasks assemble their classpath.
val isHostScreenshotBuild = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("ScreenshotTest", ignoreCase = true)
}
if (isHostScreenshotBuild) {
    configurations.matching { it.name == "debugRuntimeClasspath" }.configureEach {
        exclude(group = "com.unity3d.ads-mediation", module = "adquality-sdk")
    }
}

val prepareDebugBatteryResources by tasks.registering(Sync::class) {
    group = "battery data"
    description = "Packages reviewed Battery status vectors and fonts into debug resources."
    onlyIf { batterySnapshotDirectory.asFile.isDirectory }
    into(generatedBatteryResources)
    from(batterySnapshotDirectory.dir("bundled/drawable")) {
        include("*.xml")
        into("drawable")
    }
    from(batterySnapshotDirectory.dir("bundled/font")) {
        include("*.ttf")
        into("font")
    }
}

tasks.configureEach {
    if (name.contains("Debug") &&
        name != prepareDebugBatteryResources.name &&
        name != auditDebugBatterySnapshot.name
    ) {
        dependsOn(prepareDebugBatteryResources)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Hilt / DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)

    // Image loading and GIF decoding
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Ads Module
    implementation(project(":ads"))

    // Debug Tooling
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)

    // Lottie
    implementation(libs.lottie.compose)

    // Rate feedback email
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    // Billing
    implementation(libs.billing)

    // Required transitively by the ads module.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

}
