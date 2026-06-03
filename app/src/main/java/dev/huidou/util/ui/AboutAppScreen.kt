package dev.huidou.util.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    
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
            ContributorRow(
                name = stringResource(R.string.about_contributor_1),
                githubUrl = "https://github.com/huidoudour",
                onGitHubClick = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 贡献者 2
            ContributorRow(
                name = stringResource(R.string.about_contributor_2),
                githubUrl = "https://github.com/2249807346",
                onGitHubClick = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )

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
private fun ContributorRow(
    name: String,
    githubUrl: String,
    onGitHubClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGitHubClick(githubUrl) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = "GitHub",
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
 * 使用 ic_kotlin.xml 渐变矢量图标，包含官方多边形标记和文字
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
        // Kotlin 官方渐变多边形标记
        Icon(
            painter = painterResource(R.drawable.ic_kotlin),
            contentDescription = "Kotlin logo",
            modifier = Modifier.size(32.dp),
            tint = Color.Unspecified
        )
        
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
