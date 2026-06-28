package com.codex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.data.db.entity.PlayerStatsEntity
import com.codex.data.db.entity.ProgressEntity
import com.codex.data.repository.CodexRepository
import com.codex.domain.XpEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: PlayerStatsEntity = PlayerStatsEntity(),
    val progressoGiorni: List<ProgressEntity> = emptyList(),
    val giornoCorrente: Int = 1,
    val xpPerProssimo: Int = 200,
    val progressoLivello: Float = 0f,
    val caricamento: Boolean = true
)

class HomeViewModel(private val repo: CodexRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.osservaStats().filterNotNull(),
                repo.tuttoIlProgresso()
            ) { stats, progresso ->
                val giornoCorrente = (progresso.lastOrNull { it.completato }?.day ?: 0) + 1
                HomeUiState(
                    stats = stats,
                    progressoGiorni = progresso,
                    giornoCorrente = giornoCorrente.coerceIn(1, 21),
                    xpPerProssimo = XpEngine.xpPerProssimo(stats.xpTotale),
                    progressoLivello = XpEngine.progressoLivello(stats.xpTotale),
                    caricamento = false
                )
            }.collect { _state.value = it }
        }

        viewModelScope.launch {
            if (repo.getStats().id == 0) {
                repo.aggiungiXPAurei(0, 0) // inizializza stats
            }
        }
    }

    fun toggleTheme() = viewModelScope.launch {
        val s = repo.getStats()
        repo.impostaTheme(!s.temaScuro)
    }
}
