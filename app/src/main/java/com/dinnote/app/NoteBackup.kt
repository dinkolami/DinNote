package com.dinnote.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class NoteImportResult(
    val imported: Int,
    val skippedDuplicates: Int
)

object NoteBackup {
    fun export(context: Context, uri: Uri, notes: List<DinNote>) {
        val root = JSONObject()
            .put("format", "DinNoteBackup")
            .put("version", 2)
            .put("preferences", exportPreferences(context))
            .put("notes", JSONArray().apply {
                notes.forEach { note ->
                    put(
                        JSONObject()
                            .put("title", note.title)
                            .put("body", note.body)
                            .put("updatedAt", note.updatedAt)
                            .put("colorTag", note.colorTag)
                            .put("pinned", note.pinned)
                            .put("archived", note.archived)
                            .put("category", note.category)
                    )
                }
            })

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Fail sandaran tidak dapat dibuka.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(root.toString(2)) }
    }

    fun import(context: Context, uri: Uri, database: NoteDatabase): NoteImportResult {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Fail sandaran tidak dapat dibaca.")
        val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        require(root.optString("format") == "DinNoteBackup") {
            "Fail ini bukan sandaran DinNote."
        }
        require(root.optInt("version", 0) in 1..2) {
            "Versi sandaran ini tidak disokong."
        }

        val existing = database.allNotes().map { note ->
            noteSignature(note.title, note.body, note.colorTag, note.pinned, note.archived, note.category)
        }.toMutableSet()
        val entries = root.optJSONArray("notes")
            ?: throw IllegalArgumentException("Senarai nota dalam fail sandaran tidak sah.")
        var imported = 0
        var skipped = 0

        for (index in 0 until entries.length()) {
            val item = entries.optJSONObject(index) ?: continue
            val title = item.optString("title")
            val body = item.optString("body")
            if (title.isBlank() && body.isBlank()) continue

            val color = item.optString("colorTag", "green")
                .takeIf { it in setOf("green", "yellow", "blue", "pink", "purple") }
                ?: "green"
            val pinned = item.optBoolean("pinned", false)
            val archived = item.optBoolean("archived", false)
            val category = item.optString("category", "Lain-lain").ifBlank { "Lain-lain" }
            val signature = noteSignature(title, body, color, pinned, archived, category)

            if (signature in existing) {
                skipped++
                continue
            }

            val id = database.save(title, body, colorTag = color, category = category)
            database.setPinned(id, pinned)
            database.setArchived(id, archived)
            existing.add(signature)
            imported++
        }
        if (root.optInt("version") >= 2) restorePreferences(context, root.optJSONObject("preferences"))
        return NoteImportResult(imported, skipped)
    }

    private val backupPreferenceFiles = listOf(
        "dinnote_categories", "dinnote_library_edits", "dinnote_kitab_catalog", "dinnote_display", "dinnote_bookmarks",
        "dinnote_smart_vault", "dinnote_smart_index", "dinnote_smart_recall", "dinnote_workspace"
    )

    private fun exportPreferences(context: Context): JSONObject = JSONObject().apply {
        backupPreferenceFiles.forEach { name ->
            val values = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                when (value) {
                    is String -> values.put(key, JSONObject().put("type", "string").put("value", value))
                    is Boolean -> values.put(key, JSONObject().put("type", "boolean").put("value", value))
                    is Int -> values.put(key, JSONObject().put("type", "int").put("value", value))
                    is Float -> values.put(key, JSONObject().put("type", "float").put("value", value.toDouble()))
                    is Long -> values.put(key, JSONObject().put("type", "long").put("value", value))
                    is Set<*> -> values.put(key, JSONObject().put("type", "strings")
                        .put("value", JSONArray(value.filterIsInstance<String>())))
                }
            }
            put(name, values)
        }
    }

    private fun restorePreferences(context: Context, preferences: JSONObject?) {
        if (preferences == null) return
        backupPreferenceFiles.forEach { name ->
            val values = preferences.optJSONObject(name) ?: return@forEach
            val store = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = store.edit()
            val keys = values.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = values.optJSONObject(key) ?: continue
                when (entry.optString("type")) {
                    "string" -> {
                        if (name == "dinnote_smart_vault" && key == "items") {
                            val incoming = JSONArray(entry.getString("value"))
                            val current = JSONArray(store.getString("items", "[]") ?: "[]")
                            val merged = linkedSetOf<String>()
                            for (i in 0 until current.length()) merged.add(current.getString(i))
                            for (i in 0 until incoming.length()) merged.add(incoming.getString(i))
                            editor.putString(key, JSONArray(merged.toList()).toString())
                        } else if (name == "dinnote_workspace" && key == "draft") {
                            val incoming = entry.optString("value")
                            val current = store.getString("draft", "").orEmpty()
                            if (incoming.isNotBlank() && incoming != current) {
                                val separator = "\n\n--- Draf daripada sandaran ---\n\n"
                                if (current.isBlank()) editor.putString(key, incoming)
                                else if (!current.endsWith(separator + incoming))
                                    editor.putString(key, current + separator + incoming)
                            }
                        } else if (name != "dinnote_smart_index" || !store.contains(key)) {
                            editor.putString(key, entry.optString("value"))
                        }
                    }
                    "boolean" -> editor.putBoolean(key, entry.optBoolean("value"))
                    "int" -> editor.putInt(key, entry.optInt("value"))
                    "float" -> editor.putFloat(key, entry.optDouble("value").toFloat())
                    "long" -> editor.putLong(key, if (name == "dinnote_smart_recall")
                        maxOf(store.getLong(key, 0L), entry.optLong("value")) else entry.optLong("value"))
                    "strings" -> {
                        val array = entry.optJSONArray("value") ?: JSONArray()
                        editor.putStringSet(key, (0 until array.length()).map { array.optString(it) }.toSet())
                    }
                }
            }
            editor.apply()
        }
    }

    private fun noteSignature(
        title: String,
        body: String,
        colorTag: String,
        pinned: Boolean,
        archived: Boolean,
        category: String
    ): String = listOf(title, body, colorTag, pinned.toString(), archived.toString(), category)
        .let { JSONArray(it).toString() }
}