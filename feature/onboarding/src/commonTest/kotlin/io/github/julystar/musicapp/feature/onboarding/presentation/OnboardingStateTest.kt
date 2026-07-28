package io.github.julystar.musicapp.feature.onboarding.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OnboardingStateTest {

    @Test
    fun `default state starts on first page`() {
        val state = OnboardingState()

        assertEquals(0, state.currentPage)
        assertFalse(state.isComplete)
        assertEquals(OnboardingPage.Welcome, OnboardingPage.entries[state.currentPage])
    }

    @Test
    fun `page model preserves onboarding copy order`() {
        assertEquals(
            listOf(OnboardingPage.Welcome, OnboardingPage.AddSources, OnboardingPage.Ready),
            OnboardingPage.entries,
        )
    }

    @Test
    fun `view model advances and rewinds pages within bounds`() {
        val viewModel = OnboardingViewModel()

        viewModel.onAction(OnboardingAction.PreviousPage)
        assertEquals(0, viewModel.state.value.currentPage)

        viewModel.onAction(OnboardingAction.NextPage)
        assertEquals(1, viewModel.state.value.currentPage)

        viewModel.onAction(OnboardingAction.PreviousPage)
        assertEquals(0, viewModel.state.value.currentPage)
    }

    @Test
    fun `finish action is a singleton command`() {
        assertEquals(OnboardingAction.Finish, OnboardingAction.Finish)
    }
}
