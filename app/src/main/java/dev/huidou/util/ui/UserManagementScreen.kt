package dev.huidou.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.huidou.util.components.DataTable
import dev.huidou.util.provider.DatabaseClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val databaseClient = remember { DatabaseClient(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var users by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Map<String, Any?>?>(null) }
    
    // 加载用户数据函数
    fun loadUsers() {
        scope.launch {
            isLoading = true
            try {
                val userList = databaseClient.getAllUsers()
                users = userList
                isConnected = true
                errorMessage = null
            } catch (e: Exception) {
                isConnected = false
                errorMessage = "连接失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 初始加载
    LaunchedEffect(Unit) {
        loadUsers()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("数据库管理")
                        Text(
                            text = if (isConnected) "已连接到 AQQ 数据库" else "未连接",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadUsers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加用户"
                        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "用户列表 (${users.size} 条记录)",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 显示错误信息
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (!isConnected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ 无法连接到 AQQ 数据库")
                        Text("请确保 AQQ 应用已安装并运行", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadUsers() }) {
                            Text("重试")
                        }
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无数据")
                        Text("点击右上角 + 按钮添加用户", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val headers = listOf("ID", "姓名", "邮箱", "年龄", "城市", "操作")
                val rows = users.map { user ->
                    listOf(
                        user["id"].toString(),
                        user["name"].toString(),
                        user["email"].toString(),
                        user["age"].toString(),
                        user["city"].toString(),
                        "" // 操作列占位
                    )
                }
                
                DataTable(
                    headers = headers,
                    rows = rows,
                    onRowClick = { index ->
                        // 点击行时编辑
                        editingUser = users[index]
                    },
                    onActionClick = { index, action ->
                        when (action) {
                            "edit" -> editingUser = users[index]
                            "delete" -> showDeleteConfirm = users[index]
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // 添加用户对话框
    if (showAddDialog) {
        UserDialog(
            title = "添加用户",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email, age, city ->
                scope.launch {
                    val success = databaseClient.insertUser(
                        name = name,
                        email = email,
                        age = age.toIntOrNull() ?: 0,
                        city = city
                    )
                    if (success) {
                        loadUsers()
                        snackbarHostState.showSnackbar("添加成功")
                    } else {
                        errorMessage = "添加失败"
                        snackbarHostState.showSnackbar("添加失败，请重试")
                    }
                }
                showAddDialog = false
            }
        )
    }
    
    // 编辑用户对话框
    editingUser?.let { user ->
        UserDialog(
            title = "编辑用户",
            initialName = user["name"].toString(),
            initialEmail = user["email"].toString(),
            initialAge = user["age"].toString(),
            initialCity = user["city"].toString(),
            onDismiss = { editingUser = null },
            onConfirm = { name, email, age, city ->
                scope.launch {
                    val success = databaseClient.updateUser(
                        id = (user["id"] as Int),
                        name = name,
                        email = email,
                        age = age.toIntOrNull(),
                        city = city
                    )
                    if (success) {
                        loadUsers()
                        snackbarHostState.showSnackbar("更新成功")
                    } else {
                        errorMessage = "更新失败"
                        snackbarHostState.showSnackbar("更新失败，请重试")
                    }
                }
                editingUser = null
            }
        )
    }
    
    // 删除确认对话框
    showDeleteConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除用户 \"${user["name"]}\" 吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = databaseClient.deleteUser(user["id"] as Int)
                            if (success) {
                                loadUsers()
                                snackbarHostState.showSnackbar("删除成功")
                            } else {
                                errorMessage = "删除失败"
                                snackbarHostState.showSnackbar("删除失败，请重试")
                            }
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun UserDialog(
    title: String,
    initialName: String = "",
    initialEmail: String = "",
    initialAge: String = "",
    initialCity: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var age by remember { mutableStateOf(initialAge) }
    var city by remember { mutableStateOf(initialCity) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("年龄") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("城市") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, age, city) },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
