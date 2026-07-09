package com.github.tidetunes.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideButton
import com.github.tidetunes.core.presentation.components.TideButtonVariant
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
) {
    val page = OnboardingPage.entries[state.currentPage]
    val isLastPage = state.currentPage == OnboardingPage.entries.last().index
    val spacing = TideTunesTokens.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MiuixTheme.colorScheme.background,
                        MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                        MiuixTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        TideCardSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            cornerRadius = TideTunesTokens.shapes.xl,
            contentPadding = PaddingValues(spacing.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                OnboardingMark(page = page)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = page.title,
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = page.description,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body1,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OnboardingIndicators(currentPage = state.currentPage)

                OnboardingActions(
                    page = page,
                    isLastPage = isLastPage,
                    currentPage = state.currentPage,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun OnboardingMark(page: OnboardingPage) {
    val label = when (page) {
        OnboardingPage.Welcome -> "T"
        OnboardingPage.AddSources -> "+"
        OnboardingPage.Ready -> "OK"
    }

    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.xxl))
            .background(
                Brush.linearGradient(
                    listOf(
                        TideTunesBrand.Primary,
                        TideTunesBrand.Secondary,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onPrimary,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun OnboardingIndicators(currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingPage.entries.forEachIndexed { index, _ ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (selected) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(
                        if (selected) {
                            TideTunesBrand.Primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.28f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingActions(
    page: OnboardingPage,
    isLastPage: Boolean,
    currentPage: Int,
    onAction: (OnboardingAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (page) {
            OnboardingPage.Welcome -> {
                TideButton(
                    text = "Get Started",
                    variant = TideButtonVariant.Primary,
                    onClick = { onAction(OnboardingAction.NextPage) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OnboardingPage.AddSources -> {
                TideButton(
                    text = "Add Music Sources",
                    variant = TideButtonVariant.Primary,
                    onClick = { onAction(OnboardingAction.NavigateToSources) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TideTextButton(
                    text = "Skip",
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    onClick = { onAction(OnboardingAction.NextPage) },
                )
            }
            OnboardingPage.Ready -> {
                TideButton(
                    text = "Start Listening",
                    variant = TideButtonVariant.Primary,
                    onClick = { onAction(OnboardingAction.Finish) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!isLastPage && currentPage > 0) {
            Spacer(Modifier.height(2.dp))
            TideTextButton(
                text = "Back",
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Small,
                onClick = { onAction(OnboardingAction.PreviousPage) },
            )
        }
    }
}
