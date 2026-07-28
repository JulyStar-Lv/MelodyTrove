package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("io.github.julystar.musicapp.convention.kmp.library")
        target.plugins.apply("io.github.julystar.musicapp.convention.cmp.library")
    }
}
