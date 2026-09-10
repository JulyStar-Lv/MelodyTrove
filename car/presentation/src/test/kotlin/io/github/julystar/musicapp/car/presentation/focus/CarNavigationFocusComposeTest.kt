package io.github.julystar.musicapp.car.presentation.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.component.CarNavigationItem
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfile
import io.github.julystar.musicapp.car.presentation.theme.CarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarNavigationFocusComposeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun initialNavigationFocusMovesAlongDeclaredGraph() {
        val coordinator = CarFocusCoordinator()
        compose.setContent {
            CarTheme {
                CarFocusHost(coordinator, "navigation-test", CarFocusIds.Home) {
                    Column {
                        CarNavigationItem(
                            label = "首页",
                            icon = CarIcon.Home,
                            selected = true,
                            enabled = true,
                            iconSize = 40.dp,
                            onClick = {},
                            modifier = Modifier
                                .testTag("home")
                                .carFocusTarget(CarFocusIds.Home, down = CarFocusIds.Playlists)
                                .height(72.dp),
                        )
                        CarNavigationItem(
                            label = "播放列表",
                            icon = CarIcon.Playlists,
                            selected = false,
                            enabled = true,
                            iconSize = 40.dp,
                            onClick = {},
                            modifier = Modifier
                                .testTag("playlists")
                                .carFocusTarget(CarFocusIds.Playlists, up = CarFocusIds.Home)
                                .height(72.dp),
                        )
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("home").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("playlists").assertIsFocused()
    }

    @Test
    fun rotaryScrollMovesFocusAlongDeclaredGraph() {
        val coordinator = CarFocusCoordinator()
        compose.setContent {
            CarTheme {
                val focusManager = LocalFocusManager.current
                CarFocusHost(coordinator, "rotary-test", CarFocusIds.Home) {
                    Column(
                        Modifier
                            .testTag("input-root")
                            .carInputRouter(
                                focusManager = focusManager,
                                onPlayPause = {},
                                onNext = {},
                                onPrevious = {},
                                onStop = {},
                            ),
                    ) {
                        CarNavigationItem(
                            label = "首页",
                            icon = CarIcon.Home,
                            selected = true,
                            enabled = true,
                            iconSize = 40.dp,
                            onClick = {},
                            modifier = Modifier
                                .testTag("rotary-home")
                                .carFocusTarget(CarFocusIds.Home, down = CarFocusIds.Playlists)
                                .height(72.dp),
                        )
                        CarNavigationItem(
                            label = "播放列表",
                            icon = CarIcon.Playlists,
                            selected = false,
                            enabled = true,
                            iconSize = 40.dp,
                            onClick = {},
                            modifier = Modifier
                                .testTag("rotary-playlists")
                                .carFocusTarget(CarFocusIds.Playlists, up = CarFocusIds.Home)
                                .height(72.dp),
                        )
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("rotary-home").assertIsFocused()
        compose.onNodeWithTag("rotary-home").performRotaryScrollInput {
            rotateToScrollVertically(1f)
        }
        compose.onNodeWithTag("rotary-playlists").assertIsFocused()
    }

    @Test
    fun profileSwitchRestoresTheFocusedSemanticTarget() {
        val coordinator = CarFocusCoordinator()
        lateinit var switchProfile: () -> Unit
        compose.setContent {
            var profile by remember { mutableStateOf(CarLayoutProfile.Expanded) }
            switchProfile = { profile = CarLayoutProfile.VehiclePanel }
            CarTheme {
                CarFocusHost(coordinator, "profile-switch", CarFocusIds.Home, profile) {
                    Column {
                        CarNavigationItem(
                            "首页", CarIcon.Home, true, true, 40.dp, {},
                            Modifier.testTag("profile-home")
                                .carFocusTarget(CarFocusIds.Home, down = CarFocusIds.Playlists)
                                .height(72.dp),
                        )
                        CarNavigationItem(
                            "播放列表", CarIcon.Playlists, false, true, 40.dp, {},
                            Modifier.testTag("profile-playlists")
                                .carFocusTarget(CarFocusIds.Playlists, up = CarFocusIds.Home)
                                .height(72.dp),
                        )
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("profile-home").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("profile-playlists").assertIsFocused()
        compose.runOnIdle(switchProfile)
        compose.waitForIdle()
        compose.onNodeWithTag("profile-playlists").assertIsFocused()
    }
}
