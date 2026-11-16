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
fun LibraryHubScreen(modifier: Modifier = Modifier) {
    var section by remember { mutableStateOf("Koleksi DinNote") }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = section == "Koleksi DinNote",
                onClick = { section = "Koleksi DinNote" },
                label = { Text("Kalimah & Hikmah") }
            )
            FilterChip(
                selected = section == "Kitab",
                onClick = { section = "Kitab" },
                label = { Text("Perpustakaan Kitab") }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        when (section) {
            "Kitab" -> KitabCatalogScreen(Modifier.weight(1f))
            else -> LibraryScreen(Modifier.weight(1f))
        }
    }
}
