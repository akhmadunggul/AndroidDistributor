package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.SupplierEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierListUiState(
    val suppliers: List<SupplierEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val editingSupplier: SupplierEntity? = null,
    val nameInput: String = "",
    val phoneInput: String = "",
    val addressInput: String = "",
    val emailInput: String = "",
    val notesInput: String = "",
    val nameError: String? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class SupplierViewModel(application: Application) : AndroidViewModel(application) {

    private val supplierDao = AppDatabase.getInstance(application).supplierDao()

    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState: StateFlow<SupplierListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierDao.getAllSuppliers().collect { list ->
                _uiState.update { it.copy(suppliers = list) }
            }
        }
    }

    fun openForm() {
        _uiState.update { it.copy(isFormOpen = true, editingSupplier = null) }
    }

    fun openFormForEdit(supplier: SupplierEntity) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingSupplier = supplier,
                nameInput = supplier.name,
                phoneInput = supplier.phoneNumber,
                addressInput = supplier.address,
                emailInput = supplier.email,
                notesInput = supplier.notes,
                nameError = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false, editingSupplier = null,
                nameInput = "", phoneInput = "", addressInput = "",
                emailInput = "", notesInput = "", nameError = null
            )
        }
    }

    fun onNameChanged(v: String)    = _uiState.update { it.copy(nameInput = v,    nameError = null) }
    fun onPhoneChanged(v: String)   = _uiState.update { it.copy(phoneInput = v) }
    fun onAddressChanged(v: String) = _uiState.update { it.copy(addressInput = v) }
    fun onEmailChanged(v: String)   = _uiState.update { it.copy(emailInput = v) }
    fun onNotesChanged(v: String)   = _uiState.update { it.copy(notesInput = v) }
    fun clearSnackbar()             = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveSupplier() {
        val state = _uiState.value
        if (state.nameInput.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val editing = state.editingSupplier
                val now = System.currentTimeMillis()
                if (editing != null) {
                    supplierDao.updateSupplier(
                        editing.copy(
                            name        = state.nameInput.trim(),
                            phoneNumber = state.phoneInput.trim(),
                            address     = state.addressInput.trim(),
                            email       = state.emailInput.trim(),
                            notes       = state.notesInput.trim(),
                            updatedAt   = now
                        )
                    )
                    val saved = state.nameInput.trim()
                    _uiState.update { it.copy(isSubmitting = false) }
                    closeForm()
                    _uiState.update { it.copy(snackbarMessage = "\"$saved\" updated") }
                } else {
                    supplierDao.insertSupplier(
                        SupplierEntity(
                            name        = state.nameInput.trim(),
                            phoneNumber = state.phoneInput.trim(),
                            address     = state.addressInput.trim(),
                            email       = state.emailInput.trim(),
                            notes       = state.notesInput.trim(),
                            createdAt   = now,
                            updatedAt   = now
                        )
                    )
                    val saved = state.nameInput.trim()
                    _uiState.update { it.copy(isSubmitting = false) }
                    closeForm()
                    _uiState.update { it.copy(snackbarMessage = "\"$saved\" registered") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supplierDao.deleteSupplier(supplier)
                _uiState.update { it.copy(snackbarMessage = "\"${supplier.name}\" removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Cannot delete: supplier has existing purchase records") }
            }
        }
    }
}
