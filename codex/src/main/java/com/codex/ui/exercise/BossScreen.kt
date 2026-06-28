package com.codex.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.data.model.ExerciseItem
import com.codex.ui.common.*
import com.codex.ui.theme.*

@Composable
fun BossScreen(
    esercizio: ExerciseItem,
    rispostaSelezionata: String,
    mostraFeedback: Boolean,
    ultimaCorretta: Boolean,
    onRisposta: (String) -> Unit,
    onConferma: () -> Unit
) {
    var scaffoldIndex by remember(esercizio.id) { mutableIntStateOf(0) }
    val scaffoldItems = esercizio.scaffold
    val haScaffold = scaffoldItems.isNotEmpty()
    val scaffoldCorrente = scaffoldItems.getOrNull(scaffoldIndex)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EtichettaSezione("🏛️ Forza il Codice — Boss")

        CardPergamena {
            Text(
                esercizio.title.ifBlank { "Boss" },
                style = MaterialTheme.typography.titleLarge,
                color = OroImperiale,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                esercizio.latin,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (haScaffold && scaffoldCorrente != null) {
            Text(
                "Impalcatura ${scaffoldIndex + 1}/${scaffoldItems.size}:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CardPergamena(elevation = false) {
                Text(
                    scaffoldCorrente.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                scaffoldCorrente.options.forEach { opt ->
                    val stato = when {
                        !mostraFeedback && rispostaSelezionata == opt -> StatoChip.SELEZIONATO
                        mostraFeedback && opt == scaffoldCorrente.answer -> StatoChip.CORRETTO
                        mostraFeedback && opt == rispostaSelezionata && ultimaCorretta.not() -> StatoChip.SBAGLIATO
                        else -> StatoChip.NEUTRO
                    }
                    ChipRisposta(
                        testo = opt,
                        stato = stato,
                        onClick = { if (!mostraFeedback) onRisposta(opt) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    )
                }
            }
        } else {
            // Risposta finale: traduzione
            Text("Traduci l'iscrizione:", style = MaterialTheme.typography.bodyMedium)
            val opzioni = remember(esercizio.id) {
                (listOf(esercizio.translation) + esercizio.distractors).shuffled()
            }
            opzioni.forEach { opz ->
                val stato = when {
                    !mostraFeedback && rispostaSelezionata == opz -> StatoChip.SELEZIONATO
                    mostraFeedback && opz == esercizio.translation -> StatoChip.CORRETTO
                    mostraFeedback && opz == rispostaSelezionata && !ultimaCorretta -> StatoChip.SBAGLIATO
                    else -> StatoChip.NEUTRO
                }
                ChipRisposta(
                    testo = opz,
                    stato = stato,
                    onClick = { if (!mostraFeedback) onRisposta(opz) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (rispostaSelezionata.isNotBlank() && !mostraFeedback) {
            PulsanteOro("Conferma", onClick = {
                if (haScaffold && scaffoldCorrente != null) {
                    // Vai al prossimo scaffold o completa
                    val risposta = rispostaSelezionata
                    onRisposta(risposta)
                    if (scaffoldIndex < scaffoldItems.size - 1) {
                        scaffoldIndex++
                    } else {
                        onConferma()
                    }
                } else {
                    onConferma()
                }
            }, modifier = Modifier.fillMaxWidth())
        }

        if (mostraFeedback) {
            FeedbackBox(corretta = ultimaCorretta, insight = esercizio.insight)
            if (esercizio.sigillo.isNotBlank()) {
                CardPergamena {
                    Text(
                        "🏅 Sigillo sbloccato: ${esercizio.sigillo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OroImperiale,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ricompensa: ${esercizio.reward.xp} XP + ${esercizio.reward.aurei} Aurei",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
