package com.codex.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.data.repository.CodexRepository
import com.codex.ui.common.*
import com.codex.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: CodexRepository,
    onIniziaMissione: (Int) -> Unit,
    onApriMappa: () -> Unit,
    onApriAchievements: () -> Unit,
    onApriDashboard: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>): T = HomeViewModel(repo) as T
    })
    val st by vm.state.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    if (st.caricamento) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OroImperiale)
        }
        return
    }

    val darkTheme = st.stats.temaScuro

    CodexTheme(darkTheme = darkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "CODEX",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { vm.toggleTheme() }) {
                            Icon(
                                if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Tema",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onApriDashboard) {
                            Icon(Icons.Default.BarChart, "Dashboard", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(pad)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Profilo Decoder ──────────────────────────────────────────
                CardPergamena {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "IL DECODER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                st.stats.titolo.uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StreakBadge(st.stats.streakCorrente)
                    }
                    Spacer(Modifier.height(12.dp))
                    XpBar(
                        progresso = st.progressoLivello,
                        titolo = "Lv ${st.stats.livello}",
                        xp = st.stats.xpTotale
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "⚡ ${st.stats.xpTotale} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "🪙 ${st.stats.aureiTotali} Aurei",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "❄️ ×${st.stats.gettonigelo} Gelo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Pulsante missione ─────────────────────────────────────────
                val giorno = st.giornoCorrente
                PulsanteOro(
                    testo = "▶  Inizia la Missione — Giorno $giorno",
                    onClick = { onIniziaMissione(giorno) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )

                // ── Campagna ─────────────────────────────────────────────────
                EtichettaSezione("Campagna · 21 Giorni")
                val attoLabel = mapOf(
                    1 to "Atto I · Fondamenta",
                    2 to "Atto II · Il Motore del Verbo",
                    3 to "Atto III · La Sintassi"
                )
                for (atto in 1..3) {
                    val range = when (atto) { 1 -> 1..7; 2 -> 8..14; else -> 15..21 }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            attoLabel[atto] ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(range.toList()) { _, g ->
                                val completato = st.progressoGiorni.any { it.day == g && it.completato }
                                val corrente = g == giorno
                                GiornoChip(giorno = g, completato = completato, corrente = corrente) {
                                    if (completato || corrente) onIniziaMissione(g)
                                }
                            }
                        }
                    }
                }

                // ── Pulsanti secondari ────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onApriMappa,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("🗺 Mappa", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onApriAchievements,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("🏅 Sigilli", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // ── Motto ─────────────────────────────────────────────────────
                Text(
                    "Il latino non è una lingua morta — è un codice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GiornoChip(
    giorno: Int,
    completato: Boolean,
    corrente: Boolean,
    onClick: () -> Unit
) {
    val sfondo = when {
        completato -> VerdeSuccesso.copy(alpha = 0.2f)
        corrente -> OroImperiale.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bordo = when {
        completato -> VerdeSuccesso
        corrente -> OroImperiale
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }
    val emoji = when { completato -> "✓"; corrente -> "▶"; else -> "$giorno" }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = sfondo,
        border = BorderStroke(1.5.dp, bordo),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                emoji,
                style = if (completato || corrente) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.labelSmall,
                color = bordo,
                fontWeight = if (corrente) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
