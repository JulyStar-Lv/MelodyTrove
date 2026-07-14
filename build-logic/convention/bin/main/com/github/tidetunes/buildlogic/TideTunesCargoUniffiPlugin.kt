package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class TideTunesCargoUniffiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("dev.gobley.cargo")
        target.plugins.apply("dev.gobley.uniffi")

        target.tasks.matching { it.name.startsWith("ksp") }.configureEach {
            dependsOn(target.tasks.named("buildUniffiBindings"))
        }
    }
}
