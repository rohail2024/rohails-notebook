package com.penandpaper.data.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.penandpaper.data.db.entities.*

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun observeNotesInFolder(folderId: Long): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: Long): NoteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM notes WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)
}

@Dao
interface BlocksDao {
    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY sortIndex ASC")
    suspend fun blocksForNote(noteId: Long): List<NoteBlockEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(block: NoteBlockEntity): Long
    @Update suspend fun update(block: NoteBlockEntity)
    @Query("DELETE FROM note_blocks WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM note_blocks WHERE noteId = :noteId") suspend fun deleteByNote(noteId: Long)
}

@Dao
interface FoldersDao {
    @Query("SELECT * FROM folders ORDER BY sortIndex ASC, name ASC")
    fun observeFolders(): Flow<List<FolderEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity): Long
    @Update suspend fun update(folder: FolderEntity)
    @Query("DELETE FROM folders WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): FolderEntity?
}
