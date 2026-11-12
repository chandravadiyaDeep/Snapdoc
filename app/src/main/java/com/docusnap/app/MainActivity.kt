package com.docusnap.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
            status = result.fold({ "Merged PDF saved: ${it.name}" }, { "Merge failed: ${it.message}" })
            busy = false
        }
    }
    val pdfPickerForSplit = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { splitPdf(context, uri) }
            status = result.fold({ "Split ${it} page(s) into separate PDFs in Snapdoc storage." }, { "Split failed: ${it.message}" })
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
                        val result = withContext(Dispatchers.IO) { createPdfFromImages(context, scans) }
                        status = result.fold({ "PDF created: ${it.name}" }, { "PDF failed: ${it.message}" })
                        busy = false
                    }
                },
                onMerge = { pdfPickerForMerge.launch(arrayOf("application/pdf")) },
                onSplit = { pdfPickerForSplit.launch(arrayOf("application/pdf")) },
                onClear = { scans.forEach { it.delete() }; scans = emptyList(); status = "Current scan session cleared." }
            )
            1 -> FilesScreen(context.filesDir, Modifier.padding(padding))
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
private fun FilesScreen(root: File, modifier: Modifier = Modifier) {
    // Show generated PDFs and captured images from the app-private documents folder.
    val files = remember(root) { root.walkTopDown().filter { it.isFile && (it.extension.equals("pdf", true) || it.extension.equals("jpg", true)) }.toList() }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your files", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Saved privately inside Snapdoc.", color = Muted)
        if (files.isEmpty()) Text("No files yet. Scan a page or create a PDF.", color = Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.absolutePath }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (file.extension == "pdf") Icons.Outlined.PictureAsPdf else Icons.Outlined.Image, null, tint = Accent)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(file.name, fontWeight = FontWeight.Medium)
                            Text("${file.length() / 1024} KB", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
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

private fun createPdfFromImages(context: android.content.Context, images: List<File>): Result<File> = runCatching {
    val dir = File(context.filesDir, "documents").apply { mkdirs() }
    val output = File(dir, "Snapdoc_${System.currentTimeMillis()}.pdf")
    val pdf = PdfDocument()
    try {
        images.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: error("Cannot read ${file.name}")
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val scale = minOf(555f / bitmap.width, 802f / bitmap.height)
            val width = (bitmap.width * scale).toInt()
            val height = (bitmap.height * scale).toInt()
            val left = (595 - width) / 2f
            val top = (842 - height) / 2f
            page.canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + width, top + height), Paint(Paint.ANTI_ALIAS_FLAG))
            pdf.finishPage(page)
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
