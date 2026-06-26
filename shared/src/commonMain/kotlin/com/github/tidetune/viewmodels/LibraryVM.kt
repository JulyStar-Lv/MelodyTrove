package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import com.github.tidetune.singleton.LibraryRepository

class LibraryVM(
    libraryRepository: LibraryRepository,
) : ViewModel() {
    val tracks = libraryRepository.tracks
}
