package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class CargoUniffiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("dev.gobley.cargo")
        target.plugins.apply("dev.gobley.uniffi")

        target.tasks.matching { it.name.startsWith("ksp") }.configureEach {
            dependsOn(target.tasks.named("buildUniffiBindings"))
        }
    }
}
