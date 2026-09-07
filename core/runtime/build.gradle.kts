plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.room)
    alias(libs.plugins.kotlin.atomicfu)
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreRuntime"
            isStatic = true
            binaryOption("bundleId", "io.github.julystar.musicapp.core.runtime")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:data"))
            implementation(project(":core:lyrics-core"))
            implementation(project(":source:api"))
            implementation(project(":source:local"))
            implementation(project(":source:webdav"))
            implementation(project(":source:onedrive"))
            implementation(project(":source:smb"))
            implementation(project(":source:openlist"))
            implementation(project(":source:server"))
            implementation(project(":service:playback:domain"))
            implementation(project(":service:download:domain"))
            implementation(project(":service:download:data"))
            implementation(project(":service:librarysync:domain"))
            implementation(project(":service:librarysync:data"))
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

android {
    namespace = "io.github.julystar.musicapp.core.runtime"
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
