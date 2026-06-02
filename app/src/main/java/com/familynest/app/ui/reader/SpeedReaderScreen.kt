package com.familynest.app.ui.reader

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.drawBehind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReaderScreen(
    viewModel: SpeedReaderViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.documentTitle.ifBlank { "Speed Reader" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.pause()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )

            // RSVP focal area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                RsvpFocalDisplay(
                    currentWord = state.currentWord,
                    prevWord = state.prevWord,
                    nextWord = state.nextWord,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Word position and time estimate
            val wordsLeft = (state.words.size - state.currentIndex).coerceAtLeast(0)
            val secsLeft = if (state.wpm > 0) wordsLeft * 60 / state.wpm else 0
            Text(
                text = buildString {
                    append("${state.currentIndex + 1} / ${state.words.size}")
                    if (secsLeft > 0) {
                        val min = secsLeft / 60
                        val sec = secsLeft % 60
                        append(" · ")
                        if (min > 0) append("${min}m ")
                        append("${sec}s left")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // WPM control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Speed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        "${state.wpm} WPM",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = state.wpm.toFloat(),
                    onValueChange = { viewModel.setWpm(it.toInt()) },
                    onValueChangeFinished = { viewModel.applyWpm() },
                    valueRange = 60f..1000f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("60", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("1000 WPM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                IconButton(
                    onClick = { viewModel.seek(-10) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Rounded.FastRewind,
                        contentDescription = "Back 10 words",
                        modifier = Modifier.size(32.dp),
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                    )
                }

                IconButton(
                    onClick = { viewModel.seek(10) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = "Forward 10 words",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RsvpFocalDisplay(
    currentWord: String,
    prevWord: String,
    nextWord: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val wordColor = MaterialTheme.colorScheme.onSurface
    val orpColor = MaterialTheme.colorScheme.error
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val guideColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)

    Surface(
        modifier = modifier.height(200.dp),
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRsvpContent(
                        word = currentWord,
                        prevWord = prevWord,
                        nextWord = nextWord,
                        textMeasurer = textMeasurer,
                        wordColor = wordColor,
                        orpColor = orpColor,
                        dimColor = dimColor,
                        guideColor = guideColor,
                    )
                },
        )
    }
}

private fun DrawScope.drawRsvpContent(
    word: String,
    prevWord: String,
    nextWord: String,
    textMeasurer: TextMeasurer,
    wordColor: Color,
    orpColor: Color,
    dimColor: Color,
    guideColor: Color,
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    if (word.isEmpty()) return

    val orpIdx = orpIndex(word)

    val fontSize = when {
        word.length > 16 -> 26.sp
        word.length > 11 -> 34.sp
        word.length > 7 -> 40.sp
        else -> 48.sp
    }

    val mainStyle = TextStyle(
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
    )

    val before = word.substring(0, orpIdx)
    val orpChar = word[orpIdx].toString()
    val after = if (orpIdx + 1 < word.length) word.substring(orpIdx + 1) else ""

    val beforeLayout = if (before.isNotEmpty()) textMeasurer.measure(before, mainStyle) else null
    val orpLayout = textMeasurer.measure(orpChar, mainStyle)
    val afterLayout = if (after.isNotEmpty()) textMeasurer.measure(after, mainStyle) else null

    val orpX = centerX - orpLayout.size.width / 2f
    val beforeX = orpX - (beforeLayout?.size?.width?.toFloat() ?: 0f)
    val afterX = orpX + orpLayout.size.width
    val wordTop = centerY - orpLayout.size.height / 2f

    // Guide marks: short horizontal bars above and below ORP, with center tick
    val tickTop = wordTop - 12f
    val tickBottom = wordTop + orpLayout.size.height + 12f
    drawLine(guideColor, Offset(centerX - 20f, tickTop), Offset(centerX + 20f, tickTop), strokeWidth = 2f)
    drawLine(guideColor, Offset(centerX - 20f, tickBottom), Offset(centerX + 20f, tickBottom), strokeWidth = 2f)
    drawLine(guideColor, Offset(centerX, tickTop - 8f), Offset(centerX, tickTop - 2f), strokeWidth = 2f)
    drawLine(guideColor, Offset(centerX, tickBottom + 2f), Offset(centerX, tickBottom + 8f), strokeWidth = 2f)

    // Word parts
    beforeLayout?.let { drawText(it, wordColor, topLeft = Offset(beforeX, wordTop)) }
    drawText(orpLayout, orpColor, topLeft = Offset(orpX, wordTop))
    afterLayout?.let { drawText(it, wordColor, topLeft = Offset(afterX, wordTop)) }

    // Context words (previous / next)
    val ctxStyle = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Monospace)
    if (prevWord.isNotEmpty()) {
        val m = textMeasurer.measure(prevWord, ctxStyle)
        drawText(m, dimColor, topLeft = Offset(centerX - m.size.width / 2f, wordTop - m.size.height - 20f))
    }
    if (nextWord.isNotEmpty()) {
        val m = textMeasurer.measure(nextWord, ctxStyle)
        drawText(m, dimColor, topLeft = Offset(centerX - m.size.width / 2f, wordTop + orpLayout.size.height + 20f))
    }
}
