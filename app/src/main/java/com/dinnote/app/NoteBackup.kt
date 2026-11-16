package com.dinnote.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

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
                    )
                }
            })

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Fail sandaran tidak dapat dibuka.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(root.toString(2)) }
    }

    fun import(context: Context, uri: Uri, database: NoteDatabase): Int {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Fail sandaran tidak dapat dibaca.")
        val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        require(root.optString("format") == "DinNoteBackup") {
            "Fail ini bukan sandaran DinNote."
        }
        val entries = root.optJSONArray("notes") ?: JSONArray()
        var imported = 0

        for (index in 0 until entries.length()) {
            val item = entries.optJSONObject(index) ?: continue
            val title = item.optString("title")
            val body = item.optString("body")
            if (title.isBlank() && body.isBlank()) continue

            val id = database.save(title, body)
            database.setColor(id, item.optString("colorTag", "green"))
            database.setPinned(id, item.optBoolean("pinned", false))
            database.setArchived(id, item.optBoolean("archived", false))
            imported++
        }
        return imported
    }
}
