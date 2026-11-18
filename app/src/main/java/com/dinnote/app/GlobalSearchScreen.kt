package com.dinnote.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray

private fun cleanSearchText(value: String): String = formattedNoteText(value).text

private fun searchKey(value: String): String =
    java.text.Normalizer.normalize(cleanSearchText(value), java.text.Normalizer.Form.NFKC)
        .lowercase(java.util.Locale.ROOT)
        .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u0640\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7-\\u06E8\\u06EA-\\u06ED]"), "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')

private data class SearchHit(val source: String, val key: String, val title: String, val content: String) {
    val titleKey by lazy { searchKey(title) }
    val contentKey by lazy { searchKey(content) }
}

@Composable
fun GlobalSearchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { NoteDatabase(context).also { it.migrateOldSingleNote(context) } }
    val library = remember { context.getSharedPreferences("dinnote_library_edits", 0) }
    val catalog = remember { context.getSharedPreferences("dinnote_kitab_catalog", 0) }
    val favorites = remember { context.getSharedPreferences("dinnote_bookmarks", 0) }
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var openedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var indexRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(library, catalog) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> indexRevision++ }
        library.registerOnSharedPreferenceChangeListener(listener)
        catalog.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            library.unregisterOnSharedPreferenceChangeListener(listener)
            catalog.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val searchIndex = remember(database, library, catalog, indexRevision) {
        buildList {
            database.allNotes().forEach { note ->
                add(SearchHit("Nota", "note_${note.id}", note.title.ifBlank { "Tanpa tajuk" }, note.body))
            }
            library.all.keys.filter { it.startsWith("entry_") && it.endsWith("_title") }.forEach { titleKey ->
                val key = titleKey.removeSuffix("_title")
                add(SearchHit("Maktabah", key, library.getString(titleKey, "").orEmpty(),
                    library.getString(key + "_body", "").orEmpty()))
            }
            runCatching {
                val books = JSONArray(catalog.getString("books", "[]"))
                for (i in 0 until books.length()) {
                    val book = books.getJSONObject(i)
                    add(SearchHit("Kitab", "book_${book.optLong("id")}",
                        book.optString("title"), book.optString("content")))
                }
            }
            kalimahEntries.forEach { entry ->
                add(SearchHit("Pelbagai", "kalimah_${entry.title}", entry.title,
                    entry.arabic + "\n" + entry.meaning))
            }
        }
    }
    val hits = remember(searchIndex, query, favoritesOnly, revision) {
        val needle = searchKey(query.trim())
        searchIndex.filter { hit ->
            (!favoritesOnly || favorites.getBoolean(hit.key, false)) &&
                (needle.isBlank() && favoritesOnly || needle.isNotBlank() &&
                    (hit.titleKey.contains(needle) || hit.contentKey.contains(needle)))
        }.sortedBy { hit -> if (needle.isNotBlank() && hit.titleKey.contains(needle)) 0 else 1 }
    }
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Carian Pintar & Kegemaran", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Cari nota, kitab, hikmah dan kalimah") },
            trailingIcon = {
                if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Padam") }
            })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false },
                label = { Text("Semua") })
            FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true },
                label = { Text("★ Kegemaran") })
        }
        Text("${hits.size} hasil", style = MaterialTheme.typography.labelMedium)
        if (hits.isEmpty()) {
            Text(if (query.isBlank() && !favoritesOnly) "Taip kata kunci dalam bahasa Arab atau Melayu untuk mencari."
                else if (query.isBlank()) "Belum ada kegemaran. Tekan bintang pada hasil carian untuk menyimpan."
                else "Tiada hasil. Cuba kata kunci lain.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(hits, key = { "${it.source}:${it.key}" }) { hit ->
                Surface(modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp,
                        androidx.compose.ui.graphics.Color(0xFFD8DEE3))) {
                    Row(Modifier.fillMaxWidth().padding(10.dp)) {
                        Column(Modifier.weight(1f).clickable { openedKey = "${hit.source}:${hit.key}" }) {
                            Text(hit.source, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall)
                            Text(cleanSearchText(hit.title), fontWeight = FontWeight.SemiBold)
                            Text(cleanSearchText(hit.content), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        val isFavorite = favorites.getBoolean(hit.key, false)
                        TextButton(modifier = Modifier.semantics {
                            contentDescription = if (isFavorite) "Buang daripada kegemaran" else "Tambah ke kegemaran"
                        }, onClick = {
                            favorites.edit().putBoolean(hit.key,
                                !favorites.getBoolean(hit.key, false)).apply()
                            revision++
                        }) {
                            Text(if (isFavorite) "★" else "☆")
                        }
                    }
                }
            }
        }
    }
    searchIndex.firstOrNull { "${it.source}:${it.key}" == openedKey }?.let { hit ->
        AlertDialog(onDismissRequest = { openedKey = null },
            title = { Text(cleanSearchText(hit.title)) },
            text = {
                Column(Modifier.heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text(formattedNoteText(hit.content), modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Content),
                        textAlign = TextAlign.Start)
                }
            },
            confirmButton = { TextButton(onClick = { openedKey = null }) { Text("Tutup") } })
    }
}
