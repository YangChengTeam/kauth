package com.yangfeisg.kauth.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.component.InkDialog
import com.yangfeisg.kauth.core.designsystem.icon.KauthIcons

private const val MIN_PASSWORD_LENGTH = 6

/** The two ways accounts move between devices: out to a file, or back in from one. */
@Composable
internal fun BackupDialog(onExport: () -> Unit, onImport: () -> Unit, onDismiss: () -> Unit) {
    InkDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_backup_title),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_backup_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BackupChoice(KauthIcons.Upload, stringResource(R.string.home_backup_export)) { dismiss(then = onExport) }
                BackupChoice(KauthIcons.Download, stringResource(R.string.home_backup_import)) { dismiss(then = onImport) }
            }
        },
        buttons = {
            TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
        },
    )
}

@Composable
private fun BackupChoice(icon: Int, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(icon), contentDescription = null)
        Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/** Chooses the password a new backup is locked with; [onConfirm] then asks where to save it. */
@Composable
internal fun ExportPasswordDialog(onConfirm: (password: String) -> Unit, onDismiss: () -> Unit) {
    // Not saveable: a password has no business in the saved instance state.
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val mismatch = repeat.isNotEmpty() && repeat != password
    InkDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_export_title),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_export_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.home_password),
                    supportingText = stringResource(R.string.home_password_hint, MIN_PASSWORD_LENGTH),
                )
                PasswordField(
                    value = repeat,
                    onValueChange = { repeat = it },
                    label = stringResource(R.string.home_password_repeat),
                    supportingText = if (mismatch) stringResource(R.string.home_password_mismatch) else null,
                    isError = mismatch,
                )
            }
        },
        buttons = {
            TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
            TextButton(
                onClick = { dismiss(then = { onConfirm(password) }) },
                enabled = password.length >= MIN_PASSWORD_LENGTH && repeat == password,
            ) {
                Text(stringResource(R.string.home_backup_export_action))
            }
        },
    )
}

/** Asks for the password of a backup being imported; stays open, marked, after a wrong one. */
@Composable
internal fun ImportPasswordDialog(prompt: ImportPrompt, onUnlock: (password: String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    // The error belongs to the password that was tried; editing it clears the mark.
    var tried by remember { mutableStateOf<String?>(null) }
    val wrongPassword = prompt.wrongPassword && password == tried
    InkDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_import_title),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_import_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.home_password),
                    supportingText = if (wrongPassword) stringResource(R.string.home_password_wrong) else null,
                    isError = wrongPassword,
                    enabled = !prompt.working,
                )
            }
        },
        buttons = {
            TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
            TextButton(
                onClick = {
                    tried = password
                    onUnlock(password)
                },
                enabled = password.isNotEmpty() && !prompt.working,
            ) {
                Text(stringResource(R.string.home_backup_import_action))
            }
        },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String?,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
