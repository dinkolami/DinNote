package com.dinnote.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
    else -> Color.White
}

@Composable
fun DisplaySettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dinnote_display", Context.MODE_PRIVATE) }
    var background by remember { mutableStateOf(prefs.getString("background", "white") ?: "white") }
    var fontSize by remember { mutableFloatStateOf(prefs.getInt("font_size", 19).toFloat()) }
    var spacing by remember { mutableStateOf(prefs.getFloat("line_spacing", 1.6f)) }
    var fullScreen by remember { mutableStateOf(prefs.getBoolean("full_screen", true)) }
    var fontChoice by remember { mutableStateOf(prefs.getString("font", "Standard") ?: "Standard") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("⚙ Tetapan Paparan", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075A40))
        Text("Sesuaikan rupa dan gaya bacaan nota.")
        Text("Latar Belakang Bacaan", fontWeight = FontWeight.Bold)
        listOf("white" to "Putih", "cream" to "Krim", "green" to "Hijau lembut", "sepia" to "Sepia", "dark" to "Gelap", "pink" to "Merah jambu", "blue" to "Biru lembut").chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (key, name) ->
                    Surface(
                        modifier = Modifier.weight(1f).height(76.dp).clickable {
                            background = key
                            prefs.edit().putString("background", key).apply()
                        },
                        color = readingBackground(key),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(if (background == key) 3.dp else 1.dp, if (background == key) Color(0xFF00865A) else Color.LightGray)
                    ) {
                        Box(Modifier.padding(12.dp)) {
                            Text(name + if (background == key) " ✓" else "", color = if (key == "dark") Color.White else Color.DarkGray)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("Saiz Tulisan: ${fontSize.toInt()}", fontWeight = FontWeight.Bold)
        Slider(value = fontSize, onValueChange = {
            fontSize = it
            prefs.edit().putInt("font_size", it.toInt()).apply()
        }, valueRange = 14f..32f)
        Text("Jenis Fon", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Standard", "Serif", "Arab & Rumi").forEach { option ->
                FilterChip(selected = fontChoice == option, onClick = {
                    fontChoice = option
                    prefs.edit().putString("font", option).apply()
                }, label = { Text(option, fontSize = 11.sp) })
            }
        }
        Text("Jarak Baris", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1.3f to "Rapat", 1.6f to "Sederhana", 2.0f to "Lapang").forEach { (value, label) ->
                FilterChip(selected = spacing == value, onClick = {
                    spacing = value
                    prefs.edit().putFloat("line_spacing", value).apply()
                }, label = { Text(label) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mod Bacaan Penuh", fontWeight = FontWeight.Medium)
            Switch(checked = fullScreen, onCheckedChange = {
                fullScreen = it
                prefs.edit().putBoolean("full_screen", it).apply()
            })
        }
        Text("Pratonton", fontWeight = FontWeight.Bold)
        Surface(color = readingBackground(background), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", fontSize = (fontSize + 5).sp, color = if (background == "dark") Color.White else Color(0xFF075A40))
                Spacer(Modifier.height(12.dp))
                Text("Contoh paparan nota dengan tetapan yang dipilih.", fontSize = fontSize.sp, lineHeight = (fontSize * spacing).sp, color = if (background == "dark") Color.White else Color.DarkGray)
            }
        }
        Text("Pilihan disimpan secara automatik pada telefon ini.", fontSize = 12.sp, color = Color.Gray)
    }
}
