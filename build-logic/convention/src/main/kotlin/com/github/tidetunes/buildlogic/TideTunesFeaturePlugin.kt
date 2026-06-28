package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class TideTunesFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.github.tidetunes.convention.kmp.library")
        target.plugins.apply("com.github.tidetunes.convention.cmp.library")
    }
}
