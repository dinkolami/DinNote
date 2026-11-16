package com.dinnote.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class DinNote(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val colorTag: String = "green",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val category: String = "Lain-lain"
)

class NoteDatabase(context: Context) :
    SQLiteOpenHelper(context, "dinnote_notes.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                color_tag TEXT NOT NULL DEFAULT 'green',
                is_pinned INTEGER NOT NULL DEFAULT 0,
                is_archived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN color_tag TEXT NOT NULL DEFAULT 'green'")
            db.execSQL("ALTER TABLE notes ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'Lain-lain'")
        }
    }

    fun allNotes(): List<DinNote> {
        val notes = mutableListOf<DinNote>()
        readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "updated_at", "color_tag", "is_pinned", "is_archived", "category"),
            null, null, null, null,
            "is_pinned DESC, updated_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                notes += DinNote(
                    id = cursor.getLong(0),
                    title = cursor.getString(1),
                    body = cursor.getString(2),
                    updatedAt = cursor.getLong(3),
                    colorTag = cursor.getString(4),
                    pinned = cursor.getInt(5) == 1,
                    archived = cursor.getInt(6) == 1,
                    category = cursor.getString(7)
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

    fun setColor(id: Long, colorTag: String) {
        writableDatabase.execSQL(
            "UPDATE notes SET color_tag = ?, updated_at = ? WHERE id = ?",
            arrayOf(colorTag, System.currentTimeMillis(), id)
        )
    }

    fun setPinned(id: Long, pinned: Boolean) {
        writableDatabase.execSQL(
            "UPDATE notes SET is_pinned = ?, updated_at = ? WHERE id = ?",
            arrayOf(if (pinned) 1 else 0, System.currentTimeMillis(), id)
        )
    }

    fun setArchived(id: Long, archived: Boolean) {
        writableDatabase.execSQL(
            "UPDATE notes SET is_archived = ?, updated_at = ? WHERE id = ?",
            arrayOf(if (archived) 1 else 0, System.currentTimeMillis(), id)
        )
    }

    fun setCategory(id: Long, category: String) {
        writableDatabase.execSQL(
            "UPDATE notes SET category = ?, updated_at = ? WHERE id = ?",
            arrayOf(category, System.currentTimeMillis(), id)
        )
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
