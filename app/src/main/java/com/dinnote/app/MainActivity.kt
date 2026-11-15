package com.dinnote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
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
    val greenScheme = lightColorScheme(
        primary = Color(0xFF1B5E20),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF103B16),
        secondary = Color(0xFF4E6B50),
        background = Color(0xFFF7FAF5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8F0E5)
    )
    MaterialTheme(colorScheme = greenScheme) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("DinNote", fontWeight = FontWeight.Bold) })
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
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("السلام عليكم", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Selamat datang ke DinNote")
        Text("دفتر الملاحظات والمعرفة الإسلامية", fontSize = 19.sp)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Nota Saya", fontWeight = FontWeight.Bold)
                Text("Simpan, cari dan susun catatan peribadi anda.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Perpustakaan Ilmu", fontWeight = FontWeight.Bold)
                Text("Himpunan ilmu, kitab dan rujukan.")
            }
        }
    }
}

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember {
        NoteDatabase(context).also { it.migrateOldSingleNote(context) }
    }
    val notes = remember { mutableStateListOf<DinNote>() }
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }

    fun refreshNotes() {
        notes.clear()
        notes.addAll(database.allNotes())
    }

    LaunchedEffect(Unit) { refreshNotes() }

    val visibleNotes = notes.filter {
        it.archived == showArchived && (it.title.contains(search, ignoreCase = true) ||
            it.body.contains(search, ignoreCase = true))
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (showArchived) "Arkib Nota" else "Nota Saya", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "Nota" else "Arkib") }
                Button(onClick = {
                editingId = null
                title = ""
                body = ""
                showEditor = true
                }) { Text("+ Nota") }
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari nota") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (visibleNotes.isEmpty()) {
            Text(
                if (search.isBlank()) "Belum ada nota. Tekan + Nota untuk mula menulis."
                else "Tiada nota yang sepadan dengan carian."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleNotes, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = noteColor(item.colorTag))) {
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                editingId = item.id
                                title = item.title
                                body = item.body
                                showEditor = true
                            }.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                item.title.ifBlank { "Tanpa tajuk" },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                item.body,
                                maxLines = 3
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    database.setPinned(item.id, !item.pinned)
                                    refreshNotes()
                                }) { Text(if (item.pinned) "Lepas pin" else "Pin") }
                                TextButton(onClick = {
                                    database.setArchived(item.id, !item.archived)
                                    refreshNotes()
                                }) { Text(if (item.archived) "Pulihkan" else "Arkib") }
                                TextButton(onClick = {
                                    database.delete(item.id)
                                    refreshNotes()
                                }) { Text("Padam") }
                                TextButton(onClick = {
                                    editingId = item.id
                                    title = item.title
                                    body = item.body
                                    showEditor = true
                                }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingId == null) "Nota Baharu" else "Edit Nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tajuk nota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Kandungan (Arab atau Rumi)") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() || body.isNotBlank()) {
                        database.save(title.trim(), body, editingId)
                        refreshNotes()
                        showEditor = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun noteColor(tag: String): Color = when (tag) {
    "yellow" -> Color(0xFFFFF4CC)
    "blue" -> Color(0xFFDCEEFF)
    "pink" -> Color(0xFFFFE1E8)
    "purple" -> Color(0xFFEDE2FF)
    else -> Color(0xFFE5F2E3)
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    var selectedEntry by remember { mutableStateOf<KalimahEntry?>(null) }
    var search by remember { mutableStateOf("") }

    val filteredEntries = kalimahEntries.filter { entry ->
        entry.title.contains(search, ignoreCase = true) ||
            entry.transliteration.contains(search, ignoreCase = true) ||
            entry.meaning.contains(search, ignoreCase = true) ||
            entry.arabic.contains(search)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Koleksi Kalimah Tayyibah", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("الكلمات الطيبة", fontSize = 22.sp)
        Text("Zikir dan ungkapan yang baik — bersama bacaan, maksud dan rujukan ringkas.")

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari kalimah atau maksud") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("${filteredEntries.size} kalimah tersedia", color = MaterialTheme.colorScheme.primary)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(filteredEntries, key = { it.title }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedEntry = entry },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(entry.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            entry.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 25.sp
                        )
                        Text(entry.transliteration, fontWeight = FontWeight.Medium)
                        Text(entry.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tekan untuk maklumat lanjut", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(entry.arabic, fontSize = 27.sp)
                    Text(entry.transliteration, fontWeight = FontWeight.Bold)
                    Text(entry.meaning)
                    Text("Rujukan: ${entry.reference}", fontSize = 13.sp)
                    Text(entry.note, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) { Text("Tutup") }
            }
        )
    }
}
