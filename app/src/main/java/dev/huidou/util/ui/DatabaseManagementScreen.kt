package dev.huidou.util.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import dev.huidou.util.ui.database.DataBrowserScreen
import dev.huidou.util.ui.database.DatabaseListScreen
import dev.huidou.util.ui.database.TableListScreen

sealed class DatabaseNavigation {
    object DatabaseList : DatabaseNavigation()
    data class TableList(val dbName: String) : DatabaseNavigation()
    data class DataBrowser(val dbName: String, val tableName: String) : DatabaseNavigation()
}

@Composable
fun DatabaseManagementScreen() {
    var navigation by remember { mutableStateOf<DatabaseNavigation>(DatabaseNavigation.DatabaseList) }
    
    // 处理系统返回按钮
    when (val currentNav = navigation) {
        is DatabaseNavigation.TableList -> {
            BackHandler {
                navigation = DatabaseNavigation.DatabaseList
            }
        }
        is DatabaseNavigation.DataBrowser -> {
            BackHandler {
                navigation = DatabaseNavigation.TableList(currentNav.dbName)
            }
        }
        else -> {
            // DatabaseList 是根页面,不需要特殊处理返回
        }
    }
    
    when (val currentNav = navigation) {
        is DatabaseNavigation.DatabaseList -> {
            DatabaseListScreen(
                onDatabaseSelected = { dbName ->
                    navigation = DatabaseNavigation.TableList(dbName)
                }
            )
        }
        is DatabaseNavigation.TableList -> {
            TableListScreen(
                dbName = currentNav.dbName,
                onTableSelected = { tableName ->
                    navigation = DatabaseNavigation.DataBrowser(currentNav.dbName, tableName)
                },
                onBack = {
                    navigation = DatabaseNavigation.DatabaseList
                }
            )
        }
        is DatabaseNavigation.DataBrowser -> {
            DataBrowserScreen(
                dbName = currentNav.dbName,
                tableName = currentNav.tableName,
                onBack = {
                    navigation = DatabaseNavigation.TableList(currentNav.dbName)
                }
            )
        }
    }
}
