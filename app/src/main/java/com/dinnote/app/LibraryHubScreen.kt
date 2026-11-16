package com.dinnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Keep the original offline library available while adding the Arabic book catalogue.
 * Never replace the Kalimah Tayyibah / Kalam Hikmah collections with an empty catalogue.
 */
@Composable
fun LibraryHubScreen(modifier: Modifier = Modifier) {
    var section by remember { mutableStateOf("Koleksi DinNote") }
    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        when (section) {
            "Kitab" -> KitabCatalogScreen(Modifier.weight(1f))
            else -> LibraryScreen(Modifier.weight(1f))
        }
    }
}
