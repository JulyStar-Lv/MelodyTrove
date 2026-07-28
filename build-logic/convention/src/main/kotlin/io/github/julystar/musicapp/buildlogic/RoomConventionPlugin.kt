package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.google.devtools.ksp")
        target.plugins.apply("androidx.room")
    }
}
