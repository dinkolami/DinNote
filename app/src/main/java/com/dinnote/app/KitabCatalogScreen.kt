package com.dinnote.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun normalizedKitabText(value: String): String =
    value.lowercase(java.util.Locale.ROOT)
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي').replace('ؤ', 'و').replace('ئ', 'ي')

private fun kitabParagraphIsArabic(value: String): Boolean {
    val firstLetter = value.firstOrNull { it.isLetter() } ?: return false
    return firstLetter in '\u0621'..'\u064A' || firstLetter in '\u066E'..'\u06D3'
}

private fun kitabMatches(value: String, query: String): Boolean =
    normalizedKitabText(value).contains(normalizedKitabText(query.trim()))

private fun formatKitabCitation(title: String, author: String, volume: String, page: String, chapter: String): String =
    listOfNotNull(
        title.trim().takeIf { it.isNotEmpty() },
        author.trim().takeIf { it.isNotEmpty() }?.let { "Pengarang: $it" },
        volume.trim().takeIf { it.isNotEmpty() }?.let { "Jilid: $it" },
        page.trim().takeIf { it.isNotEmpty() }?.let { "Halaman: $it" },
        chapter.trim().takeIf { it.isNotEmpty() }?.let { "Bab: $it" }
    ).joinToString(" | ")

private data class KitabItem(val id: Long, val title: String, val author: String, val content: String, val category: String, val favorite: Boolean, val volume: String = "", val page: String = "", val chapter: String = "")

