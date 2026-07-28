package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.group = "io.github.julystar.musicapp"
    }
}
