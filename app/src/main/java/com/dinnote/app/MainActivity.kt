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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
        primary = Color(0xFF087F8C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9F2F3),
        onPrimaryContainer = Color(0xFF064E57),
        secondary = Color(0xFF4D6670),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAF1F3),
        onSecondaryContainer = Color(0xFF493719),
        tertiary = Color(0xFF526B78),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF5F7F8),
        outline = Color(0xFFD8DEE3)
    )
    MaterialTheme(colorScheme = islamicScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 14.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("◈", fontSize = 17.sp, color = Color(0xFF008F91))
                        Text("DinNote", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006B70))
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD8DEE3)),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxWidth().height(48.dp)) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple("Utama", "⌂", 0),
                                Triple("Nota", "✎", 1),
                                Triple("Maktabah", "▤", 2),
                                Triple("Tetapan", "⚙", 4)
                            )
                            tabs.forEachIndexed { index, tab ->
                                if (index > 0) {
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        thickness = 1.dp,
                                        color = Color(0xFFE3ECEC)
                                    )
                                }
                                val active = selectedTab == tab.third
                                Surface(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                        selectedTab = tab.third
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) Color(0xFFDDF4F2).copy(alpha = 0.92f)
                                            else Color.Transparent
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            tab.second,
                                            fontSize = 19.sp,
                                            lineHeight = 21.sp,
                                            color = if (active) Color(0xFF007F83) else Color(0xFF52666A)
                                        )
                                        Text(
                                            tab.first,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            maxLines = 1,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) Color(0xFF007F83) else Color(0xFF52666A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding), onOpenNotes = { openNewNote = true; selectedTab = 1 }, onOpenLibrary = { selectedTab = 2 }, onOpenSearch = { selectedTab = 3 })
                1 -> NotesScreen(Modifier.padding(padding), openNewNote = openNewNote, onNewNoteOpened = { openNewNote = false })
                2 -> LibraryHubScreen(Modifier.padding(padding))
                3 -> GlobalSearchScreen(Modifier.padding(padding))
                4 -> DisplaySettingsScreen(Modifier.padding(padding))
            }
        }
    }
}


@Composable
private fun PremiumSectionHeading(title: String, ornament: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(ornament, fontSize = 17.sp, color = Color(0xFFC69A3C))
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF087F83)
        )
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFD9B86B)
        )
    }
}

