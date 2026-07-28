package io.github.julystar.musicapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for music source implementation modules.
 * Used by: source:local, source:webdav, source:onedrive (future)
 */
class MusicSourceConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("io.github.julystar.musicapp.convention.kmp.library")
        }
    }
}
