package com.docusnap.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Bitmap.Config
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF101827)
private val Panel = Color(0xFF192538)
private val Accent = Color(0xFF8BC5FF)
private val Muted = Color(0xFF9BAAC0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize PDFBox once before any local PDF operation.
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Accent,
                onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
            )) { SnapdocApp() }
        }
    }
}

@Composable
private fun SnapdocApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var scans by remember { mutableStateOf(emptyList<File>()) }
    var status by remember { mutableStateOf("Everything is processed locally on this device.") }
    var busy by remember { mutableStateOf(false) }
    // The selected rendering preset is applied when creating a PDF.
    var scanFilter by remember { mutableStateOf("Original") }
    // Increment after file mutations so the Files screen reloads its real directory contents.
    var filesRefresh by remember { mutableIntStateOf(0) }
    var pendingExport by remember { mutableStateOf<File?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val source = pendingExport
        val destination = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && destination != null && source != null) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output -> source.inputStream().use { input -> input.copyTo(output) } }
                    ?: error("Could not create destination file")
            }.onSuccess { Toast.makeText(context, "Exported ${source.name}", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
        pendingExport = null
    }

    // TakePicture writes the full-resolution camera result to an app-owned URI.
    var pendingCapture by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCapture
        if (saved && file != null && file.exists() && file.length() > 0L) {
            scans = scans + file
            status = "Page captured. Add another page or create a PDF."
        } else {
            file?.delete()
            status = "Capture cancelled."
        }
        pendingCapture = null
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        scope.launch {
            busy = true
            val imported = withContext(Dispatchers.IO) { uris.mapNotNull { copyUriToScans(context, it) } }
            scans = scans + imported
            status = "${imported.size} image(s) added."
            busy = false
        }
    }
    val pdfPickerForMerge = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch {
            if (uris.size < 2) { status = "Select at least two PDF files to merge."; return@launch }
            busy = true
            val result = withContext(Dispatchers.IO) { mergePdfs(context, uris) }
            status = result.fold({ "Merged PDF ready: ${it.name}" }, { "Merge failed: ${it.message}" })
            if (result.isSuccess) { filesRefresh++; selectedTab = 1 }
            busy = false
        }
    }
    val pdfPickerForCompress = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { compressPdf(context, uri) }
            status = result.fold({ "Optimized PDF ready: ${it.name}" }, { "Optimization failed: ${it.message}" })
            if (result.isSuccess) { filesRefresh++; selectedTab = 1 }
            busy = false
        }
    }
    val pdfPickerForSplit = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { splitPdf(context, uri) }
            status = result.fold({ "Split ${it} page(s) into separate PDFs." }, { "Split failed: ${it.message}" })
            if (result.isSuccess) { filesRefresh++; selectedTab = 1 }
            busy = false
        }
    }

    fun startCamera() {
        val file = File(context.filesDir, "scans/page_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        pendingCapture = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        cameraLauncher.launch(uri)
    }

    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Panel) {
            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Home") }, label = { Text("Home") })
            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = "Files") }, label = { Text("Files") })
            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
        }
    }) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(padding),
                pageCount = scans.size,
                status = status,
                busy = busy,
                onScan = { startCamera() },
                onImportImages = { imagePicker.launch("image/*") },
                onCreatePdf = {
                    if (scans.isEmpty()) status = "Capture or import images first."
                    else scope.launch {
                        busy = true
                        val result = withContext(Dispatchers.IO) { createPdfFromImages(context, scans, scanFilter) }
                        status = result.fold({ "PDF ready: ${it.name}" }, { "PDF failed: ${it.message}" })
                        if (result.isSuccess) { filesRefresh++; selectedTab = 1; scans = emptyList() }
                        busy = false
                    }
                },
                onMerge = { pdfPickerForMerge.launch(arrayOf("application/pdf")) },
                onSplit = { pdfPickerForSplit.launch(arrayOf("application/pdf")) },
                onCompress = { pdfPickerForCompress.launch(arrayOf("application/pdf")) },
                scanFilter = scanFilter,
                onFilterChange = { scanFilter = it },
                onClear = { scans.forEach { it.delete() }; scans = emptyList(); status = "Current scan session cleared." }
            )
            1 -> FilesScreen(
                root = context.filesDir,
                refreshKey = filesRefresh,
                modifier = Modifier.padding(padding),
                onOpen = { file -> openLocalFile(context, file) },
                onShare = { file -> shareLocalFile(context, file) },
                onExport = { file ->
                    pendingExport = file
                    val mime = when (file.extension.lowercase(Locale.ROOT)) { "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg" }
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = mime
                        putExtra(Intent.EXTRA_TITLE, file.name)
                    }
                    exportLauncher.launch(intent)
                },
                onDelete = { file ->
                    if (file.delete()) { filesRefresh++; Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show() }
                    else Toast.makeText(context, "Could not delete ${file.name}", Toast.LENGTH_SHORT).show()
                }
            )
            else -> SettingsScreen(status, Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    pageCount: Int,
    status: String,
    busy: Boolean,
    onScan: () -> Unit,
    onImportImages: () -> Unit,
    onCreatePdf: () -> Unit,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onCompress: () -> Unit,
    scanFilter: String,
    onFilterChange: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("SNAPDOC", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Documents,\nmade simple.", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
            Text("Scan and manage files privately on your device.", color = Muted, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF233A55)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(Accent.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Accent)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("OFFLINE • ON DEVICE", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Turn paper into\nbeautiful PDFs", fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Text("Capture full-resolution pages with your camera or import images.", color = Muted, fontSize = 13.sp)
                    Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                        Text("  Scan a page")
                    }
                    OutlinedButton(onClick = onImportImages, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Text("  Import images")
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scan session", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("$pageCount page(s)", color = Accent, fontSize = 13.sp)
            }
            if (pageCount > 0) {
                Text("PDF appearance", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Original", "Grayscale", "B&W").forEach { preset ->
                        FilterChip(selected = scanFilter == preset, onClick = { onFilterChange(preset) }, label = { Text(preset) }, enabled = !busy)
                    }
                }
                Button(onClick = onCreatePdf, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                    Text("  Create PDF from pages")
                }
                TextButton(onClick = onClear) { Icon(Icons.Outlined.Delete, contentDescription = null); Text(" Clear session") }
            } else {
                Text("Captured or imported pages will appear here.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            Text("PDF tools", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile("Merge PDFs", Icons.Outlined.Merge, Modifier.weight(1f), onMerge)
                ToolTile("Split PDF", Icons.Outlined.PictureAsPdf, Modifier.weight(1f), onSplit)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile("Optimize PDF", Icons.Outlined.PictureAsPdf, Modifier.weight(1f), onCompress)
                ToolTile("Import pages", Icons.Outlined.Image, Modifier.weight(1f), onImportImages)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(status, color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToolTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(104.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FilesScreen(
    root: File,
    refreshKey: Int,
    modifier: Modifier = Modifier,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onExport: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    // Reload from disk whenever an operation creates or deletes a file.
    val files = remember(root, refreshKey) {
        root.walkTopDown().filter { file ->
            file.isFile && file.extension.lowercase(Locale.ROOT) in setOf("pdf", "jpg", "jpeg", "png")
        }.sortedByDescending { it.lastModified() }.toList()
    }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your files", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Open, share, export or delete files stored on this device.", color = Muted)
        if (files.isEmpty()) Text("No files yet. Scan a page or create a PDF.", color = Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.absolutePath }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (file.extension.equals("pdf", true)) Icons.Outlined.PictureAsPdf else Icons.Outlined.Image, null, tint = Accent)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(file.name, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("${file.length() / 1024} KB • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}", color = Muted, fontSize = 11.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onOpen(file) }) { Icon(Icons.Outlined.OpenInNew, null); Text(" Open") }
                            TextButton(onClick = { onShare(file) }) { Icon(Icons.Outlined.Share, null); Text(" Share") }
                            TextButton(onClick = { onExport(file) }) { Icon(Icons.Outlined.Download, null); Text(" Export") }
                            IconButton(onClick = { onDelete(file) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete ${file.name}", tint = Color(0xFFFF8A8A)) }
                        }
                    }
                }
            }
        }
    }
}

