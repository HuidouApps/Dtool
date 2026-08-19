package dev.huidou.util.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.huidou.util.R

/**
 * 开源许可页面
 * 列出项目使用的开源库及其许可协议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_open_source_licenses)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(licenseItems) { item ->
                LicenseCard(item)
            }
        }
    }
}

private data class LicenseItem(
    val name: String,
    val license: String,
    val description: String
)

/**
 * 项目依赖的开源库列表（以 release 实际使用为准）。
 * 说明：debug-only 依赖（如 MTDataFilesProvider）不随应用发布，不在此列出。
 */
private val licenseItems = listOf(
    LicenseItem("Jetpack Compose", "Apache License 2.0", "声明式 UI 框架（Compose UI、Runtime、Foundation、Animation）"),
    LicenseItem("Material 3 & Material Icons", "Apache License 2.0", "Material Design 3 组件与图标库"),
    LicenseItem("AndroidX 组件", "Apache License 2.0", "Core KTX、Lifecycle、Activity、DataStore、Room 等"),
    LicenseItem("Kotlin", "Apache License 2.0", "编程语言"),
    LicenseItem("Kotlinx Coroutines", "Apache License 2.0", "协程库"),
    LicenseItem("DTool（本项目）", "MIT License", "Copyright (c) 2026 HuidouApps")
)

@Composable
private fun LicenseCard(item: LicenseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.license,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
