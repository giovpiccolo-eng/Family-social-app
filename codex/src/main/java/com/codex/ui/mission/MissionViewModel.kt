package com.codex.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.data.model.ExerciseItem
import com.codex.data.repository.CodexRepository
import com.codex.domain.AdaptiveEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class FaseMissione {
    RICHIAMO, SCOPERTA, FORGIA, INCURSIONE, DECIFRA, RISULTATO
}

data class MissioneState(
    val giorno: Int = 1,
    val fase: FaseMissione = FaseMissione.RICHIAMO,
    val eserciziDellaFase: List<ExerciseItem> = emptyList(),
    val indiceEsercizio: Int = 0,
    val rispostaSelezionata: String = "",
    val mostraFeedback: Boolean = false,
    val ultimaRispostaCorretta: Boolean = false,
    val xpAccumulato: Int = 0,
    val aureiAccumulati: Int = 0,
    val risposteCorrette: Int = 0,
    val totaleDomande: Int = 0,
    val comboCount: Int = 0,
    val moltiplicatore: Int = 1,
    val secondiRestanti: Int = 1800, // 30 min
    val secondiFase: Int = 300,
    val mostraCelebrazione: Boolean = false,
    val completato: Boolean = false,
    val difficoltaCorrente: Float = 1f,
    val tempoRispostaMs: Long = 0L,
    val inizioRisposta: Long = 0L
)

