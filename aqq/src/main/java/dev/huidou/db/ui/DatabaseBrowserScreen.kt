package dev.huidou.db.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import dev.huidou.db.R
import java.io.File
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DatabaseBrowserScreen() {
    val context = LocalContext.current
    
    var databases by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDatabase by remember { mutableStateOf<String?>(null) }
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 加载数据库列表
    LaunchedEffect(Unit) {
        databases = getDatabaseList(context)
    }
    
    // 当选择数据库时，加载表列表
    LaunchedEffect(selectedDatabase) {
        if (selectedDatabase != null) {
            tables = getTableList(context, selectedDatabase!!)
        } else {
            tables = emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.title_database_list))
                        Text(
                            text = if (selectedDatabase != null) stringResource(R.string.label_current_database, selectedDatabase!!) else stringResource(R.string.label_select_database),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    databases = getDatabaseList(context)
                                    Toast.makeText(context, "数据库列表已刷新", Toast.LENGTH_SHORT).show()
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_db_24dp),
                            contentDescription = stringResource(R.string.cd_database)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 左侧：数据库列表
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = stringResource(R.string.label_database),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                
                HorizontalDivider()
                
                LazyColumn {
                    items(databases) { dbName ->
                        val isSelected = dbName == selectedDatabase
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { selectedDatabase = dbName },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = dbName,
                                modifier = Modifier.padding(12.dp),
                                style = if (isSelected) 
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                else 
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // 右侧：数据表列表
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_table_count, tables.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (selectedDatabase == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.hint_select_database))
                    }
                } else if (tables.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.label_no_tables))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tables) { tableName ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = tableName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 获取所有数据库文件列表
 */
private fun getDatabaseList(context: android.content.Context): List<String> {
    val dbPath = context.getDatabasePath("dummy").parentFile
    return dbPath?.listFiles { file ->
        file.name.endsWith(".db") || file.name.endsWith(".sqlite")
    }?.map { it.name }?.sorted() ?: emptyList()
}

/**
 * 获取指定数据库中的所有表名
 */
private fun getTableList(context: android.content.Context, dbName: String): List<String> {
    return try {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            return emptyList()
        }
        
        val database = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
        
        val cursor = database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
            null
        )
        
        val tables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()
        database.close()
        
        tables.sorted()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
