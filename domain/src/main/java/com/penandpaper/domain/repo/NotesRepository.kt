package com.penandpaper.domain.repo
import kotlinx.coroutines.flow.Flow
import com.penandpaper.domain.model.*

interface NotesRepository {
    fun observeNotes(query: String? = null, folderId: Long? = null, tagIds: List<Long>? = null): Flow<List<Note>>
    suspend fun upsertNote(note: Note): Long
    suspend fun getNote(id: Long): Note?
    suspend fun addBlock(block: NoteBlock): Long
    suspend fun updateBlock(block: NoteBlock)
    suspend fun deleteBlock(id: Long)
    suspend fun blocksForNote(noteId: Long): List<NoteBlock>

    // Folders
    fun observeFolders(): Flow<List<Folder>>
    suspend fun upsertFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolderCascade(folderId: Long)
    suspend fun moveNoteToFolder(noteId: Long, folderId: Long?)
    suspend fun reorderFolders(order: List<Long>)
}
