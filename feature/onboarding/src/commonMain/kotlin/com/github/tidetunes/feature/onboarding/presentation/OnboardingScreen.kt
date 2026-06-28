package com.github.tidetunes.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
) {
    val page = OnboardingPage.entries[state.currentPage]
    val isLastPage = state.currentPage == OnboardingPage.entries.last().index

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(48.dp))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OnboardingPage.entries.forEachIndexed { index, _ ->
                Surface(
                    modifier = Modifier.size(if (index == state.currentPage) 12.dp else 8.dp),
                    shape = CircleShape,
                    color = if (index == state.currentPage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                ) {}
                if (index < OnboardingPage.entries.size - 1) {
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        when (page) {
            OnboardingPage.Welcome -> {
                TideTunesTextButton(
                    text = "Get Started",
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(OnboardingAction.NextPage) },
                )
            }
            OnboardingPage.AddSources -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TideTunesTextButton(
                        text = "Add Music Sources",
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Medium,
                        onClick = { onAction(OnboardingAction.NavigateToSources) },
                    )
                    Spacer(Modifier.height(12.dp))
                    TideTunesTextButton(
                        text = "Skip",
                        type = TideTunesTextButtonType.Default,
                        size = TideTunesTextButtonSize.Medium,
                        onClick = { onAction(OnboardingAction.NextPage) },
                    )
                }
            }
            OnboardingPage.Ready -> {
                TideTunesTextButton(
                    text = "Start Listening",
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(OnboardingAction.Finish) },
                )
            }
        }

        if (!isLastPage && state.currentPage > 0) {
            Spacer(Modifier.height(12.dp))
            TideTunesTextButton(
                text = "Back",
                type = TideTunesTextButtonType.Default,
                size = TideTunesTextButtonSize.Small,
                onClick = { onAction(OnboardingAction.PreviousPage) },
            )
        }

        Spacer(Modifier.weight(1f))
    }
}
