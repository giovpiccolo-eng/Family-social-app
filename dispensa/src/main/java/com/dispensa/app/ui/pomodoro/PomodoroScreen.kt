package com.dispensa.app.ui.pomodoro

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dispensa.app.DispensaApp
import com.dispensa.app.R
import com.dispensa.app.pomodoro.PomodoroController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    topicTitle: String,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DispensaApp
    val vm: PomodoroViewModel = viewModel(factory = PomodoroViewModel.factory(app, topicTitle))
    val ui by vm.state.collectAsState()

    val notifPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — notification is best-effort */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pomodoro)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (ui.topicTitle.isNotBlank()) {
                Text(
                    ui.topicTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            val phaseLabel = when (ui.phase) {
                PomodoroController.Phase.Focus -> stringResource(R.string.focus_phase)
                PomodoroController.Phase.Rest -> stringResource(R.string.break_phase)
                else -> ""
            }
            if (phaseLabel.isNotBlank()) {
                Text(
                    phaseLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = when (ui.phase) {
                        PomodoroController.Phase.Focus -> MaterialTheme.colorScheme.primary
                        PomodoroController.Phase.Rest -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CountdownRing(
                    remaining = ui.remainingSec,
                    total = if (ui.totalSec == 0) ui.chosenMinutes * 60 else ui.totalSec,
                    color = when (ui.phase) {
                        PomodoroController.Phase.Focus -> MaterialTheme.colorScheme.primary
                        PomodoroController.Phase.Rest -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                )
                Text(
                    formatTime(
                        if (ui.phase == PomodoroController.Phase.Idle) ui.chosenMinutes * 60
                        else ui.remainingSec
                    ),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (ui.phase == PomodoroController.Phase.Idle) {
                Text(stringResource(R.string.choose_focus_minutes), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 25, 30).forEach { m ->
                        FilterChip(
                            selected = ui.chosenMinutes == m,
                            onClick = { vm.setMinutes(m) },
                            label = { Text("$m ${stringResource(R.string.minutes)}") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.startFocus() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) { Text(stringResource(R.string.start), fontWeight = FontWeight.SemiBold) }
            } else {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.cancel() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.stop), fontWeight = FontWeight.SemiBold) }
                if (ui.phase == PomodoroController.Phase.Focus && ui.remainingSec == 0) {
                    OutlinedButton(
                        onClick = { vm.startRest() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pausa 5 min") }
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(remaining: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ringStroke = 14.dp
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = ringStroke.toPx()
            val pad = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(pad, pad)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            val frac = if (total > 0) remaining.toFloat() / total else 1f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = -360f * frac,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
