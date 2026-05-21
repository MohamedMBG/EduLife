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

val configuredApiBaseUrl = providers.gradleProperty("edulife.apiBaseUrl")
    .orElse(localProperties.getProperty("edulife.apiBaseUrl") ?: "http://10.0.2.2:8080/api/v1/")
    .get()
val normalizedApiBaseUrl = if (configuredApiBaseUrl.endsWith("/")) {
    configuredApiBaseUrl
} else {
    "$configuredApiBaseUrl/"
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
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

// Retrofit (STABLE)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp (safe modern)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // RecyclerView for course catalog and featured lists
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
