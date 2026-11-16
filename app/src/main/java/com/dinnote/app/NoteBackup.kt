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
            .put("version", 1)
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
        require(root.optInt("version", 0) in 1..1) {
            "Versi sandaran ini tidak disokong."
        }

        val existing = database.allNotes().map { note ->
            noteSignature(note.title, note.body, note.colorTag, note.pinned, note.archived, note.category)
        }.toMutableSet()
        val entries = root.optJSONArray("notes") ?: JSONArray()
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

            val id = database.save(title, body)
            database.setColor(id, color)
            database.setPinned(id, pinned)
            database.setArchived(id, archived)
            database.setCategory(id, category)
            imported++
        }
        return NoteImportResult(imported, skipped)
    }

    private fun noteSignature(
        title: String,
        body: String,
        colorTag: String,
        pinned: Boolean,
        archived: Boolean,
        category: String
    ): String = listOf(title, body, colorTag, pinned.toString(), archived.toString(), category)
        .joinToString(separator = "\u001F")
}