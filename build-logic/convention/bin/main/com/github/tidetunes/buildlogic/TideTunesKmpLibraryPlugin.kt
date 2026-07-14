package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

open class TideTunesModuleExtension {
    var namespace: String = ""
    var iosBaseName: String = ""
}

class TideTunesKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("org.jetbrains.kotlin.multiplatform")
            plugins.apply("org.jetbrains.kotlin.plugin.serialization")
            plugins.apply("com.android.library")

            extensions.create("tidetunes", TideTunesModuleExtension::class.java)
        }
    }
}
