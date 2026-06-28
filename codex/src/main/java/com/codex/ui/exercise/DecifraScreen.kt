package com.codex.ui.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codex.data.model.ExerciseItem
import com.codex.ui.common.*
import com.codex.ui.theme.*

@Composable
fun DecifraScreen(
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
    val selezionata = rispostaSelezionata

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EtichettaSezione("🔍 Decifra")

        CardPergamena {
            if (esercizio.prompt.isNotBlank()) {
                Text(
                    esercizio.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                esercizio.latin,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Quale traduzione è corretta?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            opzioni.forEach { opzione ->
                val stato = when {
                    !mostraFeedback && selezionata == opzione -> StatoChip.SELEZIONATO
                    mostraFeedback && opzione == esercizio.answer -> StatoChip.CORRETTO
                    mostraFeedback && opzione == selezionata && !ultimaCorretta -> StatoChip.SBAGLIATO
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

        if (selezionata.isNotBlank() && !mostraFeedback) {
            PulsanteOro("Conferma", onClick = onConferma, modifier = Modifier.fillMaxWidth())
        }

        if (mostraFeedback) {
            FeedbackBox(corretta = ultimaCorretta, insight = esercizio.insight)
            if (esercizio.dive.isNotBlank()) {
                ScavoCard(testo = esercizio.dive)
            }
        }
    }
}
