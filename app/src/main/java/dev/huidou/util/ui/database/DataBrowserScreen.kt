package dev.huidou.util.ui.database

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.huidou.util.R
import dev.huidou.util.components.DataTable
import dev.huidou.util.provider.UniversalDatabaseClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBrowserScreen(
    dbName: String,
    tableName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dbClient = remember { UniversalDatabaseClient(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var data by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var columnTypes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }  // 字段名 -> 类型
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var deletingRow by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var selectedRowIndex by remember { mutableStateOf<Int?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    
    fun loadData() {
        scope.launch {
            // 先获取表结构
            val structure = dbClient.getTableStructure(dbName, tableName)
            columns = structure.map { it["name"] as String }
            // 保存字段类型信息
            columnTypes = structure.associate { 
                (it["name"] as String) to (it["type"] as? String ?: "TEXT")
            }
            
            // 再获取数据
            data = dbClient.queryData(dbName, tableName)
        }
    }
    
    LaunchedEffect(Unit) {
        loadData()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { 
                    Column {
                        Text(stringResource(R.string.title_data_browser))
                        Text("$dbName > $tableName", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_data))
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
            Text(
                text = stringResource(R.string.label_record_count, data.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.label_no_data))
                        Text(stringResource(R.string.hint_add_data), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                DataTable(
                    headers = columns,
                    rows = data.map { row ->
                        columns.map { col ->
                            row[col]?.toString() ?: ""
                        }
                    },
                    onRowLongClick = { index ->
                        selectedRowIndex = index
                        showActionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // 添加数据对话框
    if (showAddDialog) {
        AddEditDataDialog(
            dbName = dbName,
            tableName = tableName,
            columns = columns,
            columnTypes = columnTypes,
            dbClient = dbClient,
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onSuccess = {
                scope.launch {
                    loadData()
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_data_add_success))
                }
            }
        )
    }
    
    // 编辑数据对话框
    editingRow?.let { row ->
        AddEditDataDialog(
            dbName = dbName,
            tableName = tableName,
            columns = columns,
            columnTypes = columnTypes,
            dbClient = dbClient,
            isEdit = true,
            initialData = row,
            onDismiss = { editingRow = null },
            onSuccess = {
                scope.launch {
                    loadData()
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_data_update_success))
                }
            }
        )
    }
    
    // 删除确认对话框
    deletingRow?.let { row ->
        AlertDialog(
            onDismissRequest = { deletingRow = null },
            title = { Text(stringResource(R.string.title_confirm_delete)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_record)) },
            confirmButton = {
                OutlinedButton(onClick = { deletingRow = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val id = row["id"]
                        if (id != null) {
                            val success = dbClient.deleteData(
                                dbName,
                                tableName,
                                "id = ?",
                                arrayOf(id.toString())
                            )
                            if (success) {
                                loadData()
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_data_deleted))
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_delete_failed))
                            }
                        }
                    }
                    deletingRow = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }
    
    // 长按操作选择对话框
    if (showActionDialog && selectedRowIndex != null) {
        AlertDialog(
            onDismissRequest = { 
                showActionDialog = false
                selectedRowIndex = null
            },
            title = { Text(stringResource(R.string.title_select_action)) },
            text = { Text(stringResource(R.string.msg_select_record_action)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showActionDialog = false
                        selectedRowIndex = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            deletingRow = data[selectedRowIndex!!]
                            showActionDialog = false
                            selectedRowIndex = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                    Button(
                        onClick = {
                            editingRow = data[selectedRowIndex!!]
                            showActionDialog = false
                            selectedRowIndex = null
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        )
    }
}

@Composable
fun AddEditDataDialog(
    dbName: String,
    tableName: String,
    columns: List<String>,
    columnTypes: Map<String, String>,  // 字段名 -> 类型
    dbClient: UniversalDatabaseClient,
    isEdit: Boolean,
    initialData: Map<String, Any?> = emptyMap(),
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // 为每个列维护一个状态
    val columnValues = remember(columns, initialData) {
        mutableStateMapOf<String, String>().apply {
            columns.forEach { col ->
                put(col, initialData[col]?.toString() ?: "")
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) stringResource(R.string.title_edit_data) else stringResource(R.string.title_add_data)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                columns.filter { it != "id" || !isEdit }.forEach { col ->
                    OutlinedTextField(
                        value = columnValues[col] ?: "",
                        onValueChange = { columnValues[col] = it },
                        label = { Text(col) },
                        singleLine = true,
                        enabled = !(isEdit && col == "id"),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val values = mutableMapOf<String, Any?>()
                    columns.forEach { col ->
                        val value = columnValues[col]
                        if (!value.isNullOrBlank()) {
                            val columnType = columnTypes[col]?.uppercase() ?: "TEXT"
                            values[col] = when {
                                columnType.contains("TEXT") || columnType.contains("CHAR") || columnType.contains("CLOB") -> value
                                columnType.contains("INTEGER") || columnType.contains("INT") -> value.toLongOrNull() ?: value
                                columnType.contains("REAL") || columnType.contains("FLOAT") || columnType.contains("DOUBLE") || columnType.contains("NUMERIC") -> value.toDoubleOrNull() ?: value
                                columnType.contains("BLOB") -> value
                                else -> value
                            }
                        } else {
                            values[col] = null
                        }
                    }
                    
                    if (isEdit) {
                        val id = initialData["id"]
                        if (id != null) {
                            scope.launch {
                                val result = dbClient.updateData(
                                    dbName,
                                    tableName,
                                    values,
                                    "id = ?",
                                    arrayOf(id.toString())
                                )
                                if (result) {
                                    onSuccess()
                                    onDismiss()
                                }
                            }
                        }
                    } else {
                        scope.launch {
                            val result = dbClient.insertData(dbName, tableName, values)
                            if (result) {
                                onSuccess()
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = columns.any { col -> !(columnValues[col].isNullOrEmpty()) }
            ) {
                Text(if (isEdit) stringResource(R.string.action_update) else stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
