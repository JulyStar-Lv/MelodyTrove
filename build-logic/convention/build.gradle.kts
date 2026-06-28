plugins {
    `kotlin-dsl`
}


gradlePlugin {
    plugins {
        register("tidetunesKmpLibrary") {
            id = "com.github.tidetunes.convention.kmp.library"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesKmpLibraryPlugin"
            displayName = "TideTunes KMP library conventions"
            description = "Base Kotlin Multiplatform library settings for all TideTunes modules."
        }
        register("tidetunesCmpLibrary") {
            id = "com.github.tidetunes.convention.cmp.library"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesCmpLibraryPlugin"
            displayName = "TideTunes CMP library conventions"
            description = "Compose Multiplatform shared presentation settings."
        }
                register("tidetunesKmpDomain") {
            id = "com.github.tidetunes.convention.kmp.domain"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesKmpDomainPlugin"
            displayName = "TideTunes KMP domain conventions"
            description = "Pure Kotlin domain module conventions (no Compose)."
        }
        register("tidetunesMusicSource") {
            id = "com.github.tidetunes.convention.music-source"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesMusicSourcePlugin"
            displayName = "TideTunes music source conventions"
            description = "Music source implementation module conventions."
        }

        register("tidetunesFeature") {
            id = "com.github.tidetunes.convention.feature"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesFeaturePlugin"
            displayName = "TideTunes feature conventions"
            description = "Feature module conventions: CMP + Koin + Navigation + Immutable collections."
        }
        register("tidetunesRoom") {
            id = "com.github.tidetunes.convention.room"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesRoomPlugin"
            displayName = "TideTunes Room conventions"
            description = "Room KSP and schema directory conventions."
        }
        register("tidetunesCargoUniffi") {
            id = "com.github.tidetunes.convention.cargo-uniffi"
            implementationClass = "com.github.tidetunes.buildlogic.TideTunesCargoUniffiPlugin"
            displayName = "TideTunes Cargo + UniFFI conventions"
            description = "Gobley Cargo and UniFFI build conventions for the shared module."
        }
    }
}
