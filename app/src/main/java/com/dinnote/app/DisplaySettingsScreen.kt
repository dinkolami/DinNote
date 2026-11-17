package com.dinnote.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tetapan DinNote", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
            Button(onClick = {
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
            }) { Text("Simpan", fontSize = 13.sp) }
        }
        if (settingsStatus.isNotBlank()) {
            Text(settingsStatus, fontSize = 13.sp, color = Color(0xFF075B59))
        }
        HorizontalDivider(color = Color(0xFFE1E3E5))
        Text("Latar Halaman Utama", fontWeight = FontWeight.Bold)
        Text("Pilih gambar daripada telefon untuk menggantikan corak Islamik di halaman Utama. Tekan Simpan di atas selepas memilih.", fontSize = 12.sp)
        OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Pilih Gambar Latar Utama")
        }
        if (imageUri.isNotBlank()) {
            OutlinedButton(onClick = { imageUri = ""; settingsStatus = "Perubahan belum disimpan" }) {
                Text("Gunakan latar Islamik asal")
            }
            Text("Kelegapan gambar: ${(imageOpacity * 100).toInt()}%", fontSize = 13.sp)
            Slider(value = imageOpacity, onValueChange = { imageOpacity = it; settingsStatus = "Perubahan belum disimpan" }, valueRange = 0.05f..0.65f)
            val bitmap = remember(imageUri) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Box(Modifier.fillMaxWidth().height(110.dp)) {
                    Image(bitmap = bitmap, contentDescription = "Pratonton gambar latar", contentScale = ContentScale.Crop, alpha = imageOpacity, modifier = Modifier.fillMaxSize())
                }
            } else {
                Text("Gambar tidak dapat dibaca. Sila pilih semula.", fontSize = 12.sp, color = Color.Red)
            }
        }

        Text("Sesuaikan paparan nota dan Maktabah Arab–Melayu.", fontSize = 13.sp, color = Color(0xFF526B78))
        Text("Tema Bacaan", fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
        Text("Pilih gaya putih, turquoise, krim atau malam untuk bacaan.", fontSize = 12.sp)
        HorizontalDivider(color = Color(0xFFE1E3E5))
        Text("Keutamaan Maktabah", fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59), fontSize = 17.sp)
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Arab di atas, Melayu di bawah", "Melayu di atas, Arab di bawah", "Arab sahaja", "Melayu sahaja").forEach { option ->
                FilterChip(selected = bookLayout == option, onClick = {
                    bookLayout = option
                    settingsStatus = "Perubahan belum disimpan"
                }, label = { Text(option) })
            }
        }
        Text("Pilihan ini disimpan sebagai keutamaan Maktabah. Paparan kitab yang menyokong susunan ini akan disepadukan secara berperingkat.", fontSize = 12.sp, color = Color.Gray)
        HorizontalDivider(color = Color(0xFFE1E3E5))
        Text("Latar Belakang Bacaan", fontWeight = FontWeight.Bold)
        listOf("white" to "Putih", "turquoise" to "Turquoise Premium", "cream" to "Krim", "green" to "Hijau lembut", "sepia" to "Sepia", "dark" to "Gelap", "pink" to "Merah jambu", "blue" to "Biru lembut", "purple" to "Ungu", "nature" to "Alam").chunked(2).forEach { pair ->
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
                        Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(name + if (background == key) " ✓" else "", fontSize = 13.sp, color = if (key == "dark") Color.White else Color.DarkGray)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
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
        HorizontalDivider(color = Color(0xFFE1E3E5))
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
        Text("Tekan Simpan Tetapan untuk menyimpan perubahan pada telefon ini.", fontSize = 12.sp, color = Color.Gray)
    }
}
