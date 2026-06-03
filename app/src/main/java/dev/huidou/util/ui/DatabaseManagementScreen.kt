package dev.huidou.util.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.huidou.util.R
import dev.huidou.util.ui.database.DataBrowserScreen
import dev.huidou.util.ui.database.DatabaseListScreen
import dev.huidou.util.ui.database.TableListScreen
import dev.huidou.util.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

sealed class DatabaseNavigation {
    object DatabaseList : DatabaseNavigation()
    data class TableList(val dbName: String) : DatabaseNavigation()
    data class DataBrowser(val dbName: String, val tableName: String) : DatabaseNavigation()
    object Settings : DatabaseNavigation()
    object About : DatabaseNavigation()
    object AboutApp : DatabaseNavigation()
}

@Composable
fun DatabaseManagementScreen(
    themeViewModel: ThemeViewModel
) {
    var navigation by remember { mutableStateOf<DatabaseNavigation>(DatabaseNavigation.DatabaseList) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
        is DatabaseNavigation.Settings -> {
            BackHandler {
                navigation = DatabaseNavigation.DatabaseList
            }
        }
        is DatabaseNavigation.About -> {
            BackHandler {
                navigation = DatabaseNavigation.Settings
            }
        }
        is DatabaseNavigation.AboutApp -> {
            BackHandler {
                navigation = DatabaseNavigation.Settings
            }
        }
        else -> {
            // DatabaseList 是根页面,不需要特殊处理返回
        }
    }

    // 当抽屉打开时，按返回键关闭抽屉
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onDatabaseClick = {
                        navigation = DatabaseNavigation.DatabaseList
                        scope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        navigation = DatabaseNavigation.Settings
                        scope.launch { drawerState.close() }
                    },
                    currentNavigation = navigation
                )
            }
        }
    ) {
        when (val currentNav = navigation) {
            is DatabaseNavigation.DatabaseList -> {
                DatabaseListScreen(
                    onDatabaseSelected = { dbName ->
                        navigation = DatabaseNavigation.TableList(dbName)
                    },
                    onMenuClick = {
                        scope.launch { drawerState.open() }
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
            is DatabaseNavigation.Settings -> {
                SettingsScreen(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onAboutClick = {
                        navigation = DatabaseNavigation.About
                    },
                    onAboutAppClick = {
                        navigation = DatabaseNavigation.AboutApp
                    },
                    themeViewModel = themeViewModel
                )
            }
            is DatabaseNavigation.About -> {
                AboutScreen(
                    onBack = {
                        navigation = DatabaseNavigation.Settings
                    }
                )
            }
            is DatabaseNavigation.AboutApp -> {
                AboutAppScreen(
                    onBack = {
                        navigation = DatabaseNavigation.Settings
                    }
                )
            }
        }
    }
}

/**
 * 侧边栏内容组件
 * 包含应用标题、菜单项（数据库管理、设置）
 */
@Composable
private fun DrawerContent(
    onDatabaseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    currentNavigation: DatabaseNavigation
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        // Drawer Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.drawer_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.drawer_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Menu: 数据库管理
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.menu_database))
            },
            selected = currentNavigation !is DatabaseNavigation.Settings,
            onClick = onDatabaseClick,
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Menu: 设置
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.menu_settings))
            },
            selected = currentNavigation is DatabaseNavigation.Settings,
            onClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(28.dp)
        )
    }
}
