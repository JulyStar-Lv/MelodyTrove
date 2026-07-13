import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

val appVersionName = providers.gradleProperty("appVersionName").get()

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.koin.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.github.tidetunes.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "TideTunes"
            packageVersion = appVersionName
        }
    }
}

// compose-miuix-ui is published as Java 21 bytecode. Keep Gradle/Kotlin builds
// compatible with the repository toolchain, but launch the desktop app on a
// Java 21 runtime so local runs do not fail with UnsupportedClassVersionError.
val desktopRuntimeLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

afterEvaluate {
    tasks.named<JavaExec>("run") {
        javaLauncher.set(desktopRuntimeLauncher)
        executable = desktopRuntimeLauncher.get().executablePath.asFile.absolutePath
    }
}
