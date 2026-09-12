package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category

class CargoUniffiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("dev.gobley.cargo")
        target.plugins.apply("dev.gobley.uniffi")

        val documentationCategory = target.objects.named(
            Category::class.java,
            Category.DOCUMENTATION,
        )
        listOf("uniFfiConfiguration", "uniFfiConfigurationConsumable").forEach { name ->
            target.configurations.named(name).configure {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, documentationCategory)
            }
        }

        target.tasks.matching { it.name.startsWith("ksp") }.configureEach {
            dependsOn(target.tasks.named("buildUniffiBindings"))
        }
    }
}
