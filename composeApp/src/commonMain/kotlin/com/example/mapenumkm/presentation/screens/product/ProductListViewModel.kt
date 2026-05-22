package com.example.mapenumkm.presentation.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapenumkm.domain.model.Note
import com.example.mapenumkm.domain.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val state: StateFlow<ProductListState> = combine(
        repository.getAllNotes(),
        _searchQuery
    ) { notes, query ->
        ProductListState(
            products = if (query.isEmpty()) {
                notes
            } else {
                notes.filter { it.title.contains(query, ignoreCase = true) }
            },
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductListState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteProduct(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note.id!!)
        }
    }
}
