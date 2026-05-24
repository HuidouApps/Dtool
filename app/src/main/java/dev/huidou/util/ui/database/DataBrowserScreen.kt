package dev.huidou.util.ui.database

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { 
                    Column {
                        Text("数据浏览")
                        Text("$dbName > $tableName", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Add, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加数据")
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
                text = "共 ${data.size} 条记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无数据")
                        Text("点击右上角 + 添加数据", style = MaterialTheme.typography.bodySmall)
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
                    snackbarHostState.showSnackbar("数据添加成功")
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
                    snackbarHostState.showSnackbar("数据更新成功")
                }
            }
        )
    }
    
    // 删除确认对话框
    deletingRow?.let { row ->
        AlertDialog(
            onDismissRequest = { deletingRow = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗?") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            // 假设有 id 字段
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
                                    snackbarHostState.showSnackbar("数据已删除")
                                } else {
                                    snackbarHostState.showSnackbar("删除失败")
                                }
                            }
                        }
                        deletingRow = null
                    }
                ) {
                    Text("取消")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        // 假设有 id 字段
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
                                snackbarHostState.showSnackbar("数据已删除")
                            } else {
                                snackbarHostState.showSnackbar("删除失败")
                            }
                        }
                    }
                    deletingRow = null
                }) {
                    Text("删除")
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
            title = { Text("选择操作") },
            text = { Text("您想对这条记录执行什么操作?") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showActionDialog = false
                        selectedRowIndex = null
                    }
                ) {
                    Text("取消")
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
                        Text("删除")
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
                        Text("编辑")
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
    val columnValues = remember {
        mutableStateMapOf<String, String>().apply {
            columns.forEach { col ->
                put(col, initialData[col]?.toString() ?: "")
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑数据" else "添加数据") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(columns.filter { it != "id" || !isEdit }) { col ->
                    OutlinedTextField(
                        value = columnValues[col] ?: "",
                        onValueChange = { columnValues[col] = it },
                        label = { Text(col) },
                        singleLine = true,
                        enabled = !(isEdit && col == "id") // 编辑时不允许修改 id
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
                            // 根据字段类型来转换数据
                            val columnType = columnTypes[col]?.uppercase() ?: "TEXT"
                            values[col] = when {
                                // 如果是 TEXT 类型，保持字符串
                                columnType.contains("TEXT") || columnType.contains("CHAR") || columnType.contains("CLOB") -> value
                                // 如果是 INTEGER 类型，转换为整数
                                columnType.contains("INTEGER") || columnType.contains("INT") -> value.toLongOrNull() ?: value
                                // 如果是 REAL/FLOAT/DOUBLE 类型，转换为浮点数
                                columnType.contains("REAL") || columnType.contains("FLOAT") || columnType.contains("DOUBLE") || columnType.contains("NUMERIC") -> value.toDoubleOrNull() ?: value
                                // 如果是 BLOB 类型，保持原样
                                columnType.contains("BLOB") -> value
                                // 默认保持字符串
                                else -> value
                            }
                        } else {
                            // 空值处理
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
                Text(if (isEdit) "更新" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
