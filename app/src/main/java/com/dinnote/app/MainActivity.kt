
package com.dinnote.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DinNoteApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinNoteApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("DinNote", fontWeight = FontWeight.Bold)
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("⌂") },
                        label = { Text("Utama") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("✎") },
                        label = { Text("Nota") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("۞") },
                        label = { Text("Perpustakaan") }
                    )
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding))
                1 -> NotesScreen(Modifier.padding(padding))
                2 -> LibraryScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("السلام عليكم", fontSize = 28.sp,
            fontWeight = FontWeight.Bold)
        Text("Selamat datang ke DinNote")
        Text("دفتر الملاحظات والمعرفة الإسلامية",
            fontSize = 19.sp)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Nota Saya", fontWeight = FontWeight.Bold)
                Text("Simpan dan susun catatan peribadi.")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Perpustakaan Ilmu",
                    fontWeight = FontWeight.Bold)
                Text("Himpunan ilmu, kitab dan rujukan.")
            }
        }
    }
}

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("dinnote",
            Context.MODE_PRIVATE)
    }

    var title by remember {
        mutableStateOf(prefs.getString("title", "") ?: "")
    }
    var note by remember {
        mutableStateOf(prefs.getString("note", "") ?: "")
    }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nota Saya", fontSize = 24.sp,
            fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; saved = false },
            label = { Text("Tajuk nota") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it; saved = false },
            label = { Text("Tulis dalam Arab atau Rumi") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp)
        )

        Button(
            onClick = {
                prefs.edit()
                    .putString("title", title)
                    .putString("note", note)
                    .apply()
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Nota")
        }

        if (saved) Text("Nota berjaya disimpan ✓")
    }
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Perpustakaan Ilmu",
            fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Text("مكتبة العلوم")

        listOf(
            "Al-Quran dan Tafsir",
            "Hadis dan Takhrij",
            "Kitab Arab",
            "Terjemahan Melayu",
            "Koleksi Kalimah Tayyibah",
            "Penanda dan Rujukan"
        ).forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Text(item, Modifier.padding(18.dp))
            }
        }
    }
}
