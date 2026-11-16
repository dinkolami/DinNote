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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
    var openNewNote by remember { mutableStateOf(false) }
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
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 14.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📗", fontSize = 15.sp)
                        Text("dinnote", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Text("⚙") },
                        label = { Text("Tetapan") }
                    )
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding), onOpenNotes = { openNewNote = true; selectedTab = 1 }, onOpenLibrary = { selectedTab = 2 })
                1 -> NotesScreen(Modifier.padding(padding), openNewNote = openNewNote, onNewNoteOpened = { openNewNote = false })
                2 -> LibraryHubScreen(Modifier.padding(padding))
                3 -> DisplaySettingsScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenNotes: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF245B4B))
        ) {
            Box(Modifier.fillMaxWidth()) {
                // Nature landscape painted behind the compact greeting; no network required.
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF8BC8C2), Color(0xFFB9DCC5), Color(0xFF477B62))
                        )
                    )
                    drawCircle(Color(0xFFFFE3A1).copy(alpha = 0.8f), radius = h * 0.23f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.22f))
                    val distant = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.78f)
                        lineTo(w * 0.19f, h * 0.35f)
                        lineTo(w * 0.40f, h * 0.72f)
                        lineTo(w * 0.64f, h * 0.27f)
                        lineTo(w, h * 0.79f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(distant, Color(0xFF669B83))
                    val foreground = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.91f)
                        quadraticTo(w * 0.30f, h * 0.56f, w * 0.56f, h * 0.85f)
                        quadraticTo(w * 0.79f, h * 0.60f, w, h * 0.77f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(foreground, Color(0xFF2F6A52))
                    drawRect(Color(0xFF123E35).copy(alpha = 0.42f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Assalamualaikum", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Selamat datang ke DinNote", color = Color.White, fontSize = 12.sp)
                    }
                    Surface(
                        color = Color(0xFF16634F),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.clickable(onClick = onOpenNotes)
                    ) {
                        Text("+ Nota", Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Text("Ruang anda", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                Modifier.weight(1f).clickable(onClick = onOpenNotes),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0E7)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✎", fontSize = 27.sp, color = Color(0xFF145A45))
                    Text("Nota Peribadi", fontWeight = FontWeight.Bold, color = Color(0xFF173F32))
                    Text("Idea, tugasan dan catatan harian", fontSize = 12.sp, color = Color(0xFF53685D))
                    Text("Buka nota  →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF145A45))
                }
            }
            Card(
                Modifier.weight(1f).clickable(onClick = onOpenLibrary),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2EAD9)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("۞", fontSize = 27.sp, color = Color(0xFF8B6938))
                    Text("Perpustakaan", fontWeight = FontWeight.Bold, color = Color(0xFF493719))
                    Text("Zikir, kitab dan kata hikmah", fontSize = 12.sp, color = Color(0xFF756344))
                    Text("Teroka ilmu  →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B6938))
                }
            }
        }

        Text("Koleksi dalam DinNote", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(
            Modifier.fillMaxWidth().clickable(onClick = onOpenLibrary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("☾", fontSize = 23.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kalimah Tayyibah", fontWeight = FontWeight.Bold)
                        Text("Zikir dan ungkapan yang baik", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("📚", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kitab-Kitab", fontWeight = FontWeight.Bold)
                        Text("Petikan Arab, syarah dan nota pengajian", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("✧", fontSize = 25.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kalam Hikmah", fontWeight = FontWeight.Bold)
                        Text("Nasihat ulama dan peringatan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Text(
            "“Ilmu yang dicatat, mudah-mudahan menjadi manfaat.”",
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    openNewNote: Boolean = false,
    onNewNoteOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val displayPrefs = remember { context.getSharedPreferences("dinnote_display", android.content.Context.MODE_PRIVATE) }
    val database = remember {
        NoteDatabase(context).also { it.migrateOldSingleNote(context) }
    }
    val notes = remember { mutableStateListOf<DinNote>() }
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }
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
    val defaultCategories = listOf("Agama", "Pengajian", "Kitab", "Peribadi", "Kerja", "Idea", "Lain-lain")
    val categoryPrefs = remember { context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE) }
    val builtInCategories = remember {
        mutableStateListOf<String>().apply {
            val saved = categoryPrefs.getString("built_in_order", null)
            addAll(if (saved.isNullOrBlank()) defaultCategories else saved.split("|").filter { it.isNotBlank() })
        }
    }
    val customCategories = remember {
        mutableStateListOf<String>().apply {
            addAll(categoryPrefs.getStringSet("custom", emptySet()).orEmpty().sorted())
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

    LaunchedEffect(openNewNote) {
        if (openNewNote) {
            editingId = null
            title = ""
            body = TextFieldValue("")
            undoStack.clear(); redoStack.clear()
            selectedColor = "green"
            if (selectedCategory == "Semua" || selectedCategory !in allCategories) selectedCategory = "Lain-lain"
            readingMode = false; showEditor = true
            onNewNoteOpened()
        }
    }

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
                undoStack.clear(); redoStack.clear()
                selectedColor = "green"
                selectedCategory = if (selectedCategory == "Semua") "Lain-lain" else selectedCategory
                readingMode = false; showEditor = true
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
            AssistChip(onClick = { showManageCategories = true }, label = { Text("Urus kategori") })
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari nota") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "${visibleNotes.size} nota" + if (showArchived) " dalam arkib" else " tersimpan",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        if (visibleNotes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✎", fontSize = 30.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(if (search.isBlank()) "Ruang nota anda masih kosong" else "Tiada nota ditemui", fontWeight = FontWeight.Bold)
                    Text(
                        if (search.isBlank()) "Mulakan dengan menulis nota pertama anda."
                        else "Cuba kata carian atau kategori yang lain.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val orderedNotes = visibleNotes.sortedWith(
                compareByDescending<DinNote> { it.pinned }.thenByDescending { it.updatedAt }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(orderedNotes.size) { index ->
                    val item = orderedNotes[index]
                    var menuExpanded by remember(item.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp),
                        colors = CardDefaults.cardColors(containerColor = noteColor(item.colorTag)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.category, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, maxLines = 1)
                                Box {
                                    Text("⋮", modifier = Modifier.clickable { menuExpanded = true }, fontSize = 20.sp)
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(if (item.pinned) "Lepas pin" else "Pin nota") },
                                            onClick = { database.setPinned(item.id, !item.pinned); menuExpanded = false; refreshNotes() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Kongsi") },
                                            onClick = {
                                                val shareText = buildString {
                                                    if (item.title.isNotBlank()) append(item.title).append("\n\n")
                                                    append(formattedNoteText(item.body).text)
                                                }
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Kongsi nota"))
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (showArchived) "Pulihkan" else "Arkib") },
                                            onClick = { database.setArchived(item.id, !item.archived); menuExpanded = false; refreshNotes() }
                                        )
                                        DropdownMenuItem(text = { Text("Padam") }, onClick = { noteToDelete = item; menuExpanded = false })
                                    }
                                }
                            }
                            Column(
                                Modifier.fillMaxWidth().weight(1f).clickable {
                                    editingId = item.id
                                    title = item.title
                                    body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                                    undoStack.clear(); redoStack.clear()
                                    selectedColor = item.colorTag
                                    selectedCategory = item.category
                                    readingMode = true; showEditor = true
                                },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(item.title.ifBlank { "Tanpa tajuk" }, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(formattedNoteText(item.body), maxLines = 4, fontSize = 13.sp)
                            }
                            Text(
                                java.text.SimpleDateFormat("dd MMM", java.util.Locale("ms", "MY"))
                                    .format(java.util.Date(item.updatedAt)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
            title = { Text("Urus kategori nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCategories.forEach { category ->
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
                            if (category in customCategories) {
                                TextButton(onClick = {
                                    categoryToDelete = category
                                    showManageCategories = false
                                }) { Text("Padam") }
                            }
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
                    if (newName.isNotBlank() && newName != oldName && newName !in allCategories) {
                        database.renameCategory(oldName, newName)
                        val customIndex = customCategories.indexOf(oldName)
                        if (customIndex >= 0) {
                            customCategories[customIndex] = newName
                        } else {
                            val builtInIndex = builtInCategories.indexOf(oldName)
                            if (builtInIndex >= 0) builtInCategories[builtInIndex] = newName
                            categoryPrefs.edit().putString("built_in_order", builtInCategories.joinToString("|")).apply()
                        }
                        categoryPrefs.edit().putStringSet("custom", customCategories.toSet()).apply()
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
        fun persistNote() {
            if (title.isNotBlank() || body.text.isNotBlank()) {
                val id = database.save(title.trim(), body.text, editingId)
                editingId = id
                database.setColor(id, selectedColor)
                database.setCategory(id, selectedCategory)
                refreshNotes()
            }
        }
        LaunchedEffect(title, body.text, selectedCategory, selectedColor) {
            kotlinx.coroutines.delay(650)
            persistNote()
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { persistNote(); showEditor = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = if (readingMode) readingBackground(displayPrefs.getString("background", "white") ?: "white") else Color.White) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    if (readingMode) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { showEditor = false }) { Text("← Kembali") }
                            TextButton(onClick = { readingMode = false }) { Text("✎ Edit") }
                        }
                        HorizontalDivider()
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text(title.ifBlank { "Tanpa tajuk" }, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text(formattedNoteText(body.text), fontSize = displayPrefs.getInt("font_size", 19).sp, lineHeight = (displayPrefs.getInt("font_size", 19) * displayPrefs.getFloat("line_spacing", 1.6f)).sp, color = if (displayPrefs.getString("background", "white") == "dark") Color.White else Color(0xFF263B3E))
                        }
                    } else {
                    Surface(color = Color(0xFF08747A)) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { persistNote(); showEditor = false }) { Text("←", color = Color.White, fontSize = 27.sp) }
                        Text(if (editingId == null) "Nota Baharu" else "Edit Nota", modifier = Modifier.weight(1f), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            TextButton(enabled = undoStack.isNotEmpty(), onClick = {
                                if (undoStack.isNotEmpty()) { redoStack.add(body); body = undoStack.removeAt(undoStack.lastIndex) }
                            }) { Text("↶", color = Color.White, fontSize = 23.sp) }
                            TextButton(enabled = redoStack.isNotEmpty(), onClick = {
                                if (redoStack.isNotEmpty()) { undoStack.add(body); body = redoStack.removeAt(redoStack.lastIndex) }
                            }) { Text("↷", color = Color.White, fontSize = 23.sp) }
                        }
                        TextButton(onClick = {
                            persistNote()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, title + "\n\n" + body.text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Kongsi nota"))
                        }) { Text("↗", color = Color.White, fontSize = 23.sp) }
                    }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            java.text.SimpleDateFormat("dd/MM, h:mm a", java.util.Locale("ms", "MY"))
                                .format(java.util.Date()),
                            color = Color.Gray, fontSize = 13.sp
                        )
                        var categoryExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { categoryExpanded = true }) {
                                Text("▣  $selectedCategory  ▾", color = Color(0xFF536A70))
                            }
                            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                allCategories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category) }, onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    })
                                }
                            }
                        }
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, color = Color(0xFF263B3E)),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) Text("Tajuk", fontSize = 28.sp, color = Color.LightGray)
                                inner()
                            }
                        }
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = body,
                        onValueChange = { next ->
                            if (next.text != body.text) { undoStack.add(body); if (undoStack.size > 60) undoStack.removeAt(0); redoStack.clear() }
                            body = next
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp, color = Color(0xFF263B3E), lineHeight = 28.sp),
                        decorationBox = { inner ->
                            Box {
                                if (body.text.isEmpty()) Text("Tulis nota di sini", fontSize = 19.sp, color = Color.LightGray)
                                inner()
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tools = listOf("Aa" to "**", "B" to "**", "I" to "_", "U" to "__", "S" to "~~", "A" to "**", "✎" to "✍ ", "☷" to "• ", "☰" to "1. ", "☑" to "☐ ", "▧" to "▧ ", "📎" to "📎 ")
                        tools.forEach { (label, insertion) ->
                            TextButton(onClick = {
                                if (insertion.isNotEmpty()) {
                                    val a = body.selection.min
                                    val b = body.selection.max
                                    val selected = body.text.substring(a, b)
                                    val newText = if (insertion == "**" || insertion == "_" || insertion == "__") insertion + selected + insertion else insertion + selected
                                    val result = body.text.replaceRange(a, b, newText)
                                    undoStack.add(body); redoStack.clear()
                                    body = TextFieldValue(result, selection = TextRange(a + newText.length))
                                }
                            }) { Text(label, color = Color(0xFF4F9BC9), fontSize = 21.sp) }
                        }
                    }
                    }
                }
            }
        }
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
    val context = LocalContext.current
    val libraryPrefs = remember { context.getSharedPreferences("dinnote_library_edits", android.content.Context.MODE_PRIVATE) }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    val builtinLibraryCategories = listOf("Kalimah Tayyibah", "Kitab-Kitab", "Kalam Hikmah")
    val customLibraryCategories = remember(libraryRevision) {
        libraryPrefs.getString("custom_categories", "")!!.split("\\n").filter { it.isNotBlank() }
    }
    val allLibraryCategories = builtinLibraryCategories + customLibraryCategories
    var editingLibraryKey by remember { mutableStateOf<String?>(null) }
    var libraryTitle by remember { mutableStateOf("") }
    var libraryBody by remember { mutableStateOf(TextFieldValue("")) }
    fun openLibraryEditor(key: String, defaultTitle: String = "", defaultBody: String = "") {
        editingLibraryKey = key
        libraryTitle = libraryPrefs.getString(key + "_title", defaultTitle) ?: defaultTitle
        libraryBody = TextFieldValue(libraryPrefs.getString(key + "_body", defaultBody) ?: defaultBody)
    }
    fun saveLibraryEditor() {
        editingLibraryKey?.let { key ->
            libraryPrefs.edit().putString(key + "_title", libraryTitle).putString(key + "_body", libraryBody.text).apply()
            libraryRevision++
        }
    }
    val libraryItems = remember(libraryRevision, selectedCategory) {
        libraryPrefs.all.keys.filter { it.startsWith("entry_" + selectedCategory + "_") && it.endsWith("_title") }
            .map { it.removeSuffix("_title") }.sorted()
    }

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
        Text("Perpustakaan", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)


        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                allLibraryCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category; search = "" },
                        label = { Text(category, fontSize = 11.sp) }
                    )
                }
            }
            TextButton(onClick = { newCategoryName = ""; showAddCategory = true }) { Text("+ Kategori") }
        }
        if (selectedCategory in customLibraryCategories) {
            TextButton(onClick = { categoryToRename = selectedCategory; newCategoryName = selectedCategory }) {
                Text("✎ Ubah nama kategori")
            }
        }

        when (selectedCategory) {
            "Kalimah Tayyibah" -> {
                Text("الكلمات الطيبة", fontSize = 22.sp)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(entry.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(entry.arabic, modifier = Modifier.fillMaxWidth(), fontSize = 25.sp)
                                Text(entry.transliteration, fontWeight = FontWeight.Medium)
                                Text(entry.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tekan untuk maklumat lanjut", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            "Kitab-Kitab" -> {
                Text("📚 Perpustakaan Kitab", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari nama kitab atau tajuk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { openLibraryEditor("entry_Kitab-Kitab_" + System.currentTimeMillis()) }) { Text("+ Tambah Kitab") }
                libraryItems.filter { (libraryPrefs.getString(it + "_title", "") ?: "").contains(search, true) ||
                    (libraryPrefs.getString(it + "_body", "") ?: "").contains(search, true) }.forEach { key ->
                    Card(Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text((libraryPrefs.getString(key + "_body", "") ?: "").take(130), maxLines = 3)
                        }
                    }
                }
            }
            else -> {
                Text("✨ Kalam Hikmah", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari kata hikmah atau ulama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { openLibraryEditor("entry_" + selectedCategory + "_" + System.currentTimeMillis()) }) { Text("+ Tambah Kandungan") }
                libraryItems.filter { (libraryPrefs.getString(it + "_title", "") ?: "").contains(search, true) ||
                    (libraryPrefs.getString(it + "_body", "") ?: "").contains(search, true) }.forEach { key ->
                    Card(Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text((libraryPrefs.getString(key + "_body", "") ?: "").take(130), maxLines = 3)
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory || categoryToRename != null) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false; categoryToRename = null },
            title = { Text(if (categoryToRename == null) "Tambah kategori" else "Ubah nama kategori") },
            text = {
                OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it },
                    label = { Text("Nama kategori") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    val old = categoryToRename
                    if (name.isNotEmpty() && (name == old || name !in allLibraryCategories)) {
                        val updated = if (old == null) customLibraryCategories + name
                            else customLibraryCategories.map { if (it == old) name else it }
                        if (old != null && old != name) {
                            libraryPrefs.all.keys.filter { it.startsWith("entry_" + old + "_") }.forEach { key ->
                                val newKey = "entry_" + name + "_" + key.removePrefix("entry_" + old + "_")
                                libraryPrefs.edit().putString(newKey, libraryPrefs.getString(key, "")).remove(key).apply()
                            }
                        }
                        libraryPrefs.edit().putString("custom_categories", updated.joinToString("\\n")).apply()
                        selectedCategory = name
                        libraryRevision++
                        showAddCategory = false
                        categoryToRename = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false; categoryToRename = null }) { Text("Batal") }
            }
        )
    }

    if (editingLibraryKey != null) {
        LaunchedEffect(libraryTitle, libraryBody.text) {
            kotlinx.coroutines.delay(650)
            if (libraryTitle.isNotBlank() || libraryBody.text.isNotBlank()) saveLibraryEditor()
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { saveLibraryEditor(); editingLibraryKey = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = Color.White) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick = { saveLibraryEditor(); editingLibraryKey = null }) { Text("← Selesai") }
                        Text("Editor Perpustakaan", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                    androidx.compose.foundation.text.BasicTextField(
                        value = libraryTitle, onValueChange = { libraryTitle = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 25.sp, color = Color(0xFF263B3E)),
                        decorationBox = { inner -> Box { if (libraryTitle.isEmpty()) Text("Tajuk kitab / hikmah", color = Color.Gray); inner() } }
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = libraryBody, onValueChange = { libraryBody = it },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp, lineHeight = 29.sp, color = Color(0xFF263B3E)),
                        decorationBox = { inner -> Box { if (libraryBody.text.isEmpty()) Text("Tulis kandungan Arab atau Rumi...", color = Color.Gray); inner() } }
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(5.dp)) {
                        listOf("B" to "**", "I" to "_", "U" to "__", "S" to "~~", "•" to "• ", "1." to "1. ", "☑" to "☐ ").forEach { (label, marker) ->
                            TextButton(onClick = {
                                val a = libraryBody.selection.min
                                val b = libraryBody.selection.max
                                val selected = libraryBody.text.substring(a, b)
                                val insertion = if (marker in listOf("**", "_", "__", "~~")) marker + selected + marker else marker + selected
                                libraryBody = TextFieldValue(libraryBody.text.replaceRange(a, b, insertion),
                                    selection = TextRange(a + insertion.length))
                            }) { Text(label) }
                        }
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
