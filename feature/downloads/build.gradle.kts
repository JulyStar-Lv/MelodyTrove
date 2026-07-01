plugins {
    alias(libs.plugins.convention.feature)
}

compose.resources {
    publicResClass = true
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TideTunesDownloadsFeature"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.feature.downloads")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:presentation"))
            implementation(project(":service:download:domain"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.miuix.ui)
            implementation(compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.github.tidetunes.feature.downloads"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
