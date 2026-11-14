package com.opxl.sleepslide.presentation.tutorial

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.domain.repository.TutorialRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SHARING_STOP_TIMEOUT_MS = 5_000L

/**
 * Per-screen coach mark state. One instance per screen (scoped to that screen's
 * back stack entry); the screen id is fixed at construction via assisted injection.
 */
@HiltViewModel(assistedFactory = TutorialViewModel.Factory::class)
class TutorialViewModel @AssistedInject constructor(
    @Assisted private val screen: String,
    private val tutorialRepository: TutorialRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(screen: String): TutorialViewModel
    }

    /** False until DataStore reports the screen unseen — so returning users never see a flash. */
    val showCoachMarks: StateFlow<Boolean> = tutorialRepository.hasSeenCoachMark(screen)
        .map { seen -> !seen }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue = false,
        )

    fun dismiss() {
        viewModelScope.launch { tutorialRepository.markSeen(screen) }
    }
}

/** Screens call this rather than spelling out the assisted [hiltViewModel] overload. */
@Composable
fun tutorialViewModel(screen: String): TutorialViewModel =
    hiltViewModel<TutorialViewModel, TutorialViewModel.Factory>(
        creationCallback = { factory -> factory.create(screen) },
    )
