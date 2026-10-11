package com.penandpaper.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penandpaper.domain.model.Note
import com.penandpaper.domain.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val repo: NotesRepository
) : ViewModel() {
    private val folderId = MutableStateFlow<Long?>(null)

    val notes: StateFlow<List<Note>> =
        folderId.flatMapLatest { fid -> repo.observeNotes(folderId = fid) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFolder(id: Long?) {
        folderId.value = id
    }

    suspend fun createNote(): Long {
        val now = System.currentTimeMillis()
        return repo.upsertNote(Note(0, "Untitled", now, now, false, false, folderId.value))
    }
}
