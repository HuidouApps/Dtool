package dev.huidou.db.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.huidou.database.AppDatabase
import dev.huidou.database.entity.User
import dev.huidou.database.repository.DatabaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseAppScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { DatabaseRepository(database) }
    val scope = rememberCoroutineScope()
    
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        repository.getAllUsers().collect { userList ->
            users = userList
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AQQ 数据库管理") },
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
                text = "用户数据 (${users.size} 条)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users.size) { index ->
                        val user = users[index]
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "${user.name} (ID: ${user.id})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "邮箱: ${user.email}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "年龄: ${user.age} | 城市: ${user.city}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email, age, city ->
                scope.launch {
                    repository.insertUser(
                        User(
                            name = name,
                            email = email,
                            age = age.toIntOrNull() ?: 0,
                            city = city
                        )
                    )
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
                    label = { Text("姓名") }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") }
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("年龄") }
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("城市") }
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
