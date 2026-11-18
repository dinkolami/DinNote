package com.dinnote.app

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

data class SmartReferenceEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val aliases: List<String>,
    val description: String,
    val source: String = ""
)

data class SmartReferenceMatch(val start: Int, val end: Int, val entry: SmartReferenceEntry)

/** Local reference catalogue; does not modify original note content. */
class SmartReferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("smart_reference", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    fun entries(): List<SmartReferenceEntry> {
        val raw = prefs.getString("entries", null) ?: return initialEntries()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val aliases = o.optJSONArray("aliases") ?: JSONArray()
                SmartReferenceEntry(o.getString("id"), o.getString("title"),
                    o.getString("category"), (0 until aliases.length()).map { aliases.getString(it) },
                    o.getString("description"), o.optString("source"))
            }
        }.getOrElse { emptyList() }
    }

    fun save(entries: List<SmartReferenceEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(JSONObject().put("id", e.id).put("title", e.title)
                .put("category", e.category).put("aliases", JSONArray(e.aliases))
                .put("description", e.description).put("source", e.source))
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    fun upsert(entry: SmartReferenceEntry) {
        require(entry.title.isNotBlank() && entry.description.isNotBlank())
        save(entries().filterNot { it.id == entry.id } + entry)
    }

    fun delete(id: String) = save(entries().filterNot { it.id == id })

    private fun initialEntries() = listOf(
        SmartReferenceEntry("imam-shafii", "Imam al-Syafi‘i", "Ulama",
            listOf("Imam Syafie", "Imam al-Syafii", "الإمام الشافعي", "محمد بن إدريس الشافعي", "الشافعي"),
            "Muhammad bin Idris al-Syafi‘i (150–204H), pengasas mazhab Syafi‘i. Antara karya beliau ialah al-Umm dan al-Risalah.",
            "Siyar A‘lam al-Nubala’, biografi al-Syafi‘i"),
        SmartReferenceEntry("al-umm", "al-Umm", "Kitab", listOf("الأم", "كتاب الأم"),
            "Kitab fiqh karya Imam al-Syafi‘i.")
    )
}

/** Accent-insensitive matching with offsets mapped back to the original text. */
object SmartReferenceMatcher {
    private data class Folded(val text: String, val starts: List<Int>, val ends: List<Int>)
    private fun fold(source: String): Folded {
        val text = StringBuilder()
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        var offset = 0
        while (offset < source.length) {
            val cp = source.codePointAt(offset)
            val end = offset + Character.charCount(cp)
            val part = Normalizer.normalize(String(Character.toChars(cp)), Normalizer.Form.NFD)
                .lowercase(Locale.ROOT)
            var added = false
            part.forEach { c ->
                val kind = Character.getType(c)
                if (kind != Character.NON_SPACING_MARK.toInt() &&
                    kind != Character.COMBINING_SPACING_MARK.toInt() &&
                    kind != Character.ENCLOSING_MARK.toInt() && c != 'ـ') {
                    text.append(c)
                    starts.add(offset)
                    ends.add(end)
                    added = true
                }
            }
            if (!added && ends.isNotEmpty()) ends[ends.lastIndex] = end
            offset = end
        }
        return Folded(text.toString(), starts, ends)
    }

    fun find(source: String, entries: List<SmartReferenceEntry>): List<SmartReferenceMatch> {
        val folded = fold(source)
        val candidates = mutableListOf<SmartReferenceMatch>()
        fun word(c: Char) = c.isLetterOrDigit() || c == '_'
        entries.forEach { entry ->
            (entry.aliases + entry.title).map { fold(it.trim()).text }.filter { it.isNotBlank() }.distinct().forEach { key ->
                var from = 0
                while (from < folded.text.length) {
                    val start = folded.text.indexOf(key, from)
                    if (start < 0) break
                    val end = start + key.length
                    if ((start == 0 || !word(folded.text[start - 1])) &&
                        (end == folded.text.length || !word(folded.text[end]))) {
                        candidates.add(SmartReferenceMatch(folded.starts[start], folded.ends[end - 1], entry))
                    }
                    from = start + 1
                }
            }
        }
        val result = mutableListOf<SmartReferenceMatch>()
        candidates.sortedWith(compareBy<SmartReferenceMatch> { it.start }.thenByDescending { it.end - it.start })
            .forEach { match -> if (result.lastOrNull()?.end?.let { it > match.start } != true) result.add(match) }
        return result
    }
}


