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
            baseName = "TideTunesRecentlyAddedFeature"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.feature.recentlyadded")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:presentation"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.miuix.ui)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":service:download:domain"))
            implementation(project(":service:playback:domain"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}

android {
    namespace = "com.github.tidetunes.feature.recentlyadded"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
