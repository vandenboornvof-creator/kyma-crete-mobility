plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cretemobility.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cretemobility.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.cretemobility.app.HiltTestRunner"

        buildConfigField("String", "OVERPASS_API_URL", "\"https://overpass-api.de/api/\"")
        buildConfigField("String", "NOMINATIM_API_URL", "\"https://nominatim.openstreetmap.org/\"")
        buildConfigField("String", "KTEL_HERAKLION_URL", "\"https://www.ktel-hlas.gr/\"")
        buildConfigField("String", "KTEL_CHANIA_URL", "\"https://e-ktel.gr/\"")
        buildConfigField("String", "OTP_API_URL", "\"https://api.cretemobility.com/otp/\"")
        buildConfigField("String", "APP_VERSION", "\"1.0.0\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.google.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Ktor (for GTFS-RT / streaming)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Maps — OpenStreetMap
    implementation(libs.osmdroid)

    // Maps — Google (optional abstraction layer)
    implementation(libs.maps.compose)
    implementation(libs.google.maps)

    // Location
    implementation(libs.play.location)

    // Images
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Lottie
    implementation(libs.lottie)

    // Logging
    implementation(libs.timber)

    // Serialization
    implementation(libs.kotlinx.serialization)

    // Security
    implementation(libs.security.crypto)

    // Splash
    implementation(libs.splashscreen)

    // --- Testing ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.arch.core.testing)

    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
