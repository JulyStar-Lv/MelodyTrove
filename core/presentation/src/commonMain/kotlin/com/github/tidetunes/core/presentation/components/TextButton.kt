package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TideTunesTextButtonType {
    Primary,
    PrimaryVariant,
    Error,
    Default,
}

enum class TideTunesTextButtonSize {
    Medium,
    Small,
}

@Composable
fun TideTunesTextButton(
    text: String,
    type: TideTunesTextButtonType,
    size: TideTunesTextButtonSize,
    onClick: () -> Unit,
    disabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fontSize = when (size) {
        TideTunesTextButtonSize.Small -> 10.sp
        TideTunesTextButtonSize.Medium -> 14.sp
    }
    val buttonColors = when(type) {
        TideTunesTextButtonType.Default -> ButtonDefaults.textButtonColors().copy(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        TideTunesTextButtonType.Primary -> {
            ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
        TideTunesTextButtonType.PrimaryVariant -> {
            ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.surface,
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
        TideTunesTextButtonType.Error -> {
            ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }

    TextButton(
        modifier = modifier.padding(0.dp),
        colors = buttonColors,
        onClick = onClick,
        enabled = !disabled
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}