@Composable
fun KitabCatalogScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dinnote_kitab_catalog", Context.MODE_PRIVATE) }
    val displayPrefs = remember { context.getSharedPreferences("dinnote_display", Context.MODE_PRIVATE) }
    val readerBackground = displayPrefs.getString("background", "white") ?: "white"
    val editorBackground = when (readerBackground) {
        "dark" -> Color(0xFF192B2C)
        "sepia" -> Color(0xFFF7F0DF)
        else -> Color.White
    }
    val editorInk = if (readerBackground == "dark") Color(0xFFF3F7F6) else Color(0xFF263B3E)
    val readerFontSize = displayPrefs.getInt("font_size", 19).coerceIn(14, 30)
    val readerLineSpacing = displayPrefs.getFloat("line_spacing", 1.6f).coerceIn(1.2f, 2.2f)
    val readerInk = if (readerBackground == "dark") Color.White else Color(0xFF263B3E)
    val readerTextAlignment = TextAlign.Start
    val readerBorder = if (readerBackground == "dark") Color(0xFF507875) else Color(0xFFB7DCD7)

    val books = remember {
        mutableStateListOf<KitabItem>().apply {
            runCatching {
                val array = JSONArray(prefs.getString("books", "[]"))
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(KitabItem(o.getLong("id"), o.optString("title"), o.optString("author"),
                        o.optString("content"), o.optString("category", "Umum"), o.optBoolean("favorite"),
                        o.optString("volume"), o.optString("page"), o.optString("chapter")))
                }
            }
        }
    }
    fun persist() {
        val array = JSONArray()
        books.forEach { b ->
            array.put(JSONObject().put("id", b.id).put("title", b.title).put("author", b.author)
                .put("content", b.content).put("category", b.category).put("favorite", b.favorite)
                .put("volume", b.volume).put("page", b.page).put("chapter", b.chapter))
        }
        prefs.edit().putString("books", array.toString()).apply()
    }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var opened by remember { mutableStateOf<KitabItem?>(null) }
    var editing by remember { mutableStateOf<KitabItem?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf("Umum") }
    var volume by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var ascending by remember { mutableStateOf(true) }
    var bookmarkRevision by remember { mutableIntStateOf(0) }
    fun isBookmarked(id: Long): Boolean {
        @Suppress("UNUSED_VARIABLE") val revision = bookmarkRevision
        return prefs.getBoolean("bookmark_$id", false)
    }
    fun toggleBookmark(id: Long) {
        prefs.edit().putBoolean("bookmark_$id", !isBookmarked(id)).apply()
        bookmarkRevision++
    }
    var highlightRevision by remember { mutableIntStateOf(0) }
    fun isHighlighted(id: Long, paragraph: Int): Boolean {
        @Suppress("UNUSED_VARIABLE") val revision = highlightRevision
        return prefs.getBoolean("highlight_${id}_$paragraph", false)
    }
    fun toggleHighlight(id: Long, paragraph: Int) {
        prefs.edit().putBoolean("highlight_${id}_$paragraph", !isHighlighted(id, paragraph)).apply()
        highlightRevision++
    }
    var copyFeedback by remember { mutableStateOf(false) }
    var excerptDialog by remember { mutableStateOf<KitabItem?>(null) }
    var excerptText by remember { mutableStateOf("") }
    var excerptStatus by remember { mutableStateOf("") }
    fun startEdit(item: KitabItem?) {
        editing = item
        title = item?.title.orEmpty()
        author = item?.author.orEmpty()
        content = TextFieldValue(item?.content.orEmpty())
        category = item?.category ?: "Umum"
        volume = item?.volume.orEmpty()
        page = item?.page.orEmpty()
        chapter = item?.chapter.orEmpty()
        editorVisible = true
    }
    if (opened != null) {
        val item = opened!!
        Column(modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { opened = null }) { Text("‹ Kembali") }
                Row {
                    TextButton(onClick = {
                        val citation = formatKitabCitation(item.title, item.author, item.volume, item.page, item.chapter)
                        val excerpt = item.content.trim()
                        val output = if (excerpt.isEmpty()) citation else "$excerpt\n\nRujukan: $citation"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Petikan kitab DinNote", output))
                        copyFeedback = true
                    }) { Text("Salin & rujukan") }
                    TextButton(onClick = {
                        excerptText = ""
                        excerptStatus = ""
                        excerptDialog = item
                    }) { Text("Nota Kuliah") }
                    TextButton(onClick = { toggleBookmark(item.id) }) {
                        Text(if (isBookmarked(item.id)) "★ Penanda" else "☆ Penanda")
                    }
                    TextButton(onClick = { startEdit(item) }) { Text("Edit") }
                }
            }
            if (copyFeedback) {
                Text("Teks kitab dan rujukan disalin. Boleh tampal ke Nota Kuliah.", fontSize = 12.sp, color = Color(0xFF075B59))
            }
            Text(item.title, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Text(item.author, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            if (item.volume.isNotBlank() || item.page.isNotBlank() || item.chapter.isNotBlank()) {
                Text(listOfNotNull(item.chapter.takeIf { it.isNotBlank() }?.let { "Bab: $it" },
                    item.volume.takeIf { it.isNotBlank() }?.let { "Jilid: $it" },
                    item.page.takeIf { it.isNotBlank() }?.let { "Halaman: $it" }).joinToString(" • "),
                    fontSize = 13.sp, color = Color(0xFF075B59))
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            val paragraphs = remember(item.id, item.content) {
                item.content.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
            }
            val readerListState = rememberLazyListState()
            val readerScope = rememberCoroutineScope()
            var savedParagraph by remember(item.id) {
                mutableIntStateOf(prefs.getInt("reading_position_${item.id}", 0)
                    .coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0)))
            }
            LaunchedEffect(item.id, paragraphs.size) {
                if (paragraphs.isNotEmpty() && savedParagraph > 0) {
                    val safeParagraph = savedParagraph.coerceAtMost(paragraphs.lastIndex)
                    if (safeParagraph != savedParagraph) {
                        savedParagraph = safeParagraph
                        prefs.edit().putInt("reading_position_${item.id}", safeParagraph).apply()
                    }
                    readerListState.scrollToItem(safeParagraph)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Perenggan: ${paragraphs.size}", fontSize = 12.sp, color = Color(0xFF075B59))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { readerScope.launch { readerListState.animateScrollToItem(savedParagraph) } },
                        enabled = paragraphs.isNotEmpty()) {
                        Text("Sambung (${savedParagraph + 1})", fontSize = 12.sp)
                    }
                    if (savedParagraph > 0) {
                        TextButton(onClick = {
                            prefs.edit().putInt("reading_position_${item.id}", 0).apply()
                            savedParagraph = 0
                            readerScope.launch { readerListState.animateScrollToItem(0) }
                        }) { Text("Mula semula", fontSize = 12.sp) }
                    }
                }
            }
            LazyColumn(state = readerListState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(paragraphs.size) { index ->
                    val highlighted = isHighlighted(item.id, index)
                    Surface(
                        color = if (highlighted) Color(0xFFFFF2BD) else readingBackground(readerBackground),
                        border = BorderStroke(1.dp, if (highlighted) Color(0xFFD9AF55) else readerBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = {
                                    prefs.edit().putInt("reading_position_${item.id}", index).apply()
                                    savedParagraph = index
                                }) { Text(if (savedParagraph == index) "✓ Tempat bacaan #${index + 1}" else "Simpan tempat baca #${index + 1}", fontSize = 12.sp) }
                                TextButton(onClick = { toggleHighlight(item.id, index) }) {
                                    Text(if (highlighted) "★ Buang highlight" else "☆ Highlight perenggan",
                                        fontSize = 12.sp)
                                }
                            }
                            SelectionContainer {
                                Text(paragraphs[index], fontSize = readerFontSize.sp,
                                    lineHeight = (readerFontSize * readerLineSpacing).sp,
                                    color = if (highlighted) Color(0xFF263B3E) else readerInk,
                                    style = TextStyle(textDirection = TextDirection.ContentOrRtl),
                                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                                    textAlign = if (kitabParagraphIsArabic(paragraphs[index])) TextAlign.Right else readerTextAlignment)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("أدخل اسم الكتاب... / Cari kitab") }, singleLine = true)
                TextButton(onClick = { ascending = !ascending }) { Text(if (ascending) "↑↓" else "↓↑") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { startEdit(null) }) { Text("＋ Tambah kitab") }
                Text("${books.size} kitab", modifier = Modifier.padding(12.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Semua", "Kegemaran", "Penanda", "Kategori").forEach { name ->
                    FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) })
                }
            }
            val results = books.filter { b ->
                (filter != "Kegemaran" || b.favorite) &&
                    (filter != "Penanda" || isBookmarked(b.id)) &&
                    (kitabMatches(b.title, query) || kitabMatches(b.author, query) ||
                        kitabMatches(b.category, query) || kitabMatches(b.content, query)) &&
                    (filter != "Kategori" || b.category.isNotBlank())
            }.let { if (ascending) it.sortedBy { b -> b.title } else it.sortedByDescending { b -> b.title } }
            LazyColumn {
                items(results, key = { it.id }) { b ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE1E3E5))
                    ) {
                    Column(Modifier.fillMaxWidth().clickable { opened = b }.padding(16.dp)) {
                        Text(b.title, fontSize = 23.sp, textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        Text("${b.author}  •  ${b.category}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Row {
                            TextButton(onClick = { startEdit(b) }) { Text("✎ Edit") }
                            TextButton(onClick = {
                                val idx = books.indexOfFirst { it.id == b.id }
                                if (idx >= 0) { books[idx] = b.copy(favorite = !b.favorite); persist() }
                            }) { Text(if (b.favorite) "★" else "☆") }
                            TextButton(onClick = { toggleBookmark(b.id) }) {
                                Text(if (isBookmarked(b.id)) "★ Penanda" else "☆ Penanda")
                            }
                            TextButton(onClick = { opened = b }) { Text("Baca penuh") }
                        }
                    }
                    }
                }
            }
        }
    }
    excerptDialog?.let { source ->
        AlertDialog(
            onDismissRequest = { excerptDialog = null },
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            title = { Text("Petikan untuk Nota Kuliah", color = Color(0xFF075B59)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih dan salin petikan daripada pembaca kitab, kemudian tampal di sini. Rujukan kitab akan dimasukkan secara automatik.", fontSize = 13.sp)
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = clipboard.primaryClip
                        val pasted = if (clip != null && clip.itemCount > 0)
                            clip.getItemAt(0).coerceToText(context).toString() else ""
                        if (pasted.isNotBlank()) excerptText = pasted
                    }) { Text("Tampal petikan disalin") }
                    OutlinedTextField(
                        value = excerptText,
                        onValueChange = { excerptText = it },
                        label = { Text("Teks petikan Arab / Melayu") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 240.dp)
                    )
                    Text(formatKitabCitation(source.title, source.author, source.volume, source.page, source.chapter),
                        fontSize = 12.sp, color = Color(0xFF075B59))
                    if (excerptStatus.isNotBlank()) Text(excerptStatus, color = Color(0xFF075B59), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = excerptText.isNotBlank(), onClick = {
                    val citation = formatKitabCitation(source.title, source.author, source.volume, source.page, source.chapter)
                    val body = excerptText.trim() + "\n\nRujukan: " + citation
                    excerptStatus = runCatching {
                        val db = NoteDatabase(context)
                        val id = db.save("Kajian: " + source.title, body)
                        db.setCategory(id, "Nota Kuliah")
                        db.close()
                        "Berjaya disimpan dalam Nota Kuliah."
                    }.getOrElse { "Gagal menyimpan: ${it.message}" }
                }) { Text("Simpan ke Nota") }
            },
            dismissButton = {
                OutlinedButton(onClick = { excerptDialog = null }) { Text("Tutup") }
            }
        )
    }
    if (editorVisible) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { editorVisible = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false, decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFD8DEE3))
            ) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { editorVisible = false }) { Text("← Kembali") }
                        Text(if (editing == null) "Tambah Kitab" else "Edit Kitab",
                            modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = {
                            if (title.isNotBlank()) {
                                val old = editing
                                val saved = KitabItem(old?.id ?: System.currentTimeMillis(),
                                    title, author, content.text, category, old?.favorite ?: false,
                                    volume.trim(), page.trim(), chapter.trim())
                                val index = books.indexOfFirst { it.id == saved.id }
                                if (index < 0) books.add(saved) else books[index] = saved
                                persist()
                                if (opened?.id == saved.id) opened = saved
                                editorVisible = false
                            }
                        }) { Text("Simpan") }
                    }
                    HorizontalDivider(color = Color(0xFFE1E3E5))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("B" to "**", "I" to "_", "U" to "__",
                            "S" to "~~", "•" to "• ", "1." to "1. ", "☑" to "☐ ").forEach { (label, marker) ->
                            TextButton(onClick = {
                                val a = content.selection.min
                                val b = content.selection.max
                                val chosen = content.text.substring(a, b)
                                val insertion = if (marker in listOf("**", "_", "__", "~~"))
                                    marker + chosen + marker else marker + chosen
                                content = TextFieldValue(content.text.replaceRange(a, b, insertion),
                                    selection = TextRange(a + insertion.length))
                            }) { Text(label) }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE1E3E5))
                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp), label = { Text("Nama kitab") }, singleLine = true)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(author, { author = it }, modifier = Modifier.weight(1f),
                            label = { Text("Pengarang") }, singleLine = true)
                        OutlinedTextField(category, { category = it }, modifier = Modifier.weight(1f),
                            label = { Text("Kategori") }, singleLine = true)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(volume, { volume = it }, modifier = Modifier.weight(1f),
                            label = { Text("Jilid") }, singleLine = true)
                        OutlinedTextField(page, { page = it }, modifier = Modifier.weight(1f),
                            label = { Text("Halaman") }, singleLine = true)
                    }
                    OutlinedTextField(chapter, { chapter = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        label = { Text("Bab / fasal") }, singleLine = true)
                    BasicTextField(
                        value = content, onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .border(BorderStroke(1.dp, readerBorder), RoundedCornerShape(10.dp))
                            .background(editorBackground, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        textStyle = TextStyle(fontSize = readerFontSize.sp,
                            lineHeight = (readerFontSize * readerLineSpacing).sp,
                            color = editorInk, textDirection = TextDirection.ContentOrRtl),
                        decorationBox = { inner ->
                            Box {
                                if (content.text.isEmpty()) Text("Tulis kandungan Arab atau Melayu...",
                                    color = if (readerBackground == "dark") Color(0xFFB8CECB) else Color(0xFF667A7B))
                                inner()
                            }
                        }
                    )
                }
            }
        }
    }
}
