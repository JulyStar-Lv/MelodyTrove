package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.theme.CarTheme
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarSongRowComposeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun enabledRowHandlesClickAndExposesTrackContent() {
        var clickCount = 0
        compose.setContent {
            CarTheme {
                CarSongRow(
                    track = testTrack,
                    index = 1,
                    playing = false,
                    height = 96.dp,
                    onClick = { clickCount++ },
                    modifier = Modifier.testTag("song-row"),
                )
            }
        }

        compose.onNodeWithTag("song-row")
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithTag("song-row")
            .assertTextEquals("2", "Test Track", "Test Artist", "3:05")
        assertEquals(1, clickCount)
    }

    @Test
    fun playingRowExposesStateAndDisabledRowRejectsClick() {
        var clicked = false
        compose.setContent {
            CarTheme {
                CarSongRow(
                    track = testTrack,
                    index = 0,
                    playing = true,
                    enabled = false,
                    height = 96.dp,
                    onClick = { clicked = true },
                    modifier = Modifier.testTag("song-row"),
                )
            }
        }

        compose.onNodeWithTag("song-row")
            .assertIsNotEnabled()
            .assert(hasStateDescription("正在播放"))
            .performClick()
        compose.onNodeWithTag("song-row").assertTextEquals("Test Track", "Test Artist", "3:05")
        assertEquals(false, clicked)
    }

    private val testTrack = LibraryTrackItem(
        id = 17,
        title = "Test Track",
        artist = "Test Artist",
        durationMs = 185_000,
    )
}
