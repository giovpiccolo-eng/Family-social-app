package com.codex.ui.exercise

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
fun RichiamoScreen(
    esercizio: ExerciseItem,
    rispostaSelezionata: String,
    mostraFeedback: Boolean,
    ultimaCorretta: Boolean,
    onRisposta: (String) -> Unit,
    onConferma: () -> Unit
) {
    var mostraRetro by remember(esercizio.id) { mutableStateOf(false) }
    var rispostaScelta by remember(esercizio.id) { mutableStateOf<String?>(null) }

    val fronte = esercizio.front.ifBlank { esercizio.latin }
    val retro = esercizio.back.ifBlank { esercizio.answer }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EtichettaSezione("⚡ Richiamo")

        // Card flip SRS
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (mostraRetro) OroImperiale else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                AnimatedContent(
                    targetState = mostraRetro,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "flip"
                ) { lato ->
                    if (!lato) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                fronte,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (esercizio.lemma.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    esercizio.lemma,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                retro,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (esercizio.dive.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "🏺 ${esercizio.dive}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!mostraRetro) {
            PulsanteOro("Mostra risposta", onClick = { mostraRetro = true }, modifier = Modifier.fillMaxWidth())
        } else if (rispostaScelta == null) {
            Text(
                "Com'è andata?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Difficile" to "difficile", "Ok" to retro, "Facile" to "facile").forEach { (label, val_) ->
                    val targetRisposta = if (val_ == "difficile") "" else retro
                    val qualita = when (val_) { "difficile" -> "difficile"; "facile" -> "facile"; else -> retro }
                    OutlinedButton(
                        onClick = {
                            rispostaScelta = qualita
                            onRisposta(if (val_ == "difficile") "" else retro)
                            onConferma()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = when (val_) {
                                "difficile" -> RossoErrore
                                "facile" -> VerdeSuccesso
                                else -> OroImperiale
                            }
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (mostraFeedback) {
            FeedbackBox(corretta = ultimaCorretta, insight = esercizio.insight)
        }
    }
}
