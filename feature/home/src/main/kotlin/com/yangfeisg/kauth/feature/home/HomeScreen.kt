package com.yangfeisg.kauth.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangfeisg.kauth.core.backup.BackupFile
import com.yangfeisg.kauth.core.designsystem.component.InkBloom
import com.yangfeisg.kauth.core.designsystem.component.InkDialog
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun HomeRoute(
    onScanQr: () -> Unit,
    onEnterKey: () -> Unit,
    onExit: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        viewModel.backupMessages.collect { message ->
            // Each message replaces the one showing, so "exporting" gives way to its outcome.
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(
                    message = resources.backupText(message),
                    duration = if (message == BackupMessage.Exporting) SnackbarDuration.Indefinite else SnackbarDuration.Short,
                )
            }
        }
    }

    // A password chosen for export waits here while the user picks where to save.
    var exportPassword by remember { mutableStateOf<String?>(null) }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null && password != null) viewModel.export(uri, password)
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    var showBackup by remember { mutableStateOf(false) }
    var askExportPassword by remember { mutableStateOf(false) }
    val onImport = { openFile.launch(arrayOf("*/*")) }

    HomeScreen(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        onScanQr = onScanQr,
        onEnterKey = onEnterKey,
        onExit = onExit,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onNextCode = viewModel::nextCode,
        onBackup = { showBackup = true },
        onImport = onImport,
        snackbarHostState = snackbarHostState,
    )

    if (showBackup) {
        BackupDialog(
            onExport = { askExportPassword = true },
            onImport = onImport,
            onDismiss = { showBackup = false },
        )
    }
    if (askExportPassword) {
        ExportPasswordDialog(
            onConfirm = { password ->
                exportPassword = password
                createFile.launch(BackupFile.fileName(LocalDate.now().toString()))
            },
            onDismiss = { askExportPassword = false },
        )
    }
    viewModel.importPrompt?.let { prompt ->
        ImportPasswordDialog(prompt = prompt, onUnlock = viewModel::unlockImport, onDismiss = viewModel::cancelImport)
    }
}

// Generic, so every file manager and cloud drive offers to store it.
private const val BACKUP_MIME_TYPE = "application/octet-stream"

