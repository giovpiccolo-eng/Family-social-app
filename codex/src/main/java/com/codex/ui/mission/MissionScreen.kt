package com.codex.ui.mission

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.data.repository.CodexRepository
import com.codex.ui.common.*
import com.codex.ui.exercise.*
import com.codex.ui.result.SessionResultScreen
import com.codex.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    giorno: Int,
    repo: CodexRepository,
    onMissioneCompletata: () -> Unit,
    onTornaHome: () -> Unit
) {
    val vm: MissionViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>): T =
            MissionViewModel(giorno, repo) as T
    })
    val st by vm.state.collectAsStateWithLifecycle()

    if (st.completato && st.fase == FaseMissione.RISULTATO) {
        SessionResultScreen(
            giorno = giorno,
            xp = st.xpAccumulato,
            aurei = st.aureiAccumulati,
            accuratezza = if (st.totaleDomande > 0) st.risposteCorrette.toFloat() / st.totaleDomande else 0f,
            streak = 0,
            onContinua = onMissioneCompletata
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            nomeModalita(st.eserciziDellaFase.getOrNull(st.indiceEsercizio)?.mode ?: ""),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Giorno $giorno",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onTornaHome) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    MoltiplicatoreCombo(
                        st.moltiplicatore,
                        Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Progresso di fase e timer ─────────────────────────────────
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndicatoriRound(st.fase)
                    TimerMissione(
                        st.secondiRestanti,
                        1800,
                        Modifier.width(100.dp)
                    )
                }

                // Barra progresso dentro la fase
                val totEsercizi = st.eserciziDellaFase.size
                val completati = st.indiceEsercizio
                if (totEsercizi > 0) {
                    LinearProgressIndicator(
                        progress = { completati.toFloat() / totEsercizi },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── Esercizio corrente ────────────────────────────────────────
                val esercizio = st.eserciziDellaFase.getOrNull(st.indiceEsercizio)

                AnimatedContent(
                    targetState = "${st.fase}_${st.indiceEsercizio}",
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    label = "esercizio"
                ) {
                    if (esercizio != null) {
                        when (esercizio.mode) {
                            "recall" -> RichiamoScreen(
                                esercizio = esercizio,
                                rispostaSelezionata = st.rispostaSelezionata,
                                mostraFeedback = st.mostraFeedback,
                                ultimaCorretta = st.ultimaRispostaCorretta,
                                onRisposta = vm::selezionaRisposta,
                                onConferma = vm::confermaRisposta
                            )
                            "forge" -> ForgiaScreen(
                                esercizio = esercizio,
                                rispostaSelezionata = st.rispostaSelezionata,
                                mostraFeedback = st.mostraFeedback,
                                ultimaCorretta = st.ultimaRispostaCorretta,
                                onRisposta = vm::selezionaRisposta,
                                onConferma = vm::confermaRisposta
                            )
                            "crack" -> BossScreen(
                                esercizio = esercizio,
                                rispostaSelezionata = st.rispostaSelezionata,
                                mostraFeedback = st.mostraFeedback,
                                ultimaCorretta = st.ultimaRispostaCorretta,
                                onRisposta = vm::selezionaRisposta,
                                onConferma = vm::confermaRisposta
                            )
                            else -> DecifraScreen(
                                esercizio = esercizio,
                                rispostaSelezionata = st.rispostaSelezionata,
                                mostraFeedback = st.mostraFeedback,
                                ultimaCorretta = st.ultimaRispostaCorretta,
                                onRisposta = vm::selezionaRisposta,
                                onConferma = vm::confermaRisposta
                            )
                        }
                    }
                }
            }

            // ── Micro-celebrazione tra fasi ───────────────────────────────────
            MicroCelebrazione(
                visibile = st.mostraCelebrazione,
                testo = when (st.fase) {
                    FaseMissione.RICHIAMO  -> "⚡ Richiamo — Ottimo!"
                    FaseMissione.SCOPERTA  -> "🔍 Scoperta — Bene!"
                    FaseMissione.FORGIA    -> "⚒️ Forgia — Completata!"
                    FaseMissione.INCURSIONE -> "⚔️ Incursione — Superata!"
                    FaseMissione.DECIFRA   -> "📜 Codice Decifrato!"
                    FaseMissione.RISULTATO -> "🏆 Missione Completata!"
                },
                onFine = {}
            )
        }
    }
}

@Composable
private fun IndicatoriRound(faseCorrente: FaseMissione) {
    val fasi = FaseMissione.values().filter { it != FaseMissione.RISULTATO }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        fasi.forEach { f ->
            val attiva = f == faseCorrente
            val passata = f.ordinal < faseCorrente.ordinal
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = when {
                    attiva -> OroImperiale
                    passata -> VerdeSuccesso
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(if (attiva) 10.dp else 8.dp)
            ) {}
        }
    }
}
