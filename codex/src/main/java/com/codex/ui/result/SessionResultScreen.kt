package com.codex.ui.result

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codex.ui.common.*
import com.codex.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SessionResultScreen(
    giorno: Int,
    xp: Int,
    aurei: Int,
    accuratezza: Float,
    streak: Int,
    onContinua: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "result_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Giorno $giorno — Completato!",
            style = MaterialTheme.typography.headlineLarge,
            color = OroImperiale,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        CardPergamena(modifier = Modifier.fillMaxWidth()) {
            StatRow("⚡ XP Guadagnato", "+$xp")
            StatRow("🪙 Aurei", "+$aurei")
            StatRow("🎯 Accuratezza", "${(accuratezza * 100).roundToInt()}%")
        }

        Spacer(Modifier.height(32.dp))

        PulsanteOro(
            testo = "Continua ▶",
            onClick = onContinua,
            modifier = Modifier.fillMaxWidth().height(56.dp).scale(scale)
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "L'Archivio di Roma si riaccende…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatRow(label: String, valore: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            valore,
            style = MaterialTheme.typography.bodyMedium,
            color = OroImperiale,
            fontWeight = FontWeight.Bold
        )
    }
}