@Composable
private fun HomeShortcut(label: String, symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(54.dp).clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5D9DD))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(symbol, color = Color(0xFF006B46), fontSize = 20.sp)
            Spacer(Modifier.width(9.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF183A30), maxLines = 1)
            Text("›", fontSize = 21.sp, color = Color(0xFF006B46))
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenNotes: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF2FAF8),
            border = BorderStroke(1.dp, Color(0xFFB7DCD7))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Assalamualaikum", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
                    Text("Selamat datang ke DinNote", fontSize = 11.sp, color = Color(0xFF53736F))
                    Text("۞  ﷽  ۞", fontSize = 17.sp, color = Color(0xFFB48B39), modifier = Modifier.padding(top = 5.dp))
                    Text("⌒  ۞  ⌒  ۞  ⌒", fontSize = 13.sp, color = Color(0xFF087F8C), modifier = Modifier.padding(top = 2.dp))
                }
                OutlinedButton(onClick = onOpenNotes, shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                    Text("+ Nota", fontSize = 12.sp)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSearch),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD5DDDC))
        ) {
            Text("⌕  Cari nota, kitab dan kegemaran",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                fontSize = 13.sp, color = Color(0xFF52716E))
        }
        PremiumSectionHeading("Ruang anda", "✦")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeShortcut("Nota Peribadi", "✎", Modifier.weight(1f), onOpenNotes)
            HomeShortcut("Maktabah", "۞", Modifier.weight(1f), onOpenLibrary)
        }
        PremiumSectionHeading("Kandungan DinNote", "۞")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeShortcut("Kitab-Kitab", "▣", Modifier.weight(1f), onOpenLibrary)
            HomeShortcut("Kalam Hikmah", "✧", Modifier.weight(1f), onOpenLibrary)
        }
        HomeShortcut("Pelbagai", "▱", Modifier.fillMaxWidth(), onOpenLibrary)
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
    var editorBackground by remember { mutableStateOf(displayPrefs.getString("background", "white") ?: "white") }
    var editorFontSize by remember { mutableFloatStateOf(displayPrefs.getInt("font_size", 19).toFloat()) }
    var symbolMenu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryDescription by remember { mutableStateOf("") }
    var categoryToDescribe by remember { mutableStateOf<String?>(null) }
    var editedCategoryDescription by remember { mutableStateOf("") }
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
    fun categoryDescription(name: String): String = categoryPrefs.getString("description_$name", "") ?: ""

    fun refreshNotes() {
        notes.clear()
        notes.addAll(database.allNotes())
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
        modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (showArchived) "Arkib Nota" else "Nota Saya", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
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

        var notesCategoryExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            Surface(modifier = Modifier.fillMaxWidth().clickable { notesCategoryExpanded = true },
                shape = RoundedCornerShape(10.dp), color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Kategori: $selectedCategory", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("⌄", fontSize = 20.sp, color = Color(0xFF087F8C))
                }
            }
            DropdownMenu(expanded = notesCategoryExpanded, onDismissRequest = { notesCategoryExpanded = false }) {
                (listOf("Semua") + allCategories).forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = {
                        selectedCategory = category
                        notesCategoryExpanded = false
                    })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("+ Tambah kategori") }, onClick = {
                    notesCategoryExpanded = false
                    showAddCategory = true
                })
                DropdownMenuItem(text = { Text("Urus kategori") }, onClick = {
                    notesCategoryExpanded = false
                    showManageCategories = true
                })
            }
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFB7DCD7))
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
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { showAddCategory = false; newCategoryName = ""; newCategoryDescription = "" },
            title = { Text("Tambah kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nama kategori") },
                    singleLine = true
                )
                OutlinedTextField(value = newCategoryDescription, onValueChange = { newCategoryDescription = it }, label = { Text("Deskripsi sendiri (pilihan)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    if (name.isNotEmpty() && name !in builtInCategories && name !in customCategories) {
                        customCategories.add(name)
                        context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                            .edit().putStringSet("custom", customCategories.toSet()).putString("description_$name", newCategoryDescription.trim()).apply()
                        selectedCategory = name
                    }
                    newCategoryName = ""
                    newCategoryDescription = ""
                    showAddCategory = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false; newCategoryName = ""; newCategoryDescription = "" }) { Text("Batal") }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
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
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { showManageCategories = false },
            title = { Text("Urus kategori nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) { Text(category); val description = categoryDescription(category); if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { categoryToDescribe = category; editedCategoryDescription = categoryDescription(category); showManageCategories = false }) { Text("Deskripsi") }
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

    categoryToDescribe?.let { name ->
        AlertDialog(shape = RoundedCornerShape(10.dp), containerColor = Color.White, tonalElevation = 0.dp, titleContentColor = Color(0xFF075B59), textContentColor = Color(0xFF263B3E), onDismissRequest = { categoryToDescribe = null }, title = { Text("Deskripsi: $name") }, text = { OutlinedTextField(value = editedCategoryDescription, onValueChange = { editedCategoryDescription = it }, label = { Text("Deskripsi sendiri (pilihan)") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { categoryPrefs.edit().putString("description_$name", editedCategoryDescription.trim()).apply(); categoryToDescribe = null }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { categoryToDescribe = null }) { Text("Batal") } })
    }

    categoryToRename?.let { oldName ->
        AlertDialog(
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
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
                        categoryPrefs.edit().putString("description_$newName", categoryDescription(oldName)).remove("description_$oldName").apply()
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
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Padam kategori?") },
            text = {
                Text("Kategori \"$category\" akan dipadam. Nota di dalamnya tidak akan hilang; semuanya dipindahkan ke kategori Lain-lain.")
            },
            confirmButton = {
                TextButton(onClick = {
                    database.moveCategoryToDefault(category)
                    customCategories.remove(category)
                    categoryPrefs.edit().remove("description_$category").apply()
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
            Surface(modifier = Modifier.fillMaxSize().padding(3.dp), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFB7DCD7)), color = readingBackground(editorBackground)) {
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
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                body.text.split("\n").forEach { line ->
                                    val arabic = line.any { it in '\u0600'..'\u06FF' }
                                    Text(formattedNoteText(line), modifier = Modifier.fillMaxWidth(), textAlign = if (arabic) TextAlign.Right else TextAlign.Left,
                                        style = androidx.compose.ui.text.TextStyle(textDirection = if (arabic) TextDirection.Rtl else TextDirection.Ltr),
                                        fontSize = editorFontSize.sp, lineHeight = (editorFontSize * displayPrefs.getFloat("line_spacing", 1.6f)).sp,
                                        color = if (editorBackground == "dark") Color.White else Color(0xFF263B3E))
                                }
                            }
                        }
                    } else {
                    Surface(color = Color(0xFF006E70), shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp), border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 5.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { persistNote(); showEditor = false }) {
                                Text("←", color = Color.White, fontSize = 25.sp)
                            }
                            Text(
                                if (editingId == null) "✎  Catatan Baharu" else "✎  Edit Catatan",
                                modifier = Modifier.weight(1f),
                                color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1
                            )
                            TextButton(enabled = undoStack.isNotEmpty(), onClick = {
                                if (undoStack.isNotEmpty()) { redoStack.add(body); body = undoStack.removeAt(undoStack.lastIndex) }
                            }) { Text("↶", color = Color.White, fontSize = 22.sp) }
                            TextButton(enabled = redoStack.isNotEmpty(), onClick = {
                                if (redoStack.isNotEmpty()) { undoStack.add(body); body = redoStack.removeAt(redoStack.lastIndex) }
                            }) { Text("↷", color = Color.White, fontSize = 22.sp) }
                            TextButton(onClick = { persistNote(); readingMode = true }) {
                                Text("✓", color = Color(0xFFFFD56A), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            java.text.SimpleDateFormat("dd MMM yyyy • h:mm a", java.util.Locale("ms", "MY"))
                                .format(java.util.Date()),
                            color = Color.Gray, fontSize = 13.sp
                        )
                        var categoryExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { categoryExpanded = true }) {
                                Text("◇  Kategori: $selectedCategory  ▾", color = Color(0xFF006E70), fontSize = 13.sp)
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
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tools = listOf("Tᵀ" to "", "${editorFontSize.toInt()}" to "", "B" to "**", "I" to "_", "U" to "__", "A" to "**", "✎" to "✍ ", "☰" to "• ", "⋯" to "1. ", "☑" to "☐ ", "▧" to "▧ ", "📎" to "📎 ")
                        tools.forEach { (label, insertion) ->
                            TextButton(onClick = {
                                if (label == editorFontSize.toInt().toString()) {
                                    editorFontSize = if (editorFontSize >= 30f) 14f else editorFontSize + 2f
                                    displayPrefs.edit().putInt("font_size", editorFontSize.toInt()).apply()
                                } else if (label == "Tᵀ") {
                                    editorFontSize = if (editorFontSize <= 14f) 24f else editorFontSize - 2f
                                    displayPrefs.edit().putInt("font_size", editorFontSize.toInt()).apply()
                                } else if (insertion.isNotEmpty()) {
                                    val a = body.selection.min
                                    val b = body.selection.max
                                    val selected = body.text.substring(a, b)
                                    val newText = if (insertion == "**" || insertion == "_" || insertion == "__") insertion + selected + insertion else insertion + selected
                                    val result = body.text.replaceRange(a, b, newText)
                                    undoStack.add(body); redoStack.clear()
                                    body = TextFieldValue(result, selection = TextRange(a + newText.length))
                                }
                            }) { Text(label, color = Color(0xFF087F83), fontSize = 19.sp) }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFB7DCD7))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            // Islamic ornamental frame, turquoise waves and mosque skyline.
                            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                                val w = size.width
                                val h = size.height
                                val gold = Color(0xFFD5AD55)
                                val teal = Color(0xFF087F83)
                                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                // Corner ornaments remain outside the writing area.
                                for (right in listOf(false, true)) {
                                    val x = if (right) w else 0f
                                    val direction = if (right) -1f else 1f
                                    val corner = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(x, 70.dp.toPx())
                                        quadraticBezierTo(x + direction * 10.dp.toPx(), 20.dp.toPx(),
                                            x + direction * 70.dp.toPx(), 0f)
                                        moveTo(x, 43.dp.toPx())
                                        quadraticBezierTo(x + direction * 8.dp.toPx(), 9.dp.toPx(),
                                            x + direction * 43.dp.toPx(), 0f)
                                    }
                                    drawPath(corner, gold, style = stroke)
                                    drawCircle(teal, radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(x + direction * 22.dp.toPx(), 18.dp.toPx()))
                                }
                                val base = h - 4.dp.toPx()
                                val wave = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, base - 40.dp.toPx())
                                    cubicTo(w * 0.25f, base + 8.dp.toPx(), w * 0.35f, base - 65.dp.toPx(), w * 0.58f, base - 30.dp.toPx())
                                    cubicTo(w * 0.76f, base - 5.dp.toPx(), w * 0.88f, base - 68.dp.toPx(), w, base - 47.dp.toPx())
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(wave, Color(0xFFBCE5E3).copy(alpha = 0.5f))
                                val foreground = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, base - 13.dp.toPx())
                                    cubicTo(w * 0.28f, base - 42.dp.toPx(), w * 0.48f, base + 8.dp.toPx(), w * 0.69f, base - 18.dp.toPx())
                                    cubicTo(w * 0.85f, base - 42.dp.toPx(), w * 0.93f, base - 12.dp.toPx(), w, base - 30.dp.toPx())
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(foreground, teal)
                                // Subtle mosque domes at the foot of the page.
                                for (i in 0..8) {
                                    val cx = w * (i + 0.5f) / 9f
                                    val half = w / 24f
                                    val y = base - 46.dp.toPx() - (if (i == 5) 16.dp.toPx() else 0f)
                                    val dome = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(cx - half, base - 19.dp.toPx())
                                        lineTo(cx - half, y + 12.dp.toPx())
                                        quadraticBezierTo(cx - half, y + 2.dp.toPx(), cx, y)
                                        quadraticBezierTo(cx + half, y + 2.dp.toPx(), cx + half, y + 12.dp.toPx())
                                        lineTo(cx + half, base - 19.dp.toPx())
                                        close()
                                    }
                                    drawPath(dome, Color(0xFF73BAB9).copy(alpha = 0.35f))
                                }
                                drawLine(gold, androidx.compose.ui.geometry.Offset(0f, base - 10.dp.toPx()),
                                    androidx.compose.ui.geometry.Offset(w, base - 10.dp.toPx()), 1.dp.toPx())
                            }
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF087F83),
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (title.isEmpty()) Text("✎  Tajuk catatan", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF087F83))
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
                                    Text(" ۞ ", color = Color(0xFFC9A24A), fontSize = 21.sp)
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
                                }
                                Spacer(Modifier.height(12.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = body,
                                    onValueChange = { next ->
                                        if (next.text != body.text) {
                                            undoStack.add(body)
                                            if (undoStack.size > 60) undoStack.removeAt(0)
                                            redoStack.clear()
                                        }
                                        body = next
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = editorFontSize.sp,
                                        color = Color(0xFF263B3E),
                                        lineHeight = (editorFontSize * 1.5f).sp,
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (body.text.isEmpty()) Text(
                                                "✎  Tulis catatan anda di sini...",
                                                fontSize = 16.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = Color.Gray
                                            )
                                            inner()
                                        }
                                    }
                                )
                            }
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
    var selectedCategory by remember { mutableStateOf("Pelbagai") }
    var selectedEntry by remember { mutableStateOf<KalimahEntry?>(null) }
    var search by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val libraryPrefs = remember { context.getSharedPreferences("dinnote_library_edits", android.content.Context.MODE_PRIVATE) }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    val builtinLibraryCategories = listOf("Pelbagai", "Kitab-Kitab", "Kalam Hikmah")
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
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Maktabah", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))


        var libraryCategoryExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { libraryCategoryExpanded = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFB7DCD7))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("▤", fontSize = 19.sp, color = Color(0xFF087F8C))
                    Spacer(Modifier.width(9.dp))
                    Text("Kategori: $selectedCategory", modifier = Modifier.weight(1f),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF20262B))
                    VerticalDivider(modifier = Modifier.height(23.dp), color = Color(0xFFB7C0C8))
                    Spacer(Modifier.width(10.dp))
                    Text("⌄", fontSize = 20.sp, color = Color(0xFF20262B))
                }
            }
            DropdownMenu(expanded = libraryCategoryExpanded,
                onDismissRequest = { libraryCategoryExpanded = false }) {
                allLibraryCategories.forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = {
                        selectedCategory = category
                        search = ""
                        libraryCategoryExpanded = false
                    })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("+ Tambah kategori") }, onClick = {
                    libraryCategoryExpanded = false
                    newCategoryName = ""
                    showAddCategory = true
                })
            }
        }
        if (selectedCategory in customLibraryCategories) {
            TextButton(onClick = { categoryToRename = selectedCategory; newCategoryName = selectedCategory }) {
                Text("✎ Ubah nama kategori")
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Carian", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { searchExpanded = !searchExpanded }) {
                Text(if (searchExpanded) "⌃ Tutup carian" else "⌕ Buka carian", fontSize = 13.sp)
            }
        }
        if (searchExpanded) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari dalam " + selectedCategory, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                trailingIcon = {
                    if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("✕") }
                }
            )
        }

        when (selectedCategory) {
            "Pelbagai" -> {
                Text("الكلمات الطيبة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = 22.sp)

                Text("${filteredEntries.size} kalimah tersedia", color = MaterialTheme.colorScheme.primary)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(filteredEntries, key = { it.title }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedEntry = entry },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFB7DCD7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(entry.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(entry.arabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = 25.sp)
                                Text(entry.transliteration, fontWeight = FontWeight.Medium)
                                Text(entry.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tekan untuk maklumat lanjut", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                IslamicCardFooter()
                            }
                        }
                    }
                }
            }
            "Kitab-Kitab" -> {
                Text("📚 Maktabah Kitab", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Button(onClick = { openLibraryEditor("entry_Kitab-Kitab_" + System.currentTimeMillis()) }) { Text("+ Tambah Kitab") }
                libraryItems.filter { (libraryPrefs.getString(it + "_title", "") ?: "").contains(search, true) ||
                    (libraryPrefs.getString(it + "_body", "") ?: "").contains(search, true) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, border = BorderStroke(1.dp, Color(0xFFB7DCD7)), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text((libraryPrefs.getString(key + "_body", "") ?: "").take(130), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3)
                            IslamicCardFooter()
                        }
                    }
                }
            }
            else -> {
                Text("✨ Kalam Hikmah", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Button(onClick = { openLibraryEditor("entry_" + selectedCategory + "_" + System.currentTimeMillis()) }) { Text("+ Tambah Kandungan") }
                libraryItems.filter { (libraryPrefs.getString(it + "_title", "") ?: "").contains(search, true) ||
                    (libraryPrefs.getString(it + "_body", "") ?: "").contains(search, true) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, border = BorderStroke(1.dp, Color(0xFFB7DCD7)), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text((libraryPrefs.getString(key + "_body", "") ?: "").take(130), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3)
                            IslamicCardFooter()
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory || categoryToRename != null) {
        AlertDialog(
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
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
            Surface(Modifier.fillMaxSize().padding(3.dp), shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFB7DCD7)), color = Color.White) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    UnifiedEditorHeading("Catatan Maktabah",
                        onBack = { saveLibraryEditor(); editingLibraryKey = null },
                        onSave = { saveLibraryEditor(); editingLibraryKey = null })
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("B" to "**", "I" to "_", "U" to "__", "S" to "~~",
                            "•" to "• ", "1." to "1. ", "☑" to "☐ ", "✎" to "✍ ").forEach { (label, marker) ->
                            TextButton(onClick = {
                                val a = libraryBody.selection.min
                                val b = libraryBody.selection.max
                                val selected = libraryBody.text.substring(a, b)
                                val insertion = if (marker in listOf("**", "_", "__", "~~"))
                                    marker + selected + marker else marker + selected
                                libraryBody = TextFieldValue(libraryBody.text.replaceRange(a, b, insertion),
                                    selection = TextRange(a + insertion.length))
                            }) { Text(label, color = Color(0xFF087F83), fontSize = 17.sp) }
                        }
                    }
                    Surface(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp), color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                        Box(Modifier.fillMaxSize()) {
                            UnifiedEditorMosqueFooter(Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
                            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = libraryTitle, onValueChange = { libraryTitle = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold, color = Color(0xFF087F83),
                                        textDirection = TextDirection.ContentOrRtl),
                                    decorationBox = { inner ->
                                        Box {
                                            if (libraryTitle.isEmpty()) Text("✎  Tajuk catatan",
                                                color = Color(0xFF087F83), fontSize = 18.sp)
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(12.dp))
                                UnifiedEditorDivider()
                                Spacer(Modifier.height(12.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = libraryBody, onValueChange = { libraryBody = it },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp,
                                        lineHeight = 24.sp, color = Color(0xFF263B3E),
                                        textDirection = TextDirection.ContentOrRtl),
                                    decorationBox = { inner ->
                                        Box {
                                            if (libraryBody.text.isEmpty()) Text(
                                                "✎  Tulis catatan anda di sini...",
                                                color = Color.Gray, fontSize = 16.sp)
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.arabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = 27.sp)
                    Text(entry.transliteration, fontWeight = FontWeight.Bold)
                    Text(entry.meaning)
                    Text("Rujukan: ${entry.reference}", fontSize = 13.sp)
                    Text(entry.note, fontSize = 13.sp)
                    IslamicCardFooter()
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { selectedEntry = null }, shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFB7DCD7))) { Text("Tutup", color = Color(0xFF087F83)) }
            }
        )
    }
}

