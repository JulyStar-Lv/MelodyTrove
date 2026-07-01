import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.feature)
    alias(libs.plugins.convention.room)
    alias(libs.plugins.convention.cargo.uniffi)
    alias(libs.plugins.kotlin.atomicfu)
    id("com.android.library")
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
            baseName = "TideTunesShared"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetune.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:presentation"))
            implementation(project(":source:api"))
            implementation(project(":source:local"))
            implementation(project(":source:webdav"))
            implementation(project(":source:onedrive"))
            implementation(project(":service:playback:domain"))
            implementation(project(":service:playback:presentation"))
            implementation(project(":service:download:data"))
            implementation(project(":service:download:domain"))
            implementation(project(":feature:downloads"))
            implementation(project(":feature:search"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:playlist"))
            implementation(project(":feature:sources"))
            implementation(project(":feature:home"))
            implementation(project(":feature:importing"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:queue"))
            implementation(project(":feature:radio"))
            implementation(project(":feature:lyrics"))
            implementation(project(":feature:album"))
            implementation(project(":feature:artist"))
            implementation(project(":feature:browse"))
            implementation(project(":feature:library"))
            implementation(project(":feature:recentlyadded"))
            implementation(project(":feature:recentlyplayed"))
            implementation(project(":service:librarysync:domain"))
            implementation(project(":service:librarysync:data"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.components.resources)
            implementation(libs.animation)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.reorderable)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.miuix.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.dash)
            implementation(libs.media3.session)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime.ktx)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

cargo {
    packageDirectory = layout.projectDirectory.dir("../rust-libs/core")
    builds.jvm {
        embedRustLibrary = rustTarget == GobleyHost.current.rustTarget
    }
}

android {
    namespace = "com.github.tidetunes.shared"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
        ndk.abiFilters += setOf("arm64-v8a", "x86_64")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
