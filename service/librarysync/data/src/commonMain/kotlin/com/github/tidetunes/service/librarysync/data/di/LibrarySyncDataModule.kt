package com.github.tidetunes.service.librarysync.data.di

import org.koin.dsl.module

/**
 * Koin module for library sync data implementations.
 *
 * Actual implementations (RoomLibrarySyncTaskRepository, LegacyLibrarySyncController)
 * currently live in shared due to dependencies on the Room database and Rust bridge.
 * When the database is extracted into core:data, those implementations will move here.
 *
 * For now, this module acts as a dependency boundary: feature modules depend on
 * service:librarysync:domain for interfaces, and the Koin graph wires platform-configured
 * implementations from shared.
 */
val librarySyncDataModule = module {
    // LibrarySyncTaskRepository and LibrarySyncController implementations
    // are registered in shared's LibrarySyncModule.
    // This module is a placeholder for future extraction of sync data
    // from shared into the proper service layer.
}
