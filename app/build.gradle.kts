import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

val kotlinVersion: String by rootProject.extra

android {
    namespace = "com.uwetrottmann.wpdisplay"
    compileSdk = 33 /* Android 13 (T) */

    buildFeatures {
        // https://developer.android.com/topic/libraries/view-binding
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.uwetrottmann.wpdisplay"
        minSdk = 21 /* Android 5 (L) */
        targetSdk = 33 /* Android 13 (T) */
        versionCode = 24
        versionName = "17.1.0"

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

    signingConfigs {
        create("release") {
            if (rootProject.file("keystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(rootProject.file("keystore.properties")))

                storeFile = file(props["storeFile"]!!)
                storePassword = props["storePassword"]!!.toString()
                keyAlias = props["keyAlias"]!!.toString()
                keyPassword = props["keyPassword"]!!.toString()
            }
        }
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

}

dependencies {
    implementation(project(":dtareader"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    // https://developer.android.com/jetpack/androidx/releases/fragment
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    // https://developer.android.com/jetpack/androidx/releases/appcompat
    implementation("androidx.appcompat:appcompat:1.5.1")
    // https://developer.android.com/jetpack/androidx/releases/preference
    implementation("androidx.preference:preference-ktx:1.2.0")
    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    // ViewModel and LiveData
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    // Material Design
    // https://github.com/material-components/material-components-android/releases
    implementation("com.google.android.material:material:1.7.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
