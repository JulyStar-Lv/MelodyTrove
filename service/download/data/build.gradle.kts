plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TideTunesDownloadData"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.service.download.data")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":service:download:domain"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":core:domain"))
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.github.tidetunes.service.download.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
