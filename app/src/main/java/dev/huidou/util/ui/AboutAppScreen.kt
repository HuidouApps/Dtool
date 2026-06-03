package dev.huidou.util.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.huidou.util.R

/**
 * 关于应用页面
 * 显示头像、贡献者信息、Kotlin 徽标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_app_btn)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── 头像 ──
            Image(
                painter = painterResource(R.mipmap.dev_huidou),
                contentDescription = "HuidouApps avatar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "HuidouApps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── 贡献者 ──
            Text(
                text = stringResource(R.string.about_contributors_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 贡献者 1
            ContributorRow(name = stringResource(R.string.about_contributor_1))
            Spacer(modifier = Modifier.height(12.dp))
            // 贡献者 2
            ContributorRow(name = stringResource(R.string.about_contributor_2))

            Spacer(modifier = Modifier.weight(1f))
            
            // ── Kotlin 徽标 ──
            KotlinBadge()
        }
    }
}

/**
 * 贡献者行
 */
@Composable
private fun ContributorRow(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Kotlin 徽标
 * 使用 Kotlin 官方品牌色 #7F52FF，包含官方多边形标记和文字
 */
@Composable
private fun KotlinBadge() {
    val kotlinPurple = Color(0xFF7F52FF)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kotlin 官方多边形标记
        Canvas(
            modifier = Modifier.size(32.dp)
        ) {
            val scaleX = size.width / 24f
            val scaleY = size.height / 24f
            
            // 第一个多边形（右下角三角形）
            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(1.3f * scaleX, 24f * scaleY)
                lineTo((1.3f + 11.3f) * scaleX, (24f - 11.5f) * scaleY)
                lineTo(24f * scaleX, 24f * scaleY)
                close()
            }
            drawPath(
                path = path1,
                color = kotlinPurple
            )
            
            // 第二个多边形（左上角三角形）
            val path2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(12f * scaleX, 0f)
                lineTo(0f, 12.5f * scaleY)
                close()
            }
            drawPath(
                path = path2,
                color = kotlinPurple
            )
            
            // 第三个多边形（主体部分）
            val path3 = androidx.compose.ui.graphics.Path().apply {
                moveTo(13.4f * scaleX, 0f)
                lineTo(0f, 14f * scaleY)
                lineTo(0f, 24f * scaleY)
                lineTo(12f * scaleX, 12f * scaleY)
                lineTo(24f * scaleX, 0f)
                close()
            }
            drawPath(
                path = path3,
                color = kotlinPurple
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Kotlin 文字
        Text(
            text = "Kotlin",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = kotlinPurple
        )
    }
}
