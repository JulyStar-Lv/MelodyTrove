package com.github.tidetunes.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for music source implementation modules.
 * Used by: source:local, source:webdav, source:onedrive (future)
 */
class TideTunesMusicSourcePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("com.github.tidetunes.convention.kmp.library")
            plugins.apply("com.github.tidetunes.convention.cmp.library")
        }
    }
}