private fun openLocalFile(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val mime = when (file.extension.lowercase(Locale.ROOT)) { "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg" }
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }.onFailure { Toast.makeText(context, "No app can open this file: ${it.message}", Toast.LENGTH_LONG).show() }
}

private fun shareLocalFile(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val mime = when (file.extension.lowercase(Locale.ROOT)) { "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }.onFailure { Toast.makeText(context, "Could not share file: ${it.message}", Toast.LENGTH_LONG).show() }
}

@Composable
private fun SettingsScreen(status: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Privacy-first processing", color = Accent, fontWeight = FontWeight.SemiBold)
        Text("Snapdoc processes scans and PDF files on this device. It does not require an account or upload your documents.", color = Muted)
        Text("Camera capture uses the Android camera app. Imported files are selected by you through Android's document picker.", color = Muted)
        Text(status, color = Muted, fontSize = 12.sp)
    }
}

private fun copyUriToScans(context: android.content.Context, uri: Uri): File? = try {
    val dir = File(context.filesDir, "scans").apply { mkdirs() }
    val target = File(dir, "import_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
    target.takeIf { it.exists() && it.length() > 0 }
} catch (_: Exception) { null }

private fun createPdfFromImages(context: android.content.Context, images: List<File>, filter: String): Result<File> = runCatching {
    val dir = File(context.filesDir, "documents").apply { mkdirs() }
    val output = File(dir, "Snapdoc_${System.currentTimeMillis()}.pdf")
    val pdf = PdfDocument()
    try {
        images.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: error("Cannot read ${file.name}")
            val processed = applyScanFilter(bitmap, filter)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val scale = minOf(555f / processed.width, 802f / processed.height)
            val width = (processed.width * scale).toInt()
            val height = (processed.height * scale).toInt()
            val left = (595 - width) / 2f
            val top = (842 - height) / 2f
            page.canvas.drawBitmap(processed, null, android.graphics.RectF(left, top, left + width, top + height), Paint(Paint.ANTI_ALIAS_FLAG))
            pdf.finishPage(page)
            if (processed !== bitmap) processed.recycle()
            bitmap.recycle()
        }
        FileOutputStream(output).use { pdf.writeTo(it) }
    } finally { pdf.close() }
    output
}

