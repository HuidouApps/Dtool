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
import androidx.compose.ui.res.stringResource
import dev.huidou.util.R
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
    var selectedUserIndex by remember { mutableStateOf<Int?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    
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
                errorMessage = context.getString(R.string.error_connect_failed, e.message)
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
                        Text(stringResource(R.string.title_database_management))
                        Text(
                            text = if (isConnected) stringResource(R.string.status_connected_aqq) else stringResource(R.string.status_not_connected),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadUsers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh)
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_user)
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
                    text = stringResource(R.string.label_user_list_count, users.size),
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
                        Text(stringResource(R.string.error_cannot_connect_aqq))
                        Text(stringResource(R.string.hint_ensure_aqq_running), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadUsers() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.label_no_data))
                        Text(stringResource(R.string.hint_add_user), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val headers = listOf(
                    stringResource(R.string.header_id),
                    stringResource(R.string.header_name),
                    stringResource(R.string.header_email),
                    stringResource(R.string.header_age),
                    stringResource(R.string.header_city)
                )
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
                    onRowLongClick = { index ->
                        selectedUserIndex = index
                        showActionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // 添加用户对话框
    if (showAddDialog) {
        UserDialog(
            title = stringResource(R.string.title_add_user),
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
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_add_success))
                    } else {
                        errorMessage = context.getString(R.string.msg_add_failed)
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_add_failed_retry))
                    }
                }
                showAddDialog = false
            }
        )
    }
    
    // 编辑用户对话框
    editingUser?.let { user ->
        UserDialog(
            title = stringResource(R.string.title_edit_user),
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
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_update_success))
                    } else {
                        errorMessage = context.getString(R.string.msg_update_failed)
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_update_failed_retry))
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
            title = { Text(stringResource(R.string.title_confirm_delete)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_user, user["name"].toString())) },
            confirmButton = {
                OutlinedButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = databaseClient.deleteUser(user["id"] as Int)
                        if (success) {
                            loadUsers()
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_delete_success))
                        } else {
                            errorMessage = context.getString(R.string.msg_delete_failed)
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_delete_failed_retry))
                        }
                    }
                    showDeleteConfirm = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }
    
    // 长按操作选择对话框
    if (showActionDialog && selectedUserIndex != null) {
        AlertDialog(
            onDismissRequest = { 
                showActionDialog = false
                selectedUserIndex = null
            },
            title = { Text(stringResource(R.string.title_select_action)) },
            text = { Text(stringResource(R.string.msg_select_user_action)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showActionDialog = false
                        selectedUserIndex = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = users[selectedUserIndex!!]
                            showActionDialog = false
                            selectedUserIndex = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                    Button(
                        onClick = {
                            editingUser = users[selectedUserIndex!!]
                            showActionDialog = false
                            selectedUserIndex = null
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
fun UserDialog(
    title: String,
    initialName: String = "",
    initialEmail: String = "",
    initialAge: String = "",
    initialCity: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var age by remember(initialAge) { mutableStateOf(initialAge) }
    var city by remember(initialCity) { mutableStateOf(initialCity) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.header_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.header_email)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text(stringResource(R.string.header_age)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.header_city)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, age, city) },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
