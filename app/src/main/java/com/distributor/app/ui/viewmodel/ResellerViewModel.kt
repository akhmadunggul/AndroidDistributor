package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.ResellerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResellerListUiState(
    val resellers: List<ResellerEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val nameInput: String = "",
    val phoneInput: String = "",
    val addressInput: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class ResellerViewModel(application: Application) : AndroidViewModel(application) {

    private val resellerDao = AppDatabase.getInstance(application).resellerDao()

    private val _uiState = MutableStateFlow(ResellerListUiState())
    val uiState: StateFlow<ResellerListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            resellerDao.getAllResellers().collect { resellers ->
                _uiState.update { it.copy(resellers = resellers) }
            }
        }
    }

    fun openForm() {
        _uiState.update { it.copy(isFormOpen = true) }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                nameInput = "", phoneInput = "", addressInput = "",
                nameError = null, phoneError = null
            )
        }
    }

    fun onNameChanged(v: String)    = _uiState.update { it.copy(nameInput = v,    nameError  = null) }
    fun onPhoneChanged(v: String)   = _uiState.update { it.copy(phoneInput = v,   phoneError = null) }
    fun onAddressChanged(v: String) = _uiState.update { it.copy(addressInput = v) }
    fun clearSnackbar()             = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveReseller() {
        val state = _uiState.value

        if (state.nameInput.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        if (state.phoneInput.isBlank()) {
            _uiState.update { it.copy(phoneError = "Phone number is required") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                resellerDao.insertReseller(
                    ResellerEntity(
                        name        = state.nameInput.trim(),
                        phoneNumber = state.phoneInput.trim(),
                        address     = state.addressInput.trim()
                    )
                )
                val savedName = state.nameInput.trim()
                _uiState.update { it.copy(isSubmitting = false) }
                closeForm()
                _uiState.update { it.copy(snackbarMessage = "\"$savedName\" registered") }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("UNIQUE", ignoreCase = true) == true)
                    "Phone number \"${state.phoneInput.trim()}\" is already registered"
                else
                    "Failed to save: ${e.message}"
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            }
        }
    }

    fun deleteReseller(reseller: ResellerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                resellerDao.deleteReseller(reseller)
                _uiState.update { it.copy(snackbarMessage = "\"${reseller.name}\" removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Cannot delete: reseller has existing transactions") }
            }
        }
    }
}
