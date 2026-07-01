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
            baseName = "TideTunesLibraryFeature"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.feature.library")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:presentation"))
            implementation(project(":service:download:domain"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.miuix.ui)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}

android {
    namespace = "com.github.tidetunes.feature.library"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
