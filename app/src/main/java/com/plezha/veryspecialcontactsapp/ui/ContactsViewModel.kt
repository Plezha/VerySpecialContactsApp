package com.plezha.veryspecialcontactsapp.ui


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plezha.veryspecialcontactsapp.contactservice.ContactDetails
import com.plezha.veryspecialcontactsapp.data.ContactRepository
import com.plezha.veryspecialcontactsapp.data.DeduplicationResult
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val isLoading: Boolean = true,
    val contacts: ImmutableMap<Char, List<ContactDetails>> = persistentMapOf(),
    val error: String? = null,
    val deduplicationMessage: String? = null,
)

class ContactsViewModel (
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            contactRepository.getContacts()
                .collect { result ->
                    result.fold(
                        onSuccess = { contactsList ->
                            val groupedContacts = contactsList
                                .filter { !it.displayName.isNullOrBlank() }
                                .groupBy { it.displayName!!.first().uppercaseChar() }
                                .toSortedMap()
                                .toPersistentMap()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                contacts = groupedContacts,
                                error = null,
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error fetching contacts: ${exception.localizedMessage}",
                            )
                        }
                    )
                }
        }
    }

    fun deleteAllDuplicates() {
        _uiState.value = _uiState.value.copy(isLoading = true, deduplicationMessage = "Deduplicating...", error = null)
        viewModelScope.launch {
            val result = contactRepository.deleteDuplicateContacts()
            val message = when (result) {
                is DeduplicationResult.Success -> "${result.count} duplicate contact(s) deleted successfully."
                is DeduplicationResult.NoDuplicates -> "No duplicate contacts found to delete."
                is DeduplicationResult.PermissionError -> result.message
                is DeduplicationResult.Error -> "Deduplication failed: ${result.message}"
            }
            _uiState.value = _uiState.value.copy(deduplicationMessage = message)

            if (result is DeduplicationResult.Success || result is DeduplicationResult.NoDuplicates) {
                loadContacts()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        contactRepository.disconnect()
    }
}