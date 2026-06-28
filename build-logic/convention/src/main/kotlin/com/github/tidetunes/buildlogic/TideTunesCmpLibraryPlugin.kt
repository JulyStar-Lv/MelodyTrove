package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class TideTunesCmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.compose")
        target.plugins.apply("org.jetbrains.kotlin.plugin.compose")
    }
}
