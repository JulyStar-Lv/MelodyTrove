import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.gobley.cargo)
    alias(libs.plugins.gobley.uniffi)
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization")
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
            baseName = "TideTuneShared"
            isStatic = true
            binaryOption("bundleId", "com.github.tidetune.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.animation)
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

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(tasks.named("buildUniffiBindings"))
}

cargo {
    packageDirectory = layout.projectDirectory.dir("../rust-libs/tidetune-core")
    builds.jvm {
        embedRustLibrary = rustTarget == GobleyHost.current.rustTarget
    }
}

android {
    namespace = "com.github.tidetune.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
        ndk.abiFilters += setOf("arm64-v8a", "x86_64")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
