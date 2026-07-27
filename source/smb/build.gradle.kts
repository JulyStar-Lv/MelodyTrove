plugins {
    alias(libs.plugins.convention.music.source)
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TideTunesSourceSmb"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetunes.source.smb")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":source:api"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.github.tidetunes.source.smb"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
