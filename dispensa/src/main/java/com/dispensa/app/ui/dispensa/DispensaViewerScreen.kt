package com.dispensa.app.ui.dispensa

import android.content.Intent
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.dispensa.app.DispensaApp
import com.dispensa.app.R
import com.dispensa.app.data.model.DispensaStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispensaViewerScreen(
    topicId: String,
    dispensaId: String,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as DispensaApp
    val repo = app.graph.topicRepository
    val topics by repo.topics.collectAsState()
    val topic = topics.firstOrNull { it.id == topicId }
    val dispensa = topic?.dispense?.firstOrNull { it.id == dispensaId }
    val file: File? = dispensa?.let { repo.dispensaFile(topicId, it) }

    val safeTitle = remember(topic) {
        (topic?.title ?: "dispensa")
            .replace(Regex("[^A-Za-z0-9_\\-]+"), "_")
            .take(40)
            .trim('_')
            .ifEmpty { "dispensa" }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loaded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { file?.let { shareHtmlFile(ctx, it, safeTitle) } },
                        enabled = file != null && loaded,
                    ) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share)) }
                    IconButton(
                        onClick = { webViewRef?.let { printAsPdf(ctx, it, safeTitle) } },
                        enabled = webViewRef != null && loaded,
                    ) { Icon(Icons.Filled.PictureAsPdf, contentDescription = stringResource(R.string.save_pdf)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                dispensa == null -> CenterText("Dispensa non trovata.")
                dispensa.status == DispensaStatus.GENERATING ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                dispensa.status == DispensaStatus.FAILED ->
                    CenterText(dispensa.error ?: stringResource(R.string.error_generic))
                file == null || !file.exists() -> CenterText(stringResource(R.string.error_generic))
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    loaded = true
                                }
                            }
                            loadDataWithBaseURL(
                                "https://cdn.jsdelivr.net/",
                                file.readText(),
                                "text/html",
                                "utf-8",
                                null,
                            )
                            webViewRef = this
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(24.dp))
    }
}

private fun printAsPdf(context: android.content.Context, webView: WebView, title: String) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
    val jobName = "Dispensa_$title"
    val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        webView.createPrintDocumentAdapter(jobName)
    } else {
        @Suppress("DEPRECATION") webView.createPrintDocumentAdapter()
    }
    val attrs = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    printManager.print(jobName, adapter, attrs)
}

private fun shareHtmlFile(context: android.content.Context, file: File, title: String) {
    val target = File(context.cacheDir, "Dispensa_$title.html").apply {
        writeText(file.readText())
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "Condividi dispensa"))
}
