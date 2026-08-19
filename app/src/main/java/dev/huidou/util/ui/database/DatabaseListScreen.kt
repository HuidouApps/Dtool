package dev.huidou.util.ui.database

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.huidou.util.R
import dev.huidou.util.provider.UniversalDatabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(
    onDatabaseSelected: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Log.d("DatabaseListScreen", "=== DatabaseListScreen Composable entered ===")
    
    val dbClient = remember { 
        Log.d("DatabaseListScreen", "Creating UniversalDatabaseClient...")
        UniversalDatabaseClient(context).also {
            Log.d("DatabaseListScreen", "UniversalDatabaseClient created")
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var databases by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isServiceConnected by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var databaseToDelete by remember { mutableStateOf<String?>(null) }
    var selectedDatabaseIndex by remember { mutableStateOf<Int?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }

    // ==================== 批量管理模式状态 ====================
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedNames = remember { mutableStateListOf<String>() }
    var batchMenuExpanded by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    val renameTexts = remember { mutableStateMapOf<String, String>() } // oldName -> newName
    
    fun loadDatabases(showLoading: Boolean = true) {
        scope.launch {
            if (showLoading) isLoading = true
            
            // 检查服务连接状态
            isServiceConnected = dbClient.isServiceConnected()
            
            if (!isServiceConnected) {
                // 等待服务连接，最多重试5次
                var retries = 0
                while (!dbClient.isServiceConnected() && retries < 5) {
                    delay(500) // 每次等待500ms
                    retries++
                    isServiceConnected = dbClient.isServiceConnected()
                }
            }
            
            databases = dbClient.getDatabases()
            isLoading = false
            
            if (databases.isEmpty() && retryCount < 3) {
                // 如果为空且重试次数少于3次，自动重试
                retryCount++
                delay(1000)
                loadDatabases(false)
            } else {
                retryCount = 0
            }
        }
    }

    // ==================== 批量管理模式辅助函数 ====================

    fun toggleSelection(name: String) {
        if (name in selectedNames) selectedNames.remove(name) else selectedNames.add(name)
    }

    fun enterBatchMode() {
        isBatchMode = true
        selectedNames.clear()
    }

    fun exitBatchMode() {
        isBatchMode = false
        selectedNames.clear()
        batchMenuExpanded = false
    }

    fun isRenameValid(): Boolean {
        if (renameTexts.isEmpty()) return false
        val allNames = databases.map { it["name"] as String }
        val newNames = renameTexts.values.map { it.trim() }
        if (newNames.any { it.isBlank() }) return false
        if (newNames.any { it.contains('/') || it.contains('\\') }) return false
        if (newNames.any { !it.endsWith(".db") && !it.endsWith(".sqlite") }) return false
        if (newNames.toSet().size != newNames.size) return false
        renameTexts.forEach { (old, new) ->
            val t = new.trim()
            if (t != old && allNames.contains(t)) return false
        }
        return true
    }

    LaunchedEffect(Unit) {
        loadDatabases()
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isBatchMode) {
                        Text(stringResource(R.string.label_selected_count, selectedNames.size))
                    } else {
                        Column {
                            Text(stringResource(R.string.title_database_management))
                            Text(
                                text = if (isLoading) stringResource(R.string.label_loading) else stringResource(R.string.label_database_count, databases.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isBatchMode) {
                        IconButton(onClick = { exitBatchMode() }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_exit_batch_mode)
                            )
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list),
                                contentDescription = stringResource(R.string.cd_open_settings)
                            )
                        }
                    }
                },
                actions = {
                    if (isBatchMode) {
                        val allSelected = databases.isNotEmpty() && selectedNames.size == databases.size
                        TextButton(onClick = {
                            if (allSelected) {
                                selectedNames.clear()
                            } else {
                                selectedNames.clear()
                                selectedNames.addAll(databases.map { it["name"] as String })
                            }
                        }) {
                            Text(stringResource(if (allSelected) R.string.action_deselect_all else R.string.action_select_all))
                        }
                        Box {
                            IconButton(
                                onClick = { batchMenuExpanded = true },
                                enabled = selectedNames.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.cd_batch_actions)
                                )
                            }
                            DropdownMenu(
                                expanded = batchMenuExpanded,
                                onDismissRequest = { batchMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_batch_delete)) },
                                    onClick = {
                                        batchMenuExpanded = false
                                        showBatchDeleteDialog = true
                                    },
                                    enabled = selectedNames.isNotEmpty()
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_batch_rename)) },
                                    onClick = {
                                        batchMenuExpanded = false
                                        renameTexts.clear()
                                        selectedNames.forEach { renameTexts[it] = it }
                                        showBatchRenameDialog = true
                                    },
                                    enabled = selectedNames.isNotEmpty()
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { enterBatchMode() },
                            enabled = databases.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Filled.Checklist,
                                contentDescription = stringResource(R.string.cd_batch_mode)
                            )
                        }
                        IconButton(
                            onClick = { loadDatabases() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                modifier = if (isLoading) Modifier else Modifier
                            )
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_database))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                // 加载状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.label_loading_database_list))
                        if (!isServiceConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.label_waiting_service),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else if (databases.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.label_no_database))
                        Text(stringResource(R.string.hint_create_database), style = MaterialTheme.typography.bodySmall)
                        
                        // 刷新按钮
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadDatabases() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_refresh))
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(databases) { index, db ->
                        val dbName = db["name"] as String
                        val dbSize = db["size"] as Long
                        val lastModified = db["last_modified"] as Long
                        val isSelected = dbName in selectedNames

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isBatchMode) toggleSelection(dbName) else onDatabaseSelected(dbName)
                                    },
                                    onLongClick = {
                                        if (!isBatchMode) {
                                            selectedDatabaseIndex = index
                                            showActionDialog = true
                                        }
                                    }
                                ),
                            colors = if (isBatchMode && isSelected) {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = if (isBatchMode) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isBatchMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleSelection(dbName) }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dbName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.label_db_size_and_date, formatFileSize(dbSize), formatDate(lastModified)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 创建数据库对话框
    if (showCreateDialog) {
        CreateDatabaseDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val success = dbClient.createDatabase(name)
                    if (success) {
                        loadDatabases()
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_database_created))
                    } else {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_database_create_failed))
                    }
                }
                showCreateDialog = false
            }
        )
    }
    
    // 删除确认对话框
    databaseToDelete?.let { dbName ->
        AlertDialog(
            onDismissRequest = { databaseToDelete = null },
            title = { Text(stringResource(R.string.title_confirm_delete)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_database, dbName)) },
            confirmButton = {
                OutlinedButton(onClick = { databaseToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = dbClient.deleteDatabase(dbName)
                        if (success) {
                            loadDatabases()
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_database_deleted))
                        } else {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_delete_failed))
                        }
                    }
                    databaseToDelete = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }
    
    // 批量删除确认对话框
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text(stringResource(R.string.title_batch_delete)) },
            text = { Text(stringResource(R.string.msg_confirm_batch_delete, selectedNames.size)) },
            confirmButton = {
                OutlinedButton(onClick = { showBatchDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatchDeleteDialog = false
                    val toDelete = selectedNames.toList()
                    scope.launch {
                        var ok = 0
                        var fail = 0
                        toDelete.forEach { if (dbClient.deleteDatabase(it)) ok++ else fail++ }
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_batch_delete_result, ok, fail))
                        loadDatabases()
                        exitBatchMode()
                    }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }

    // 批量重命名对话框
    if (showBatchRenameDialog) {
        AlertDialog(
            onDismissRequest = { showBatchRenameDialog = false },
            title = { Text(stringResource(R.string.title_batch_rename)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    selectedNames.forEach { oldName ->
                        Text(
                            text = oldName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = renameTexts[oldName] ?: oldName,
                            onValueChange = { renameTexts[oldName] = it },
                            label = { Text(stringResource(R.string.label_new_database_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchRenameDialog = false
                        val renames = renameTexts.map { it.key to it.value.trim() }
                        scope.launch {
                            var ok = 0
                            var fail = 0
                            renames.forEach { (old, new) ->
                                if (new == old) ok++
                                else if (dbClient.renameDatabase(old, new)) ok++ else fail++
                            }
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_batch_rename_result, ok, fail))
                            loadDatabases()
                            exitBatchMode()
                        }
                    },
                    enabled = isRenameValid()
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 长按操作选择对话框
    if (showActionDialog && selectedDatabaseIndex != null) {
        AlertDialog(
            onDismissRequest = { 
                showActionDialog = false
                selectedDatabaseIndex = null
            },
            title = { Text(stringResource(R.string.title_select_action)) },
            text = { Text(stringResource(R.string.msg_select_database_action)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showActionDialog = false
                        selectedDatabaseIndex = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        databaseToDelete = databases[selectedDatabaseIndex!!]["name"] as String
                        showActionDialog = false
                        selectedDatabaseIndex = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }
}

@Composable
fun CreateDatabaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var dbName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_create_database)) },
        text = {
            OutlinedTextField(
                value = dbName,
                onValueChange = { dbName = it },
                label = { Text(stringResource(R.string.label_database_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_database_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dbName) },
                enabled = dbName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
