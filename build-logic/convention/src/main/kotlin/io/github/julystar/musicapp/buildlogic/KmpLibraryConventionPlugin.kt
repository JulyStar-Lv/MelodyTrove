package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

open class KmpModuleExtension {
    var namespace: String = ""
    var iosBaseName: String = ""
}

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("org.jetbrains.kotlin.multiplatform")
            plugins.apply("org.jetbrains.kotlin.plugin.serialization")
            plugins.apply("com.android.library")

            extensions.create("musicApp", KmpModuleExtension::class.java)
        }
    }
}
