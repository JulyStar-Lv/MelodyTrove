import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class PrintAppVersionTask : DefaultTask() {
    @get:Input
    abstract val appVersionName: Property<String>

    @get:Input
    abstract val appVersionCode: Property<Int>

    @TaskAction
    fun printVersion() {
        println("${appVersionName.get()} (${appVersionCode.get()})")
    }
}

fun gitOutput(vararg arguments: String): String =
    providers.exec {
        commandLine("git", *arguments)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()

val appVersionBase = providers.gradleProperty("appVersionBase").get()
require(Regex("""\d+\.\d+\.\d+""").matches(appVersionBase)) {
    "appVersionBase must use X.Y.Z format"
}

val versionTags = gitOutput("tag", "--points-at", "HEAD").lineSequence().filter(String::isNotBlank)
val stableTagPattern = Regex("""v(\d+\.\d+\.\d+)""")
val prereleaseTagPattern = Regex("""pre-v(\d+\.\d+\.\d+-beta\.\d+)""")
val taggedVersion = sequenceOf(stableTagPattern, prereleaseTagPattern)
    .mapNotNull { pattern ->
        versionTags.mapNotNull { tag -> pattern.matchEntire(tag)?.groupValues?.get(1) }.firstOrNull()
    }
    .firstOrNull()
val gitCommitCount = gitOutput("rev-list", "--count", "HEAD").toIntOrNull()?.coerceAtLeast(1) ?: 1
val gitCommitSha = gitOutput("rev-parse", "--short=12", "HEAD").ifBlank { "unknown" }
val resolvedAppVersionName = providers.environmentVariable("APP_VERSION_NAME").orNull
    ?.trim()
    ?.takeIf(String::isNotBlank)
    ?: taggedVersion
    ?: "$appVersionBase-dev.$gitCommitCount+$gitCommitSha"
val resolvedAppVersionCode = providers.environmentVariable("APP_VERSION_CODE").orNull
    ?.trim()
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: gitCommitCount
val appPackageVersion = if ("-dev." in resolvedAppVersionName) {
    val (major, minor) = appVersionBase.split(".")
    "$major.$minor.${resolvedAppVersionCode.coerceAtMost(65_535)}"
} else {
    resolvedAppVersionName.substringBefore("-").substringBefore("+")
}
require(appPackageVersion.substringBefore(".").toInt() > 0) {
    "Desktop package versions must start with a positive major version"
}

extra["appVersionName"] = resolvedAppVersionName
extra["appVersionCode"] = resolvedAppVersionCode
extra["appPackageVersion"] = appPackageVersion

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

tasks.register<PrintAppVersionTask>("printAppVersion") {
    group = "help"
    description = "Prints the resolved application version and build number."
    appVersionName.set(resolvedAppVersionName)
    appVersionCode.set(resolvedAppVersionCode)
}
