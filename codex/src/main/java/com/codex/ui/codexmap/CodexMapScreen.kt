package com.codex.ui.codexmap

import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.data.db.entity.MasteryEntity
import com.codex.data.model.SkillNode
import com.codex.data.repository.CodexRepository
import com.codex.ui.common.CardPergamena
import com.codex.ui.common.EtichettaSezione
import com.codex.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexMapScreen(
    repo: CodexRepository,
    onIndietro: () -> Unit
) {
    var nodi by remember { mutableStateOf<List<SkillNode>>(emptyList()) }
    var maestrie by remember { mutableStateOf<List<MasteryEntity>>(emptyList()) }
    var nodoSelezionato by remember { mutableStateOf<SkillNode?>(null) }

    LaunchedEffect(Unit) {
        nodi = withContext(Dispatchers.IO) { repo.skillTree().nodes }
    }
    LaunchedEffect(Unit) {
        repo.tuttaMaestria().collect { maestrie = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mappa Codex", style = MaterialTheme.typography.headlineMedium,
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
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Ogni nodo è un protocollo grammaticale dell'Archivio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            for (atto in 1..3) {
                val attoLabel = when (atto) {
                    1 -> "Atto I · Fondamenta"
                    2 -> "Atto II · Il Motore del Verbo"
                    else -> "Atto III · La Sintassi"
                }
                EtichettaSezione(attoLabel)

                val nodiAtto = nodi.filter { it.act == atto }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    nodiAtto.forEach { nodo ->
                        val maestria = maestrie.find { it.skillId == nodo.id }
                        NodoCard(
                            nodo = nodo,
                            maestria = maestria?.punteggio ?: 0f,
                            selezionato = nodoSelezionato?.id == nodo.id,
                            onClick = {
                                nodoSelezionato = if (nodoSelezionato?.id == nodo.id) null else nodo
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NodoCard(
    nodo: SkillNode,
    maestria: Float,
    selezionato: Boolean,
    onClick: () -> Unit
) {
    val colore = when {
        maestria >= 80f -> VerdeSuccesso
        maestria >= 50f -> OroImperiale
        maestria > 0f -> GrigioNote
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selezionato) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selezionato) MaterialTheme.colorScheme.primary else colore.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(colore)
                )
                Text(
                    nodo.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${maestria.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colore
                )
            }
            if (selezionato) {
                Spacer(Modifier.height(6.dp))
                Text(nodo.descrizione, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (nodo.requires.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Richiede: ${nodo.requires.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (maestria / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = colore,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
