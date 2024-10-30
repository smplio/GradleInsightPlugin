plugins {
    kotlin("jvm") version "1.7.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.smplio.gradle.build.insights"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

publishing {
    publications {
        repositories {
            mavenLocal()
        }
    }
}

gradlePlugin {
    plugins {
        create("gradleInsights") {
            id = "com.smplio.gradle.build-insights"
            displayName = "GradleInsights"
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