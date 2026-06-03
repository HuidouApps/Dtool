package dev.huidou.util.ui



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.unit.dp
import dev.huidou.util.R

/**
 * 设置页面
 * UI 布局与数据库管理页保持一致：左侧 ic_list 菜单按钮 + 标题/副标题 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onAboutAppClick: () -> Unit = {}
) {

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
                icon = Icons.Filled.Language,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(R.string.language_settings),
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 主题设置
            SettingsItemCard(
                icon = Icons.Filled.Palette,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(R.string.theme_settings),
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════════════
            // Section: 关于
            // ═══════════════════════════════════════════════
            SectionTitle(text = stringResource(R.string.section_about))

            // 开发者页
            SettingsItemCard(
                icon = Icons.Filled.Info,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(R.string.visit_github),
                onClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 关于应用
            SettingsItemCard(
                icon = Icons.Filled.Info,
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(R.string.about_app_btn),
                onClick = onAboutAppClick
            )
        }
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
    icon: ImageVector,
    iconBackgroundColor: androidx.compose.ui.graphics.Color,
    iconTintColor: androidx.compose.ui.graphics.Color,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
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
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTintColor
                )
            }

            // 文字标题，左侧 16dp 间距
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
        }
    }
}
