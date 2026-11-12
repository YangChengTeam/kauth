package com.yangfeisg.kauth.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.component.InkDialog

/** Asked from a row's delete seal. [onDelete] runs once the scroll has rolled up. */
@Composable
internal fun DeleteAccountDialog(item: AccountItem, onDelete: () -> Unit, onDismiss: () -> Unit) {
    InkDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_delete_title),
        content = {
            Text(
                text = stringResource(R.string.home_delete_body, item.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        buttons = {
            TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
            // The scroll rolls up first, so the row is in view as it washes away.
            TextButton(onClick = { dismiss(then = onDelete) }) {
                Text(stringResource(R.string.home_delete), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

/** Opened from a row's edit seal: the service and account name, both editable. */
@Composable
internal fun EditAccountDialog(
    item: AccountItem,
    onSave: (issuer: String, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var issuer by rememberSaveable { mutableStateOf(item.issuer) }
    var name by rememberSaveable { mutableStateOf(item.name) }
    InkDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_edit_title),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text(stringResource(R.string.home_label_issuer)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.home_label_name)) },
                    singleLine = true,
                )
            }
        },
        buttons = {
            TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
            TextButton(
                onClick = {
                    onSave(issuer, name)
                    dismiss()
                },
                enabled = issuer.isNotBlank() || name.isNotBlank(),
            ) {
                Text(stringResource(R.string.home_save))
            }
        },
    )
}
