plugins {
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
}

android {
    namespace = "io.github.julystar.musicapp.car.presentation"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":service:playback:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.koin.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}
