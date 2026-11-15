package com.dinnote.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class DinNote(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long
)

class NoteDatabase(context: Context) :
    SQLiteOpenHelper(context, "dinnote_notes.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future database upgrades will be added here without deleting notes.
    }

    fun allNotes(): List<DinNote> {
        val notes = mutableListOf<DinNote>()
        readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "updated_at"),
            null, null, null, null,
            "updated_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                notes += DinNote(
                    id = cursor.getLong(0),
                    title = cursor.getString(1),
                    body = cursor.getString(2),
                    updatedAt = cursor.getLong(3)
                )
            }
        }
        return notes
    }

    fun save(title: String, body: String, id: Long? = null): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("updated_at", System.currentTimeMillis())
        }
        return if (id == null) {
            writableDatabase.insertOrThrow("notes", null, values)
        } else {
            writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
            id
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    fun migrateOldSingleNote(context: Context) {
        val prefs = context.getSharedPreferences("dinnote", Context.MODE_PRIVATE)
        if (prefs.getBoolean("sqlite_migrated", false)) return
        val oldTitle = prefs.getString("title", "").orEmpty()
        val oldBody = prefs.getString("note", "").orEmpty()
        if (oldTitle.isNotBlank() || oldBody.isNotBlank()) {
            save(oldTitle.ifBlank { "Nota lama" }, oldBody)
        }
        prefs.edit().putBoolean("sqlite_migrated", true).remove("title").remove("note").apply()
    }
}
