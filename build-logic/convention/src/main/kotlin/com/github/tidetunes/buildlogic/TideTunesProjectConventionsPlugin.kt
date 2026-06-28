package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class TideTunesProjectConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.group = "com.github.tidetunes"
    }
}
