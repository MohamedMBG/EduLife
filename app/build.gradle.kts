import org.gradle.api.GradleException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val defaultDebugApiBaseUrl = "http://10.0.2.2:8080/api/v1/"
val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    val normalizedTaskName = taskName.lowercase()
    normalizedTaskName.contains("release") || normalizedTaskName == "assemble"
}
val gradleApiBaseUrl = providers.gradleProperty("edulife.apiBaseUrl").orNull
val configuredApiBaseUrl = if (releaseTaskRequested) {
    gradleApiBaseUrl
        ?: throw GradleException(
            "Release builds require edulife.apiBaseUrl. " +
                    "Pass -Pedulife.apiBaseUrl=https://your-api.example/api/v1/."
        )
} else {
    gradleApiBaseUrl ?: localProperties.getProperty("edulife.apiBaseUrl") ?: defaultDebugApiBaseUrl
}.trim()
val normalizedApiBaseUrl = if (configuredApiBaseUrl.endsWith("/")) {
    configuredApiBaseUrl
} else {
    "$configuredApiBaseUrl/"
}

// Release artifacts must receive an explicit HTTPS API endpoint, because Firebase bearer tokens
// are attached to protected API calls. Debug builds can still use local.properties or the default.
if (releaseTaskRequested && !normalizedApiBaseUrl.startsWith("https://")) {
    throw GradleException("Release builds must set edulife.apiBaseUrl to an HTTPS endpoint.")
}

android {
    namespace = "com.baghdad.edulife"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.baghdad.edulife"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The API URL is injected at build time so emulator and physical-device debug builds can target the right backend.
        buildConfigField("String", "API_BASE_URL", "\"$normalizedApiBaseUrl\"")
    }

    buildFeatures {
        // BuildConfig carries non-secret debug environment values such as the local API base URL.
        buildConfig = true
    }

    buildTypes {
        debug {
            // Debug builds remain unminified so stack traces, breakpoints, and API testing stay simple.
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            // R8 removes unused code and obfuscates release classes to reduce reverse-engineering risk.
            isMinifyEnabled = true

            // Resource shrinking runs after code shrinking and removes unused packaged assets from release builds.
            isShrinkResources = true

            // The optimized Android rules are paired with EduLife-specific rules that protect API serialization.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enables java.time.* (and other API 26+ stdlib) on minSdk 24 via D8 desugaring.
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    // The Firebase BoM keeps all Firebase SDK versions compatible during Sprint 0 wiring.
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))

    // Firebase Auth is the MVP authentication provider and replaces custom client-side auth flows.
    implementation("com.google.firebase:firebase-auth")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

// Retrofit (STABLE) — 2.11.0 per audit 2026-06-19 P3-6 (was 2.9.0, 2020-era).
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

// OkHttp (safe modern)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Bottom navigation uses the Material BottomNavigationView (bundled in libs.material above);
    // the JitPack SmoothBottomBar was removed in audit 2026-06-19 P3-5 to drop GitHub-built jars.

    // RecyclerView for course catalog and featured lists
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide for loading remote course thumbnail images
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // EncryptedSharedPreferences for at-rest encryption of the persisted EduLife session.
    // Pinned to 1.1.0-alpha06 because the stable 1.0.0 line drags in the deprecated
    // androidx.security.crypto.MasterKeys helper and lacks the newer MasterKey.Builder API.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Backports java.time.* (Instant, LocalDate, DateTimeFormatter) for minSdk 24.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