@Composable
private fun UnifiedEditorHeading(title: String, onBack: () -> Unit, onSave: () -> Unit) {
    Surface(color = Color(0xFF006E70), shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 24.sp) }
            Text("✎  " + title, modifier = Modifier.weight(1f), fontSize = 15.sp,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onSave) { Text("✓", color = Color(0xFFFFD56A), fontSize = 27.sp) }
        }
    }
}

@Composable
private fun UnifiedEditorMosqueFooter(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(82.dp)) {
        val w = size.width
        val h = size.height
        val teal = Color(0xFF008F93)
        val gold = Color(0xFFD5AD55)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        drawRect(Color(0xFFEAF8F7).copy(alpha = 0.65f))
        for (i in 0..10) {
            val cx = w * (i + 0.5f) / 11f
            val half = w / 25f
            val peak = if (i % 3 == 0) h * 0.13f else h * 0.33f
            val arch = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - half, h)
                lineTo(cx - half, h * 0.57f)
                quadraticBezierTo(cx - half, peak + h * 0.08f, cx, peak)
                quadraticBezierTo(cx + half, peak + h * 0.08f, cx + half, h * 0.57f)
                lineTo(cx + half, h)
            }
            drawPath(arch, teal.copy(alpha = 0.26f), style = stroke)
        }
        val wave = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.84f)
            quadraticBezierTo(w * 0.23f, h * 0.57f, w * 0.48f, h * 0.83f)
            quadraticBezierTo(w * 0.72f, h * 1.02f, w, h * 0.68f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(wave, teal.copy(alpha = 0.68f))
        drawPath(wave, gold.copy(alpha = 0.85f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun UnifiedEditorDivider() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
        Text(" ۞ ", color = Color(0xFFC9A24A), fontSize = 21.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
    }
}

@Composable
private fun IslamicCardFooter() {
    // Shared compact Islamic ruler divider: 75% opacity, centered and flush to the card bottom.
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth().height(24.dp)
    ) {
        val w = size.width
        val h = size.height
        val teal = Color(0xFF008F93).copy(alpha = 0.75f)
        val gold = Color(0xFFD4A443).copy(alpha = 0.75f)
        val center = w / 2f
        val half = w * 0.23f
        val y = h - 1.dp.toPx()
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        drawLine(teal, androidx.compose.ui.geometry.Offset(center-half, y),
            androidx.compose.ui.geometry.Offset(center+half, y), strokeWidth = 1.dp.toPx())
        drawLine(gold, androidx.compose.ui.geometry.Offset(center-half*0.88f, y-4.dp.toPx()),
            androidx.compose.ui.geometry.Offset(center+half*0.88f, y-4.dp.toPx()),
            strokeWidth = 1.dp.toPx())
        for (i in -5..5) {
            val x = center + i * half / 6f
            val tickHeight = if (i % 2 == 0) 6.dp.toPx() else 3.dp.toPx()
            drawLine(teal, androidx.compose.ui.geometry.Offset(x,y),
                androidx.compose.ui.geometry.Offset(x,y-tickHeight), strokeWidth = 1.dp.toPx())
        }
        val diamond = androidx.compose.ui.graphics.Path().apply {
            moveTo(center, y-15.dp.toPx())
            lineTo(center+7.dp.toPx(), y-8.dp.toPx())
            lineTo(center, y-1.dp.toPx())
            lineTo(center-7.dp.toPx(), y-8.dp.toPx())
            close()
        }
        drawPath(diamond, gold, style = stroke)
        drawCircle(teal, radius = 2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(center,y-8.dp.toPx()))
    }
}
