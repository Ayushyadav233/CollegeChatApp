// app/build.gradle.kts (Module: app)

plugins {
    // Correctly referencing plugins defined in libs.versions.toml
    // No 'apply false' here for module-level plugins
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // If you are using Compose, uncomment this line:
    // alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.collegechatapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.collegechatapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    // Corrected way to set jvmTarget, addressing deprecation warning
    kotlin {
        jvmToolchain(11) // Use jvmToolchain for Kotlin JVM target
    }
    buildFeatures {
        // compose = true // Uncomment if you are using Compose
    }
}

dependencies {
    // AndroidX Core KTX and Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // AppCompat library for older Android versions compatibility
    implementation(libs.androidx.appcompat)

    // Material Design Components (IMPORTANT for UI improvements)
    implementation(libs.google.android.material)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ConstraintLayout
    implementation(libs.androidx.constraintlayout)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // If you are using Compose for testing, uncomment these:
    // androidTestImplementation(platform(libs.androidx.compose.bom))
    // androidTestImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation(libs.androidx.ui.tooling)
    // debugImplementation(libs.androidx.ui.test.manifest)
}
