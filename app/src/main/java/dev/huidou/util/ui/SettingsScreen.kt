package dev.huidou.util.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import java.util.Locale
import dev.huidou.util.R
import dev.huidou.util.provider.UniversalDatabaseClient
import dev.huidou.util.ui.theme.ThemeMode
import dev.huidou.util.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * 设置页面
 * UI 布局与数据库管理页保持一致：左侧 ic_list 菜单按钮 + 标题/副标题 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit = {},
    themeViewModel: ThemeViewModel,
    onOpenSourceLicenses: () -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 当前应用语言（跟随系统，或应用内/系统设置选定的语言）
    val appLocales = remember { AppCompatDelegate.getApplicationLocales() }
    val followSystemLabel = stringResource(R.string.language_system)
    val currentLanguageName = remember(appLocales, followSystemLabel) {
        val current = appLocales[0]
        if (current == null) followSystemLabel else current.getDisplayName(current)
    }

    // ==================== 数据库存储统计 ====================
    val dbClient = remember { UniversalDatabaseClient(context) }
    val scope = rememberCoroutineScope()
    var dbCount by remember { mutableStateOf(0) }
    var totalSize by remember { mutableStateOf(0L) }
    var isStatsLoading by remember { mutableStateOf(true) }

    fun loadStorageStats() {
        scope.launch {
            isStatsLoading = true
            dbCount = dbClient.getDatabases().size
            totalSize = dbClient.getDatabasesTotalSize()
            isStatsLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStorageStats()
    }

    val storageSubtitle = if (isStatsLoading) {
        stringResource(R.string.label_loading)
    } else {
        stringResource(R.string.label_db_storage, dbCount, formatSize(totalSize))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.title_app_settings))
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_list),
                            contentDescription = stringResource(R.string.cd_open_settings)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {




            // ═══════════════════════════════════════════════
            // Section: 设置
            // ═══════════════════════════════════════════════
            SectionTitle(text = stringResource(R.string.section_settings))

            // 语言设置
            SettingsItemCard(
                iconRes = R.drawable.ic_language,
                iconBackgroundColor = colorResource(R.color.light_and_night).copy(alpha = 0.1f),
                iconTintColor = colorResource(R.color.light_and_night),
                title = stringResource(R.string.language_settings),
                subtitle = currentLanguageName,
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 主题设置
            SettingsItemCard(
                iconRes = R.drawable.ic_palette,
                iconBackgroundColor = colorResource(R.color.light_and_night).copy(alpha = 0.1f),
                iconTintColor = colorResource(R.color.light_and_night),
                title = stringResource(R.string.theme_settings),
                onClick = { showThemeDialog = true }
            )

            // ═══════════════════════════════════════════════
            // Section: 存储
            // ═══════════════════════════════════════════════
            SectionTitle(text = stringResource(R.string.section_storage))

            // 数据库存储统计
            SettingsItemCard(
                iconRes = R.drawable.ic_storage,
                iconBackgroundColor = colorResource(R.color.light_and_night).copy(alpha = 0.1f),
                iconTintColor = colorResource(R.color.light_and_night),
                title = stringResource(R.string.database_storage),
                subtitle = storageSubtitle,
                onClick = { loadStorageStats() }
            )

            // ═══════════════════════════════════════════════
            // Section: 关于
            // ═══════════════════════════════════════════════
            SectionTitle(text = stringResource(R.string.section_about))

            // 开源许可
            SettingsItemCard(
                iconRes = R.drawable.ic_code,
                iconBackgroundColor = colorResource(R.color.light_and_night).copy(alpha = 0.1f),
                iconTintColor = colorResource(R.color.light_and_night),
                title = stringResource(R.string.title_open_source_licenses),
                onClick = onOpenSourceLicenses
            )

        }
    }

    // 主题切换对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            themeViewModel = themeViewModel,
            onDismiss = { showThemeDialog = false }
        )
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * 主题选择对话框
 */
@Composable
private fun ThemeSelectionDialog(
    themeViewModel: ThemeViewModel,
    onDismiss: () -> Unit
) {
    val currentMode by themeViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings)) },
        text = {
            Column {
                ThemeOptionRow(
                    label = stringResource(R.string.theme_mode_light),
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                ThemeOptionRow(
                    label = stringResource(R.string.theme_mode_dark),
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.DARK) }
                )
                ThemeOptionRow(
                    label = stringResource(R.string.theme_mode_system),
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.SYSTEM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 动态取色开关（Android 12+ 跟随壁纸配色）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeViewModel.setDynamicColor(!dynamicColorEnabled) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dynamic_color),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.dynamic_color_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dynamicColorEnabled,
                        onCheckedChange = { themeViewModel.setDynamicColor(it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    )
}

/**
 * 应用支持的语言列表（与 res/xml/locales_config.xml 保持一致）
 */
private val supportedLocaleTags = listOf(
    "zh-CN", "zh-TW", "zh-HK",
    "en-US", "en-GB",
    "ar-SA", "bn-BD", "de-DE", "el-GR", "es-ES", "fr-FR",
    "hi-IN", "id-ID", "it-IT", "ja-JP", "ko-KR", "pt-BR",
    "ru-RU", "th-TH", "tr-TR", "vi-VN"
)

/**
 * 语言选择对话框
 * 以 AppCompatDelegate 作为语言唯一事实来源：应用内选择实时生效（自动重建 Activity），
 * 并与系统「设置→应用→语言」（Android 13+）双向同步。
 */
@Composable
private fun LanguageSelectionDialog(
    onDismiss: () -> Unit
) {
    val currentLocales = remember { AppCompatDelegate.getApplicationLocales() }
    val currentTag = currentLocales[0]?.toLanguageTag()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_settings)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item(key = "system") {
                    ThemeOptionRow(
                        label = stringResource(R.string.language_system),
                        selected = currentLocales.isEmpty,
                        onClick = {
                            onDismiss()
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.getEmptyLocaleList()
                            )
                        }
                    )
                }
                items(items = supportedLocaleTags, key = { it }) { tag ->
                    val locale = Locale.forLanguageTag(tag)
                    ThemeOptionRow(
                        label = locale.getDisplayName(locale), // 以母语显示
                        selected = tag == currentTag,
                        onClick = {
                            onDismiss()
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(tag)
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Section 标题组件
 * 对应 XML 中的 TextView (textAppearanceTitleSmall)
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(
                start = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    )
}

/**
 * 设置项卡片组件
 * 对应 XML 中的 MaterialCardView + LinearLayout 结构
 * - 40dp 圆形图标背景 + 24dp 图标
 * - 右侧文字标题
 * - 12dp 圆角卡片，无阴影，无边框
 */
@Composable
private fun SettingsItemCard(
    @DrawableRes iconRes: Int,
    iconBackgroundColor: androidx.compose.ui.graphics.Color,
    iconTintColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium, // 12dp corner radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 40dp 圆形图标背景
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                // 24dp 图标
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTintColor
                )
            }

            // 文字标题 + 可选副标题，左侧 16dp 间距
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 格式化文件大小（与数据库列表页的 formatFileSize 保持一致）
 */
private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
