plugins {
    id("java-library")
    kotlin("jvm")
}

group = "com.uwetrottmann"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // https://github.com/square/okio/blob/master/CHANGELOG.md
    implementation("com.squareup.okio:okio:3.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
