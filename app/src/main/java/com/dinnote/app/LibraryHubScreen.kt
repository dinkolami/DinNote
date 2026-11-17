package com.dinnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Keep both original offline collections and the editable book catalogue. */
@Composable
fun LibraryHubScreen(modifier: Modifier = Modifier, onNavigate: (Int) -> Unit = {}) {
    var section by remember { mutableStateOf("Koleksi DinNote") }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = section == "Koleksi DinNote",
                onClick = { section = "Koleksi DinNote" },
                modifier = Modifier.weight(1f),
                label = { Text("📖 Kalimah & Hikmah", maxLines = 1) }
            )
            FilterChip(
                selected = section == "Kitab",
                onClick = { section = "Kitab" },
                modifier = Modifier.weight(1f),
                label = { Text("▤ Maktabah Kitab", maxLines = 1) }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        when (section) {
            "Kitab" -> KitabCatalogScreen(Modifier.weight(1f))
            else -> LibraryScreen(Modifier.weight(1f), onNavigate = onNavigate)
        }
    }
}
