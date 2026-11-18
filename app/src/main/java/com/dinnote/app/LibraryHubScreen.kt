package com.dinnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
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
    val context = LocalContext.current
    val background = remember(context) {
        runCatching { context.assets.open("maktabah_background.webp").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    Box(modifier = modifier.fillMaxSize()) {
        background?.let { bitmap ->
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                modifier = Modifier.matchParentSize(),
                alignment = Alignment.BottomCenter,
                contentScale = ContentScale.FillBounds)
        }
    Column(modifier = Modifier.fillMaxSize()) {
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

        when (section) {
            "Kitab" -> KitabCatalogScreen(Modifier.weight(1f))
            else -> LibraryScreen(Modifier.weight(1f), onNavigate = onNavigate)
        }
    }
}
}
