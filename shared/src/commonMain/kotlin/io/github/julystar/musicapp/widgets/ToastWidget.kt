package io.github.julystar.musicapp.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.core.presentation.components.DesignToast
import io.github.julystar.musicapp.core.presentation.overlay.ToastVM
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ToastFrame(
    toastVM: ToastVM = koinViewModel(),
) {
    val spacing = DesignTokens.spacing
    var message by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        toastVM.toast.collectLatest { msg ->
            message = msg
            visible = true
            delay(2000)
            visible = false
        }
    }

    // TODO: KMP - toastRes uses Android R.int resource IDs, needs migration to compose resources

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            DesignToast(
                message = message,
                modifier = Modifier.padding(bottom = spacing.xxl),
            )
        }
    }
}
