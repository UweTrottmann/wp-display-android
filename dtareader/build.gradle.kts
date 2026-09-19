import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    kotlin("jvm")
}

group = "com.uwetrottmann"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.okio)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}
