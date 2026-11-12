package com.docusnap.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.foundation.text.selection.SelectionContainer

private val EditorAccent = androidx.compose.ui.graphics.Color(0xFF8BC5FF)
private val EditorPanel = androidx.compose.ui.graphics.Color(0xFF192538)
private val EditorMuted = androidx.compose.ui.graphics.Color(0xFF9BAAC0)

@Composable
fun EditorScreen(onBack: () -> Unit, onFilesChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("dashboard") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Choose a real file to begin editing.") }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedPage by remember { mutableIntStateOf(0) }
    var overlayText by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageName by remember { mutableStateOf("edited_image") }
    var ocrResult by remember { mutableStateOf("") }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { openPdfForEditing(context, uri) }
            result.onSuccess { pair ->
                pdfFile = pair.first
                pdfPages = pair.second
                pageOrder = pair.second.indices.toList()
                selectedPage = 0
                mode = "pdf"
                message = "PDF loaded. Reorder, remove, rotate pages or add a text overlay."
            }.onFailure { message = "Could not open PDF: " + it.message }
            busy = false
        }
    }
    val addPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val current = pdfFile
        if (uri != null && current != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { appendPdfPages(context, current, uri) }
            result.onSuccess { pair ->
                pdfFile = pair.first
                pdfPages = pair.second
                pageOrder = pair.second.indices.toList()
                message = "Pages added from the selected PDF."
            }.onFailure { message = "Could not add pages: " + it.message }
            busy = false
        }
    }
    val ocrPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { extractTextFromDocument(context, uri) }
            result.onSuccess { text ->
                ocrResult = text
                mode = "ocr"
                message = if (text.isBlank()) "No readable text was found. Try a sharper, brighter scan." else "Text extracted on this device."
            }.onFailure { message = "Text extraction failed: " + it.message }
            busy = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { loadImage(context, uri) }
            result.onSuccess { bitmap ->
                imageBitmap = bitmap
                imageName = "Snapdoc_image_" + System.currentTimeMillis()
                mode = "image"
                message = "Image loaded. Rotate, crop to square, or convert to grayscale."
            }.onFailure { message = "Could not open image: " + it.message }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (mode == "dashboard") onBack() else { mode = "dashboard"; message = "Choose a real file to begin editing." } }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(if (mode == "dashboard") "Editor" else if (mode == "pdf") "PDF Editor" else if (mode == "image") "Image Editor" else "OCR Text Extractor", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Offline editing on this device", color = EditorMuted, fontSize = 12.sp)
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (mode) {
            "dashboard" -> {
                Text("Choose an editor", color = EditorMuted)
                Card(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, colors = CardDefaults.cardColors(containerColor = EditorPanel), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PictureAsPdf, null, tint = EditorAccent, modifier = Modifier.size(34.dp))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("PDF Editor", fontWeight = FontWeight.SemiBold)
                            Text("Preview, reorder, remove, add pages and add text overlays", color = EditorMuted, fontSize = 12.sp)
                        }
                    }
                }
                Card(onClick = { ocrPicker.launch(arrayOf("image/*", "application/pdf")) }, colors = CardDefaults.cardColors(containerColor = EditorPanel), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PictureAsPdf, null, tint = EditorAccent, modifier = Modifier.size(34.dp))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("OCR Text Extractor", fontWeight = FontWeight.SemiBold)
                            Text("Extract selectable text from images and multi-page PDFs offline", color = EditorMuted, fontSize = 12.sp)
                        }
                    }
                }
                Card(onClick = { imagePicker.launch(arrayOf("image/*")) }, colors = CardDefaults.cardColors(containerColor = EditorPanel), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Image, null, tint = EditorAccent, modifier = Modifier.size(34.dp))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Image Editor", fontWeight = FontWeight.SemiBold)
                            Text("Preview, rotate, center-crop to square and grayscale", color = EditorMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            "pdf" -> {
                val file = pdfFile
                Text((pageOrder.size).toString() + " page(s) • tap a page to select it", color = EditorMuted, fontSize = 12.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(pageOrder, key = { _, index -> index }) { displayIndex, originalIndex ->
                        val bitmap = pdfPages.getOrNull(originalIndex)
                        if (bitmap != null) Card(
                            onClick = { selectedPage = displayIndex },
                            colors = CardDefaults.cardColors(containerColor = if (selectedPage == displayIndex) EditorPanel else EditorPanel.copy(alpha = 0.55f)),
                            border = if (selectedPage == displayIndex) androidx.compose.foundation.BorderStroke(2.dp, EditorAccent) else null
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text("Page " + (displayIndex + 1), color = EditorAccent, fontWeight = FontWeight.SemiBold)
                                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 260.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        if (displayIndex > 0) {
                                            val list = pageOrder.toMutableList()
                                            val item = list.removeAt(displayIndex)
                                            list.add(displayIndex - 1, item)
                                            pageOrder = list
                                            selectedPage = displayIndex - 1
                                        }
                                    }, enabled = displayIndex > 0) { Icon(Icons.Outlined.ArrowUpward, "Move page up") }
                                    IconButton(onClick = {
                                        if (displayIndex < pageOrder.lastIndex) {
                                            val list = pageOrder.toMutableList()
                                            val item = list.removeAt(displayIndex)
                                            list.add(displayIndex + 1, item)
                                            pageOrder = list
                                            selectedPage = displayIndex + 1
                                        }
                                    }, enabled = displayIndex < pageOrder.lastIndex) { Icon(Icons.Outlined.ArrowDownward, "Move page down") }
                                    IconButton(onClick = {
                                        pageOrder = pageOrder.filterIndexed { index, _ -> index != displayIndex }
                                        selectedPage = (selectedPage - 1).coerceAtLeast(0)
                                    }) { Icon(Icons.Outlined.Delete, "Remove page") }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(overlayText, { overlayText = it }, Modifier.fillMaxWidth(), label = { Text("Text to add on selected page") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { addPdfPicker.launch(arrayOf("application/pdf")) }, enabled = !busy) { Text("Add PDF pages") }
                    OutlinedButton(onClick = {
                        val f = file
                        if (f != null && pageOrder.isNotEmpty()) scope.launch {
                            busy = true
                            val result = withContext(Dispatchers.IO) { rotatePdfPage(context, f, pageOrder[selectedPage]) }
                            result.onSuccess { pdfFile = it; pdfPages = renderPdfThumbnails(it); pageOrder = pdfPages.indices.toList(); message = "Selected page rotated." }
                                .onFailure { message = "Rotation failed: " + it.message }
                            busy = false
                        }
                    }, enabled = !busy && pageOrder.isNotEmpty()) { Text("Rotate 90°") }
                }
                Button(onClick = {
                    val f = file
                    if (f != null) scope.launch {
                        busy = true
                        val result = withContext(Dispatchers.IO) { savePdfEdits(context, f, pageOrder, selectedPage, overlayText) }
                        result.onSuccess { pdfFile = it; pdfPages = renderPdfThumbnails(it); pageOrder = pdfPages.indices.toList(); overlayText = ""; message = "Edited PDF saved as " + it.name; onFilesChanged() }
                            .onFailure { message = "Save failed: " + it.message }
                        busy = false
                    }
                }, Modifier.fillMaxWidth(), enabled = !busy && pageOrder.isNotEmpty()) { Text("Save edited PDF") }
            }
            "ocr" -> {
                Text("Recognized text", color = EditorMuted, fontSize = 13.sp)
                if (ocrResult.isBlank()) {
                    Text("No text was detected in the selected document.", color = EditorMuted)
                } else {
                    SelectionContainer {
                        Text(ocrResult, modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(onClick = {
                        val result = saveOcrText(context, ocrResult)
                        result.onSuccess { message = "Text saved as " + it.name; onFilesChanged() }
                            .onFailure { message = "Could not save text: " + it.message }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("Save extracted text") }
                }
                OutlinedButton(onClick = { ocrPicker.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Text("Choose another image or PDF")
                }
            }
            "image" -> {
                val bitmap = imageBitmap
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imageBitmap = rotateBitmap(bitmap) }, enabled = !busy) { Text("Rotate 90°") }
                        OutlinedButton(onClick = { imageBitmap = centerCropSquare(bitmap) }, enabled = !busy) { Text("Square crop") }
                        OutlinedButton(onClick = { imageBitmap = grayscaleBitmap(bitmap) }, enabled = !busy) { Text("Grayscale") }
                    }
                    Button(onClick = {
                        scope.launch {
                            busy = true
                            val result = withContext(Dispatchers.IO) { saveEditedImage(context, bitmap, imageName) }
                            result.onSuccess { message = "Edited image saved as " + it.name; onFilesChanged() }
                                .onFailure { message = "Image save failed: " + it.message }
                            busy = false
                        }
                    }, Modifier.fillMaxWidth(), enabled = !busy) { Text("Save edited image") }
                }
            }
        }
        Text(message, color = EditorMuted, fontSize = 12.sp)
    }
}