@androidx.compose.runtime.Composable
fun SmartReferenceSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { SmartReferenceStore(context) }
    var enabled by remember { mutableStateOf(store.enabled) }
    var entries by remember { mutableStateOf(store.entries()) }
    var editing by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    var deleting by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    Text("Smart Reference — المرجع الذكي", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Aktifkan rujukan automatik")
        Switch(checked = enabled, onCheckedChange = { enabled = it; store.enabled = it })
    }
    OutlinedButton(onClick = { editing = SmartReferenceEntry(title="",category="Ulama",aliases=emptyList(),description="") }) { Text("+ Tambah rujukan") }
    entries.forEach { entry ->
        Row(Modifier.fillMaxWidth()) {
            Text(entry.title, Modifier.weight(1f))
            TextButton(onClick={editing=entry}) { Text("Edit") }
            TextButton(onClick={deleting=entry}) { Text("Padam") }
        }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest={deleting=null},title={Text("Padam rujukan?")},text={Text(entry.title)},
            confirmButton={TextButton(onClick={store.delete(entry.id);entries=store.entries();deleting=null}){Text("Padam")}},
            dismissButton={TextButton(onClick={deleting=null}){Text("Batal")}})
    }
    editing?.let { entry ->
        var title by remember(entry.id) { mutableStateOf(entry.title) }
        var category by remember(entry.id) { mutableStateOf(entry.category) }
        var aliases by remember(entry.id) { mutableStateOf(entry.aliases.joinToString("\n")) }
        var description by remember(entry.id) { mutableStateOf(entry.description) }
        var source by remember(entry.id) { mutableStateOf(entry.source) }
        AlertDialog(onDismissRequest={editing=null},title={Text("Rujukan ilmu")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(title,{title=it},label={Text("Nama utama")})
                OutlinedTextField(category,{category=it},label={Text("Kategori")})
                OutlinedTextField(aliases,{aliases=it},label={Text("Alias — satu setiap baris")})
                OutlinedTextField(description,{description=it},label={Text("Keterangan")},minLines=3)
                OutlinedTextField(source,{source=it},label={Text("Sumber")})
            }},confirmButton={TextButton(enabled=title.isNotBlank() && description.isNotBlank(),onClick={
                store.upsert(entry.copy(title=title.trim(),category=category,aliases=aliases.lines().map{it.trim()}.filter{it.isNotEmpty()},description=description,source=source))
                entries=store.entries();editing=null
            }){Text("Simpan")}},dismissButton={TextButton(onClick={editing=null}){Text("Batal")}})
    }
}

@Composable
fun ReferenceReaderText(text: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current, onDoubleTap: ((Int)->Unit)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { SmartReferenceStore(context) }
    val matches = remember(text,store) { if(store.enabled) SmartReferenceMatcher.find(text.text,store.entries()) else emptyList() }
    val marked = remember(text,matches) { buildAnnotatedString {
        append(text)
        matches.forEach { addStyle(SpanStyle(color=Color(0xFF087F65),textDecoration=TextDecoration.Underline),it.start,it.end) }
    } }
    var selected by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val editAction by rememberUpdatedState(onDoubleTap)
    Text(marked,modifier=modifier.pointerInput(marked,matches,onDoubleTap != null) {
        detectTapGestures(onTap={p ->
            layout?.let { l -> val i=l.getOffsetForPosition(p)
                if(i in 0 until marked.length && l.getBoundingBox(i).contains(p)) selected=matches.firstOrNull { i >= it.start && i < it.end }?.entry
            }
        },onDoubleTap=if(onDoubleTap==null) null else {p -> layout?.let { editAction?.invoke(it.getOffsetForPosition(p)) }})
    },style=style,onTextLayout={layout=it})
    selected?.let { entry ->
        AlertDialog(onDismissRequest={selected=null},title={Text(entry.title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(entry.category,color=Color(0xFF075B59));Text(entry.description)
            if(entry.source.isNotBlank()) Text("Sumber: "+entry.source)
        }},confirmButton={TextButton(onClick={selected=null}){Text("Tutup")}})
    }
}
