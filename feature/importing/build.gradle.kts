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
            baseName = "TideTunesImportingFeature"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.feature.importing")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:presentation"))
            implementation(project(":source:api"))
            implementation(project(":service:librarysync:domain"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.miuix.ui)
            implementation(libs.components.resources)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}

android {
    namespace = "com.github.tidetunes.feature.importing"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
