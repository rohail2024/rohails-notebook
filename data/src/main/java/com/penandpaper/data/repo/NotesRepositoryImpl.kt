package com.penandpaper.data.repo
import com.penandpaper.domain.repo.NotesRepository
import com.penandpaper.domain.model.*
import com.penandpaper.data.db.AppDb
import com.penandpaper.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepositoryImpl(
    private val db: AppDb
) : NotesRepository {
    override fun observeNotes(query: String?, folderId: Long?, tagIds: List<Long>?): Flow<List<Note>> =
        (if (folderId != null) db.notesDao().observeNotesInFolder(folderId) else db.notesDao().observeNotes())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun upsertNote(note: Note): Long = db.notesDao().upsert(note.toEntity())
    override suspend fun getNote(id: Long): Note? = db.notesDao().getNote(id)?.toDomain()
    override suspend fun addBlock(block: NoteBlock): Long = db.blocksDao().add(block.toEntity())
    override suspend fun updateBlock(block: NoteBlock) = db.blocksDao().update(block.toEntity())
    override suspend fun deleteBlock(id: Long) = db.blocksDao().delete(id)
    override suspend fun blocksForNote(noteId: Long): List<NoteBlock> =
        db.blocksDao().blocksForNote(noteId).map { it.toDomain() }

    override fun observeFolders(): Flow<List<Folder>> =
        db.foldersDao().observeFolders().map { it.map { e -> e.toDomain() } }
    override suspend fun upsertFolder(folder: Folder): Long = db.foldersDao().upsert(folder.toEntity())
    override suspend fun updateFolder(folder: Folder) = db.foldersDao().update(folder.toEntity())
    override suspend fun deleteFolderCascade(folderId: Long) {
        db.notesDao().deleteByFolder(folderId)
        db.foldersDao().delete(folderId)
    }
    override suspend fun moveNoteToFolder(noteId: Long, folderId: Long?) {
        val n = db.notesDao().getNote(noteId) ?: return
        db.notesDao().upsert(n.copy(folderId = folderId))
    }
    override suspend fun reorderFolders(order: List<Long>) {
        order.forEachIndexed { idx, id ->
            val f = db.foldersDao().get(id) ?: return@forEachIndexed
            db.foldersDao().update(f.copy(sortIndex = idx))
        }
    }
}

private fun NoteEntity.toDomain() = Note(id, title, createdAt, updatedAt, isPinned, isLocked, folderId)
private fun NoteBlockEntity.toDomain() = NoteBlock(id, noteId, BlockType.valueOf(type.uppercase()), sortIndex, payloadRef, metaJson)
private fun FolderEntity.toDomain() = Folder(id, parentId, name, color, sortIndex, layout, iconSize, contentMode, preferLight)
private fun Note.toEntity() = NoteEntity(id, title, createdAt, updatedAt, isPinned, isLocked, folderId)
private fun NoteBlock.toEntity() = NoteBlockEntity(id, noteId, type.name.lowercase(), sortIndex, payloadRef, metaJson)
private fun Folder.toEntity() = FolderEntity(id, parentId, name, color, sortIndex, layout, iconSize, contentMode, preferLight)
