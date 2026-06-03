package dev.huidou.util.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun DataTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    onRowClick: ((Int) -> Unit)? = null,
    onRowLongClick: ((Int) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    
    // 根据每列最长内容计算固定列宽，保证所有行列对齐
    val columnWidths = remember(headers, rows) {
        headers.indices.map { colIndex ->
            val maxLen = maxOf(
                headers[colIndex].length,
                rows.maxOfOrNull { it.getOrNull(colIndex)?.length ?: 0 } ?: 0
            )
            // 每字符约 8dp，加上 24dp 内边距，最小 60dp（短列不必过宽）
            maxOf((maxLen * 8 + 24).dp, 60.dp)
        }
    }
    
    Column(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        // Table headers
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            headers.forEachIndexed { index, header ->
                Box(
                    modifier = Modifier
                        .width(columnWidths[index])
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp)
                ) {
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Table rows
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.combinedClickable(
                    onClick = { onRowClick?.invoke(rowIndex) },
                    onLongClick = { onRowLongClick?.invoke(rowIndex) }
                )
            ) {
                row.forEachIndexed { colIndex, cell ->
                    val bgColor = if (rowIndex % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    
                    Box(
                        modifier = Modifier
                            .width(columnWidths[colIndex])
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(bgColor)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
