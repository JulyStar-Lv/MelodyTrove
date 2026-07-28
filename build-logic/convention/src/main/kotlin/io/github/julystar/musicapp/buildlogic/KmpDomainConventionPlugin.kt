package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for pure Kotlin domain modules (no Compose, no Room, no platform deps).
 * Used by: core:domain, service:*:domain, source:api
 */
class KmpDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("io.github.julystar.musicapp.convention.kmp.library")
    }
}
