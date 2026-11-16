package com.dinnote.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
    val islamicScheme = lightColorScheme(
        primary = Color(0xFF145A45),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7E9DF),
        onPrimaryContainer = Color(0xFF103D30),
        secondary = Color(0xFF9A7845),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF0E4CB),
        onSecondaryContainer = Color(0xFF493719),
        tertiary = Color(0xFF526B78),
        background = Color(0xFFF6F4ED),
        surface = Color(0xFFFFFEFA),
        surfaceVariant = Color(0xFFE9EEE8),
        outline = Color(0xFFD6D2C5)
    )
    MaterialTheme(colorScheme = islamicScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("DinNote", fontWeight = FontWeight.Bold)
                            Text("دفتر العلم والذكر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
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
                2 -> LibraryScreen2(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Color(0xFFF3E6C8), fontSize = 20.sp)
                Text("Assalamualaikum", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ruang mencatat, mengingati dan menghimpunkan ilmu.",
                    color = Color(0xFFE4EFE8),
                    fontSize = 14.sp
                )
                Text("دَفْتَرُ الْعِلْمِ وَالذِّكْرِ", color = Color(0xFFF3E6C8), fontSize = 17.sp)
            }
        }
        Text(
            "Ruang DinNote",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("✎", fontSize = 27.sp, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nota Peribadi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Catat idea, susun tugasan dan simpan nota penting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("۞", fontSize = 27.sp, color = MaterialTheme.colorScheme.secondary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Perpustakaan Ilmu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Kalimah Tayyibah, kitab-kitab dan Kalam Hikmah.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "Ilmu yang dicatat, mudah-mudahan menjadi manfaat.",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 13.sp
        )
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
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var selectedColor by remember { mutableStateOf("green") }
    var showArchived by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showManageCategories by remember { mutableStateOf(false) }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renamedCategory by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var noteToDelete by remember { mutableStateOf<DinNote?>(null) }
    val builtInCategories = listOf("Agama", "Pengajian", "Peribadi", "Kerja", "Idea", "Lain-lain")
    val customCategories = remember {
        mutableStateListOf<String>().apply {
            addAll(context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                .getStringSet("custom", emptySet()).orEmpty().sorted())
        }
    }
    val allCategories = builtInCategories + customCategories

    fun refreshNotes() {
        notes.clear()
        notes.addAll(database.allNotes())
    }

    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                NoteBackup.export(context, uri, database.allNotes())
                "Fail sandaran berjaya disimpan."
            }.getOrElse { "Sandaran gagal: ${it.message ?: "ralat"}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                val result = NoteBackup.import(context, uri, database)
                refreshNotes()
                val restoredCustomCategories = notes.map { it.category }
                    .filter { it !in builtInCategories }
                    .distinct()
                restoredCustomCategories.forEach { category ->
                    if (category !in customCategories) customCategories.add(category)
                }
                context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                    .edit().putStringSet("custom", customCategories.toSet()).apply()
                buildString {
                    append("${result.imported} nota dipulihkan.")
                    if (result.skippedDuplicates > 0) {
                        append(" ${result.skippedDuplicates} nota pendua sedia ada dilangkau.")
                    }
                }
            }.getOrElse { "Pemulihan gagal: ${it.message ?: "ralat"}" }
        }
    }

    LaunchedEffect(Unit) { refreshNotes() }

    val visibleNotes = notes.filter {
        it.archived == showArchived &&
            (selectedCategory == "Semua" || it.category == selectedCategory) &&
            (it.title.contains(search, ignoreCase = true) ||
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
                body = TextFieldValue("")
                selectedColor = "green"
                selectedCategory = if (selectedCategory == "Semua") "Lain-lain" else selectedCategory
                showEditor = true
                }) { Text("+ Nota") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { exportLauncher.launch("DinNote-Backup.json") }) {
                Text("Eksport sandaran")
            }
            OutlinedButton(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
            }) {
                Text("Pulihkan sandaran")
            }
        }
        backupMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            (listOf("Semua") + allCategories).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
            AssistChip(onClick = { showAddCategory = true }, label = { Text("+ Kategori") })
            if (customCategories.isNotEmpty()) {
                AssistChip(onClick = { showManageCategories = true }, label = { Text("Urus kategori") })
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
                                body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                                selectedColor = item.colorTag
                                selectedCategory = item.category
                                showEditor = true
                            }.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                item.title.ifBlank { "Tanpa tajuk" },
                                fontWeight = FontWeight.Bold
                            )
                            Text(item.category, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                            Text(formattedNoteText(item.body), maxLines = 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    val shareText = buildString {
                                        if (item.title.isNotBlank()) append(item.title).append("\n\n")
                                        append(formattedNoteText(item.body).text)
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Kongsi nota"))
                                }) { Text("Kongsi") }
                                TextButton(onClick = {
                                    database.setPinned(item.id, !item.pinned)
                                    refreshNotes()
                                }) { Text(if (item.pinned) "Lepas pin" else "Pin") }
                                TextButton(onClick = {
                                    database.setArchived(item.id, !item.archived)
                                    refreshNotes()
                                }) { Text(if (item.archived) "Pulihkan" else "Arkib") }
                                TextButton(onClick = { noteToDelete = item }) { Text("Padam") }
                                TextButton(onClick = {
                                    editingId = item.id
                                    title = item.title
                                    body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                                    selectedColor = item.colorTag
                                    selectedCategory = item.category
                                    showEditor = true
                                }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false; newCategoryName = "" },
            title = { Text("Tambah kategori") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nama kategori") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    if (name.isNotEmpty() && name !in builtInCategories && name !in customCategories) {
                        customCategories.add(name)
                        context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                            .edit().putStringSet("custom", customCategories.toSet()).apply()
                        selectedCategory = name
                    }
                    newCategoryName = ""
                    showAddCategory = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false; newCategoryName = "" }) { Text("Batal") }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Padam nota?") },
            text = { Text("Nota \"${note.title.ifBlank { "Tanpa tajuk" }}\" akan dipadam. Tindakan ini tidak boleh dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    database.delete(note.id)
                    noteToDelete = null
                    refreshNotes()
                }) { Text("Ya, padam") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Batal") }
            }
        )
    }
    if (showManageCategories) {
        AlertDialog(
            onDismissRequest = { showManageCategories = false },
            title = { Text("Urus kategori sendiri") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                categoryToRename = category
                                renamedCategory = category
                                showManageCategories = false
                            }) { Text("Tukar nama") }
                            TextButton(onClick = {
                                categoryToDelete = category
                                showManageCategories = false
                            }) { Text("Padam") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageCategories = false }) { Text("Selesai") }
            }
        )
    }

    categoryToRename?.let { oldName ->
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text("Tukar nama kategori") },
            text = {
                OutlinedTextField(
                    value = renamedCategory,
                    onValueChange = { renamedCategory = it },
                    label = { Text("Nama baharu") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renamedCategory.trim()
                    if (newName.isNotBlank() && newName !in builtInCategories &&
                        (newName == oldName || newName !in customCategories)
                    ) {
                        database.renameCategory(oldName, newName)
                        val index = customCategories.indexOf(oldName)
                        if (index >= 0) customCategories[index] = newName
                        context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                            .edit().putStringSet("custom", customCategories.toSet()).apply()
                        if (selectedCategory == oldName) selectedCategory = newName
                        refreshNotes()
                    }
                    categoryToRename = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) { Text("Batal") }
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Padam kategori?") },
            text = {
                Text("Kategori \"$category\" akan dipadam. Nota di dalamnya tidak akan hilang; semuanya dipindahkan ke kategori Lain-lain.")
            },
            confirmButton = {
                TextButton(onClick = {
                    database.moveCategoryToDefault(category)
                    customCategories.remove(category)
                    context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                        .edit().putStringSet("custom", customCategories.toSet()).apply()
                    if (selectedCategory == category) selectedCategory = "Semua"
                    categoryToDelete = null
                    refreshNotes()
                }) { Text("Padam kategori") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Batal") }
            }
        )
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingId == null) "Nota Baharu" else "Edit Nota") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tajuk nota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    var categoryMenuExpanded by remember { mutableStateOf(false) }
                    Text("Kategori nota", fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                            Text(selectedCategory)
                        }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            allCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text("Warna nota", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("green", "yellow", "blue", "pink", "purple").forEach { tag ->
                            Surface(
                                modifier = Modifier.size(34.dp).clickable { selectedColor = tag },
                                shape = MaterialTheme.shapes.small,
                                color = noteColor(tag),
                                border = if (selectedColor == tag)
                                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else null
                            ) {}
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            val start = body.selection.min
                            val end = body.selection.max
                            val selected = body.text.substring(start, end)
                            val updated = body.text.replaceRange(start, end, "**$selected**")
                            body = TextFieldValue(updated, selection = TextRange(start + 2, end + 2))
                        }) { Text("B  Tebal") }
                        OutlinedButton(onClick = {
                            val start = body.selection.min
                            val end = body.selection.max
                            val selected = body.text.substring(start, end)
                            val updated = body.text.replaceRange(start, end, "_${selected}_")
                            body = TextFieldValue(updated, selection = TextRange(start + 1, end + 1))
                        }) { Text("I  Condong") }
                    }
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Kandungan (Arab atau Rumi)") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val wordCount = body.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                    Text(
                        "${body.text.length} aksara  •  $wordCount perkataan",
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        "Petua: Asingkan tajuk, petikan Arab, terjemahan dan rujukan pada baris berbeza supaya nota lebih kemas.",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() || body.text.isNotBlank()) {
                        val savedId = database.save(title.trim(), body.text, editingId)
                        database.setColor(savedId, selectedColor)
                        database.setCategory(savedId, selectedCategory)
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

fun formattedNoteText(source: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("""\*\*(.+?)\*\*|_(.+?)_""")
    var cursor = 0
    pattern.findAll(source).forEach { match ->
        append(source.substring(cursor, match.range.first))
        val boldText = match.groups[1]?.value
        if (boldText != null) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(boldText)
            pop()
        } else {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(match.groups[2]?.value.orEmpty())
            pop()
        }
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
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
    var selectedCategory by remember { mutableStateOf("Kalimah Tayyibah") }
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
        Text("Perpustakaan DinNote 🌿", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Koleksi ilmu dan amalan yang boleh dibaca secara offline.")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Kalimah Tayyibah", "Kitab-Kitab", "Kalam Hikmah").forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category; search = "" },
                    label = { Text(category, fontSize = 11.sp) }
                )
            }
        }

        when (selectedCategory) {
            "Kalimah Tayyibah" -> {
                Text("الكلمات الطيبة", fontSize = 22.sp)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari kalimah atau maksud") },