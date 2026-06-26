package com.github.tidetune.components

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

enum class TideTuneTextButtonType {
    Primary,
    PrimaryVariant,
    Error,
    Default,
}

enum class TideTuneTextButtonSize {
    Medium,
    Small,
}

@Composable
fun TideTuneTextButton(
    text: String,
    type: TideTuneTextButtonType,
    size: TideTuneTextButtonSize,
    onClick: () -> Unit,
    disabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fontSize = when (size) {
        TideTuneTextButtonSize.Small -> 10.sp
        TideTuneTextButtonSize.Medium -> 14.sp
    }
    val buttonColors = when(type) {
        TideTuneTextButtonType.Default -> ButtonDefaults.textButtonColors().copy(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        TideTuneTextButtonType.Primary -> {
            ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
        TideTuneTextButtonType.PrimaryVariant -> {
            ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.surface,
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
        TideTuneTextButtonType.Error -> {
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