package dev.huidou.util.ui.database

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.huidou.util.provider.UniversalDatabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    dbName: String,
    onTableSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dbClient = remember { UniversalDatabaseClient(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<String?>(null) }
    
    fun loadTables(showLoading: Boolean = true) {
        scope.launch {
            if (showLoading) isLoading = true
            
            // 等待服务连接
            var retries = 0
            while (!dbClient.isServiceConnected() && retries < 5) {
                delay(500)
                retries++
            }
            
            tables = dbClient.getTables(dbName)
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        loadTables()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { 
                    Column {
                        Text("数据表管理")
                        Text(
                            text = if (isLoading) "加载中..." else "数据库: $dbName",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadTables() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "创建表")
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在加载表列表...")
                    }
                }
            } else if (tables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无数据表")
                        Text("点击右上角 + 创建表", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadTables() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("刷新")
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tables) { tableName ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTableSelected(tableName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tableName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(onClick = { tableToDelete = tableName }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 创建表对话框
    if (showCreateDialog) {
        CreateTableDialog(
            dbName = dbName,
            dbClient = dbClient,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                loadTables()
                scope.launch {
                    snackbarHostState.showSnackbar("表创建成功")
                }
            }
        )
    }
    
    // 删除确认对话框
    tableToDelete?.let { tableName ->
        AlertDialog(
            onDismissRequest = { tableToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除表 \"$tableName\" 吗？此操作不可恢复！") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = dbClient.dropTable(dbName, tableName)
                            if (success) {
                                loadTables()
                                snackbarHostState.showSnackbar("表已删除")
                            } else {
                                snackbarHostState.showSnackbar("删除失败")
                            }
                        }
                        tableToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { tableToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableDialog(
    dbName: String,
    dbClient: UniversalDatabaseClient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    
    // 字段列表: Pair<字段名, Pair<长度, Pair<类型, 是否主键>>>
    data class ColumnDef(
        var name: String = "",
        var length: String = "",
        var type: String = "INTEGER",
        var isPrimaryKey: Boolean = false
    )
    
    var columns by remember { 
        mutableStateOf(listOf(ColumnDef(name = "id", type = "INTEGER", isPrimaryKey = true))) 
    }
    
    val sqlTypes = listOf("INTEGER", "TEXT", "REAL", "BLOB", "NUMERIC")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建数据表") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 表名输入
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("表名") },
                    placeholder = { Text("例如: users") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "字段定义:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // 字段列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(columns.size) { index ->
                        val column = columns[index]
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 字段名
                                    OutlinedTextField(
                                        value = column.name,
                                        onValueChange = {
                                            columns = columns.toMutableList().apply {
                                                this[index] = column.copy(name = it)
                                            }
                                        },
                                        label = { Text("字段名") },
                                        placeholder = { Text("name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // 删除按钮(至少保留一个字段)
                                    if (columns.size > 1) {
                                        IconButton(onClick = {
                                            columns = columns.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除字段",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 字段长度
                                    OutlinedTextField(
                                        value = column.length,
                                        onValueChange = {
                                            columns = columns.toMutableList().apply {
                                                this[index] = column.copy(length = it)
                                            }
                                        },
                                        label = { Text("长度") },
                                        placeholder = { Text("可选") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // 数据类型
                                    ExposedDropdownMenuBox(
                                        expanded = false,
                                        onExpandedChange = { }
                                    ) {
                                        var expanded by remember { mutableStateOf(false) }
                                        
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                value = column.type,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("类型") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .weight(1f)
                                            )
                                            
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                sqlTypes.forEach { type ->
                                                    DropdownMenuItem(
                                                        text = { Text(type) },
                                                        onClick = {
                                                            columns = columns.toMutableList().apply {
                                                                this[index] = column.copy(type = type)
                                                            }
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // 主键复选框
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = column.isPrimaryKey,
                                            onCheckedChange = {
                                                columns = columns.toMutableList().apply {
                                                    this[index] = column.copy(isPrimaryKey = it)
                                                }
                                            }
                                        )
                                        Text("主键", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 添加字段按钮
                Button(
                    onClick = {
                        columns = columns + ColumnDef()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加字段")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 生成 SQL 列定义
                    val columnDefs = columns.map { col ->
                        val lengthPart = if (col.length.isNotBlank()) "(${col.length})" else ""
                        val pkPart = if (col.isPrimaryKey) " PRIMARY KEY" else ""
                        "${col.name} ${col.type}$lengthPart$pkPart"
                    }.joinToString(", ")
                    
                    val success = dbClient.createTable(dbName, tableName, columnDefs)
                    if (success) {
                        onSuccess()
                        onDismiss()
                    }
                },
                enabled = tableName.isNotBlank() && columns.all { it.name.isNotBlank() }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
