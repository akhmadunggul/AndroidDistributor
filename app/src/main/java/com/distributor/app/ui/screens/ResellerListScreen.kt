package com.distributor.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.ResellerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResellerListScreen(
    viewModel: ResellerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var resellerToDelete by remember { mutableStateOf<ResellerEntity?>(null) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.isFormOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            sheetState = sheetState
        ) {
            ResellerForm(viewModel = viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_resellers)) },
                actions = { LanguageMenuIcon() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openForm() }) {
                Icon(Icons.Default.Add, contentDescription = "Add reseller")
            }
        }
    ) { padding ->
        if (uiState.resellers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.resellers_empty_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.resellers_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(uiState.resellers, key = { it.id }) { reseller ->
                    ResellerCard(
                        reseller = reseller,
                        onEdit   = { viewModel.openFormForEdit(reseller) },
                        onDelete = { resellerToDelete = reseller }
                    )
                }

                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }

    resellerToDelete?.let { reseller ->
        AlertDialog(
            onDismissRequest = { resellerToDelete = null },
            title = { Text(stringResource(R.string.reseller_delete_confirm_title)) },
            text  = { Text(stringResource(R.string.reseller_delete_confirm_message, reseller.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReseller(reseller)
                    resellerToDelete = null
                }) {
                    Text(
                        stringResource(R.string.reseller_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { resellerToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ResellerCard(reseller: ResellerEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reseller.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = reseller.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (reseller.address.isNotBlank()) {
                    Text(
                        text = reseller.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reseller.email.isNotBlank()) {
                    Text(
                        text = reseller.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit ${reseller.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${reseller.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ResellerForm(viewModel: ResellerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val contactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.Contacts.DISPLAY_NAME
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIdx  = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val phone = if (phoneIdx >= 0) cursor.getString(phoneIdx) ?: "" else ""
                        val name  = if (nameIdx  >= 0) cursor.getString(nameIdx)  ?: "" else ""
                        viewModel.onPhoneChanged(phone)
                        if (uiState.nameInput.isBlank() && name.isNotBlank()) {
                            viewModel.onNameChanged(name)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(
                if (uiState.editingReseller != null) R.string.reseller_edit_form_title
                else R.string.reseller_form_title
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        OutlinedTextField(
            value = uiState.nameInput,
            onValueChange = viewModel::onNameChanged,
            label = { Text(stringResource(R.string.reseller_name_label)) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { e -> { Text(e) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.phoneInput,
            onValueChange = viewModel::onPhoneChanged,
            label = { Text(stringResource(R.string.reseller_phone_label)) },
            placeholder = { Text(stringResource(R.string.reseller_phone_placeholder)) },
            isError = uiState.phoneError != null,
            supportingText = uiState.phoneError?.let { e -> { Text(e) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            trailingIcon = {
                IconButton(onClick = {
                    contactLauncher.launch(
                        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    )
                }) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = stringResource(R.string.cd_pick_contact)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.addressInput,
            onValueChange = viewModel::onAddressChanged,
            label = { Text(stringResource(R.string.reseller_address_label)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.emailInput,
            onValueChange = viewModel::onEmailChanged,
            label = { Text(stringResource(R.string.reseller_email_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = { viewModel.closeForm() },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_cancel)) }

        androidx.compose.material3.Button(
            onClick = { viewModel.saveReseller() },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    stringResource(
                        if (uiState.editingReseller != null) R.string.reseller_save_changes_button
                        else R.string.reseller_register_button
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
