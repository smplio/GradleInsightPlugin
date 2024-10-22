plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.23"
    `maven-publish`
}

group = "com.smplio"
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
            id = "com.smplio.gradle-insights"
            displayName = "GradleInsigts"
            implementationClass = "com.smplio.GradleInsightsPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}