package com.dinnote.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

private data class KitabItem(val id: Long, val title: String, val author: String, val content: String, val category: String, val favorite: Boolean)

@Composable
fun KitabCatalogScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dinnote_kitab_catalog", Context.MODE_PRIVATE) }
    val books = remember {
        mutableStateListOf<KitabItem>().apply {
            runCatching {
                val array = JSONArray(prefs.getString("books", "[]"))
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(KitabItem(o.getLong("id"), o.optString("title"), o.optString("author"),
                        o.optString("content"), o.optString("category", "Umum"), o.optBoolean("favorite")))
                }
            }
        }
    }
    fun persist() {
        val array = JSONArray()
        books.forEach { b ->
            array.put(JSONObject().put("id", b.id).put("title", b.title).put("author", b.author)
                .put("content", b.content).put("category", b.category).put("favorite", b.favorite))
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
    var ascending by remember { mutableStateOf(true) }
    fun startEdit(item: KitabItem?) {
        editing = item
        title = item?.title.orEmpty()
        author = item?.author.orEmpty()
        content = TextFieldValue(item?.content.orEmpty())
        category = item?.category ?: "Umum"
        editorVisible = true
    }
    if (opened != null) {
        val item = opened!!
        Column(modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { opened = null }) { Text("‹ Kembali") }
                TextButton(onClick = { startEdit(item) }) { Text("Edit") }
            }
            Text(item.title, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Text(item.author, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            LazyColumn { item { Text(item.content, fontSize = 22.sp, lineHeight = 38.sp, style = TextStyle(textDirection = TextDirection.ContentOrRtl),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) } }
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
                listOf("Semua", "Kegemaran", "Kategori").forEach { name ->
                    FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) })
                }
            }
            val results = books.filter { b ->
                (filter != "Kegemaran" || b.favorite) &&
                    (b.title.contains(query, true) || b.author.contains(query, true) ||
                        b.category.contains(query, true) || b.content.contains(query, true)) &&
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
                            TextButton(onClick = { opened = b }) { Text("Baca penuh") }
                        }
                    }
                    }
                }
            }
        }
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
                                    title, author, content.text, category, old?.favorite ?: false)
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
                    BasicTextField(
                        value = content, onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        textStyle = TextStyle(fontSize = 19.sp, lineHeight = 29.sp,
                            color = Color(0xFF263B3E), textDirection = TextDirection.ContentOrRtl),
                        decorationBox = { inner ->
                            Box {
                                if (content.text.isEmpty()) Text("Tulis kandungan Arab atau Melayu...",
                                    color = Color.Gray)
                                inner()
                            }
                        }
                    )
                }
            }
        }
    }
}
