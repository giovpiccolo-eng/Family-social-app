package com.dispensa.app.ui.topic

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dispensa.app.DispensaApp
import com.dispensa.app.R
import com.dispensa.app.data.model.Dispensa
import com.dispensa.app.data.model.DispensaStatus
import com.dispensa.app.data.model.Photo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    topicId: String,
    onBack: () -> Unit,
    onOpenDispensa: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as DispensaApp
    val vm: TopicDetailViewModel = viewModel(factory = TopicDetailViewModel.factory(app, topicId))
    val state by vm.state.collectAsState()
    val topic = state.topic

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) pendingPhotoFile?.let { vm.onPhotoTaken(it) }
        pendingPhotoFile = null
        pendingPhotoUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val file = vm.newPhotoFile()
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            pendingPhotoFile = file
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val target = vm.newPhotoFile()
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            if (target.exists() && target.length() > 0) vm.onPhotoTaken(target)
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) vm.importPdf(ctx.contentResolver, uri, ctx.cacheDir)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            topic?.title ?: "",
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!topic?.subject.isNullOrBlank()) {
                            Text(
                                topic!!.subject,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
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
        if (topic == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Photos section header + action buttons
            SectionHeader(stringResource(R.string.photos_section))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceButton(
                    icon = Icons.Filled.CameraAlt,
                    label = stringResource(R.string.take_photo),
                    modifier = Modifier.weight(1f),
                    enabled = !state.importingPdf,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val file = vm.newPhotoFile()
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                            pendingPhotoFile = file
                            pendingPhotoUri = uri
                            takePictureLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )
                SourceButton(
                    icon = Icons.Filled.Image,
                    label = stringResource(R.string.from_gallery),
                    modifier = Modifier.weight(1f),
                    enabled = !state.importingPdf,
                    onClick = {
                        pickPhotoLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                )
                SourceButton(
                    icon = Icons.Filled.PictureAsPdf,
                    label = stringResource(R.string.from_pdf),
                    modifier = Modifier.weight(1f),
                    enabled = !state.importingPdf,
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                )
            }

            if (state.importingPdf) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.importing_pdf),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (topic.photos.isEmpty()) {
                EmptyHint(stringResource(R.string.no_photos_yet))
            } else {
                PhotoGrid(
                    photos = topic.photos,
                    photoFile = { vm.photoFile(it) },
                    onDelete = { vm.deletePhoto(it) },
                )
            }

            // Generate button
            Button(
                onClick = { showGenerateDialog = true },
                enabled = topic.photos.isNotEmpty() && !state.generating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                if (state.generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.generating))
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.generate_dispensa), fontWeight = FontWeight.SemiBold)
                }
            }
            if (state.generating) {
                Text(
                    stringResource(R.string.generating_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // Dispense list
            Spacer(Modifier.height(6.dp))
            SectionHeader(stringResource(R.string.dispense_section))
            if (topic.dispense.isEmpty()) {
                EmptyHint("—")
            } else {
                topic.dispense.sortedByDescending { it.createdAt }.forEach { d ->
                    DispensaRow(
                        dispensa = d,
                        onOpen = { if (d.status == DispensaStatus.READY) onOpenDispensa(d.id) },
                        onDelete = { vm.deleteDispensa(d.id) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showGenerateDialog) {
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text(stringResource(R.string.generate_dispensa)) },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.optional_notes_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    vm.generate(notes) { dispensaId ->
                        onOpenDispensa(dispensaId)
                    }
                }) { Text(stringResource(R.string.generate_dispensa)) }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    state.infoMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearInfo() },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { vm.clearInfo() }) { Text("OK") }
            },
        )
    }

    state.generationError?.let { err ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            icon = { Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.error_generic)) },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { vm.clearError() }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun SourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    photoFile: (Photo) -> File,
    onDelete: (Photo) -> Unit,
) {
    // Inside a vertical scroll, LazyVerticalGrid can't measure. Use a manual chunked row layout.
    val rows = photos.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        val ctx = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(photoFile(p))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        FilledIconButton(
                            onClick = { onDelete(p) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xAA000000),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Pad incomplete rows so cells keep size.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DispensaRow(
    dispensa: Dispensa,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (icon, tint) = when (dispensa.status) {
                DispensaStatus.READY -> Icons.Filled.Description to MaterialTheme.colorScheme.tertiary
                DispensaStatus.GENERATING -> Icons.Filled.HourglassTop to MaterialTheme.colorScheme.secondary
                DispensaStatus.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (dispensa.status) {
                        DispensaStatus.READY -> "Dispensa pronta"
                        DispensaStatus.GENERATING -> "In generazione…"
                        DispensaStatus.FAILED -> "Generazione fallita"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("it")).format(Date(dispensa.createdAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (dispensa.status == DispensaStatus.FAILED && !dispensa.error.isNullOrBlank()) {
                    Text(
                        dispensa.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
