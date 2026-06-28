package com.codex.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.data.db.entity.MasteryEntity
import com.codex.data.repository.CodexRepository
import com.codex.ui.common.*
import com.codex.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repo: CodexRepository,
    onIndietro: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var accMedia by remember { mutableFloatStateOf(0f) }
    var durataMin by remember { mutableIntStateOf(0) }
    var maestrie by remember { mutableStateOf<List<MasteryEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            accMedia = repo.accuratezzaMedia()
            durataMin = repo.durataTotal() / 60
            repo.tuttaMaestria().collect { maestrie = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Dashboard Magister", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = onIndietro) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
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
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CardPergamena {
                EtichettaSezione("Panoramica")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatColumn("${(accMedia * 100).roundToInt()}%", "Accuratezza")
                    StatColumn("$durataMin min", "Tempo totale")
                    StatColumn("${maestrie.size}", "Nodi studiati")
                }
            }

            EtichettaSezione("Maestria per Nodo")
            val deboli = maestrie.filter { it.punteggio < 60f }.sortedBy { it.punteggio }
            val forti = maestrie.filter { it.punteggio >= 80f }.sortedByDescending { it.punteggio }

            if (deboli.isNotEmpty()) {
                CardPergamena {
                    Text("⚠ Da Rinforzare", style = MaterialTheme.typography.labelLarge, color = RossoErrore)
                    Spacer(Modifier.height(8.dp))
                    deboli.take(5).forEach { m ->
                        MaestriaRow(m.skillId, m.punteggio)
                    }
                }
            }
            if (forti.isNotEmpty()) {
                CardPergamena {
                    Text("✓ Punti di Forza", style = MaterialTheme.typography.labelLarge, color = VerdeSuccesso)
                    Spacer(Modifier.height(8.dp))
                    forti.take(5).forEach { m ->
                        MaestriaRow(m.skillId, m.punteggio)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Dashboard per genitori / educatori — solo dati locali",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatColumn(valore: String, label: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(valore, style = MaterialTheme.typography.headlineMedium, color = OroImperiale, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MaestriaRow(skillId: String, punteggio: Float) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(skillId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            "${punteggio.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = when {
                punteggio >= 80 -> VerdeSuccesso
                punteggio >= 50 -> OroImperiale
                else -> RossoErrore
            },
            fontWeight = FontWeight.Bold
        )
    }
}
