package com.penandpaper.data.db
import androidx.room.Database
import androidx.room.RoomDatabase
import com.penandpaper.data.db.entities.*

@Database(
    entities = [NoteEntity::class, NoteBlockEntity::class, FolderEntity::class],
    version = 1
)
abstract class AppDb : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    abstract fun blocksDao(): BlocksDao
    abstract fun foldersDao(): FoldersDao
}
