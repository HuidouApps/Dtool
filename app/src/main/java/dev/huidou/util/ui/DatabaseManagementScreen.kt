package dev.huidou.util.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    object OpenSourceLicenses : DatabaseNavigation()
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
                navigation = DatabaseNavigation.DatabaseList
            }
        }
        is DatabaseNavigation.AboutApp -> {
            BackHandler {
                navigation = DatabaseNavigation.DatabaseList
            }
        }
        is DatabaseNavigation.OpenSourceLicenses -> {
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                DrawerContent(
                    onDatabaseClick = {
                        navigation = DatabaseNavigation.DatabaseList
                        scope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        navigation = DatabaseNavigation.Settings
                        scope.launch { drawerState.close() }
                    },
                    onAboutClick = {
                        navigation = DatabaseNavigation.About
                        scope.launch { drawerState.close() }
                    },
                    onAboutAppClick = {
                        navigation = DatabaseNavigation.AboutApp
                        scope.launch { drawerState.close() }
                    },
                    currentNavigation = navigation
                )
            }
        }
    ) {
        // 防止深色模式下闪白的背景层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = navigation,
                transitionSpec = {
                    // 判断是前进还是后退
                    val isForward = when {
                        initialState is DatabaseNavigation.DatabaseList && targetState is DatabaseNavigation.TableList -> true
                        initialState is DatabaseNavigation.TableList && targetState is DatabaseNavigation.DataBrowser -> true
                        // 从侧边栏进入开发者页/关于应用视为前进
                        targetState is DatabaseNavigation.About || targetState is DatabaseNavigation.AboutApp -> true
                        // 从开发者页/关于应用返回主页视为后退
                        targetState is DatabaseNavigation.DatabaseList &&
                            (initialState is DatabaseNavigation.About || initialState is DatabaseNavigation.AboutApp) -> false
                        // 从设置页进入开源许可视为前进
                        targetState is DatabaseNavigation.OpenSourceLicenses -> true
                        // 从开源许可返回设置页视为后退
                        targetState is DatabaseNavigation.Settings && initialState is DatabaseNavigation.OpenSourceLicenses -> false
                        targetState is DatabaseNavigation.DatabaseList && initialState is DatabaseNavigation.TableList -> false
                        targetState is DatabaseNavigation.TableList && initialState is DatabaseNavigation.DataBrowser -> false
                        else -> true // 默认使用前进动画
                    }
                    
                    if (isForward) {
                        // 前进动画：从右滑入 + 淡入 + 轻微缩放，向左滑出
                        (
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) +
                            fadeIn(
                                animationSpec = tween(durationMillis = 350, delayMillis = 50)
                            ) +
                            scaleIn(
                                animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing),
                                initialScale = 0.95f
                            )
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth / 3 }
                            ) +
                            fadeOut(
                                animationSpec = tween(durationMillis = 250)
                            ) +
                            scaleOut(
                                animationSpec = tween(durationMillis = 350),
                                targetScale = 0.95f
                            )
                        )
                    } else {
                        // 后退动画：从左滑入 + 淡入，向右滑出
                        (
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ) +
                            fadeIn(
                                animationSpec = tween(durationMillis = 350, delayMillis = 50)
                            ) +
                            scaleIn(
                                animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing),
                                initialScale = 0.95f
                            )
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> fullWidth / 3 }
                            ) +
                            fadeOut(
                                animationSpec = tween(durationMillis = 250)
                            ) +
                            scaleOut(
                                animationSpec = tween(durationMillis = 350),
                                targetScale = 0.95f
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { currentNav ->
                when (currentNav) {
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
                            themeViewModel = themeViewModel,
                            onOpenSourceLicenses = {
                                navigation = DatabaseNavigation.OpenSourceLicenses
                            }
                        )
                    }
                    is DatabaseNavigation.About -> {
                        AboutScreen(
                            onBack = {
                                navigation = DatabaseNavigation.DatabaseList
                            }
                        )
                    }
                    is DatabaseNavigation.AboutApp -> {
                        AboutAppScreen(
                            onBack = {
                                navigation = DatabaseNavigation.DatabaseList
                            }
                        )
                    }
                    is DatabaseNavigation.OpenSourceLicenses -> {
                        OpenSourceLicensesScreen(
                            onBack = {
                                navigation = DatabaseNavigation.Settings
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取应用版本名称
 */
private fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
}

/**
 * 侧边栏内容组件
 * 包含应用标题、菜单项（主要功能：数据库管理、设置；关于：开发者页、关于应用）
 */
@Composable
private fun DrawerContent(
    onDatabaseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    currentNavigation: DatabaseNavigation
) {
    val context = LocalContext.current
    val versionName = getVersionName(context)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Drawer Header - 美化头部设计
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.drawer_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.drawer_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }

        // 分隔线
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 主要功能分组
        Text(
            text = stringResource(R.string.drawer_section_main),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
            fontWeight = FontWeight.SemiBold
        )

        // 数据库管理（数据库列表/表列表/数据浏览均视为选中）
        DrawerMenuItem(
            icon = Icons.Filled.Storage,
            label = stringResource(R.string.menu_database),
            selected = currentNavigation is DatabaseNavigation.DatabaseList ||
                currentNavigation is DatabaseNavigation.TableList ||
                currentNavigation is DatabaseNavigation.DataBrowser,
            onClick = onDatabaseClick
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 设置
        DrawerMenuItem(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.menu_settings),
            selected = currentNavigation is DatabaseNavigation.Settings,
            onClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 关于分组
        Text(
            text = stringResource(R.string.drawer_section_about),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
            fontWeight = FontWeight.SemiBold
        )

        // 开发者页
        DrawerMenuItem(
            icon = Icons.Filled.Person,
            label = stringResource(R.string.menu_developer_page),
            selected = currentNavigation is DatabaseNavigation.About,
            onClick = onAboutClick
        )

        // 关于应用
        DrawerMenuItem(
            icon = Icons.Filled.Info,
            label = stringResource(R.string.menu_about_app),
            selected = currentNavigation is DatabaseNavigation.AboutApp,
            onClick = onAboutAppClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // Footer - 底部版本信息
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            thickness = 1.dp
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "DTool v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 单个侧边栏菜单项
 * 选中状态高亮显示，未选中状态带边框和点击效果
 */
@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = true,
            onClick = onClick,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else {
        // 未选中状态 - 带边框和点击效果
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() }
                )
                // 与选中态 NavigationDrawerItem 的 56dp 最小高度保持一致，避免点击切换时高度跳变
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
