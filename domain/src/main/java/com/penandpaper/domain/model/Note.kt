package com.penandpaper.domain.model

data class Note(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val isLocked: Boolean,
    val folderId: Long?
)

data class NoteBlock(
    val id: Long,
    val noteId: Long,
    val type: BlockType,
    val sortIndex: Int,
    val payloadRef: String?,
    val metaJson: String?
)

enum class BlockType { TEXT, IMAGE, SCAN }
