package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import com.github.tidetunes.widgets.ToastFrame

@Composable
internal fun AppShell(
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(
                    start = scaffoldPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = scaffoldPadding.calculateRightPadding(LayoutDirection.Ltr),
                    top = scaffoldPadding.calculateTopPadding(),
                )
                .fillMaxSize(),
        ) {
            content(scaffoldPadding)
            ToastFrame()
        }
    }
}
