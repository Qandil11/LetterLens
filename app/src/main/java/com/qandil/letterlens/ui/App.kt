package com.qandil.letterlens.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.qandil.letterlens.BuildConfig
import com.qandil.letterlens.data.ExplainReq
import com.qandil.letterlens.data.ExplainRes
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

private enum class Page { Scan, Result }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    // --- HTTP client ---
    val http = remember {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
    // Point to your server
    val serverUrl = remember { BuildConfig.LETTER_LENS_API }

    // --- Camera permission ---
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val askCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPermission = granted }

    LaunchedEffect(Unit) { if (!hasCamPermission) askCamera.launch(Manifest.permission.CAMERA) }

    // --- App state ---
    var page by remember { mutableStateOf(Page.Scan) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var ocr by remember { mutableStateOf<String?>(null) }
    var res by remember { mutableStateOf<ExplainRes?>(null) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(title = { Text("Letter Lens UK") })
                // Tabs show which page you’re on
                TabRow(selectedTabIndex = if (page == Page.Scan) 0 else 1) {
                    Tab(selected = page == Page.Scan, onClick = { page = Page.Scan }, text = { Text("Scan") })
                    Tab(selected = page == Page.Result, onClick = { page = Page.Result }, text = { Text("Result") })
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { paddings ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {

            // CAMERA / PREVIEW (only on Scan page & not while explaining)
            AnimatedVisibility(visible = page == Page.Scan && hasCamPermission && !loading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    CameraPreview(imageCapture, lifecycle)

                    // Scrim for legibility
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.6f to Color(0x66000000),
                                    1f to Color(0x99000000)
                                )
                            )
                    )
                }
            }

            // Permission prompt
            if (!hasCamPermission) {
                Spacer(Modifier.height(16.dp))
                Text("Camera permission is required to scan a letter.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { askCamera.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant camera permission")
                }
            }

            // CONTENT AREA (scrollable)
            Spacer(Modifier.height(12.dp))
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp)
            ) {
                // ⬇️ OCR section — now ONLY on Scan tab
                if (page == Page.Scan && ocr != null) {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("OCR (redacted)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Text(redactPii(ocr!!))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Result page content
                if (page == Page.Result) {
                    res?.let { r ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Type:", fontWeight = FontWeight.SemiBold)
                                    Text(r.type)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Deadline:", fontWeight = FontWeight.SemiBold)
                                    Text(r.deadline ?: "—")
                                }
                                Divider()
                                Text("Summary", style = MaterialTheme.typography.titleMedium)
                                Text(r.summary)
                                Divider()
                                Text("Actions", style = MaterialTheme.typography.titleMedium)
                                r.actions.forEach { a -> Text("• $a") }
                                Divider()
                                Text("Citations", style = MaterialTheme.typography.titleMedium)
                                r.citations.forEach { c -> Text(c) }
                            }
                        }
                    } ?: Text("No result yet. Scan and tap Explain.")
                }
            }

            // ACTION BAR
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        captureAndOcr(
                            context,
                            imageCapture,
                            onText = { text -> ocr = text },
                            onError = { msg -> scope.launch { snack.showSnackbar(msg, withDismissAction = true) } }
                        )
                    },
                    enabled = page == Page.Scan && hasCamPermission && !loading
                ) { Text("Scan") }

                Button(
                    onClick = {
                        if (ocr.isNullOrBlank()) {
                            scope.launch { snack.showSnackbar("Scan a letter first", withDismissAction = true) }
                            return@Button
                        }
                        scope.launch {
                            loading = true
                            val redacted = redactPii(ocr!!)
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    http.post("$serverUrl/explain") {
                                        contentType(ContentType.Application.Json)
                                        setBody(ExplainReq(redacted, hint = null))
                                    }.body<ExplainRes>()
                                }
                            }
                            result.onSuccess { reply ->
                                res = reply
                                page = Page.Result           // jump to Result automatically
                            }.onFailure { e ->
                                snack.showSnackbar(
                                    "Explain failed: ${e.message ?: "unknown error"}",
                                    withDismissAction = true
                                )
                            }
                            loading = false
                        }
                    },
                    enabled = !ocr.isNullOrBlank() && !loading
                ) { Text(if (loading) "Explaining…" else "Explain") }

                Spacer(Modifier.weight(1f))

                // Optional: Clear button
                TextButton(onClick = { ocr = null; res = null; page = Page.Scan }) {
                    Text("Clear")
                }
            }
        }
    }
}


@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lifecycle: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val view = PreviewView(ctx).apply {
                // show as large as possible without stretching faces
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3) // keep 4:3
                    .build()
                    .also { it.setSurfaceProvider(view.surfaceProvider) }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycle,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }, ContextCompat.getMainExecutor(ctx))
            view
        }
    )
}


private fun captureAndOcr(
    context: Context,
    imageCapture: ImageCapture,
    onText: (String) -> Unit,
    onError: (String) -> Unit = {}
) {
    val photo = File(context.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(photo).build()
    imageCapture.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                onError("Capture failed: ${exc.message}")
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val img = InputImage.fromFilePath(context, Uri.fromFile(photo))
                    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    rec.process(img)
                        .addOnSuccessListener { onText(it.text) }
                        .addOnFailureListener { e -> onError("OCR failed: ${e.message}") }
                } catch (t: Throwable) {
                    onError("Creating InputImage failed: ${t.message}")
                }
            }
        }
    )
}

private fun redactPii(s: String): String {
    val postcodes = Regex("\\b[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}\\b", RegexOption.IGNORE_CASE)
    val ni = Regex("\\b[ABCEGHJ-PRSTW-Z]{2}\\d{6}[A-D]\\b", RegexOption.IGNORE_CASE)
    return s.replace(postcodes, "⟦POSTCODE⟧").replace(ni, "⟦NI⟧")
}
