package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.icon_visibility
import tidetunes.core.presentation.generated.resources.icon_visibility_off
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SimpleFormText(
    label: String?,
    value: String,
    onChange: (value: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
        }
        TideTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = value,
            onValueChange = {value -> onChange(value)},
        )
    }
}

@Composable
fun FormWidget(
    label: String,
    block: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        block()
    }
}

@Composable
fun FormText(
    label: String,
    value: String,
    onChange: (value: String) -> Unit,
    error: StringResource? = null,
    isPassword: Boolean = false
) {
    var passwordVisibleState = remember { mutableStateOf(false) }
    val passwordVisible = passwordVisibleState.value

    FormWidget(
        label = label
    ) {
        if (!isPassword) {
            TideTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = value,
                onValueChange = onChange,
            )
        } else {
            TideTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = value,
                onValueChange = onChange,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val painter = if (!passwordVisible) {
                        painterResource(Res.drawable.icon_visibility)
                    } else {
                        painterResource(Res.drawable.icon_visibility_off)
                    }

                    TideIconButton(
                        size = TideIconButtonSize.Medium,
                        variant = TideIconButtonVariant.Default,
                        painter = painter,
                        onClick = {
                            passwordVisibleState.value = !passwordVisible
                        },
                    )
                }
            )
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                color = MiuixTheme.colorScheme.error,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun FormSwitch(
    label: String,
    value: Boolean,
    onChange: (value: Boolean) -> Unit,
) {
    FormWidget(
        label = label
    ) {
        AppSwitch(
            checked = value,
            onCheckedChange = onChange
        )
    }
}
