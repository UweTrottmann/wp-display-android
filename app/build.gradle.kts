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
        targetSdk = 35 /* Android 15 */
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

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.material)

    implementation(libs.timber)
    implementation(libs.mpandroidchart)

}