class MissionViewModel(
    private val giorno: Int,
    private val repo: CodexRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MissioneState(giorno = giorno))
    val state: StateFlow<MissioneState> = _state.asStateFlow()

    private val tuttiGli = mutableListOf<ExerciseItem>()
    private var timerJob: Job? = null
    private var inizioSessione = System.currentTimeMillis()
    private val tempiRisposta = mutableListOf<Long>()

    init { carica() }

    private fun carica() = viewModelScope.launch {
        val es = repo.eserciziPerGiorno(giorno)
        tuttiGli.addAll(es)
        iniziaFase(FaseMissione.RICHIAMO)
        avviaTimer()
    }

    private fun iniziaFase(fase: FaseMissione) {
        val esercizi = filtraFase(fase).shuffled().take(limiteFase(fase))
        val secondi = secondiFase(fase)
        _state.update {
            it.copy(
                fase = fase,
                eserciziDellaFase = esercizi,
                indiceEsercizio = 0,
                rispostaSelezionata = "",
                mostraFeedback = false,
                secondiFase = secondi,
                inizioRisposta = System.currentTimeMillis()
            )
        }
    }

    private fun filtraFase(fase: FaseMissione): List<ExerciseItem> = when (fase) {
        FaseMissione.RICHIAMO  -> tuttiGli.filter { it.mode == "recall" }
            .plus(tuttiGli.filter { it.mode == "recall" }.take(2)) // include SRS
        FaseMissione.SCOPERTA  -> tuttiGli.filter { it.mode == "decode" }
        FaseMissione.FORGIA    -> tuttiGli.filter { it.mode == "forge" }
        FaseMissione.INCURSIONE -> tuttiGli.filter { it.mode in listOf("recall","decode","forge","raid") }
        FaseMissione.DECIFRA   -> tuttiGli.filter { it.mode in listOf("decode","crack") }
        FaseMissione.RISULTATO -> emptyList()
    }

    private fun limiteFase(fase: FaseMissione) = when (fase) {
        FaseMissione.RICHIAMO  -> 5
        FaseMissione.SCOPERTA  -> 3
        FaseMissione.FORGIA    -> 4
        FaseMissione.INCURSIONE -> 7
        FaseMissione.DECIFRA   -> 1
        FaseMissione.RISULTATO -> 0
    }

    private fun secondiFase(fase: FaseMissione) = when (fase) {
        FaseMissione.RICHIAMO  -> 300
        FaseMissione.SCOPERTA  -> 420
        FaseMissione.FORGIA    -> 480
        FaseMissione.INCURSIONE -> 360
        FaseMissione.DECIFRA   -> 240
        FaseMissione.RISULTATO -> 0
    }

    fun selezionaRisposta(risposta: String) {
        if (_state.value.mostraFeedback) return
        val now = System.currentTimeMillis()
        val tempoRisposta = now - _state.value.inizioRisposta
        tempiRisposta.add(tempoRisposta)
        _state.update { it.copy(rispostaSelezionata = risposta, tempoRispostaMs = tempoRisposta) }
    }

    fun confermaRisposta() = viewModelScope.launch {
        val st = _state.value
        val esercizio = st.eserciziDellaFase.getOrNull(st.indiceEsercizio) ?: return@launch
        val corretta = st.rispostaSelezionata.trim().equals(esercizio.answer.trim(), ignoreCase = true)

        val nuovoCombo = if (corretta) st.comboCount + 1 else 0
        val moltiplicatore = when {
            nuovoCombo >= 5 -> 3
            nuovoCombo >= 3 -> 2
            else -> 1
        }
        val xp = if (corretta) (esercizio.reward.xp * moltiplicatore) else 0
        val aurei = if (corretta) (esercizio.reward.aurei * moltiplicatore) else 0
        val qualitaSRS = when {
            corretta && st.tempoRispostaMs < 3000 -> 3
            corretta -> 2
            else -> 0
        }

        repo.aggiornaMaestria(esercizio.skill, corretta)
        repo.aggiornaCartaSRS(esercizio.id, qualitaSRS)

        _state.update {
            it.copy(
                mostraFeedback = true,
                ultimaRispostaCorretta = corretta,
                xpAccumulato = it.xpAccumulato + xp,
                aureiAccumulati = it.aureiAccumulati + aurei,
                risposteCorrette = it.risposteCorrette + (if (corretta) 1 else 0),
                totaleDomande = it.totaleDomande + 1,
                comboCount = nuovoCombo,
                moltiplicatore = moltiplicatore
            )
        }

        delay(800)
        prossimoEsercizio()
    }

    private fun prossimoEsercizio() {
        val st = _state.value
        val prossimo = st.indiceEsercizio + 1
        if (prossimo >= st.eserciziDellaFase.size) {
            prossimaTappa()
        } else {
            _state.update {
                it.copy(
                    indiceEsercizio = prossimo,
                    rispostaSelezionata = "",
                    mostraFeedback = false,
                    inizioRisposta = System.currentTimeMillis()
                )
            }
        }
    }

    private fun prossimaTappa() {
        val fase = _state.value.fase
        val prossimaFase = when (fase) {
            FaseMissione.RICHIAMO  -> FaseMissione.SCOPERTA
            FaseMissione.SCOPERTA  -> FaseMissione.FORGIA
            FaseMissione.FORGIA    -> FaseMissione.INCURSIONE
            FaseMissione.INCURSIONE -> FaseMissione.DECIFRA
            FaseMissione.DECIFRA   -> FaseMissione.RISULTATO
            FaseMissione.RISULTATO -> FaseMissione.RISULTATO
        }
        _state.update { it.copy(mostraCelebrazione = true) }
        viewModelScope.launch {
            delay(900)
            _state.update { it.copy(mostraCelebrazione = false) }
            if (prossimaFase == FaseMissione.RISULTATO) {
                completaMissione()
            } else {
                iniziaFase(prossimaFase)
            }
        }
    }

    private fun completaMissione() = viewModelScope.launch {
        timerJob?.cancel()
        val st = _state.value
        val durata = ((System.currentTimeMillis() - inizioSessione) / 1000).toInt()
        val accuratezza = if (st.totaleDomande > 0)
            st.risposteCorrette.toFloat() / st.totaleDomande else 0f

        repo.completaGiorno(giorno, st.xpAccumulato, st.aureiAccumulati)
        repo.aggiungiXPAurei(st.xpAccumulato, st.aureiAccumulati)
        repo.aggiornaStreak()
        repo.registraSessione(
            day = giorno,
            durata = durata,
            xp = st.xpAccumulato,
            aurei = st.aureiAccumulati,
            accuratezza = accuratezza,
            round = 5
        )
        verificaAchievement(st)
        _state.update { it.copy(fase = FaseMissione.RISULTATO, completato = true) }
    }

    private suspend fun verificaAchievement(st: MissioneState) {
        val acc = if (st.totaleDomande > 0) st.risposteCorrette.toFloat() / st.totaleDomande else 0f
        if (acc == 1f) repo.sbloccaAchievement("incursione_perfetta")
        val statsPost = repo.getStats()
        if (statsPost.streakCorrente >= 7) repo.sbloccaAchievement("settimana_decoder")
        if (giorno == 7) repo.sbloccaAchievement("boss_atto1")
        if (giorno == 14) repo.sbloccaAchievement("boss_atto2")
        if (giorno == 21) repo.sbloccaAchievement("imperator")
    }

    private fun avviaTimer() {
        timerJob = viewModelScope.launch {
            while (_state.value.secondiRestanti > 0) {
                delay(1000)
                _state.update { it.copy(secondiRestanti = it.secondiRestanti - 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