/**
 * Extracts text from a selected image or every page of a PDF.
 * ML Kit's bundled Latin recognizer runs on-device; no document is uploaded.
 */
private fun extractTextFromDocument(context: Context, uri: Uri): Result<String> = runCatching {
    val mime = context.contentResolver.getType(uri).orEmpty()
    val isPdf = mime.equals("application/pdf", ignoreCase = true) ||
        uri.toString().substringBefore('?').endsWith(".pdf", ignoreCase = true)
    val source = copyEditorUri(context, uri, if (isPdf) ".pdf" else ".img")
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        if (isPdf) {
            val descriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            try {
                val pages = (0 until renderer.pageCount).map { index ->
                    val page = renderer.openPage(index)
                    try {
                        val scale = minOf(1f, 1800f / maxOf(page.width, page.height))
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        try {
                            val recognized = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
                            "----- Page ${index + 1} -----\n" + recognized.text
                        } finally { bitmap.recycle() }
                    } finally { page.close() }
                }
                pages.joinToString("\n\n")
            } finally { renderer.close(); descriptor.close() }
        } else {
            val bitmap = BitmapFactory.decodeFile(source.absolutePath) ?: error("Unsupported or unreadable image.")
            try {
                val scale = minOf(1f, 2400f / maxOf(bitmap.width, bitmap.height))
                val input = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
                try { Tasks.await(recognizer.process(InputImage.fromBitmap(input, 0))).text }
                finally { if (input !== bitmap) input.recycle() }
            } finally { bitmap.recycle() }
        }
    } finally { recognizer.close(); source.delete() }
}

