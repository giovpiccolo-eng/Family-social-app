package com.codex.ui.exercise

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.ui.theme.*

@Composable
fun FeedbackBox(corretta: Boolean, insight: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (corretta) VerdeSuccesso.copy(alpha = 0.12f) else RossoErrore.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (corretta) VerdeSuccesso else RossoErrore)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (corretta) "✓ Corretto!" else "✗ Non esatto",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (corretta) VerdeSuccesso else RossoErrore,
                    fontWeight = FontWeight.Bold
                )
                if (insight.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        insight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ScavoCard(testo: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = OroImperiale.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, OroImperiale.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(12.dp)) {
            Text("🏺 ", style = MaterialTheme.typography.bodySmall)
            Text(
                testo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
