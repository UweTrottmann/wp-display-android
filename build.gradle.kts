// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.github.ben-manes.versions") version("0.51.0")
    // 2.0.0 not officially supported by coroutines
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("com.android.application") version "8.5.0" apply false
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
