package com.distributor.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.utils.BackupScheduler
import com.distributor.app.utils.BackupStore
import com.distributor.app.utils.BusinessConfig
import com.distributor.app.utils.BusinessConfigStore
import com.distributor.app.utils.LicenseStore
import com.distributor.app.utils.DemoDataSeeder
import com.distributor.app.utils.DriveBackupManager
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
    val snackbarMessage: String? = null,
    val lastBackupMs: Long = 0L,
    val isBackingUp: Boolean = false,
    val showRestoreConfirm: Boolean = false,
    val backupEmail: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val context = getApplication<Application>()
        val config  = BusinessConfigStore.get(context)
        val logo    = BusinessConfigStore.getLogoFile(context).takeIf { it.exists() }

        BackupScheduler.schedule(context)

        _uiState.update {
            it.copy(
                businessName = config.businessName,
                ownerPhone   = config.ownerPhone,
                address      = config.address,
                logoFile     = logo,
                lastBackupMs = BackupStore.getLastBackupMs(context),
                backupEmail  = LicenseStore.getEmail(context)
            )
        }
    }

    // ── Business settings ─────────────────────────────────────────────────────

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
            try {
                val db = AppDatabase.getInstance(context)
                db.clearAllTables()
                DemoDataSeeder.seed(db)
                _uiState.update { it.copy(isResetting = false, snackbarMessage = successMessage) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isResetting = false, snackbarMessage = "Seed error: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
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

    // ── Backup email ──────────────────────────────────────────────────────────

    fun onBackupEmailChanged(v: String) = _uiState.update { it.copy(backupEmail = v) }

    fun saveBackupEmail() {
        val context = getApplication<Application>()
        LicenseStore.saveEmail(context, _uiState.value.backupEmail)
        _uiState.update { it.copy(
            backupEmail     = LicenseStore.getEmail(context),
            snackbarMessage = "backup_email_saved"
        ) }
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    fun triggerBackupNow() {
        val context = getApplication<Application>()
        if (LicenseStore.getEmail(context).isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "backup_email_required") }
            return
        }
        _uiState.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            val success = DriveBackupManager.backup(context)
            if (success) {
                val now = System.currentTimeMillis()
                BackupStore.saveLastBackupMs(context, now)
                _uiState.update { it.copy(isBackingUp = false, lastBackupMs = now, snackbarMessage = "backup_success") }
            } else {
                _uiState.update { it.copy(isBackingUp = false, snackbarMessage = "backup_failed") }
            }
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    fun showRestoreConfirm()    = _uiState.update { it.copy(showRestoreConfirm = true) }
    fun dismissRestoreConfirm() = _uiState.update { it.copy(showRestoreConfirm = false) }

    fun restoreFromDrive() {
        val context = getApplication<Application>()
        _uiState.update { it.copy(showRestoreConfirm = false, isBackingUp = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = DriveBackupManager.downloadBackup(context)
                if (tempFile == null) {
                    _uiState.update { it.copy(isBackingUp = false, snackbarMessage = "restore_not_found") }
                    return@launch
                }
                AppDatabase.getInstance(context).close()
                val dbFile = context.getDatabasePath("distributor.db")
                dbFile.parentFile?.mkdirs()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()

                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)!!
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
                Process.killProcess(Process.myPid())
            } catch (e: Exception) {
                _uiState.update { it.copy(isBackingUp = false, snackbarMessage = "restore_failed") }
            }
        }
    }
}
