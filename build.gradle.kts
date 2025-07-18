// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version("0.52.0")
    // https://kotlinlang.org/docs/releases.html#release-details
    // https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin
    // Before updating look for compatible coroutines, Gradle and Android plugin version
    // 2.1.21 is compatible with Gradle 7.6.3–8.12.1 and AGP 7.3.1–8.7.2
    id("org.jetbrains.kotlin.jvm") version "2.1.21" apply false
    id("com.android.application") version "8.7.2" apply false
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// reject preview releases for dependencyUpdates task
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