private fun copyUriToCache(context: android.content.Context, uri: Uri): File {
    val file = File(context.cacheDir, "input_${System.currentTimeMillis()}_${(0..9999).random()}.pdf")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use(input::copyTo) }
        ?: error("Unable to open selected PDF")
    return file
}

private fun mergePdfs(context: android.content.Context, uris: List<Uri>): Result<File> = runCatching {
    val sources = uris.map { copyUriToCache(context, it) }
    val outputDir = File(context.filesDir, "documents").apply { mkdirs() }
    val output = File(outputDir, "Merged_${System.currentTimeMillis()}.pdf")
    val merger = PDFMergerUtility()
    sources.forEach { merger.addSource(it) }
    merger.destinationFileName = output.absolutePath
    merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    sources.forEach { it.delete() }
    output
}

private fun splitPdf(context: android.content.Context, uri: Uri): Result<Int> = runCatching {
    val source = copyUriToCache(context, uri)
    val document = PDDocument.load(source)
    try {
        val parts = Splitter().split(document)
        val outputDir = File(context.filesDir, "documents/split_${System.currentTimeMillis()}").apply { mkdirs() }
        parts.forEachIndexed { index, part ->
            part.use { it.save(File(outputDir, "page_${index + 1}.pdf")) }
        }
        parts.size
    } finally {
        document.close()
        source.delete()
    }
}

// Apply a predictable offline scan preset without changing the source image files.
private fun applyScanFilter(source: Bitmap, filter: String): Bitmap {
    if (filter == "Original") return source
    val output = source.copy(Config.ARGB_8888, true)
    val pixels = IntArray(output.width * output.height)
    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    for (index in pixels.indices) {
        val color = pixels[index]
        val gray = (android.graphics.Color.red(color) * 0.299 +
            android.graphics.Color.green(color) * 0.587 +
            android.graphics.Color.blue(color) * 0.114).toInt()
        val value = if (filter == "B&W") { if (gray >= 170) 255 else 0 } else gray
        pixels[index] = android.graphics.Color.rgb(value, value, value)
    }
    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    return output
}

// Re-render PDF pages as JPEG-backed pages to reduce scanned-document size.
// All processing happens in app-private cache/storage; the input remains untouched.
private fun compressPdf(context: android.content.Context, uri: Uri): Result<File> = runCatching {
    val input = copyUriToCache(context, uri)
    val outputDir = File(context.filesDir, "documents").apply { mkdirs() }
    val output = File(outputDir, "Optimized_${System.currentTimeMillis()}.pdf")
    val descriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    val pdf = PdfDocument()
    try {
        for (pageIndex in 0 until renderer.pageCount) {
            val sourcePage = renderer.openPage(pageIndex)
            try {
                val maxDimension = 1600
                val ratio = minOf(1f, maxDimension.toFloat() / maxOf(sourcePage.width, sourcePage.height))
                val width = (sourcePage.width * ratio).toInt().coerceAtLeast(1)
                val height = (sourcePage.height * ratio).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
                sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val scale = minOf(555f / width, 802f / height)
                val drawWidth = width * scale
                val drawHeight = height * scale
                val left = (595f - drawWidth) / 2f
                val top = (842f - drawHeight) / 2f
                page.canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + drawWidth, top + drawHeight), Paint(Paint.ANTI_ALIAS_FLAG))
                pdf.finishPage(page)
                bitmap.recycle()
            } finally { sourcePage.close() }
        }
        FileOutputStream(output).use { pdf.writeTo(it) }
    } finally {
        pdf.close()
        renderer.close()
        descriptor.close()
        input.delete()
    }
    output
}
