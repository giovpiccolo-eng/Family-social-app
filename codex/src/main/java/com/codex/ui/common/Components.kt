package com.codex.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.ui.theme.*
import kotlinx.coroutines.delay

// ── Barra XP ──────────────────────────────────────────────────────────────────

@Composable
fun XpBar(
    progresso: Float,
    titolo: String,
    xp: Int,
    modifier: Modifier = Modifier
) {
    val animProg by animateFloatAsState(progresso, animationSpec = tween(600), label = "xp")
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                titolo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "$xp XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animProg },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ── Streak badge ──────────────────────────────────────────────────────────────

@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    val fiamma = if (streak > 0) "🔥" else "❄️"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(fiamma, fontSize = 16.sp)
            Text(
                "$streak",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Card pergamena ─────────────────────────────────────────────────────────────

@Composable
fun CardPergamena(
    modifier: Modifier = Modifier,
    elevation: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkMode.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (elevation) 4.dp else 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

// ── Pulsante oro ──────────────────────────────────────────────────────────────

@Composable
fun PulsanteOro(
    testo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    abilitato: Boolean = true
) {
    val scale by animateFloatAsState(
        if (abilitato) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )
    Button(
        onClick = onClick,
        enabled = abilitato,
        modifier = modifier.scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = OroImperiale,
            contentColor = InchiostroProfondo,
            disabledContainerColor = GrigioNote,
            disabledContentColor = PergamenaScura
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            testo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Risposta chip ─────────────────────────────────────────────────────────────

@Composable
fun ChipRisposta(
    testo: String,
    stato: StatoChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (sfondo, bordo, tColor) = when (stato) {
        StatoChip.NEUTRO -> Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSurface
        )
        StatoChip.SELEZIONATO -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary
        )
        StatoChip.CORRETTO -> Triple(
            VerdeSuccesso.copy(alpha = 0.15f),
            VerdeSuccesso,
            VerdeSuccesso
        )
        StatoChip.SBAGLIATO -> Triple(
            RossoErrore.copy(alpha = 0.15f),
            RossoErrore,
            RossoErrore
        )
    }

    val scale by animateFloatAsState(
        if (stato == StatoChip.CORRETTO || stato == StatoChip.SBAGLIATO) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(8.dp),
        color = sfondo,
        border = BorderStroke(1.5.dp, bordo)
    ) {
        Text(
            testo,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = tColor,
            textAlign = TextAlign.Center
        )
    }
}

enum class StatoChip { NEUTRO, SELEZIONATO, CORRETTO, SBAGLIATO }

// ── Timer missione ─────────────────────────────────────────────────────────────

@Composable
fun TimerMissione(secondiRimasti: Int, totale: Int, modifier: Modifier = Modifier) {
    val frac = (secondiRimasti.toFloat() / totale).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(frac, animationSpec = tween(1000), label = "timer")
    val colore = when {
        frac > 0.5f -> VerdeSuccesso
        frac > 0.25f -> OroImperiale
        else -> RossoErrore
    }
    val minuti = secondiRimasti / 60
    val sec = secondiRimasti % 60

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%d:%02d".format(minuti, sec),
            style = MaterialTheme.typography.titleSmall,
            color = colore,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { animFrac },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = colore,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ── Moltiplicatore combo ──────────────────────────────────────────────────────

@Composable
fun MoltiplicatoreCombo(valore: Int, modifier: Modifier = Modifier) {
    if (valore <= 1) return
    val scale by animateFloatAsState(
        if (valore > 1) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "combo"
    )
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(6.dp),
        color = OroImperiale.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, OroImperiale)
    ) {
        Text(
            "×$valore",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = OroImperiale,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Celebrazione micro ─────────────────────────────────────────────────────────

@Composable
fun MicroCelebrazione(
    visibile: Boolean,
    testo: String = "+XP",
    onFine: () -> Unit
) {
    LaunchedEffect(visibile) {
        if (visibile) {
            delay(800)
            onFine()
        }
    }
    AnimatedVisibility(
        visible = visibile,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                testo,
                style = MaterialTheme.typography.headlineLarge,
                color = OroImperiale,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Label sezione monospazio ──────────────────────────────────────────────────

@Composable
fun EtichettaSezione(testo: String, modifier: Modifier = Modifier) {
    Text(
        testo.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp
    )
}

// ── Icona modalità esercizio ──────────────────────────────────────────────────

fun emojiModalita(mode: String): String = when (mode) {
    "recall" -> "⚡"
    "decode" -> "🔍"
    "forge"  -> "⚒️"
    "raid"   -> "⚔️"
    "crack"  -> "🏛️"
    "dive"   -> "🏺"
    else     -> "📜"
}

fun nomeModalita(mode: String): String = when (mode) {
    "recall" -> "Richiamo"
    "decode" -> "Decifra"
    "forge"  -> "Forgia"
    "raid"   -> "Incursione"
    "crack"  -> "Forza il Codice"
    "dive"   -> "Scavo"
    else     -> "Esercizio"
}
