@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "com.jessecorbett"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    jvm()

    android {
        namespace = "com.jessecorbett.kotlinx.serialization.diff"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    wasmWasi {
        nodejs()
    }

    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }

        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kotlinx-serialization-diff", version.toString())

    pom {
        name = "Fibonacci library"
        description = "A mathematics calculation library."
        inceptionYear = "2024"
        url = "https://github.com/kotlin-hands-on/fibonacci/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "kotlin-hands-on"
                name = "Kotlin Developer Advocate"
                url = "https://github.com/kotlin-hands-on/"
            }
        }
        scm {
            url = "https://github.com/kotlin-hands-on/fibonacci/"
            connection = "scm:git:git://github.com/kotlin-hands-on/fibonacci.git"
            developerConnection = "scm:git:ssh://git@github.com/kotlin-hands-on/fibonacci.git"
        }
    }
}
