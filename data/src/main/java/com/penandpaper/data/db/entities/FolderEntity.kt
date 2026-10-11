package com.penandpaper.data.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long? = null,
    val name: String,
    val color: Long? = null,
    val sortIndex: Int = 0,
    val layout: String = "list",
    val iconSize: String = "medium",
    val contentMode: String = "auto",
    val preferLight: Boolean = false
)
