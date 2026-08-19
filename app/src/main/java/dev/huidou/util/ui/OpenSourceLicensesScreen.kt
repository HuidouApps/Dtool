package dev.huidou.util.ui

import androidx.annotation.StringRes
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
    @StringRes val nameRes: Int,
    val license: String,
    @StringRes val descriptionRes: Int
)

/**
 * 项目依赖的开源库列表（以 release 实际使用为准）。
 * 说明：debug-only 依赖（如 MTDataFilesProvider）不随应用发布，不在此列出。
 * 库名与协议为专有名词不做本地化；描述文本走字符串资源，跟随当前语言。
 */
private val licenseItems = listOf(
    LicenseItem(R.string.license_name_jetpack_compose, "Apache License 2.0", R.string.license_desc_compose),
    LicenseItem(R.string.license_name_material3, "Apache License 2.0", R.string.license_desc_material3),
    LicenseItem(R.string.license_name_androidx, "Apache License 2.0", R.string.license_desc_androidx),
    LicenseItem(R.string.license_name_kotlin, "Apache License 2.0", R.string.license_desc_kotlin),
    LicenseItem(R.string.license_name_coroutines, "Apache License 2.0", R.string.license_desc_coroutines),
    LicenseItem(R.string.license_name_dtool, "MIT License", R.string.license_desc_dtool)
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
                text = stringResource(item.nameRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.license,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (item.descriptionRes != 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
