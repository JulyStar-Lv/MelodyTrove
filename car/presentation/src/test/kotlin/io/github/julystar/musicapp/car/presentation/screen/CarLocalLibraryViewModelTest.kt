package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.core.domain.repository.PermissionChecker
import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CarLocalLibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resumesImportAfterPermissionIsGranted() = runTest(dispatcher) {
        val permission = FakePermissionChecker(granted = false)
        var imports = 0
        val viewModel = CarLocalLibraryViewModel(permission) {
            imports += 1
            SourceAccountLibrarySyncResult(importedCount = 3, skippedCount = 1, failedCount = 0)
        }

        viewModel.importLocalMusic()
        advanceUntilIdle()

        assertEquals(1, permission.requests)
        assertIs<CarLocalLibraryState.AwaitingPermission>(viewModel.state.value)

        permission.granted.value = true
        advanceUntilIdle()

        assertEquals(1, imports)
        val complete = assertIs<CarLocalLibraryState.Complete>(viewModel.state.value)
        assertEquals(3, complete.result.importedCount)
    }

    @Test
    fun exposesImportFailure() = runTest(dispatcher) {
        val permission = FakePermissionChecker(granted = true)
        val viewModel = CarLocalLibraryViewModel(permission) {
            error("Music folder is unavailable")
        }

        viewModel.importLocalMusic()
        advanceUntilIdle()

        val failed = assertIs<CarLocalLibraryState.Failed>(viewModel.state.value)
        assertEquals("Music folder is unavailable", failed.message)
    }
}

private class FakePermissionChecker(granted: Boolean) : PermissionChecker {
    val granted = MutableStateFlow(granted)
    var requests = 0

    override val havePermission = this.granted

    override fun requestStoragePermission() {
        requests += 1
    }
}
