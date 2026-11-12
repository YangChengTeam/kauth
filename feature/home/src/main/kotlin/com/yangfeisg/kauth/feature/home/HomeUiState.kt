package com.yangfeisg.kauth.feature.home

internal data class HomeUiState(
    val isLoading: Boolean = true,
    /** Whether any account exists, regardless of the search. */
    val hasAccounts: Boolean = false,
    /** The accounts matching the search, with their current codes. */
    val items: List<AccountItem> = emptyList(),
)

internal data class AccountItem(
    val id: Long,
    val issuer: String,
    val name: String,
    val title: String,
    val code: String,
    val isTimeBased: Boolean,
    val period: Int,
    /** A time based code in its last seconds. */
    val expiringSoon: Boolean,
)

/** A picked backup file that needs its password; [working] while it is being tried. */
internal data class ImportPrompt(
    val bytes: ByteArray,
    val working: Boolean = false,
    val wrongPassword: Boolean = false,
)

internal sealed interface BackupMessage {
    /** Encrypting takes a moment; shown until the outcome replaces it. */
    data object Exporting : BackupMessage
    data class Exported(val count: Int) : BackupMessage
    data class Imported(val added: Int, val skipped: Int) : BackupMessage
    data object Invalid : BackupMessage
    data object Failed : BackupMessage
}
