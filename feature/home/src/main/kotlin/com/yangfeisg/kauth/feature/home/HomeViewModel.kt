package com.yangfeisg.kauth.feature.home

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yangfeisg.kauth.core.data.AccountRepository
import com.yangfeisg.kauth.core.data.backup.AccountBackup
import com.yangfeisg.kauth.core.data.backup.ImportResult
import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpType
import com.yangfeisg.kauth.core.otp.OtpGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val backup: AccountBackup,
) : ViewModel() {

    // Compose state rather than a flow, so the search field never lags behind the keyboard.
    var query by mutableStateOf("")
        private set

    val uiState: StateFlow<HomeUiState> =
        combine(repository.accounts, snapshotFlow { query }, secondTicks()) { accounts, query, now ->
            HomeUiState(
                isLoading = false,
                hasAccounts = accounts.isNotEmpty(),
                items = accounts
                    .filter { it.displayName.contains(query.trim(), ignoreCase = true) }
                    .map { it.toItem(now) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query = value
    }

    fun rename(id: Long, issuer: String, name: String) {
        viewModelScope.launch { repository.rename(id, issuer.trim(), name.trim()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun nextCode(id: Long) {
        viewModelScope.launch { repository.incrementCounter(id) }
    }

    /** An encrypted backup the user picked, waiting for its password. */
    var importPrompt by mutableStateOf<ImportPrompt?>(null)
        private set

    private val _backupMessages = Channel<BackupMessage>(Channel.BUFFERED)

    /** One-off outcomes of exporting or importing, to show once. */
    val backupMessages: Flow<BackupMessage> = _backupMessages.receiveAsFlow()

    fun export(target: Uri, password: String) {
        viewModelScope.launch {
            _backupMessages.send(BackupMessage.Exporting)
            val message = try {
                BackupMessage.Exported(backup.export(target, password.toCharArray()))
            } catch (_: IOException) {
                BackupMessage.Failed
            } catch (_: SecurityException) {
                BackupMessage.Failed
            }
            _backupMessages.send(message)
        }
    }

    fun import(source: Uri) {
        viewModelScope.launch { onImportResult(backup.read(source)) }
    }

    fun unlockImport(password: String) {
        val prompt = importPrompt ?: return
        importPrompt = prompt.copy(working = true, wrongPassword = false)
        viewModelScope.launch { onImportResult(backup.restore(prompt.bytes, password.toCharArray())) }
    }

    fun cancelImport() {
        importPrompt = null
    }

    private suspend fun onImportResult(result: ImportResult) {
        when (result) {
            is ImportResult.NeedsPassword -> importPrompt = ImportPrompt(result.bytes)
            ImportResult.WrongPassword -> importPrompt = importPrompt?.copy(working = false, wrongPassword = true)
            ImportResult.Invalid -> {
                importPrompt = null
                _backupMessages.send(BackupMessage.Invalid)
            }
            is ImportResult.Imported -> {
                importPrompt = null
                _backupMessages.send(BackupMessage.Imported(result.added, result.skipped))
            }
        }
    }

    private fun Account.toItem(nowMillis: Long): AccountItem {
        val isTimeBased = type == OtpType.TOTP
        return AccountItem(
            id = id,
            issuer = issuer,
            name = name,
            title = displayName,
            code = runCatching { OtpGenerator.generate(this, nowMillis) }.getOrDefault(""),
            isTimeBased = isTimeBased,
            period = period,
            expiringSoon = isTimeBased && period - nowMillis / 1000 % period <= EXPIRING_SECONDS,
        )
    }

    /** The current time, emitted on every wall-clock second so codes roll over on time. */
    private fun secondTicks(): Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(1000 - now % 1000)
        }
    }

    private companion object {
        const val EXPIRING_SECONDS = 5
    }
}
