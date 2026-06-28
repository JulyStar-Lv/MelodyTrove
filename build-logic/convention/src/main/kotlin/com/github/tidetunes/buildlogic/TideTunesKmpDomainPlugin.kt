package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for pure Kotlin domain modules (no Compose, no Room, no platform deps).
 * Used by: core:domain, service:*:domain, source:api
 */
class TideTunesKmpDomainPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.github.tidetunes.convention.kmp.library")
    }
}
