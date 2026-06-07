package com.dispensa.app.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dispensa.app.DispensaApp
import com.dispensa.app.pomodoro.PomodoroController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PomodoroUi(
    val phase: PomodoroController.Phase = PomodoroController.Phase.Idle,
    val remainingSec: Int = 0,
    val totalSec: Int = 0,
    val topicTitle: String = "",
    val chosenMinutes: Int = 20,
)

class PomodoroViewModel(
    private val controller: PomodoroController,
    initialTopicTitle: String,
) : ViewModel() {

    private val chosenMinutes = MutableStateFlow(20)
    private val tickerNudge = MutableStateFlow(0L)
    private val titleHint = MutableStateFlow(initialTopicTitle)

    val state: StateFlow<PomodoroUi> = combine(
        controller.snapshot,
        chosenMinutes,
        tickerNudge,
        titleHint,
    ) { snap, mins, _, titleFromUi ->
        val remaining = when (snap.phase) {
            PomodoroController.Phase.Idle -> 0
            else -> ((snap.endAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        }
        PomodoroUi(
            phase = snap.phase,
            remainingSec = remaining,
            totalSec = snap.durationSec,
            topicTitle = snap.topicTitle.ifBlank { titleFromUi },
            chosenMinutes = mins,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PomodoroUi())

    init {
        // 1 Hz ticker drives the UI countdown.
        viewModelScope.launch {
            while (true) {
                tickerNudge.value = System.currentTimeMillis()
                delay(500)
            }
        }
    }

    fun setMinutes(min: Int) { chosenMinutes.value = min.coerceIn(5, 60) }

    fun startFocus() {
        viewModelScope.launch {
            controller.startFocus(chosenMinutes.value, titleHint.value)
        }
    }

    fun startRest() {
        viewModelScope.launch { controller.startRest(5, titleHint.value) }
    }

    fun cancel() {
        viewModelScope.launch { controller.cancel() }
    }

    companion object {
        fun factory(app: DispensaApp, topicTitle: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { PomodoroViewModel(app.graph.pomodoroController, topicTitle) }
        }
    }
}
