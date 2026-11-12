package com.yangfeisg.kauth.core.data.backup

import android.content.Context
import android.net.Uri
import com.yangfeisg.kauth.core.backup.BackupFile
import com.yangfeisg.kauth.core.backup.InvalidBackupException
import com.yangfeisg.kauth.core.backup.WrongPasswordException
import com.yangfeisg.kauth.core.data.AccountRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moves accounts between devices by hand, as a password protected file the user keeps
 * wherever they like. There is no server, so this is the only way accounts leave the device.
 */
@Singleton
class AccountBackup @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val repository: AccountRepository,
) {
    /** Writes every account to [target]. Returns how many were written. */
    suspend fun export(target: Uri, password: CharArray): Int {
        val accounts = repository.accounts.first()
        withContext(Dispatchers.IO) {
            val bytes = BackupFile.encrypt(accounts, password)
            val stream = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Cannot write $target")
            stream.use { it.write(bytes) }
        }
        return accounts.size
    }

    /** Reads [source]; an encrypted backup then waits for its password in [ImportResult.NeedsPassword]. */
    suspend fun read(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(source)?.use { it.readBytes() }
        } catch (_: IOException) {
            null
        } ?: return@withContext ImportResult.Invalid
        if (BackupFile.isEncrypted(bytes)) ImportResult.NeedsPassword(bytes) else restore(bytes, password = null)
    }

    /** Adds the accounts in [bytes] from [read], opened with [password]. */
    suspend fun restore(bytes: ByteArray, password: CharArray?): ImportResult {
        val accounts = try {
            withContext(Dispatchers.Default) { BackupFile.decrypt(bytes, password) }
        } catch (_: WrongPasswordException) {
            return ImportResult.WrongPassword
        } catch (_: InvalidBackupException) {
            return ImportResult.Invalid
        }
        val added = repository.addAll(accounts)
        return ImportResult.Imported(added = added, skipped = accounts.size - added)
    }
}

sealed interface ImportResult {
    class NeedsPassword(val bytes: ByteArray) : ImportResult
    data object WrongPassword : ImportResult
    data object Invalid : ImportResult
    data class Imported(val added: Int, val skipped: Int) : ImportResult
}
