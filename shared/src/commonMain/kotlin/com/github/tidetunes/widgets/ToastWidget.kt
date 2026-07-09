package com.github.tidetunes.widgets

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
import com.github.tidetunes.core.presentation.components.TideToast
import com.github.tidetunes.core.presentation.overlay.ToastVM
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ToastFrame(
    toastVM: ToastVM = koinViewModel(),
) {
    val spacing = TideTunesTokens.spacing
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
            TideToast(
                message = message,
                modifier = Modifier.padding(bottom = spacing.xxl),
            )
        }
    }
}
