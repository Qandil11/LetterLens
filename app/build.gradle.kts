plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"

}

android {
    namespace = "com.qandil.letterlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qandil.letterlens"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "LETTER_LENS_API", "\"http://10.0.2.2:8080\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

        // ...
        buildFeatures { buildConfig = true }




    buildTypes {
            debug {
                buildConfigField("String","LETTER_LENS_API","\"http://10.31.227.115:8080\"") // local
                // or your LAN IP while testing on device
            }
            release {
                buildConfigField("String","LETTER_LENS_API","\"https://letterlens.onrender.com\"")

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Camera & OCR
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Networking
    implementation("io.ktor:ktor-client-okhttp:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Utils
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
}