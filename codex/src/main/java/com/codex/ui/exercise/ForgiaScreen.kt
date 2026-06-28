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
fun ForgiaScreen(
    esercizio: ExerciseItem,
    rispostaSelezionata: String,
    mostraFeedback: Boolean,
    ultimaCorretta: Boolean,
    onRisposta: (String) -> Unit,
    onConferma: () -> Unit
) {
    val opzioni = remember(esercizio.id) {
        (listOf(esercizio.answer) + esercizio.distractors).shuffled()
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EtichettaSezione("⚒️ Forgia")

        CardPergamena {
            if (esercizio.lemma.isNotBlank()) {
                Text(
                    esercizio.lemma,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                esercizio.target.ifBlank { esercizio.prompt },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (esercizio.stem.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "tema: ${esercizio.stem}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OroImperiale
                )
            }
        }

        Text(
            "Forgia la forma corretta:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            opzioni.forEach { opzione ->
                val stato = when {
                    !mostraFeedback && rispostaSelezionata == opzione -> StatoChip.SELEZIONATO
                    mostraFeedback && opzione == esercizio.answer -> StatoChip.CORRETTO
                    mostraFeedback && opzione == rispostaSelezionata && !ultimaCorretta -> StatoChip.SBAGLIATO
                    else -> StatoChip.NEUTRO
                }
                ChipRisposta(
                    testo = opzione,
                    stato = stato,
                    onClick = { if (!mostraFeedback) onRisposta(opzione) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (rispostaSelezionata.isNotBlank() && !mostraFeedback) {
            PulsanteOro("Conferma", onClick = onConferma, modifier = Modifier.fillMaxWidth())
        }

        if (mostraFeedback) {
            FeedbackBox(corretta = ultimaCorretta, insight = esercizio.insight)
            if (esercizio.hint.isNotBlank() && !ultimaCorretta) {
                CardPergamena(elevation = false) {
                    Text(
                        "💡 ${esercizio.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OroImperiale
                    )
                }
            }
        }
    }
}
