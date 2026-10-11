package com.penandpaper.data.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val folderId: Long? = null
)

@Entity(tableName = "note_blocks")
data class NoteBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val type: String, // "text" | "image" | "scan"
    val sortIndex: Int = 0,
    val payloadRef: String? = null,
    val metaJson: String? = null
)
