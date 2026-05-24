package dev.huidou.util.ui

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
