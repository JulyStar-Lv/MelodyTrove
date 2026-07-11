package com.github.tidetunes.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
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
    val spacing = TideTunesTokens.spacing
    val principles = listOf(
        "Simple",
        "Calm",
        "Immersive",
        "Music First",
        "Content First",
        "Adaptive",
        "Native",
        "Cross Platform",
        "Plugin Driven",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        TideTunesBrand.Primary.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    radius = 780f,
                ),
            )
            .background(
                Brush.radialGradient(
                    listOf(
                        TideTunesBrand.Secondary.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    radius = 860f,
                ),
            )
            .background(Color(0xD60C0A14))
            .padding(horizontal = spacing.xl, vertical = spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .heightIn(min = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CoverLogo()
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "TideTunes",
                color = TideTunesBrand.Primary,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "One Library. Every Source.",
                color = Color(0xFF9B97B0),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                principles.take(3).forEach { principle ->
                    CoverPrincipleChip(text = principle)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                principles.drop(3).take(3).forEach { principle ->
                    CoverPrincipleChip(text = principle)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                principles.drop(6).forEach { principle ->
                    CoverPrincipleChip(text = principle)
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                TideTunesBrand.Primary,
                                TideTunesBrand.Secondary,
                            ),
                        ),
                    )
                    .clickable { onAction(OnboardingAction.Finish) }
                    .padding(horizontal = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enter TideTunes",
                    color = Color.White,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "TideTunes Design System · v3.0 · 2024",
                color = Color(0xFF9B97B0),
                style = MiuixTheme.textStyles.footnote2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CoverLogo() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(28.dp))
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
            text = "♪",
            color = Color.White,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CoverPrincipleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(Color(0x99161224))
            .border(
                width = 1.dp,
                color = Color(0x12F0EDF8),
                shape = RoundedCornerShape(TideTunesTokens.shapes.full),
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFFF0EDF8),
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
