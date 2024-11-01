import com.smplio.gradle.getArtifactVersionFromGit

plugins {
    kotlin("jvm") version "1.7.0"
    `java-gradle-plugin`
    `maven-publish`

    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.smplio.gradle.build.insights"
version = getArtifactVersionFromGit()

repositories {
    mavenCentral()
    gradlePluginPortal()
}

publishing {
    publications {
        repositories {
            mavenCentral()
            mavenLocal()
        }
    }
}

gradlePlugin {
    vcsUrl.set("https://github.com/smplio/GradleInsightPlugin")
    website.set("https://github.com/smplio/GradleInsightPlugin")
    plugins {
        create("gradleInsights") {
            id = "com.smplio.gradle.build-insights"
            displayName = "GradleInsights"
            description = "Swissknife plugin, that let's you visualize your build statistics"
            tags.set(listOf(
                "build",
                "metrics",
            ))
            implementationClass = "com.smplio.gradle.build.insights.GradleInsightsPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))

    implementation(libs.dropwizard.metrics.core)
    implementation(libs.json)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