private fun Resources.backupText(message: BackupMessage): String = when (message) {
    BackupMessage.Exporting -> getString(R.string.home_exporting)
    is BackupMessage.Exported -> getQuantityString(R.plurals.home_exported, message.count, message.count)
    is BackupMessage.Imported -> {
        val imported = getQuantityString(R.plurals.home_imported, message.added, message.added)
        when {
            message.skipped == 0 -> imported
            message.added == 0 -> getQuantityString(R.plurals.home_imported_none, message.skipped, message.skipped)
            else -> getString(R.string.home_imported_skipped, imported, message.skipped)
        }
    }
    BackupMessage.Invalid -> getString(R.string.home_import_invalid)
    BackupMessage.Failed -> getString(R.string.home_export_failed)
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onEnterKey: () -> Unit,
    onExit: () -> Unit,
    onRename: (id: Long, issuer: String, name: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onNextCode: (id: Long) -> Unit,
    onBackup: () -> Unit,
    onImport: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    // The one row swiped open on its seals, and the account being edited or asked about removing.
    var openRowId by remember { mutableStateOf<Long?>(null) }
    var editTarget by remember { mutableStateOf<AccountItem?>(null) }
    var deleteTarget by remember { mutableStateOf<AccountItem?>(null) }
    // Accounts confirmed for removal, washing away before they are deleted. Ids are never reused.
    var dissolvingIds by remember { mutableStateOf(emptySet<Long>()) }
    // Rows rise in one by one when the list first shows; rows scrolled in later just appear.
    var entranceDone by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            delay(ENTRANCE_WINDOW_MILLIS)
            entranceDone = true
        }
    }
    // Opening the menu spills ink from the add button across the page, and what is left
    // of the list goes out of focus (the blur needs Android 12).
    val inkSpread = animateFloatAsState(
        targetValue = if (menuExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow),
        label = "ink",
    )
    var fabCenter by remember { mutableStateOf(Offset.Zero) }
    val blur by animateDpAsState(if (menuExpanded) 10.dp else 0.dp, label = "blur")

    // Back on the first screen would leave the app, so it asks first. Declared before the
    // menu's handler: the handler declared last wins, and an open menu should just close.
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler { confirmExit = true }
    BackHandler(enabled = menuExpanded) { menuExpanded = false }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blur),
            // See-through, so the landscape painted behind the whole app shows here too.
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                if (uiState.hasAccounts) SearchField(query = query, onQueryChange = onQueryChange, onBackup = onBackup)
            },
            // Above the add button, which floats over the scaffold rather than in it.
            snackbarHost = {
                SnackbarHost(snackbarHostState, Modifier.padding(bottom = if (uiState.hasAccounts) 84.dp else 0.dp))
            },
        ) { padding ->
            when {
                uiState.isLoading -> Unit
                !uiState.hasAccounts -> EmptyState(
                    onScanQr = onScanQr,
                    onEnterKey = onEnterKey,
                    onImport = onImport,
                    modifier = Modifier.padding(padding),
                )
                uiState.items.isEmpty() -> Text(
                    text = stringResource(R.string.home_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Leave the last row clear of the add button.
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 96.dp,
                    ),
                ) {
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { index, item ->
                        AccountRow(
                            item = item,
                            onCopy = { context.copyCode(item.code) },
                            open = openRowId == item.id,
                            onOpenChange = { open ->
                                openRowId = if (open) item.id else openRowId.takeUnless { it == item.id }
                            },
                            onEdit = {
                                openRowId = null
                                editTarget = item
                            },
                            onDelete = {
                                openRowId = null
                                deleteTarget = item
                            },
                            onNextCode = { onNextCode(item.id) },
                            dissolving = item.id in dissolvingIds,
                            onDissolved = { onDelete(item.id) },
                            modifier = Modifier
                                .animateItem()
                                .staggeredEntrance(index, enabled = !entranceDone),
                        )
                    }
                }
            }
        }
        InkBloom(spread = { inkSpread.value }, modifier = Modifier.fillMaxSize(), origin = fabCenter)
        if (menuExpanded) {
            // A tap anywhere off the menu closes it.
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { menuExpanded = false },
            )
        }
        AnimatedVisibility(
            visible = uiState.hasAccounts,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            AddAccountFab(
                expanded = menuExpanded,
                onToggle = { menuExpanded = !menuExpanded },
                onScanQr = {
                    menuExpanded = false
                    onScanQr()
                },
                onEnterKey = {
                    menuExpanded = false
                    onEnterKey()
                },
                onPositioned = { fabCenter = it },
            )
        }
    }

    if (confirmExit) {
        InkDialog(
            onDismissRequest = { confirmExit = false },
            title = stringResource(R.string.home_exit_title),
            buttons = {
                TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.home_cancel)) }
                // The scroll rolls up first, then the ink floods the page.
                TextButton(onClick = { dismiss(then = onExit) }) {
                    Text(stringResource(R.string.home_exit), color = MaterialTheme.colorScheme.tertiary)
                }
            },
        )
    }

    deleteTarget?.let { target ->
        DeleteAccountDialog(
            item = target,
            onDelete = { dissolvingIds = dissolvingIds + target.id },
            onDismiss = { deleteTarget = null },
        )
    }
    editTarget?.let { target ->
        EditAccountDialog(
            item = target,
            onSave = { issuer, name -> onRename(target.id, issuer, name) },
            onDismiss = { editTarget = null },
        )
    }
}

private const val ENTRANCE_WINDOW_MILLIS = 900L

private fun Context.copyCode(code: String) {
    val clip = ClipData.newPlainText("code", code).apply {
        // Keeps the code out of the clipboard preview and keyboard suggestions.
        description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
