package dev.huidou.util.ui.database

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.huidou.util.provider.UniversalDatabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(
    onDatabaseSelected: (String) -> Unit
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
    
    LaunchedEffect(Unit) {
        loadDatabases()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("数据库管理")
                        Text(
                            text = if (isLoading) "加载中..." else "共 ${databases.size} 个数据库",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadDatabases() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = if (isLoading) Modifier else Modifier
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "创建数据库")
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
                        Text("正在加载数据库列表...")
                        if (!isServiceConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "等待服务连接...",
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
                        Text("暂无数据库")
                        Text("点击右上角 + 创建数据库", style = MaterialTheme.typography.bodySmall)
                        
                        // 刷新按钮
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadDatabases() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("刷新")
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(databases) { index, db ->
                        val dbName = db["name"] as String
                        val dbSize = db["size"] as Long
                        val lastModified = db["last_modified"] as Long
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onDatabaseSelected(dbName) },
                                    onLongClick = {
                                        selectedDatabaseIndex = index
                                        showActionDialog = true
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dbName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "大小: ${formatFileSize(dbSize)} | 修改时间: ${formatDate(lastModified)}",
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
                        snackbarHostState.showSnackbar("数据库创建成功")
                    } else {
                        snackbarHostState.showSnackbar("数据库创建失败")
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
            title = { Text("确认删除") },
            text = { Text("确定要删除数据库 \"$dbName\" 吗?此操作不可恢复!") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val success = dbClient.deleteDatabase(dbName)
                            if (success) {
                                loadDatabases()
                                snackbarHostState.showSnackbar("数据库已删除")
                            } else {
                                snackbarHostState.showSnackbar("删除失败")
                            }
                        }
                        databaseToDelete = null
                    }
                ) {
                    Text("取消")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = dbClient.deleteDatabase(dbName)
                        if (success) {
                            loadDatabases()
                            snackbarHostState.showSnackbar("数据库已删除")
                        } else {
                            snackbarHostState.showSnackbar("删除失败")
                        }
                    }
                    databaseToDelete = null
                }) {
                    Text("删除")
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
            title = { Text("选择操作") },
            text = { Text("您想对这个数据库执行什么操作?") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showActionDialog = false
                        selectedDatabaseIndex = null
                    }
                ) {
                    Text("取消")
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
                    Text("删除")
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
        title = { Text("创建数据库") },
        text = {
            OutlinedTextField(
                value = dbName,
                onValueChange = { dbName = it },
                label = { Text("数据库名称") },
                placeholder = { Text("例如: my_database.db") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dbName) },
                enabled = dbName.isNotBlank()
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
