
package com.penandpaper.app.ui.list
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penandpaper.domain.model.Folder
import com.penandpaper.domain.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repo: NotesRepository
) : ViewModel() {
    val folders: StateFlow<List<Folder>> =
        repo.observeFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    suspend fun addFolder(name: String) { repo.upsertFolder(Folder(0, null, name, null, sortIndex = folders.value.size)) }
    suspend fun deleteFolderCascade(id: Long) { repo.deleteFolderCascade(id) }
    suspend fun updateFolder(folder: Folder) { repo.updateFolder(folder) }
    fun reorderUp(id: Long) = viewModelScope.launch {
        val ids = folders.value.map { it.id }.toMutableList()
        val idx = ids.indexOf(id)
        if (idx > 0) {
            ids[idx] = ids[idx-1].also { ids[idx-1] = ids[idx] }
            repo.reorderFolders(ids)
        }
    }
    fun reorderDown(id: Long) = viewModelScope.launch {
        val ids = folders.value.map { it.id }.toMutableList()
        val idx = ids.indexOf(id)
        if (idx >= 0 && idx < ids.lastIndex) {
            ids[idx] = ids[idx+1].also { ids[idx+1] = ids[idx] }
            repo.reorderFolders(ids)
        }
    }
}
