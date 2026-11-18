package com.dinnote.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun readingBackground(value: String): Color = when (value) {
    "cream" -> Color(0xFFFFF6E5)
    "green" -> Color(0xFFE3F3E8)
    "sepia" -> Color(0xFFF1E2C6)
    "dark" -> Color(0xFF20272B)
    "pink" -> Color(0xFFFFE9EF)
    "blue" -> Color(0xFFE5F3FF)
    "purple" -> Color(0xFFF0E7FF)
    "nature" -> Color(0xFFEAF3E7)
    "turquoise" -> Color(0xFFDDF4F2)
    else -> Color.White
}

@Composable
fun DisplaySettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dinnote_display", Context.MODE_PRIVATE) }
    val noteDatabase = remember { NoteDatabase(context).also { it.migrateOldSingleNote(context) } }
    var backupStatus by remember { mutableStateOf("") }
    var settingsStatus by remember { mutableStateOf("") }
    val saveDataLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupStatus = runCatching {
                NoteBackup.export(context, uri, noteDatabase.allNotes())
                "Data berjaya disimpan ke folder pilihan."
            }.getOrElse { "Gagal menyimpan data: ${it.message}" }
        }
    }
    val restoreDataLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupStatus = runCatching {
                val result = NoteBackup.import(context, uri, noteDatabase)
                "${result.imported} nota dipulihkan; ${result.skippedDuplicates} pendua dilangkau."
            }.getOrElse { "Gagal memulihkan data: ${it.message}" }
        }
    }
    var imageUri by remember { mutableStateOf(prefs.getString("background_image_uri", "") ?: "") }
    var imageOpacity by remember { mutableFloatStateOf(prefs.getFloat("background_image_opacity", 0.18f)) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri.toString()
            settingsStatus = "Gambar dipilih. Tekan Simpan Tetapan."
        }
    }
    var background by remember { mutableStateOf(prefs.getString("background", "white") ?: "white") }
    var fontSize by remember { mutableFloatStateOf(prefs.getInt("font_size", 19).toFloat()) }
    var spacing by remember { mutableStateOf(prefs.getFloat("line_spacing", 1.6f)) }
    var fullScreen by remember { mutableStateOf(prefs.getBoolean("full_screen", true)) }
    var borderShape by remember { mutableStateOf(prefs.getString("border_shape", "rounded") ?: "rounded") }
    var borderColor by remember { mutableStateOf(prefs.getString("border_color", "teal") ?: "teal") }
    var borderWidth by remember { mutableFloatStateOf(prefs.getFloat("border_width", 1f)) }
    var selectedSymbol by remember { mutableStateOf(prefs.getString("selected_symbol", "✦") ?: "✦") }
    var fontChoice by remember { mutableStateOf(prefs.getString("font", "Standard") ?: "Standard") }
    var primaryLanguage by remember { mutableStateOf(prefs.getString("primary_language", "Arab & Melayu") ?: "Arab & Melayu") }
    val referenceMadhhab = "Syafi‘i"
    var bookLayout by remember { mutableStateOf(prefs.getString("book_layout", "Arab di atas, Melayu di bawah") ?: "Arab di atas, Melayu di bawah") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFF003C35), Color(0xFF08675D), Color(0xFF00483F))))) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val gold = Color(0xFFE7C77E)
                val arch = Path().apply {
                    moveTo(0f, h * .62f)
                    cubicTo(w * .08f, h * .32f, w * .20f, h * .34f, w * .25f, h * .32f)
                    cubicTo(w * .30f, h * .12f, w * .43f, h * .16f, w * .50f, 2f)
                    cubicTo(w * .57f, h * .16f, w * .70f, h * .12f, w * .75f, h * .32f)
                    cubicTo(w * .80f, h * .34f, w * .92f, h * .32f, w, h * .62f)
                }
                drawPath(arch, Color(0xFF18A08A), style = Stroke(width = 7.dp.toPx()))
                drawPath(arch, gold, style = Stroke(width = 3.dp.toPx()))
                listOf(.11f, .89f).forEach { fraction ->
                    val x = w * fraction
                    val y = h * .48f
                    val r = 7.dp.toPx()
                    drawLine(gold, Offset(x, h * .39f), Offset(x, y), 1.dp.toPx())
                    drawCircle(gold.copy(alpha = .12f), r * 2.4f, Offset(x, y + r))
                    val lantern = Path().apply {
                        moveTo(x, y); lineTo(x + r, y + r)
                        lineTo(x + r * .65f, y + r * 2.2f)
                        lineTo(x, y + r * 2.8f)
                        lineTo(x - r * .65f, y + r * 2.2f)
                        lineTo(x - r, y + r); close()
                    }
                    drawPath(lantern, gold)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(painter = painterResource(R.drawable.ic_dinnote_header), contentDescription = null, modifier = Modifier.size(40.dp))
                Text("Tetapan DinNote", fontSize = 25.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color(0xFFFFF9E9), textAlign = TextAlign.Center)
                Text("Sesuaikan aplikasi mengikut pilihan anda", fontSize = 11.sp, color = Color(0xFFDDF4F2), textAlign = TextAlign.Center)
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD39B), contentColor = Color(0xFF164641)), onClick = {
                val saved = prefs.edit()
                    .putString("background", background)
                    .putString("background_image_uri", imageUri)
                    .putFloat("background_image_opacity", imageOpacity)
                    .putInt("font_size", fontSize.toInt())
                    .putFloat("line_spacing", spacing)
                    .putBoolean("full_screen", fullScreen)
                    .putString("border_shape", borderShape)
                    .putString("border_color", borderColor)
                    .putFloat("border_width", borderWidth)
                    .putString("selected_symbol", selectedSymbol)
                    .putString("font", fontChoice)
                    .putString("primary_language", primaryLanguage)
                    .putString("reference_madhhab", referenceMadhhab)
                    .putString("book_layout", bookLayout)
                    .commit()
                settingsStatus = if (saved) "Tetapan berjaya disimpan" else "Gagal menyimpan tetapan"
            }) { Text("✓  Simpan", fontSize = 13.sp) }
            Text("────  ❖  ────", color = Color(0xFFE7C77E), fontSize = 12.sp)
        }
        }
        if (settingsStatus.isNotBlank()) {
            Text(settingsStatus, fontSize = 13.sp, color = Color(0xFF075B59))
        }
        SettingsPanel {
        Text("Latar Halaman Utama", fontWeight = FontWeight.Bold, color = Color(0xFF075B59))
        Text("Pilih gambar latar atau gunakan corak asal.", fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
            Text("Pilih gambar")
        }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { imageUri = ""; settingsStatus = "Perubahan belum disimpan" }) {
                Text("Latar asal")
            }
        }
        if (imageUri.isNotBlank()) {
            Text("Kelegapan gambar: ${kotlin.math.round(imageOpacity * 100).toInt()}%", fontSize = 13.sp)
            Slider(value = imageOpacity, onValueChange = { imageOpacity = it; settingsStatus = "Perubahan belum disimpan" }, valueRange = 0.05f..1f)
            val bitmap = remember(imageUri) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(10.dp))) {
                    Image(bitmap = bitmap, contentDescription = "Pratonton gambar latar", contentScale = ContentScale.Crop, alpha = imageOpacity, modifier = Modifier.fillMaxSize())
                }
            } else {
                Text("Gambar tidak dapat dibaca. Sila pilih semula.", fontSize = 12.sp, color = Color.Red)
            }
        }

        }
        SettingsPanel {
        Text("Tema Bacaan", fontWeight = FontWeight.Bold, color = Color(0xFF075B59))
        listOf("white" to "Putih", "turquoise" to "Turquoise", "cream" to "Krim", "green" to "Hijau lembut", "sepia" to "Sepia", "dark" to "Gelap", "pink" to "Merah jambu", "blue" to "Biru lembut", "purple" to "Ungu", "nature" to "Alam").chunked(3).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (key, name) ->
                    Surface(
                        modifier = Modifier.weight(1f).height(44.dp).clickable {
                            background = key
                            settingsStatus = "Perubahan belum disimpan"
                        },
                        color = readingBackground(key),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(if (background == key) 2.dp else 1.dp, if (background == key) Color(0xFF087F83) else Color(0xFFB7DCD7))
                    ) {
                        Box(Modifier.padding(horizontal = 5.dp, vertical = 5.dp)) {
                            Text(name + if (background == key) " ✓" else "", fontSize = 11.sp, color = if (key == "dark") Color.White else Color.DarkGray)
                        }
                    }
                }
                repeat(3 - pair.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        }
        SettingsPanel {
        Text("Keutamaan Maktabah", fontWeight = FontWeight.Bold, color = Color(0xFF075B59), fontSize = 17.sp)
        Text("Bahasa utama", fontWeight = FontWeight.Medium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Arab & Melayu", "Arab", "Melayu").forEach { option ->
                FilterChip(selected = primaryLanguage == option, onClick = {
                    primaryLanguage = option
                    settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(option) })
            }
        }
        Text("Mazhab rujukan utama: Syafi‘i", fontWeight = FontWeight.Medium)
        Text("DinNote memfokuskan rujukan mazhab Syafi‘i.", fontSize = 12.sp, color = Color.Gray)
        Text("Susunan bacaan kitab", fontWeight = FontWeight.Medium)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(
                listOf("Arab di atas, Melayu di bawah" to "Arab di atas\nMelayu di bawah",
                    "Melayu di atas, Arab di bawah" to "Melayu di atas\nArab di bawah"),
                listOf("Arab sahaja" to "Arab sahaja", "Melayu sahaja" to "Melayu sahaja")
            ).forEach { options ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { (option, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = bookLayout == option,
                            onClick = {
                                bookLayout = option
                                settingsStatus = "Perubahan belum disimpan"
                            },
                            label = {
                                Text(label, modifier = Modifier.fillMaxWidth(),
                                    fontSize = 12.sp, lineHeight = 15.sp,
                                    textAlign = TextAlign.Center)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
        Text("Pilihan ini disimpan sebagai keutamaan Maktabah. Paparan kitab yang menyokong susunan ini akan disepadukan secara berperingkat.", fontSize = 12.sp, color = Color.Gray)
        }
        SettingsPanel {
        Text("Border item terpilih", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("rounded" to "Bucu bulat", "pill" to "Pill", "square" to "Petak", "dashed" to "Putus").forEach { (key, label) ->
                FilterChip(selected = borderShape == key, onClick = {
                    borderShape = key; settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(label) })
            }
        }
        Text("Warna border", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("teal" to "Turquoise", "blue" to "Biru", "purple" to "Ungu", "pink" to "Merah jambu", "red" to "Merah", "orange" to "Oren", "gray" to "Kelabu").forEach { (key, label) ->
                FilterChip(selected = borderColor == key, onClick = {
                    borderColor = key; settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(label) })
            }
        }
        Text("Ketebalan border: ${borderWidth.toInt()} dp", fontWeight = FontWeight.Bold)
        Slider(value = borderWidth, onValueChange = { borderWidth = it; settingsStatus = "Perubahan belum disimpan" }, valueRange = 1f..4f, steps = 2)
        Text("Ikon dan simbol pilihan", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("✦", "★", "✓", "☑", "❖", "۞", "☪", "﷽", "•", "→").forEach { symbol ->
                FilterChip(
                    selected = selectedSymbol == symbol,
                    onClick = {
                        selectedSymbol = symbol
                        settingsStatus = "Perubahan belum disimpan"
                    },
                    label = { Text(symbol, fontSize = 18.sp) }
                )
            }
        }
        }
        SettingsPanel {
        Text("Saiz Tulisan: ${fontSize.toInt()} sp", fontWeight = FontWeight.Bold)
        Slider(value = fontSize, onValueChange = {
            fontSize = it
            settingsStatus = "Perubahan belum disimpan"
        }, valueRange = 14f..30f)
        Text("Jenis Fon", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Standard", "Serif", "Arab & Rumi").forEach { option ->
                FilterChip(selected = fontChoice == option, onClick = {
                    fontChoice = option
                    settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(option, fontSize = 11.sp) })
            }
        }
        Text("Jarak Baris", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1.3f to "Rapat", 1.6f to "Sederhana", 2.0f to "Lapang").forEach { (value, label) ->
                FilterChip(selected = spacing == value, onClick = {
                    spacing = value
                    settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(label) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mod Bacaan Penuh", fontWeight = FontWeight.Medium)
            Switch(checked = fullScreen, onCheckedChange = {
                fullScreen = it
                settingsStatus = "Perubahan belum disimpan"
            })
        }
        Text("Pratonton", fontWeight = FontWeight.Bold)
        val previewShape = when (borderShape) {
            "square" -> RoundedCornerShape(0.dp)
            "pill" -> RoundedCornerShape(36.dp)
            "dashed" -> CutCornerShape(12.dp)
            else -> RoundedCornerShape(10.dp)
        }
        val previewBorderColor = when (borderColor) {
            "blue" -> Color(0xFF3478B9)
            "purple" -> Color(0xFF8855B8)
            "pink" -> Color(0xFFD46B91)
            "red" -> Color(0xFFC84A4A)
            "orange" -> Color(0xFFCE7A32)
            "gray" -> Color(0xFF8A949A)
            else -> Color(0xFF087F8C)
        }
        Surface(
            color = readingBackground(background),
            shape = previewShape,
            border = androidx.compose.foundation.BorderStroke(borderWidth.dp, previewBorderColor)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, fontSize = fontSize.sp, lineHeight = (fontSize * spacing).sp, color = if (background == "dark") Color.White else Color(0xFF075A40))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFB7DCD7))
                Text("$selectedSymbol Contoh terjemahan dan nota kajian kitab mazhab Syafi‘i.", fontSize = fontSize.sp, lineHeight = (fontSize * spacing).sp, color = if (background == "dark") Color.White else Color.DarkGray)
            }
        }
        }
        HorizontalDivider(color = Color(0xFFE1E3E5))
        SettingsPanel {
        Text("Simpan & Pulihkan Data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Simpan fail data DinNote dalam folder khas pilihan anda, contohnya Dokumen/DinNote Backup. Fail ini boleh diimport semula apabila diperlukan.", fontSize = 13.sp)
        Button(onClick = {
            val filename = "DinNote-Data-" +
                java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
                    .format(java.util.Date()) + ".json"
            saveDataLauncher.launch(filename)
        }, modifier = Modifier.fillMaxWidth()) { Text("Simpan data ke telefon") }
        OutlinedButton(onClick = {
            restoreDataLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
        }, modifier = Modifier.fillMaxWidth()) { Text("Import data untuk pemulihan") }
        if (backupStatus.isNotBlank()) {
            Text(backupStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        }
        Text("Tekan Simpan Tetapan untuk menyimpan perubahan pada telefon ini.", fontSize = 12.sp, color = Color.Gray)
        HorizontalDivider(color = Color(0xFFE1E3E5))
        Text("About Us · Tentang DinNote", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("DinNote", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075B59))
            Text("مِنَ التَّدْوِينِ إِلَى كُنُوزِ الْعِلْمِ", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 20.sp, color = Color(0xFF075B59))
            Text("Daripada catatan kepada khazanah ilmu.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color(0xFF075B59))
        }
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFFDF8), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E1D7))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
