import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

val kotlinVersion: String by rootProject.extra

android {
    namespace = "com.uwetrottmann.wpdisplay"
    compileSdk = 35 /* Android 15 */

    buildFeatures {
        buildConfig = true
        // https://developer.android.com/topic/libraries/view-binding
        viewBinding = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "com.uwetrottmann.wpdisplay"
        minSdk = 21 /* Android 5 (L) */
        targetSdk = 34 /* Android 14 */
        versionCode = 28
        versionName = "17.4.0"

        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        // for CI server (reports are not public)
        textReport = true
        // Note: do not use textOutput = file("stdout"), just set no file.
    }

    val keystoreConfigFile = rootProject.file("../upload-keystore-uwe-trottmann.properties")
    val hasKeystoreConfig = keystoreConfigFile.exists()
    signingConfigs {
        create("release") {
            if (hasKeystoreConfig) {
                val props = Properties()
                props.load(FileInputStream(keystoreConfigFile))

                storeFile = file(props["storeFile"] as String)
                storePassword = props["storePassword"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            if (hasKeystoreConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

}

dependencies {
    implementation(project(":dtareader"))

    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/CHANGES.md
    // 1.10 is using Kotlin 2.1
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core:1.13.1")
    // https://developer.android.com/jetpack/androidx/releases/fragment
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    // https://developer.android.com/jetpack/androidx/releases/appcompat
    implementation("androidx.appcompat:appcompat:1.7.0")
    // https://developer.android.com/jetpack/androidx/releases/preference
    implementation("androidx.preference:preference-ktx:1.2.1")
    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // ViewModel and LiveData
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    val lifecycleVersion = "2.8.3"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    // Material Design
    // https://github.com/material-components/material-components-android/releases
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
