package com.distributor.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.utils.BusinessConfig
import com.distributor.app.utils.BusinessConfigStore
import com.distributor.app.utils.DemoDataSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val businessName: String = "",
    val ownerPhone: String = "",
    val address: String = "",
    val logoFile: File? = null,
    val isSubmitting: Boolean = false,
    val isResetting: Boolean = false,
    val snackbarMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val context = getApplication<Application>()
        val config = BusinessConfigStore.get(context)
        val logoFile = BusinessConfigStore.getLogoFile(context).takeIf { it.exists() }
        _uiState.update {
            it.copy(
                businessName = config.businessName,
                ownerPhone   = config.ownerPhone,
                address      = config.address,
                logoFile     = logoFile
            )
        }
    }

    fun onBusinessNameChanged(value: String) = _uiState.update { it.copy(businessName = value) }
    fun onOwnerPhoneChanged(value: String)   = _uiState.update { it.copy(ownerPhone = value) }
    fun onAddressChanged(value: String)      = _uiState.update { it.copy(address = value) }

    fun saveLogo(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logoFile = BusinessConfigStore.getLogoFile(context)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    logoFile.outputStream().use { output -> input.copyTo(output) }
                }
                _uiState.update { it.copy(logoFile = logoFile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "logo_error") }
            }
        }
    }

    fun removeLogo() {
        val context = getApplication<Application>()
        BusinessConfigStore.getLogoFile(context).delete()
        _uiState.update { it.copy(logoFile = null) }
    }

    fun saveConfig(successMessage: String) {
        val context = getApplication<Application>()
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            BusinessConfigStore.save(
                context,
                BusinessConfig(
                    businessName = _uiState.value.businessName,
                    ownerPhone   = _uiState.value.ownerPhone,
                    address      = _uiState.value.address
                )
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = successMessage) }
        }
    }

    fun seedDemoData(successMessage: String) {
        val context = getApplication<Application>()
        _uiState.update { it.copy(isResetting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            db.clearAllTables()
            DemoDataSeeder.seed(db)
            _uiState.update { it.copy(isResetting = false, snackbarMessage = successMessage) }
        }
    }

    fun resetAllData(successMessage: String) {
        val context = getApplication<Application>()
        _uiState.update { it.copy(isResetting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(context).clearAllTables()
            _uiState.update { it.copy(isResetting = false, snackbarMessage = successMessage) }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}
