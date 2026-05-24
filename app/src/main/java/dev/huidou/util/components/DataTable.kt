package dev.huidou.util.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DataTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        // Table headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            headers.forEach { header ->
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp)
                ) {
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Table rows
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .background(
                        if (index % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
