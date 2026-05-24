package dev.huidou.util.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.huidou.util.components.DataTable
import dev.huidou.util.provider.DatabaseClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val databaseClient = remember { DatabaseClient(context) }
    
    var users by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    
    // 加载用户数据
    LaunchedEffect(Unit) {
        try {
            val userList = databaseClient.getAllUsers()
            users = userList
            isConnected = true
        } catch (e: Exception) {
            isConnected = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("数据库管理")
                        Text(if (isConnected) "已连接到 AQQ 数据库" else "未连接", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
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
            Text(
                text = "用户列表 (${users.size} 条记录)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (!isConnected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ 无法连接到 AQQ 数据库")
                        Text("请确保 AQQ 应用已安装", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据，点击右上角添加")
                }
            } else {
                val headers = listOf("ID", "姓名", "邮箱", "年龄", "城市")
                val rows = users.map { user ->
                    listOf(
                        user["id"].toString(),
                        user["name"].toString(),
                        user["email"].toString(),
                        user["age"].toString(),
                        user["city"].toString()
                    )
                }
                
                DataTable(
                    headers = headers,
                    rows = rows,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email, age, city ->
                val success = databaseClient.insertUser(
                    name = name,
                    email = email,
                    age = age.toIntOrNull() ?: 0,
                    city = city
                )
                if (success) {
                    // 重新加载数据
                    users = databaseClient.getAllUsers()
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加用户") },
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
