plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.gobley.cargo) apply false
    alias(libs.plugins.gobley.uniffi) apply false
    alias(libs.plugins.kotlin.atomicfu) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.ksp) apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
}