private fun saveOcrText(context: Context, text: String): Result<File> = runCatching {
    val directory = File(context.filesDir, "documents").apply { mkdirs() }
    val output = File(directory, "OCR_Text_" + System.currentTimeMillis() + ".txt")
    output.writeText(text, Charsets.UTF_8)
    output
}

private fun openPdfForEditing(context: Context, uri: Uri): Result<Pair<File, List<Bitmap>>> = runCatching {
    val file = copyEditorUri(context, uri, ".pdf")
    file to renderPdfThumbnails(file)
}

private fun appendPdfPages(context: Context, current: File, uri: Uri): Result<Pair<File, List<Bitmap>>> = runCatching {
    val extra = copyEditorUri(context, uri, ".pdf")
    val original = PDDocument.load(current)
    val addition = PDDocument.load(extra)
    val out = File(File(context.filesDir, "documents").apply { mkdirs() }, "Editor_added_" + System.currentTimeMillis() + ".pdf")
    try {
        for (page in addition.pages) original.importPage(page)
        original.save(out)
    } finally { original.close(); addition.close(); extra.delete() }
    out to renderPdfThumbnails(out)
}

private fun renderPdfThumbnails(file: File): List<Bitmap> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    try {
        return (0 until renderer.pageCount).map { index ->
            val page = renderer.openPage(index)
            try {
                val ratio = minOf(1f, 900f / maxOf(page.width, page.height))
                val bitmap = Bitmap.createBitmap((page.width * ratio).toInt().coerceAtLeast(1), (page.height * ratio).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally { page.close() }
        }
    } finally { renderer.close(); descriptor.close() }
}

private fun savePdfEdits(context: Context, source: File, order: List<Int>, selectedDisplayPage: Int, text: String): Result<File> = runCatching {
    require(order.isNotEmpty()) { "Keep at least one page before saving." }
    val input = PDDocument.load(source)
    val outputDoc = PDDocument()
    try {
        order.forEach { outputDoc.importPage(input.getPage(it)) }
        if (text.isNotBlank()) {
            val page = outputDoc.getPage(selectedDisplayPage.coerceIn(0, outputDoc.numberOfPages - 1))
            PDPageContentStream(outputDoc, page, AppendMode.APPEND, true, true).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 16f)
                stream.newLineAtOffset(48f, 48f)
                stream.showText(text.take(180).replace(Regex("[^\\x20-\\x7E]"), "?"))
                stream.endText()
            }
        }
        val out = File(File(context.filesDir, "documents").apply { mkdirs() }, "Edited_" + System.currentTimeMillis() + ".pdf")
        outputDoc.save(out)
        out
    } finally { input.close(); outputDoc.close() }
}

private fun rotatePdfPage(context: Context, source: File, originalIndex: Int): Result<File> = runCatching {
    val doc = PDDocument.load(source)
    try {
        val page = doc.getPage(originalIndex)
        page.rotation = (page.rotation + 90) % 360
        val out = File(File(context.filesDir, "documents").apply { mkdirs() }, "Rotated_" + System.currentTimeMillis() + ".pdf")
        doc.save(out)
        out
    } finally { doc.close() }
}

private fun copyEditorUri(context: Context, uri: Uri, suffix: String): File {
    val target = File(context.cacheDir, "editor_" + System.currentTimeMillis() + suffix)
    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: error("Cannot read selected file")
    require(target.length() > 0) { "Selected file is empty." }
    return target
}

private fun loadImage(context: Context, uri: Uri): Result<Bitmap> = runCatching {
    val input = context.contentResolver.openInputStream(uri) ?: error("Cannot read image")
    val bitmap = input.use { BitmapFactory.decodeStream(it) } ?: error("Unsupported image format")
    val maxEdge = 2400
    val ratio = minOf(1f, maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height))
    if (ratio == 1f) bitmap else Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true).also { bitmap.recycle() }
}

private fun rotateBitmap(source: Bitmap): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also { if (it !== source) source.recycle() }
}

private fun centerCropSquare(source: Bitmap): Bitmap {
    val edge = minOf(source.width, source.height)
    val x = (source.width - edge) / 2
    val y = (source.height - edge) / 2
    return Bitmap.createBitmap(source, x, y, edge, edge).also { if (it !== source) source.recycle() }
}

private fun grayscaleBitmap(source: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
    canvas.drawBitmap(source, 0f, 0f, paint)
    source.recycle()
    return result
}

private fun saveEditedImage(context: Context, bitmap: Bitmap, baseName: String): Result<File> = runCatching {
    val dir = File(context.filesDir, "documents").apply { mkdirs() }
    val file = File(dir, baseName + ".jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
    file
}
