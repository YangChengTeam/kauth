package com.yangfeisg.kauth.feature.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yangfeisg.kauth.core.data.AccountRepository
import com.yangfeisg.kauth.core.otp.GoogleAuthMigration
import com.yangfeisg.kauth.core.otp.OtpAuthUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ScanViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    /** The camera last saw a QR code that is not an account setup code. */
    var invalidCode by mutableStateOf(false)
        private set

    /** Codes scanned so far of a Google Authenticator export spread over several codes. */
    var migrationProgress by mutableStateOf<MigrationProgress?>(null)
        private set

    // The camera keeps reporting the same code frame after frame; only the first one counts.
    private var saved = false
    private val scannedBatches = mutableMapOf<Int, MutableSet<Int>>()

    fun onQrCode(text: String, onSaved: () -> Unit) {
        if (saved) return
        if (GoogleAuthMigration.isMigration(text)) {
            onMigrationCode(text, onSaved)
            return
        }
        val account = OtpAuthUri.parse(text)
        if (account == null) {
            invalidCode = true
            return
        }
        saved = true
        viewModelScope.launch {
            repository.add(account)
            onSaved()
        }
    }

    private fun onMigrationCode(text: String, onSaved: () -> Unit) {
        val batch = GoogleAuthMigration.parse(text)?.takeIf { it.accounts.isNotEmpty() }
        if (batch == null) {
            invalidCode = true
            return
        }
        val scanned = scannedBatches.getOrPut(batch.id) { mutableSetOf() }
        if (!scanned.add(batch.index)) return
        invalidCode = false
        val done = scanned.size >= batch.size
        if (done) saved = true else migrationProgress = MigrationProgress(scanned.size, batch.size)
        viewModelScope.launch {
            // Accounts already here, say from scanning the same export twice, are skipped.
            repository.addAll(batch.accounts)
            if (done) onSaved()
        }
    }
}

internal data class MigrationProgress(val scanned: Int, val total: Int)
