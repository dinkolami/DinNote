@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dinnote.app

import android.os.Bundle
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Save the last fatal exception locally for diagnosis without ADB.
        // Do not include note contents or editor text in the report.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable).take(16000)
                java.io.File(filesDir, "dinnote_last_crash.txt").writeText(
                    "DinNote crash\\nAndroid: ${android.os.Build.VERSION.RELEASE}\\n" +
                        "Device: ${android.os.Build.MODEL}\\n" + trace
                )
            } catch (_: Throwable) { }
            if (previousHandler != null) previousHandler.uncaughtException(thread, throwable)
            else android.os.Process.killProcess(android.os.Process.myPid())
        }
        val crashReport = java.io.File(filesDir, "dinnote_last_crash.txt")
        if (crashReport.exists()) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Laporan ralat DinNote")
                .setMessage("DinNote terhenti secara tiba-tiba. Kongsi laporan ralat untuk membantu pembaikan. Nota anda tidak disertakan.")
                .setPositiveButton("Kongsi laporan") { _, _ ->
                    val report = runCatching { crashReport.readText() }.getOrDefault("Laporan tidak dapat dibaca")
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Laporan crash DinNote")
                        putExtra(android.content.Intent.EXTRA_TEXT, report)
                    }
                    startActivity(android.content.Intent.createChooser(send, "Kongsi laporan ralat"))
                    crashReport.delete()
                }
                .setNegativeButton("Kemudian") { _, _ -> }
                .setNeutralButton("Padam laporan") { _, _ -> crashReport.delete() }
                .show()
        }
        // Keep the Android system navigation area white with a subtle divider.
        window.navigationBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.rgb(216, 222, 227)
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        // Use dark system status-bar icons (including the clock) on the light DinNote background.
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContent { DinNoteApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DinNoteApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var openNewNote by remember { mutableStateOf(false) }
    var openPinnedNoteId by remember { mutableStateOf<Long?>(null) }
    val appContext = LocalContext.current
    val installedVersion = remember {
        try {
            @Suppress("DEPRECATION")
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            info.versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }
    val islamicScheme = lightColorScheme(
        primary = Color(0xFF167E80),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5F3F0),
        onPrimaryContainer = Color(0xFF15575B),
        secondary = Color(0xFF6B7777),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF4F0E7),
        onSecondaryContainer = Color(0xFF493719),
        tertiary = Color(0xFFC8A66B),
        background = Color(0xFFFCF9F2),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF7F6F2),
        outline = Color(0xFFD7DEDA)
    )
    val appKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    MaterialTheme(colorScheme = islamicScheme) {
        Scaffold(
            modifier = Modifier.navigationBarsPadding().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // Reserve only the Android status-bar inset; no empty app header.
                Spacer(Modifier.fillMaxWidth().statusBarsPadding())
            },
            bottomBar = {
                if (!appKeyboardVisible) Surface(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD8DEE3)),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxWidth().height(48.dp)) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple("Utama", "⌂", 0),
                                Triple("Nota", "✎", 1),
                                Triple("Maktabah", "▤", 2),
                                Triple("Tetapan", "⚙", 4)
                            )
                            tabs.forEachIndexed { index, tab ->
                                if (index > 0) {
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        thickness = 1.dp,
                                        color = Color(0xFFE3ECEC)
                                    )
                                }
                                val active = selectedTab == tab.third
                                Surface(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                        selectedTab = tab.third
                                        if (tab.third == 1) openNewNote = false
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) Color(0xFFDDF4F2).copy(alpha = 0.92f)
                                            else Color.Transparent
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            tab.second,
                                            fontSize = 19.sp,
                                            lineHeight = 21.sp,
                                            color = if (active) Color(0xFF007F83) else Color(0xFF52666A)
                                        )
                                        Text(
                                            tab.first,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            maxLines = 1,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) Color(0xFF007F83) else Color(0xFF52666A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding), onOpenNotes = { openNewNote = false; selectedTab = 1 }, onOpenPinnedNote = { id -> openPinnedNoteId = id; selectedTab = 1 }, onOpenLibrary = { selectedTab = 2 }, onOpenSearch = { selectedTab = 3 }, onOpenSettings = { selectedTab = 4 })
                1 -> NotesScreen(Modifier.padding(padding), openNewNote = openNewNote, onNewNoteOpened = { openNewNote = false }, openNoteId = openPinnedNoteId, onNoteOpened = { openPinnedNoteId = null })
                2 -> LibraryHubScreen(Modifier.padding(padding), onNavigate = { tab -> selectedTab = tab; if (tab == 1) openNewNote = false })
                3 -> GlobalSearchScreen(Modifier.padding(padding))
                4 -> DisplaySettingsScreen(Modifier.padding(padding))
            }
        }
    }
}


@Composable
private fun PremiumSectionHeading(title: String, ornament: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 1.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = Color(0xFF006B5B),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 3.dp),
            border = BorderStroke(1.dp, Color(0xFFC69A3C)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(ornament, fontSize = 14.sp, color = Color(0xFFF1C86B))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFC69A3C)
        )
    }
}

@Composable
private fun HomeShortcut(label: String, symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(54.dp).clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5D9DD))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(symbol, color = Color(0xFF006B46), fontSize = 20.sp)
            Spacer(Modifier.width(9.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF183A30), maxLines = 1)
            Text("›", fontSize = 21.sp, color = Color(0xFF006B46))
        }
    }
}

@Composable
private fun DashboardLineIcon(kind: String) {
    androidx.compose.foundation.Canvas(Modifier.size(30.dp)) {
        val ink = Color(0xFF075B50)
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(ink, androidx.compose.ui.geometry.Offset(w*x1,h*y1),
                androidx.compose.ui.geometry.Offset(w*x2,h*y2), strokeWidth=stroke,
                cap=androidx.compose.ui.graphics.StrokeCap.Round)
        }
        fun rect(x: Float,y: Float,r: Float,b: Float) {
            drawRoundRect(ink, topLeft=androidx.compose.ui.geometry.Offset(w*x,h*y),
                size=androidx.compose.ui.geometry.Size(w*(r-x),h*(b-y)),
                cornerRadius=androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
        when(kind) {
            "note" -> { rect(.19f,.1f,.81f,.9f); line(.32f,.32f,.68f,.32f); line(.32f,.48f,.68f,.48f); line(.32f,.64f,.55f,.64f) }
            "library" -> { rect(.13f,.18f,.33f,.83f); rect(.39f,.12f,.59f,.83f); rect(.65f,.24f,.85f,.83f) }
            "search" -> { drawCircle(ink,w*.23f,androidx.compose.ui.geometry.Offset(w*.42f,h*.4f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke)); line(.59f,.59f,.85f,.85f) }
            "bookmark" -> { line(.27f,.12f,.73f,.12f); line(.27f,.12f,.27f,.9f); line(.73f,.12f,.73f,.9f); line(.27f,.9f,.5f,.72f); line(.5f,.72f,.73f,.9f) }
            "school" -> { rect(.13f,.22f,.87f,.81f); line(.5f,.22f,.5f,.81f); line(.22f,.36f,.4f,.36f); line(.6f,.36f,.78f,.36f); line(.22f,.51f,.4f,.51f); line(.6f,.51f,.78f,.51f) }
            "settings" -> { drawCircle(ink,w*.3f,androidx.compose.ui.geometry.Offset(w*.5f,h*.5f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke)); drawCircle(ink,w*.09f,androidx.compose.ui.geometry.Offset(w*.5f,h*.5f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke)); for(i in 0..7) { val a=i*Math.PI/4; val dx=kotlin.math.cos(a).toFloat(); val dy=kotlin.math.sin(a).toFloat(); line(.5f+.33f*dx,.5f+.33f*dy,.5f+.44f*dx,.5f+.44f*dy) } }
        }
    }
}

@Composable
private fun DinNoteDashboardTile(
    title: String, subtitle: String, symbol: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(106.dp).defaultMinSize(minWidth = 110.dp)
            .semantics { contentDescription = "Buka $title: $subtitle" }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFDF7),
        border = BorderStroke(1.dp, Color(0xFFB7D5C8)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DashboardLineIcon(symbol)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(38.dp).height(2.dp).background(Color(0xFFD3AE65)))
            Spacer(Modifier.height(5.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF183A30), maxLines = 2, lineHeight = 17.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF53736F),
                maxLines = 2, lineHeight = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Lightweight Islamic turquoise-and-gold geometry behind the home dashboard.
 * Drawn in Compose: no external bitmap, network or image decoder required.
 * Cards stay opaque white for clear Arabic and Malay reading.
 */
@Composable
private fun IslamicGeometricBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val cream = Color(0xFFFFF9EF)
        val pale = Color(0xFFE4D8C8)
        val teal = Color(0xFF009C9B)
        val gold = Color(0xFFD8AC61)
        drawRect(cream)
        val unit = 44.dp.toPx()
        val thin = 0.65.dp.toPx()
        val rows = (size.height / unit).toInt() + 2
        val cols = (size.width / unit).toInt() + 2
        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = col * unit + if (row % 2 == 0) 0f else unit / 2f
                val y = row * unit
                val radius = unit * 0.30f
                for (k in 0..7) {
                    val a = k * Math.PI / 4.0
                    val b = (k + 3) * Math.PI / 4.0
                    drawLine(
                        pale.copy(alpha = 0.34f),
                        androidx.compose.ui.geometry.Offset(x + radius * kotlin.math.cos(a).toFloat(), y + radius * kotlin.math.sin(a).toFloat()),
                        androidx.compose.ui.geometry.Offset(x + radius * kotlin.math.cos(b).toFloat(), y + radius * kotlin.math.sin(b).toFloat()),
                        strokeWidth = thin
                    )
                }
            }
        }
        fun star(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
            val shape = androidx.compose.ui.graphics.Path()
            for (i in 0..15) {
                val angle = i * Math.PI / 8.0 - Math.PI / 2.0
                val r = if (i % 2 == 0) radius else radius * 0.67f
                val px = cx + r * kotlin.math.cos(angle).toFloat()
                val py = cy + r * kotlin.math.sin(angle).toFloat()
                if (i == 0) shape.moveTo(px, py) else shape.lineTo(px, py)
            }
            shape.close()
            drawPath(shape, color.copy(alpha = alpha))
        }
        // Asymmetric ornaments echo the reference without obscuring content.
        star(-8.dp.toPx(), size.height * 0.22f, 112.dp.toPx(), gold, 0.19f)
        star(-8.dp.toPx(), size.height * 0.22f, 84.dp.toPx(), teal, 0.20f)
        star(size.width * 0.94f, size.height * 0.12f, 44.dp.toPx(), teal, 0.12f)
        star(size.width * 0.90f, size.height * 0.80f, 56.dp.toPx(), gold, 0.12f)
        star(size.width * 0.90f, size.height * 0.80f, 39.dp.toPx(), teal, 0.14f)
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenNotes: () -> Unit,
    onOpenPinnedNote: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val homePrefs = remember { context.getSharedPreferences("dinnote_display", android.content.Context.MODE_PRIVATE) }
    val savedBackgroundUri = homePrefs.getString("background_image_uri", "") ?: ""
    val savedBackgroundOpacity = homePrefs.getFloat("background_image_opacity", 0.18f)
    val homeBackgroundBitmap = remember(savedBackgroundUri) {
        if (savedBackgroundUri.isBlank()) null else runCatching {
            context.contentResolver.openInputStream(Uri.parse(savedBackgroundUri))?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    // Prefer the bundled Islamic Premium artwork when present; never crash on older APKs.
    // A user-selected wallpaper always takes precedence over the bundled design.
    val premiumBackgroundBitmap = remember(context) {
        runCatching {
            context.assets.open("islamic_premium_background.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val pinnedNotes = remember { mutableStateListOf<DinNote>() }
    val recentlyAddedNotes = remember { mutableStateListOf<DinNote>() }
    val reminder = remember { kalimahEntries.randomOrNull() }
    var expandedReminder by remember { mutableStateOf(false) }
    val pinnedDatabase = remember { NoteDatabase(context).also { it.migrateOldSingleNote(context) } }
    LaunchedEffect(Unit) {
        val activeNotes = pinnedDatabase.allNotes().filter { !it.archived }
        pinnedNotes.clear()
        pinnedNotes.addAll(activeNotes.filter { it.pinned }.sortedByDescending { it.updatedAt }.take(5))
        recentlyAddedNotes.clear()
        recentlyAddedNotes.addAll(activeNotes.sortedByDescending { it.updatedAt }.take(3))
    }
    Box(modifier = modifier.fillMaxSize()) {
        // Never stack the geometric pattern beneath a user-selected wallpaper.
        if (homeBackgroundBitmap == null && premiumBackgroundBitmap == null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.islamic_premium_emerald_gold),
                contentDescription = "Hiasan masjid Islamic Premium",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        val displayedBackground = homeBackgroundBitmap ?: premiumBackgroundBitmap
        if (displayedBackground != null) {
            androidx.compose.foundation.Image(
                bitmap = displayedBackground,
                contentDescription = if (homeBackgroundBitmap != null) "Latar pilihan pengguna" else "Latar Islamic Premium",
                contentScale = ContentScale.Crop,
                alpha = if (homeBackgroundBitmap != null) savedBackgroundOpacity.coerceIn(0.05f, 1f) else 0.28f,
                modifier = Modifier.matchParentSize()
            )
        }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DinNoteFrontHeader()
        Text(
            "Catat ilmu • Teroka kitab • Simpan hikmah",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF075B50)
        )
        Surface(
            color = Color(0xFF006B5B),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFC69A3C)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("✦", fontSize = 14.sp, color = Color(0xFFF1C86B))
                Text("Pintasan Utama", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DinNoteDashboardTile("Nota Saya", "Tulis & urus catatan", "note",
                Modifier.weight(1f), onOpenNotes)
            DinNoteDashboardTile("Maktabah", "Kitab & rujukan", "library",
                Modifier.weight(1f), onOpenLibrary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DinNoteDashboardTile("Carian", "Arab & Melayu", "search",
                Modifier.weight(1f), onOpenSearch)
            DinNoteDashboardTile("Penanda", "Kitab & bacaan", "bookmark",
                Modifier.weight(1f), onOpenLibrary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DinNoteDashboardTile("Mazhab Syafi‘i", "Kitab & hukum", "school",
                Modifier.weight(1f), onOpenLibrary)
            DinNoteDashboardTile("Tetapan", "Paparan & data", "settings",
                Modifier.weight(1f), onOpenSettings)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenNotes),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF075B50),
            border = BorderStroke(1.dp, Color(0xFFB48B39))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("✎", fontSize = 22.sp, color = Color(0xFFF1C86B))
                Spacer(Modifier.width(12.dp))
                Text("Catatan & Nota Saya", modifier = Modifier.weight(1f),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("›", fontSize = 22.sp, color = Color.White)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLibrary),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFFFDF7),
            border = BorderStroke(1.dp, Color(0xFFB48B39))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("▤", fontSize = 22.sp, color = Color(0xFF075B50))
                Spacer(Modifier.width(12.dp))
                Text("Buka Maktabah", modifier = Modifier.weight(1f),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B50))
                Text("›", fontSize = 22.sp, color = Color(0xFF075B50))
            }
        }
        if (pinnedNotes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 1.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF006B5B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC69A3C)),
                    shadowElevation = 2.dp
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✦", fontSize = 14.sp, color = Color(0xFFF1C86B))
                        Text("Nota Penting • Dipinkan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

            }
            pinnedNotes.forEach { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPinnedNote(note.id) },
                    color = Color(0xFFFFFEFA),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB9D6CE)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("✦", fontSize = 12.sp, color = Color(0xFFC49A48))
                        Text(note.title.ifBlank { "Tanpa tajuk" },
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF075B59), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("›", fontSize = 16.sp, color = Color(0xFF08766C))
                    }
                }
            }
        }
        if (recentlyAddedNotes.isNotEmpty()) {
            PremiumSectionHeading("Nota Terbaharu", "✎")
            recentlyAddedNotes.forEach { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPinnedNote(note.id) },
                    color = Color(0xFFFFFEFA),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB9D6CE)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("✎", fontSize = 12.sp, color = Color(0xFF08766C))
                        if (note.category.isNotBlank()) {
                            Text(note.category, fontSize = 10.sp, color = Color(0xFF53736F),
                                maxLines = 1)
                        }
                        Text(note.title.ifBlank { "Tanpa tajuk" },
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF075B59), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("›", fontSize = 16.sp, color = Color(0xFF08766C))
                    }
                }
            }
        }
        reminder?.let { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = "Buka hikmah hari ini dan baca keterangan penuh" }
                    .clickable { expandedReminder = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFDF7),
                border = BorderStroke(1.dp, Color(0xFFB9D6CE)),
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("✦ Hikmah Hari Ini", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF08766C))
                    Text(formattedNoteText(entry.arabic), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right,
                        fontSize = 18.sp, lineHeight = 30.sp,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
                        color = Color(0xFF183A30))
                    Text(formattedNoteText(entry.meaning), fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF52666A))
                }
            }
        }
        if (expandedReminder && reminder != null) {
            val entry = reminder
            AlertDialog(
                onDismissRequest = { expandedReminder = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                title = { Text(entry.title, color = Color(0xFF075B59)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            formattedNoteText(entry.arabic),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
                            fontSize = 22.sp,
                            lineHeight = 36.sp
                        )
                        UnifiedEditorDivider()
                        Text(formattedNoteText(entry.meaning), fontSize = 16.sp, lineHeight = 25.sp)
                        if (entry.transliteration.isNotBlank()) {
                            Text(formattedNoteText(entry.transliteration), fontWeight = FontWeight.Medium)
                        }
                        if (entry.reference.isNotBlank()) {
                            Text(formattedNoteText("Rujukan: ${entry.reference}"), fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expandedReminder = false }) { Text("Tutup") }
                }
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLibrary),
            color = Color(0xFF006B5B),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFC69A3C)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("▤", fontSize = 14.sp, color = Color(0xFFF1C86B))
                Text("Sambung Bacaan", modifier = Modifier.weight(1f),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Buka Maktabah ›", fontSize = 12.sp, color = Color.White)
            }
        }
        Text(
            "DinNote • Nota dan rujukan di hujung jari",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = Color(0xFF53736F)
        )
    }
    }
}

@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    openNewNote: Boolean = false,
    onNewNoteOpened: () -> Unit = {},
    openNoteId: Long? = null,
    onNoteOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val displayPrefs = remember { context.getSharedPreferences("dinnote_display", android.content.Context.MODE_PRIVATE) }
    val database = remember {
        NoteDatabase(context).also { it.migrateOldSingleNote(context) }
    }
    val notes = remember { mutableStateListOf<DinNote>() }
    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(false) }
    val noteBodyFocusRequester = remember { FocusRequester() }
    var focusBodyAfterDoubleTap by remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    val recoveryPrefs = remember { context.getSharedPreferences("dinnote_note_recovery", android.content.Context.MODE_PRIVATE) }
    var recoveryAvailable by remember { mutableStateOf(recoveryPrefs.contains("body")) }

    var selectedColor by remember { mutableStateOf("green") }
    // Note backgrounds are always white; legacy background preferences are ignored.
    var editorFontSize by remember { mutableFloatStateOf(displayPrefs.getInt("font_size", 19).toFloat()) }
    var symbolMenu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryDescription by remember { mutableStateOf("") }
    var categoryToDescribe by remember { mutableStateOf<String?>(null) }
    var editedCategoryDescription by remember { mutableStateOf("") }
    var showManageCategories by remember { mutableStateOf(false) }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var renamedCategory by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<DinNote?>(null) }
    val defaultCategories = listOf("Agama", "Pengajian", "Kitab", "Peribadi", "Kerja", "Idea", "Lain-lain")
    val categoryPrefs = remember { context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE) }
    val builtInCategories = remember {
        mutableStateListOf<String>().apply {
            val saved = categoryPrefs.getString("built_in_order", null)
            addAll(if (saved.isNullOrBlank()) defaultCategories else saved.split("|").filter { it.isNotBlank() })
        }
    }
    val customCategories = remember {
        mutableStateListOf<String>().apply {
            addAll(categoryPrefs.getStringSet("custom", emptySet()).orEmpty().sorted())
        }
    }
    val allCategories = builtInCategories + customCategories
    fun categoryDescription(name: String): String = categoryPrefs.getString("description_$name", "") ?: ""

    fun refreshNotes() {
        notes.clear()
        notes.addAll(database.allNotes())
    }

    LaunchedEffect(Unit) { refreshNotes() }
    if (recoveryAvailable && !showEditor) {
        AlertDialog(onDismissRequest = { recoveryAvailable = false },
            title = { Text("Pulihkan draf nota?") },
            text = { Text("DinNote menemui draf yang belum disahkan tersimpan. Pulihkan sebagai nota baharu supaya nota asal tidak ditindih.") },
            confirmButton = { TextButton(onClick = {
                editingId = null
                title = recoveryPrefs.getString("title", "") ?: ""
                val recovered = recoveryPrefs.getString("body", "") ?: ""
                body = TextFieldValue(recovered, selection = TextRange(recovered.length))
                selectedColor = recoveryPrefs.getString("color", "green") ?: "green"
                selectedCategory = recoveryPrefs.getString("category", "Lain-lain") ?: "Lain-lain"
                undoStack.clear(); redoStack.clear()
                recoveryAvailable = false
                readingMode = false; showEditor = true
            }) { Text("Pulihkan") } },
            dismissButton = { TextButton(onClick = {
                recoveryPrefs.edit().clear().apply()
                recoveryAvailable = false
            }) { Text("Buang draf") } })
    }


    LaunchedEffect(openNoteId) {
        if (openNoteId != null) {
            refreshNotes()
            notes.firstOrNull { it.id == openNoteId }?.let { item ->
                editingId = item.id
                title = item.title
                body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                undoStack.clear(); redoStack.clear()
                selectedColor = item.colorTag
                selectedCategory = item.category
                readingMode = true
                showEditor = true
            }
            onNoteOpened()
        }
    }

    LaunchedEffect(openNewNote) {
        if (openNewNote) {
            editingId = null
            title = ""
            body = TextFieldValue("")
            undoStack.clear(); redoStack.clear()
            selectedColor = "green"
            if (selectedCategory == "Semua" || selectedCategory !in allCategories) selectedCategory = "Lain-lain"
            readingMode = false; showEditor = true
            onNewNoteOpened()
        }
    }

    val visibleNotes = remember(notes.toList(), showArchived, selectedCategory, search) {
        val term = normalizeArabicSearch(search.trim())
        notes.filter {
            it.archived == showArchived &&
                (selectedCategory == "Semua" || it.category == selectedCategory) &&
                (term.isEmpty() || normalizeArabicSearch(it.title).contains(term) ||
                    normalizeArabicSearch(formattedNoteText(it.body).text).contains(term))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        var gridMode by remember { mutableStateOf(false) }
        var sortMode by remember { mutableStateOf("Tarikh") }
        var sortExpanded by remember { mutableStateOf(false) }
        var notesCategoryExpanded by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (showArchived) "Arkib Nota" else "Nota Saya", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
                Text("Tulis, susun dan simpan ilham anda", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showArchived = !showArchived; selectedCategory = "Semua"; search = "" }) {
                Icon(Icons.Default.DateRange, contentDescription = if (showArchived) "Nota Aktif" else "Arkib")
            }
            if (!showArchived) FilledIconButton(onClick = {
                editingId = null; title = ""; body = TextFieldValue("")
                undoStack.clear(); redoStack.clear(); selectedColor = "green"
                selectedCategory = if (selectedCategory == "Semua") "Lain-lain" else selectedCategory
                readingMode = false; showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = "Nota baharu") }
        }
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Cari nota anda...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            (listOf("Semua") + allCategories).distinct().forEach { category ->
                FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category },
                    label = { Text(category, fontSize = 11.sp) }, shape = RoundedCornerShape(50))
            }
            Box {
                IconButton(onClick = { notesCategoryExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Urus kategori") }
                DropdownMenu(expanded = notesCategoryExpanded, onDismissRequest = { notesCategoryExpanded = false }) {
                    DropdownMenuItem(text = { Text("+ Tambah kategori") }, onClick = { notesCategoryExpanded = false; showAddCategory = true })
                    DropdownMenuItem(text = { Text("Urus kategori") }, onClick = { notesCategoryExpanded = false; showManageCategories = true })
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("${visibleNotes.size} nota" + if (showArchived) " dalam arkib" else " tersimpan",
                modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                TextButton(onClick = { sortExpanded = true }) { Text(sortMode, fontSize = 11.sp) }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    listOf("Tarikh", "Tajuk", "Kategori").forEach { mode ->
                        DropdownMenuItem(text = { Text(mode) }, onClick = { sortMode = mode; sortExpanded = false })
                    }
                }
            }
            IconButton(onClick = { gridMode = !gridMode }) { Icon(if (gridMode) Icons.Default.List else Icons.Default.Menu, contentDescription = if (gridMode) "Paparan senarai" else "Paparan grid") }
        }

        if (visibleNotes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✦", fontSize = 26.sp, color = Color(0xFFB5964C))
                    Text(if (search.isNotBlank() || selectedCategory != "Semua") "Tiada nota ditemui" else if (showArchived) "Arkib nota masih kosong" else "Ruang nota anda masih kosong", fontWeight = FontWeight.Bold)
                    Text(
                        if (search.isNotBlank() || selectedCategory != "Semua") "Cuba kata carian atau kategori yang lain."
                        else if (showArchived) "Nota yang diarkibkan akan muncul di sini dan boleh dikembalikan bila-bila masa."
                        else "Catat idea, ilmu dan perkara penting anda di sini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val orderedNotes = visibleNotes.sortedWith(
                compareByDescending<DinNote> { it.pinned }.let { base ->
                    when (sortMode) {
                        "Tajuk" -> base.thenBy { it.title.lowercase() }
                        "Kategori" -> base.thenBy { it.category.lowercase() }.thenByDescending { it.updatedAt }
                        else -> base.thenByDescending { it.updatedAt }
                    }
                }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (gridMode) 2 else 1),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(orderedNotes.size, key = { orderedNotes[it].id }) { index ->
                    val item = orderedNotes[index]
                    var menuExpanded by remember(item.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                editingId = item.id
                                title = item.title
                                body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                                undoStack.clear(); redoStack.clear()
                                selectedColor = item.colorTag
                                selectedCategory = item.category
                                readingMode = true
                                showEditor = true
                            },
                            onDoubleClick = {
                                editingId = item.id
                                title = item.title
                                body = TextFieldValue(item.body, selection = TextRange(item.body.length))
                                undoStack.clear(); redoStack.clear()
                                selectedColor = item.colorTag
                                selectedCategory = item.category
                                readingMode = false
                                showEditor = true
                            }
                        ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEFA)),
                        border = BorderStroke(1.dp, Color(0xFFE5EAE3)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val tint = when {
                                item.category.contains("Kuliah", true) || item.category == "Pengajian" -> Color(0xFF987333)
                                item.category == "Idea" -> Color(0xFF8962A5)
                                item.category == "Kitab" -> Color(0xFF477A9D)
                                else -> Color(0xFF147A76)
                            }
                            if (!gridMode) Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(when(item.category) {
                                    "Kitab" -> Icons.Default.List
                                    "Idea" -> Icons.Default.Star
                                    else -> Icons.Default.Edit
                                }, contentDescription = null, tint = tint)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.title.ifBlank { "Tanpa tajuk" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text((if (item.pinned) "• " else "") + item.category + " · " +
                                    java.text.SimpleDateFormat("dd MMM", java.util.Locale("ms", "MY")).format(java.util.Date(item.updatedAt)),
                                    color = tint, fontSize = 10.sp, maxLines = 1)
                            }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu nota") }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        if (!showArchived) DropdownMenuItem(
                                            text = { Text(if (item.pinned) "Lepas pin" else "Pin nota") },
                                            onClick = { database.setPinned(item.id, !item.pinned); menuExpanded = false; refreshNotes() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Kongsi") },
                                            onClick = {
                                                val shareText = buildString {
                                                    if (item.title.isNotBlank()) append(item.title).append("\n\n")
                                                    append(formattedNoteText(item.body).text)
                                                }
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Kongsi nota"))
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (showArchived) "Kembalikan ke Nota Aktif" else "Arkibkan", color = MaterialTheme.colorScheme.primary) },
                                            onClick = { database.setArchived(item.id, !item.archived); menuExpanded = false; refreshNotes() }
                                        )
                                        DropdownMenuItem(text = { Text("Padam") }, onClick = { noteToDelete = item; menuExpanded = false })
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        AlertDialog(
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { showAddCategory = false; newCategoryName = ""; newCategoryDescription = "" },
            title = { Text("Tambah kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nama kategori") },
                    singleLine = true
                )
                OutlinedTextField(value = newCategoryDescription, onValueChange = { newCategoryDescription = it }, label = { Text("Deskripsi sendiri (pilihan)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    if (name.isNotEmpty() && name !in builtInCategories && name !in customCategories) {
                        customCategories.add(name)
                        context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                            .edit().putStringSet("custom", customCategories.toSet()).putString("description_$name", newCategoryDescription.trim()).apply()
                        selectedCategory = name
                    }
                    newCategoryName = ""
                    newCategoryDescription = ""
                    showAddCategory = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false; newCategoryName = ""; newCategoryDescription = "" }) { Text("Batal") }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { noteToDelete = null },
            title = { Text("Padam nota?") },
            text = { Text("Nota \"${note.title.ifBlank { "Tanpa tajuk" }}\" akan dipadam. Tindakan ini tidak boleh dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    database.delete(note.id)
                    noteToDelete = null
                    refreshNotes()
                }) { Text("Ya, padam") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Batal") }
            }
        )
    }
    if (showManageCategories) {
        AlertDialog(
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { showManageCategories = false },
            title = { Text("Urus kategori nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) { Text(category); val description = categoryDescription(category); if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { categoryToDescribe = category; editedCategoryDescription = categoryDescription(category); showManageCategories = false }) { Text("Deskripsi") }
                            TextButton(onClick = {
                                categoryToRename = category
                                renamedCategory = category
                                showManageCategories = false
                            }) { Text("Tukar nama") }
                            if (category in customCategories) {
                                TextButton(onClick = {
                                    categoryToDelete = category
                                    showManageCategories = false
                                }) { Text("Padam") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageCategories = false }) { Text("Selesai") }
            }
        )
    }

    categoryToDescribe?.let { name ->
        AlertDialog(shape = RoundedCornerShape(0.dp), containerColor = Color.White, tonalElevation = 0.dp, titleContentColor = Color(0xFF075B59), textContentColor = Color(0xFF263B3E), onDismissRequest = { categoryToDescribe = null }, title = { Text("Deskripsi: $name") }, text = { OutlinedTextField(value = editedCategoryDescription, onValueChange = { editedCategoryDescription = it }, label = { Text("Deskripsi sendiri (pilihan)") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { categoryPrefs.edit().putString("description_$name", editedCategoryDescription.trim()).apply(); categoryToDescribe = null }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { categoryToDescribe = null }) { Text("Batal") } })
    }

    categoryToRename?.let { oldName ->
        AlertDialog(
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { categoryToRename = null },
            title = { Text("Tukar nama kategori") },
            text = {
                OutlinedTextField(
                    value = renamedCategory,
                    onValueChange = { renamedCategory = it },
                    label = { Text("Nama baharu") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renamedCategory.trim()
                    if (newName.isNotBlank() && newName != oldName && newName !in allCategories) {
                        database.renameCategory(oldName, newName)
                        categoryPrefs.edit().putString("description_$newName", categoryDescription(oldName)).remove("description_$oldName").apply()
                        val customIndex = customCategories.indexOf(oldName)
                        if (customIndex >= 0) {
                            customCategories[customIndex] = newName
                        } else {
                            val builtInIndex = builtInCategories.indexOf(oldName)
                            if (builtInIndex >= 0) builtInCategories[builtInIndex] = newName
                            categoryPrefs.edit().putString("built_in_order", builtInCategories.joinToString("|")).apply()
                        }
                        categoryPrefs.edit().putStringSet("custom", customCategories.toSet()).apply()
                        if (selectedCategory == oldName) selectedCategory = newName
                        refreshNotes()
                    }
                    categoryToRename = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) { Text("Batal") }
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Padam kategori?") },
            text = {
                Text("Kategori \"$category\" akan dipadam. Nota di dalamnya tidak akan hilang; semuanya dipindahkan ke kategori Lain-lain.")
            },
            confirmButton = {
                TextButton(onClick = {
                    database.moveCategoryToDefault(category)
                    customCategories.remove(category)
                    categoryPrefs.edit().remove("description_$category").apply()
                    context.getSharedPreferences("dinnote_categories", android.content.Context.MODE_PRIVATE)
                        .edit().putStringSet("custom", customCategories.toSet()).apply()
                    if (selectedCategory == category) selectedCategory = "Semua"
                    categoryToDelete = null
                    refreshNotes()
                }) { Text("Padam kategori") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Batal") }
            }
        )
    }

    if (showEditor) {
        fun formatSelection(marker: String) {
            val updated = wrapEditorSelection(body, marker)
            if (updated == body) return
            undoStack.add(body)
            if (undoStack.size > 60) undoStack.removeAt(0)
            redoStack.clear()
            body = updated
        }
        fun resizeSelection(delta: Int) {
            if (body.selection.collapsed) {
                editorFontSize = (editorFontSize + delta).coerceIn(14f, 30f)
                displayPrefs.edit().putInt("font_size", editorFontSize.toInt()).apply()
            } else {
                val updated = resizeEditorTextSelection(body, delta, editorFontSize.toInt())
                if (updated != body) {
                    undoStack.add(body)
                    if (undoStack.size > 60) undoStack.removeAt(0)
                    redoStack.clear()
                    body = updated
                }
            }
        }
        fun persistNote(): Boolean {
            if (title.isBlank() && body.text.isBlank() && editingId == null) {
                recoveryPrefs.edit().clear().apply()
                recoveryAvailable = false
                return true
            }
            recoveryPrefs.edit().putString("title", title).putString("body", body.text)
                .putString("color", selectedColor).putString("category", selectedCategory).apply()
            return try {
                val id = database.save(title.trim(), body.text, editingId, selectedColor, selectedCategory)
                editingId = id
                refreshNotes()
                recoveryPrefs.edit().clear().apply()
                recoveryAvailable = false
                true
            } catch (e: Exception) {
                android.util.Log.e("DinNote", "Note save failed", e)
                android.widget.Toast.makeText(context, "Nota belum disimpan. Draf dikekalkan. Cuba lagi.", android.widget.Toast.LENGTH_LONG).show()
                false
            }
        }
        androidx.activity.compose.BackHandler(enabled = true) {
            if (persistNote()) showEditor = false
        }
        LaunchedEffect(title, body.text, selectedCategory, selectedColor, readingMode) {
            if (!readingMode && (editingId != null || title.isNotBlank() || body.text.isNotBlank())) {
                val saved = notes.firstOrNull { it.id == editingId }
                if (saved != null && saved.title == title.trim() && saved.body == body.text &&
                    saved.colorTag == selectedColor && saved.category == selectedCategory) {
                    recoveryPrefs.edit().clear().apply()
                    recoveryAvailable = false
                    return@LaunchedEffect
                }
                recoveryPrefs.edit().putString("title", title).putString("body", body.text)
                    .putString("color", selectedColor).putString("category", selectedCategory).apply()
                kotlinx.coroutines.delay(650)
                persistNote()
            } else if (!readingMode && editingId == null) {
                recoveryPrefs.edit().clear().apply()
                recoveryAvailable = false
            }
        }
        // Editor is part of the Activity composition, not a separate Dialog window.
        // Android IME resizing now uses the Activity's window and shared insets.
        val availableWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
        val editorGutter = ((availableWidth - 840).coerceAtLeast(0) / 2).dp
        val readingPadding = if (availableWidth >= 600) 24.dp else 8.dp
        Box(Modifier.fillMaxSize().background(Color.White).padding(horizontal = editorGutter)) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Column(Modifier.fillMaxSize()) {
                    if (readingMode) {
                        Column(
                            Modifier.fillMaxSize()
                                .padding(horizontal = readingPadding, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showEditor = false }, modifier = Modifier.size(40.dp)) {
                                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF007C80))
                                }
                                IconButton(onClick = { readingMode = false }, modifier = Modifier.size(40.dp)) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF007C80))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                title.ifBlank { "Tanpa tajuk" },
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = {},
                                    onDoubleClick = { readingMode = false }
                                ),
                                textAlign = TextAlign.Center,
                                fontSize = 19.sp,
                                lineHeight = 26.sp,
                                color = IslamicPremiumPalette.Emerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = Color(0xFF008F93))
                                Text(" ۞ ", fontSize = 19.sp, color = Color(0xFF008F93))
                                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = Color(0xFF008F93))
                            }
                            Spacer(Modifier.height(8.dp))
                            // Render the complete note in one text layout, matching the editor.
                            // Compose resolves mixed Arabic/Malay direction per paragraph.
                            if (body.text.contains("{{block:")) {
                                IslamicStructuredReader(body.text, editorFontSize) { readingMode = false }
                            } else ReferenceReaderText(
                                text = formattedNoteText(body.text),
                                modifier = Modifier.fillMaxWidth(),
                                onDoubleTap = { visibleOffset ->
                                    val sourceOffset = sourceOffsetForFormattedText(body.text, visibleOffset)
                                    body = body.copy(selection = TextRange(sourceOffset))
                                    focusBodyAfterDoubleTap = true
                                    readingMode = false
                                },
                                style = androidx.compose.ui.text.TextStyle(
                                    textDirection = TextDirection.ContentOrRtl,
                                    fontSize = editorFontSize.sp,
                                    lineHeight = (editorFontSize * displayPrefs.getFloat("line_spacing", 1.6f).coerceIn(1.2f, 2.2f)).sp,
                                    color = Color(0xFF263B3E)
                                )
                            )
                        }
                    } else {
                    LaunchedEffect(focusBodyAfterDoubleTap) {
                        if (focusBodyAfterDoubleTap) {
                            kotlinx.coroutines.delay(100)
                            noteBodyFocusRequester.requestFocus()
                            focusBodyAfterDoubleTap = false
                        }
                    }
                    Surface(color = Color.White) {
                        Row(
                            Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { if (persistNote()) showEditor = false }) {
                                Text("←", color = Color(0xFF087F83), fontSize = 23.sp)
                            }
                            Text(
                                "Edit",
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF103B50), fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1
                            )
                            TextButton(enabled = undoStack.isNotEmpty(), onClick = {
                                if (undoStack.isNotEmpty()) { redoStack.add(body); body = undoStack.removeAt(undoStack.lastIndex) }
                            }) { Text("↶", color = if (undoStack.isNotEmpty()) Color(0xFF087F83) else Color.LightGray, fontSize = 21.sp) }
                            TextButton(enabled = redoStack.isNotEmpty(), onClick = {
                                if (redoStack.isNotEmpty()) { undoStack.add(body); body = redoStack.removeAt(redoStack.lastIndex) }
                            }) { Text("↷", color = if (redoStack.isNotEmpty()) Color(0xFF087F83) else Color.LightGray, fontSize = 21.sp) }
                            TextButton(onClick = { if (persistNote()) readingMode = true }) {
                                Text("Simpan", color = Color(0xFF087F83), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    val focusKeyboard = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
                    var smartMenu by remember { mutableStateOf(false) }
                    var showEditorTools by remember { mutableStateOf(false) }
                    if (!focusKeyboard) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            java.text.SimpleDateFormat("dd MMM yyyy • h:mm a", java.util.Locale("ms", "MY"))
                                .format(java.util.Date()),
                            color = Color.Gray, fontSize = 13.sp
                        )
                        var categoryExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { categoryExpanded = true }) {
                                Text("◇  Kategori  ▾", color = Color(0xFF009F9A), fontSize = 13.sp)
                            }
                            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                allCategories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category) }, onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    })
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 0.dp),
                        shape = RoundedCornerShape(0.dp),
                        color = Color.White
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF087F83),
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (title.isEmpty()) Text("Catatan Baharu", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF087F83))
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = Color(0xFFD5DADD), thickness = 1.dp)
                                Spacer(Modifier.height(4.dp))
                                val bodyScrollState = rememberScrollState()
                                var bodyTextLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                                var bodyViewportHeight by remember { mutableIntStateOf(0) }
                                val keyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
                                LaunchedEffect(body.selection, bodyTextLayout, bodyViewportHeight, keyboardVisible) {
                                    // Allow IME resize and text layout to settle before positioning the caret.
                                    kotlinx.coroutines.delay(80)
                                    val layout = bodyTextLayout ?: return@LaunchedEffect
                                    if (bodyViewportHeight <= 0) return@LaunchedEffect
                                    val caret = runCatching { layout.getCursorRect(body.selection.end.coerceIn(0, body.text.length)) }.getOrNull() ?: return@LaunchedEffect
                                    val target = when {
                                        caret.bottom > bodyScrollState.value + bodyViewportHeight - 24f ->
                                            (caret.bottom - bodyViewportHeight + 48f).toInt()
                                        caret.top < bodyScrollState.value + 16f ->
                                            (caret.top - 24f).toInt()
                                        else -> bodyScrollState.value
                                    }
                                    if (target != bodyScrollState.value) {
                                        bodyScrollState.animateScrollTo(target.coerceIn(0, bodyScrollState.maxValue))
                                    }
                                }
                                val nativeToolbar = androidx.compose.ui.platform.LocalTextToolbar.current
                                val toolbarView = androidx.compose.ui.platform.LocalView.current
                                val openSmartMenu by rememberUpdatedState(newValue = { smartMenu = true })
                                val selectionToolbar = remember(toolbarView, nativeToolbar) {
                                    SmartSelectionToolbar(toolbarView, nativeToolbar) { openSmartMenu() }
                                }
                                DisposableEffect(selectionToolbar) {
                                    onDispose { selectionToolbar.hide() }
                                }
                                CompositionLocalProvider(androidx.compose.ui.platform.LocalTextToolbar provides selectionToolbar) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = body,
                                    // Same-length transformation keeps IME cursor offsets stable.
                                    onValueChange = { next ->
                                        if (next.text != body.text) {
                                            undoStack.add(body)
                                            if (undoStack.size > 60) undoStack.removeAt(0)
                                            redoStack.clear()
                                        }
                                        body = next
                                    },
                                    visualTransformation = remember { NoteFormattingTransformation() },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .focusRequester(noteBodyFocusRequester)
                                        .onSizeChanged { bodyViewportHeight = it.height }
                                        .verticalScroll(bodyScrollState),
                                    onTextLayout = { bodyTextLayout = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = editorFontSize.sp,
                                        color = Color(0xFF263B3E),
                                        lineHeight = (editorFontSize * displayPrefs.getFloat("line_spacing", 1.6f).coerceIn(1.2f, 2.2f)).sp,
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (body.text.isEmpty()) Text(
                                                "Sentuh di sini untuk menulis...",
                                                fontSize = 16.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = Color(0xFF667A7B)
                                            )
                                            inner()
                                        }
                                    }
                                )
                                }
                            }
                        }
                    }
                    UnifiedFocusToolbar(body, editorFontSize, ::formatSelection, { selectedColor ->
                        val updated = colorEditorSelection(body, selectedColor)
                        if (updated != body) {
                            undoStack.add(body)
                            if (undoStack.size > 60) undoStack.removeAt(0)
                            redoStack.clear()
                            body = updated
                        }
                    }, ::resizeSelection)
                    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick = { showEditorTools = !showEditorTools }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text(if (showEditorTools) "Tutup alat ▴" else "⋯ Alat editor", fontSize = 12.sp)
                        }
                    }
                    if (showEditorTools || smartMenu) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {

                    var smartSource by remember { mutableStateOf("") }
                    var smartResult by remember { mutableStateOf("") }
                    var smartLabel by remember { mutableStateOf("") }
                    var smartDialog by remember { mutableStateOf(false) }
                    var smartBusy by remember { mutableStateOf(false) }
                    // Smart Format: clear a selection, or confirm clearing the entire note.
                    var clearFormatConfirm by remember { mutableStateOf(false) }
                    fun applyClearFormat() {
                        val updated = clearEditorFormatting(body)
                        if (updated != body) {
                            undoStack.add(body)
                            if (undoStack.size > 60) undoStack.removeAt(0)
                            redoStack.clear()
                            body = updated
                        }
                    }
                    val pasteContext = androidx.compose.ui.platform.LocalContext.current
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(enabled = body.text.isNotEmpty(), onClick = {
                            if (body.selection.collapsed) clearFormatConfirm = true else applyClearFormat()
                        }) { Text("Tx  Buang format", color = Color(0xFF087F83), fontSize = 13.sp) }
                        var pasteMenu by remember { mutableStateOf(false) }
                        fun pasteText(keepFormat: Boolean) {
                            val clipboard = pasteContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val pasted = runCatching {
                                clipboard.primaryClip?.let { clip ->
                                    if (clip.itemCount > 0) {
                                        val item = clip.getItemAt(0)
                                        if (keepFormat) clipboardEditorText(item.coerceToStyledText(pasteContext))
                                        else item.coerceToText(pasteContext)?.toString()
                                    } else null
                                }
                            }.getOrNull()
                            if (!pasted.isNullOrEmpty()) {
                                val start = body.selection.min.coerceIn(0, body.text.length)
                                val end = body.selection.max.coerceIn(start, body.text.length)
                                undoStack.add(body.copy(composition = null))
                                if (undoStack.size > 60) undoStack.removeAt(0)
                                redoStack.clear()
                                body = TextFieldValue(body.text.replaceRange(start, end, pasted),
                                    selection = TextRange(start + pasted.length))
                                noteBodyFocusRequester.requestFocus()
                            } else {
                                android.widget.Toast.makeText(pasteContext, "Clipboard kosong", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        Box {
                            TextButton(onClick = { pasteMenu = true }) {
                                Text("▣ Tampal ▾", color = Color(0xFF087F83), fontSize = 13.sp)
                            }
                            DropdownMenu(expanded = pasteMenu, onDismissRequest = { pasteMenu = false }) {
                                DropdownMenuItem(text = { Text("Tampal teks biasa") }, onClick = { pasteMenu = false; pasteText(false) })
                                DropdownMenuItem(text = { Text("Tampal dengan format") }, onClick = { pasteMenu = false; pasteText(true) })
                                DropdownMenuItem(text = { Text("Tampal blok DinNote") }, onClick = {
                                    pasteMenu = false
                                    val transfer = pasteContext.getSharedPreferences("dinnote_smart_transfer", android.content.Context.MODE_PRIVATE)
                                    val block = transfer.getString("block", null)
                                    if (!block.isNullOrEmpty()) {
                                        val a = body.selection.min.coerceIn(0, body.text.length)
                                        val b = body.selection.max.coerceIn(a, body.text.length)
                                        val before = smartFormattedExcerpt(body.text, 0, a)
                                        val after = smartFormattedExcerpt(body.text, b, body.text.length)
                                        undoStack.add(body.copy(composition = null))
                                        if (undoStack.size > 60) undoStack.removeAt(0)
                                        redoStack.clear()
                                        body = TextFieldValue(before + block + after, selection = TextRange(before.length + block.length))
                                        noteBodyFocusRequester.requestFocus()
                                    } else android.widget.Toast.makeText(pasteContext, "Tiada blok DinNote disalin", android.widget.Toast.LENGTH_SHORT).show()
                                })
                            }
                        }
                    }
                    if (clearFormatConfirm) AlertDialog(
                        onDismissRequest = { clearFormatConfirm = false },
                        title = { Text("Buang semua format?") },
                        text = { Text("Isi nota dikekalkan. Warna, saiz dan penekanan tulisan akan dibuang.") },
                        confirmButton = { TextButton(onClick = { applyClearFormat(); clearFormatConfirm = false }) { Text("Buang format") } },
                        dismissButton = { TextButton(onClick = { clearFormatConfirm = false }) { Text("Batal") } }
                    )
                    // Smart Block: select the paragraph at the cursor without changing stored text.
                    var smartBlockMenu by remember { mutableStateOf(false) }
                    var noteVaultOpen by remember { mutableStateOf(false) }
                    if (noteVaultOpen) SmartVaultDialog(onDismiss = { noteVaultOpen = false }, onInsert = { block ->
                        val a = body.selection.min.coerceIn(0, body.text.length)
                        val b = body.selection.max.coerceIn(a, body.text.length)
                        val before = smartFormattedExcerpt(body.text, 0, a)
                        val after = smartFormattedExcerpt(body.text, b, body.text.length)
                        undoStack.add(body.copy(composition = null))
                        if (undoStack.size > 60) undoStack.removeAt(0)
                        redoStack.clear()
                        body = TextFieldValue(before + block + after, selection = TextRange(before.length + block.length))
                    })
                    val blockText = body.text
                    val blockCursor = body.selection.min.coerceIn(0, blockText.length)
                    val blockStart = if (blockCursor == 0) 0 else blockText.lastIndexOf('\n', blockCursor - 1) + 1
                    val blockEnd = blockText.indexOf('\n', blockCursor).let { if (it < 0) blockText.length else it }
                    val canSelectBlock = blockEnd > blockStart && blockText.substring(blockStart, blockEnd).isNotBlank()
                    Box(Modifier.wrapContentWidth()) {
                        TextButton(onClick = { smartBlockMenu = true }) {
                            Text("▤ Smart Block", color = if (canSelectBlock) Color(0xFF087F83) else Color.Gray, fontSize = 13.sp)
                        }
                        DropdownMenu(expanded = smartBlockMenu, onDismissRequest = { smartBlockMenu = false }, containerColor = Color.White, shape = RoundedCornerShape(12.dp)) {
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("▤", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Pilih perenggan", fontSize = 14.sp) }, onClick = {
                                smartBlockMenu = false
                                body = body.copy(selection = TextRange(blockStart, blockEnd), composition = null)
                            })
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("⧉", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Duplikasi perenggan", fontSize = 14.sp) }, onClick = {
                                smartBlockMenu = false
                                val paragraph = blockText.substring(blockStart, blockEnd)
                                undoStack.add(body)
                                if (undoStack.size > 60) undoStack.removeAt(0)
                                redoStack.clear()
                                val updated = blockText.substring(0, blockEnd) + "\n" + paragraph + blockText.substring(blockEnd)
                                body = TextFieldValue(updated, selection = TextRange(blockEnd + 1, blockEnd + 1 + paragraph.length))
                            })
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("↑", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Pindah ke atas", fontSize = 14.sp) }, enabled = blockStart > 0, onClick = {
                                smartBlockMenu = false
                                val previousEnd = blockStart - 1
                                val previousStart = if (previousEnd == 0) 0 else blockText.lastIndexOf('\n', previousEnd - 1) + 1
                                val paragraph = blockText.substring(blockStart, blockEnd)
                                val previous = blockText.substring(previousStart, previousEnd)
                                undoStack.add(body)
                                if (undoStack.size > 60) undoStack.removeAt(0)
                                redoStack.clear()
                                body = TextFieldValue(blockText.substring(0, previousStart) + paragraph + "\n" + previous + blockText.substring(blockEnd), selection = TextRange(previousStart, previousStart + paragraph.length))
                            })
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("↓", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Pindah ke bawah", fontSize = 14.sp) }, enabled = blockEnd < blockText.length, onClick = {
                                smartBlockMenu = false
                                val nextStart = blockEnd + 1
                                val nextEnd = blockText.indexOf('\n', nextStart).let { if (it < 0) blockText.length else it }
                                val paragraph = blockText.substring(blockStart, blockEnd)
                                val next = blockText.substring(nextStart, nextEnd)
                                undoStack.add(body)
                                if (undoStack.size > 60) undoStack.removeAt(0)
                                redoStack.clear()
                                val newStart = blockStart + next.length + 1
                                body = TextFieldValue(blockText.substring(0, blockStart) + next + "\n" + paragraph + blockText.substring(nextEnd), selection = TextRange(newStart, newStart + paragraph.length))
                            })
                            DropdownMenuItem(text = { Text("Simpan ke Smart Vault") }, enabled = canSelectBlock, onClick = {
                                smartBlockMenu = false
                                saveSmartVault(pasteContext, smartFormattedExcerpt(blockText, blockStart, blockEnd))
                                android.widget.Toast.makeText(pasteContext, "Petikan disimpan", android.widget.Toast.LENGTH_SHORT).show()
                            })
                            DropdownMenuItem(text = { Text("Buka Smart Vault") }, onClick = {
                                smartBlockMenu = false
                                noteVaultOpen = true
                            })
                            DropdownMenuItem(text = { Text("Salin dengan format DinNote") }, enabled = canSelectBlock, onClick = {
                                smartBlockMenu = false
                                val block = smartFormattedExcerpt(blockText, blockStart, blockEnd)
                                if (formattedNoteText(block).text.isNotBlank()) {
                                    pasteContext.getSharedPreferences("dinnote_smart_transfer", android.content.Context.MODE_PRIVATE)
                                        .edit().putString("block", block).apply()
                                    android.widget.Toast.makeText(pasteContext, "Blok disalin dengan format", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("❞", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Simpan sebagai petikan", fontSize = 14.sp) }, onClick = {
                                smartBlockMenu = false
                                smartSource = smartFormattedExcerpt(blockText, blockStart, blockEnd)
                                smartLabel = "Simpan petikan"
                                smartResult = ""
                                smartDialog = formattedNoteText(smartSource).text.isNotBlank()
                            })
                            HorizontalDivider(color = Color(0xFFE7EEEE))
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("文", color = Color(0xFF087F83), fontSize = 20.sp) }, text = { Text("Terjemah perenggan", fontSize = 14.sp) }, onClick = {
                                smartBlockMenu = false
                                body = body.copy(selection = TextRange(blockStart, blockEnd), composition = null)
                                smartSource = smartFormattedExcerpt(blockText, blockStart, blockEnd)
                                smartLabel = "Terjemah"
                                smartResult = ""
                                smartDialog = true
                                smartBusy = true
                                ArabicMalayTranslator.translate(formattedNoteText(smartSource).text) { result ->
                                    smartBusy = false
                                    smartResult = result.getOrElse { "" }
                                }
                            })
                            DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text("▧", color = Color(0xFF087F83), fontSize = 22.sp) }, text = { Text("Warnakan perenggan", fontSize = 14.sp) }, onClick = {
                                smartBlockMenu = false
                                undoStack.add(body)
                                if (undoStack.size > 60) undoStack.removeAt(0)
                                redoStack.clear()
                                body = colorEditorSelection(body.copy(selection = TextRange(blockStart, blockEnd), composition = null), 0xFF087F83L)
                            })
                        }
                    }

                    // Smart Touch: only appears for an explicit text selection.
                    if (!body.selection.collapsed) {
                        Box(Modifier.wrapContentWidth()) {
                            TextButton(onClick = { smartMenu = true }) {
                                Text("☝ ✦ Smart Touch", color = Color(0xFF087F83), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            DropdownMenu(expanded = smartMenu, onDismissRequest = { smartMenu = false }, containerColor = Color.White, shape = RoundedCornerShape(12.dp)) {
                                listOf("Box al-Quran", "Box Hadis", "Box Teks Arab", "Box Kalam Hikmah", "Highlight emas", "Highlight hijau", "Terjemah", "Tambah syarah", "Simpan petikan").forEach { action ->
                                    DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text(when (action) { "Terjemah" -> "文"; "Tambah syarah" -> "✎"; else -> "❞" }, color = Color(0xFF087F83), fontSize = 20.sp) }, text = { Text(action, fontSize = 14.sp) }, onClick = {
                                        smartMenu = false
                                        if (action.startsWith("Box")) {
                                            undoStack.add(body)
                                            redoStack.clear()
                                            body = insertIslamicEditorBlock(body, when (action) {
                                                "Box al-Quran" -> "quran"
                                                "Box Hadis" -> "hadith"
                                                "Box Teks Arab" -> "arabic"
                                                else -> "hikmah"
                                            })
                                        } else if (action.startsWith("Highlight")) {
                                            undoStack.add(body)
                                            redoStack.clear()
                                            body = highlightEditorSelection(body, if (action == "Highlight emas") "#F5E3A0" else "#D5ECDD")
                                        } else {
                                        val start = body.selection.min.coerceIn(0, body.text.length)
                                        val end = body.selection.max.coerceIn(start, body.text.length)
                                        smartSource = smartFormattedExcerpt(body.text, start, end)
                                        smartLabel = action
                                        smartResult = ""
                                        smartDialog = formattedNoteText(smartSource).text.isNotBlank()
                                        if (smartDialog && action == "Terjemah") {
                                            smartBusy = true
                                            ArabicMalayTranslator.translate(formattedNoteText(smartSource).text) { result ->
                                                smartBusy = false
                                                smartResult = result.getOrElse { "" }
                                            }
                                        }
                                        }
                                    })
                                }
                            }
                        }
                    }
                    if (smartDialog) {
                        AlertDialog(
                            onDismissRequest = { if (!smartBusy) smartDialog = false },
                            title = { Text(smartLabel) },
                            text = {
                                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                                    Text(formattedNoteText(smartSource), fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr))
                                    if (smartBusy) Text("Menyediakan terjemahan...")
                                    else if (smartLabel != "Simpan petikan") {
                                        if (smartLabel == "Terjemah") Text("Draf terjemahan — semak maksud sebelum menyimpan.", fontSize = 12.sp)
                                        OutlinedTextField(value = smartResult, onValueChange = { smartResult = it }, label = { Text("Huraian / terjemahan") })
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(enabled = !smartBusy && (smartLabel == "Simpan petikan" || smartResult.isNotBlank()), onClick = {
                                    if (smartLabel == "Simpan petikan") {
                                        database.save("Petikan: " + title.ifBlank { "Nota" }, smartSource, null)
                                        refreshNotes()
                                    } else {
                                        val insertion = "\n\n" + (if (smartLabel == "Terjemah") "Maksudnya (draf):" else "Syarah:") + "\n" + smartResult + "\n"
                                        val offset = body.selection.max.coerceIn(0, body.text.length)
                                        undoStack.add(body)
                                        if (undoStack.size > 60) undoStack.removeAt(0)
                                        redoStack.clear()
                                        body = TextFieldValue(body.text.substring(0, offset) + insertion + body.text.substring(offset), selection = TextRange(offset + insertion.length))
                                    }
                                    smartDialog = false
                                }) { Text("Simpan") }
                            },
                            dismissButton = { TextButton(enabled = !smartBusy, onClick = { smartDialog = false }) { Text("Batal") } }
                        )
                    }

                    // Smart Article: editable templates using existing note formatting tokens.
                    var showNoteTemplateMenu by remember { mutableStateOf(false) }
                    Box(Modifier.wrapContentWidth()) {
                        TextButton(onClick = { showNoteTemplateMenu = true }) {
                            Text("＋ Smart Article", color = Color(0xFF075D49), fontSize = 13.sp)
                        }
                        DropdownMenu(
                            expanded = showNoteTemplateMenu,
                            onDismissRequest = { showNoteTemplateMenu = false }
                        ) {
                            val templates = listOf(
                                "Artikel Islamik: ayat, maksud dan perhatian" to listOf("Firman Allah SWT:", "[Ayat Arab]", "Maksudnya:", "[Terjemahan]", "Rujukan:", "[Surah dan nombor ayat]", "Perhatian:", "[Huraian]"),
                                "Hadis dan pengajaran" to listOf("Sabda Rasulullah SAW:", "[Matan hadis]", "Maksudnya:", "[Terjemahan]", "Riwayat:", "[Sumber dan nombor hadis]", "Pengajaran:", "[Huraian]"),
                                "Petikan kitab dan syarah" to listOf("Petikan kitab:", "[Teks Arab]", "Maksudnya:", "[Terjemahan]", "Rujukan:", "[Kitab, pengarang, jilid dan halaman]", "Syarah:", "[Huraian]"),
                                "Faedah dan kesimpulan" to listOf("Faedah:", "[Isi utama]", "Kesimpulan:", "[Rumusan]"),
                                "Blok • Firman Allah" to listOf("Firman Allah SWT:", "[Ayat Arab]"),
                                "Blok • Hadis" to listOf("Sabda Rasulullah SAW:", "[Matan hadis]"),
                                "Blok • Kalam ulama" to listOf("Kalam ulama:", "[Petikan]"),
                                "Blok • Maksud" to listOf("Maksudnya:", "[Terjemahan]"),
                                "Blok • Perhatian" to listOf("Perhatian:", "[Huraian]"),
                                "Blok • Faedah" to listOf("Faedah:", "[Isi utama]")
                            )
                            templates.forEach { (label, lines) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        val template = lines.chunked(2).joinToString("\n\n", prefix = "\n\n", postfix = "\n\n") { pair ->
                                            "{{color:#075D49}}**" + pair.first() + "**{{/color}}\n" + pair.drop(1).joinToString("\n")
                                        }
                                        val start = body.selection.max.coerceIn(0, body.text.length)
                                        val updated = body.text.substring(0, start) + template + body.text.substring(start)
                                        undoStack.add(body)
                                        if (undoStack.size > 60) undoStack.removeAt(0)
                                        redoStack.clear()
                                        val placeholderStart = template.indexOf('[')
                                        val placeholderEnd = template.indexOf(']', placeholderStart)
                                        val selection = if (placeholderStart >= 0 && placeholderEnd >= placeholderStart) TextRange(start + placeholderStart, start + placeholderEnd + 1) else TextRange(start + template.length)
                                        body = TextFieldValue(updated, selection = selection)
                                        showNoteTemplateMenu = false
                                        noteBodyFocusRequester.requestFocus()
                                    }
                                )
                            }
                        }
                    }
                        }
                    }

                    }
                }
            }
        }
    }
    } // NotesScreen root Box
}

/**
 * Render Markdown emphasis directly while editing, without changing underlying
 * storage or cursor offsets. Every source character has one transformed UTF-16
 * character; formatting delimiters become invisible word-joiners.
 * This avoids the offset-mapping crashes seen with shrinking transformations.
 */

// Native floating selection toolbar with a DinNote action. The standard Compose
// toolbar remains responsible for cursor-only menus.
private class SmartSelectionToolbar(
    private val view: android.view.View,
    private val fallback: androidx.compose.ui.platform.TextToolbar,
    private val onSmart: () -> Unit
) : androidx.compose.ui.platform.TextToolbar {
    private var mode: android.view.ActionMode? = null
    private var rect = androidx.compose.ui.geometry.Rect.Zero
    override val status: androidx.compose.ui.platform.TextToolbarStatus
        get() = if (mode != null) androidx.compose.ui.platform.TextToolbarStatus.Shown else fallback.status

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (onCopyRequested == null && onCutRequested == null) {
            mode?.finish()
            fallback.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
            return
        }
        fallback.hide()
        this.rect = rect
        mode?.finish()
        val actions = listOf(
            "Copy" to onCopyRequested, "Paste" to onPasteRequested,
            "Cut" to onCutRequested, "Select all" to onSelectAllRequested,
            "✦ Smart Touch" to onSmart
        )
        mode = view.startActionMode(object : android.view.ActionMode.Callback2() {
            override fun onCreateActionMode(actionMode: android.view.ActionMode, menu: android.view.Menu): Boolean {
                actions.forEachIndexed { index, entry ->
                    if (entry.second != null) menu.add(0, index + 1, index, entry.first)
                        .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                return true
            }
            override fun onPrepareActionMode(actionMode: android.view.ActionMode, menu: android.view.Menu) = false
            override fun onActionItemClicked(actionMode: android.view.ActionMode, item: android.view.MenuItem): Boolean {
                val action = actions.getOrNull(item.itemId - 1)?.second ?: return false
                action()
                return true
            }
            override fun onDestroyActionMode(actionMode: android.view.ActionMode) {
                if (mode === actionMode) mode = null
            }
            override fun onGetContentRect(actionMode: android.view.ActionMode, target: android.view.View, outRect: android.graphics.Rect) {
                val bounds = this@SmartSelectionToolbar.rect
                outRect.set(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
            }
        }, android.view.ActionMode.TYPE_FLOATING)
        if (mode == null) fallback.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
    }
    override fun hide() {
        mode?.finish()
        mode = null
        fallback.hide()
    }
}

private fun clipboardEditorText(input: CharSequence?): String? {
    if (input == null) return null
    if (input !is android.text.Spanned) return input.toString()
    val out = StringBuilder()
    var start = 0
    while (start < input.length) {
        val end = input.nextSpanTransition(start, input.length, android.text.style.CharacterStyle::class.java)
        val styles = input.getSpans(start, end, android.text.style.StyleSpan::class.java)
        val bold = styles.any { it.style and android.graphics.Typeface.BOLD != 0 }
        val italic = styles.any { it.style and android.graphics.Typeface.ITALIC != 0 }
        val underline = input.getSpans(start, end, android.text.style.UnderlineSpan::class.java).isNotEmpty()
        val color = input.getSpans(start, end, android.text.style.ForegroundColorSpan::class.java).lastOrNull()
        val hex = color?.let { String.format(java.util.Locale.ROOT, "#%06X", it.foregroundColor and 0xFFFFFF) }
        if (hex != null) out.append("{{color:").append(hex).append("}}")
        if (bold) out.append("**")
        if (italic) out.append("_")
        if (underline) out.append("++")
        out.append(input.subSequence(start, end).toString())
        if (underline) out.append("++")
        if (italic) out.append("_")
        if (bold) out.append("**")
        if (hex != null) out.append("{{/color}}")
        start = end
    }
    return out.toString()
}

private fun clearEditorFormatting(value: TextFieldValue): TextFieldValue {
    val source = value.text
    val start = value.selection.min.coerceIn(0, source.length)
    val end = value.selection.max.coerceIn(start, source.length)
    if (start == end) {
        val plain = formattedNoteText(source).text
        val cursor = formattedNoteText(smartFormattedExcerpt(source, 0, start)).length.coerceIn(0, plain.length)
        return TextFieldValue(plain, selection = TextRange(cursor))
    }
    val before = smartFormattedExcerpt(source, 0, start)
    val selected = formattedNoteText(smartFormattedExcerpt(source, start, end)).text
    val after = smartFormattedExcerpt(source, end, source.length)
    if (selected.isEmpty()) return value
    return TextFieldValue(before + selected + after,
        selection = TextRange(before.length, before.length + selected.length))
}

private fun exactEditorFormatOpener(source: String, start: Int, end: Int, kind: String): NoteFormatToken? {
    val (tokens, ranges) = noteFormatRanges(source)
    val hidden = BooleanArray(source.length)
    tokens.forEach { token -> for (i in token.start until token.end) hidden[i] = true }
    fun visibleBetween(a: Int, b: Int): Boolean =
        (a.coerceIn(0, source.length) until b.coerceIn(0, source.length)).any { !hidden[it] }
    val visibleSelected = (start until end).filter { !hidden[it] }
    val first = visibleSelected.firstOrNull() ?: return null
    val last = visibleSelected.last()
    return ranges.asSequence().mapNotNull { (a, b, _) ->
        val opener = tokens.firstOrNull { it.end == a && it.kind == kind && !it.closing } ?: return@mapNotNull null
        if (a <= first && b > last && !visibleBetween(a, first) && !visibleBetween(last + 1, b)) opener else null
    }.maxByOrNull { it.start }
}

private fun selectedEditorFontSize(value: TextFieldValue, fallback: Int): Int {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    if (start == end) return fallback
    val opening = Regex("""\{\{size:(\d{1,2})\}\}""")
    val preceding = opening.findAll(value.text.substring(0, start)).lastOrNull()
    val exact = preceding?.takeIf { it.range.last + 1 == start && value.text.startsWith("{{/size}}", end) }
    return exact?.groupValues?.get(1)?.toIntOrNull()
        ?: noteFormatRanges(value.text).second
            .filter { (a, b, style) -> a <= start && b >= end && style.fontSize.isSp }
            .lastOrNull()?.third?.fontSize?.value?.toInt()
        ?: fallback
}

private fun resizeEditorTextSelection(value: TextFieldValue, delta: Int, defaultSize: Int): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    if (start == end) return value
    val source = value.text
    val opening = Regex("""\{\{size:(\d{1,2})\}\}""")
    // Replace an existing exact selection wrapper instead of nesting size spans.
    val existing = exactEditorFormatOpener(source, start, end, "size")
    val current = existing?.let { opening.matchEntire(source.substring(it.start, it.end))?.groupValues?.get(1)?.toIntOrNull() }
        ?: noteFormatRanges(source).second
            .filter { (a, b, style) -> a <= start && b >= end && style.fontSize.isSp }
            .lastOrNull()?.third?.fontSize?.value?.toInt()
        ?: defaultSize
    val size = (current + delta).coerceIn(14, 30)
    if (size == current) return value
    if (existing != null) {
        val replacement = "{{size:$size}}"
        val updated = source.replaceRange(existing.start, existing.end, replacement)
        val shift = replacement.length - (existing.end - existing.start)
        fun remap(offset: Int) = if (offset >= existing.end) offset + shift else if (offset > existing.start) existing.start else offset
        return TextFieldValue(updated, selection = TextRange(remap(start), remap(end)))
    }
    val prefix = "{{size:$size}}"
    return TextFieldValue(
        source.replaceRange(start, end, prefix + source.substring(start, end) + "{{/size}}"),
        selection = TextRange(start + prefix.length, end + prefix.length)
    )
}

private val editorPalette = listOf(
    0xFF222222, 0xFF777777, 0xFFD32F2F, 0xFFE87924,
    0xFFB68A27, 0xFF16804A, 0xFF008C91, 0xFF2563C7
)

private fun colorEditorSelection(value: TextFieldValue, color: Long): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    if (start == end) return value
    val hex = "#%06X".format(color and 0xFFFFFF)
    val prefix = "{{color:$hex}}"
    val source = value.text
    // Recolor an existing selection without nesting color markers.
    val existing = exactEditorFormatOpener(source, start, end, "color")
    if (existing != null) {
        val updated = source.replaceRange(existing.start, existing.end, prefix)
        return TextFieldValue(updated, selection = TextRange(start, end))
    }
    val selected = source.substring(start, end)
    return TextFieldValue(
        source.replaceRange(start, end, prefix + selected + "{{/color}}"),
        selection = TextRange(start + prefix.length, end + prefix.length)
    )
}

@Composable
private fun EditorColorPicker(onPick: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { expanded = true },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
        modifier = Modifier.width(43.dp).height(32.dp)
    ) { Text("🎨", fontSize = 16.sp) }
    // Use an in-window dialog instead of a popup DropdownMenu. Popup
    // positioning has caused crashes on some foldable/IME configurations.
    if (expanded) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expanded = false }) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Warna tulisan", color = Color(0xFF087F83), fontWeight = FontWeight.SemiBold)
                    editorPalette.chunked(4).forEach { paletteRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            paletteRow.forEach { argb ->
                                Box(
                                    Modifier.size(48.dp)
                                        .background(Color(argb.toInt()), RoundedCornerShape(8.dp))
                                        .clickable {
                                            expanded = false
                                            onPick(argb)
                                        }
                                )
                            }
                        }
                    }
                    TextButton(onClick = { expanded = false }) { Text("Batal") }
                }
            }
        }
    }
}

private data class NoteFormatToken(val start: Int, val end: Int, val style: SpanStyle?, val closing: Boolean, val kind: String)

private fun noteFormatTokens(source: String): List<NoteFormatToken> {
    val pattern = Regex("""\{\{size:(\d{1,2})\}\}|\{\{/size\}\}|\{\{color:(#[0-9A-Fa-f]{6})\}\}|\{\{/color\}\}|\{\{highlight:(#[0-9A-Fa-f]{6})\}\}|\{\{/highlight\}\}|\{\{block:(quran|hadith|arabic|hikmah)\}\}|\{\{/block\}\}|\*\*|\+\+|_""")
    return pattern.findAll(source).map { m ->
        val token = m.value
        val kind = when {
            token.startsWith("{{color:") || token == "{{/color}}" -> "color"
            token.startsWith("{{highlight:") || token == "{{/highlight}}" -> "highlight"
            token.startsWith("{{block:") || token == "{{/block}}" -> "block"
            token.startsWith("{{") -> "size"
            token == "**" -> "bold"
            token == "++" -> "underline"
            else -> "italic"
        }
        val style = when (kind) {
            "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
            "underline" -> SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
            "color" -> m.groups[2]?.value?.let { SpanStyle(color = Color(android.graphics.Color.parseColor(it))) }
            "highlight" -> m.groups[3]?.value?.let { SpanStyle(background = Color(android.graphics.Color.parseColor(it))) }
            "block" -> m.groups[4]?.value?.let { SpanStyle(background = smartBlockColor(it), fontSize = 22.sp) }
            else -> m.groups[1]?.value?.toIntOrNull()?.let { SpanStyle(fontSize = it.coerceIn(14, 30).sp) }
        }
        NoteFormatToken(m.range.first, m.range.last + 1, style, (token == "{{/size}}" || token == "{{/color}}" || token == "{{/highlight}}" || token == "{{/block}}"), kind)
    }.toList()
}

/** Produce a self-contained excerpt with only effective formatting scopes. */
private fun smartFormattedExcerpt(source: String, start: Int, end: Int): String {
    val from = start.coerceIn(0, source.length)
    val to = end.coerceIn(from, source.length)
    val (paired, ranges) = noteFormatRanges(source)
    val hidden = BooleanArray(source.length)
    paired.forEach { token ->
        for (i in token.start until token.end) hidden[i] = true
    }
    val openers = paired.associateBy { it.end }
    val scopes = ranges.mapNotNull { range ->
        openers[range.first]?.let { Triple(range.first, range.second, it) }
    }
    val out = StringBuilder()
    var previous = emptyList<NoteFormatToken>()
    fun close(tokens: List<NoteFormatToken>) {
        tokens.asReversed().forEach { token ->
            out.append(when (token.kind) {
                "size" -> "{{/size}}"
                "color" -> "{{/color}}"
                "highlight" -> "{{/highlight}}"
                "block" -> "{{/block}}"
                else -> source.substring(token.start, token.end)
            })
        }
    }
    for (i in from until to) {
        if (hidden[i]) continue
        val current = scopes.filter { i >= it.first && i < it.second }
            .map { it.third }.groupBy { it.kind }.values
            .mapNotNull { it.maxByOrNull { token -> token.start } }
            .sortedBy { it.kind }
        // Compare actual markers rather than positions so identical nested styles collapse.
        if (current.map { source.substring(it.start, it.end) } != previous.map { source.substring(it.start, it.end) }) {
            close(previous)
            current.forEach { out.append(source.substring(it.start, it.end)) }
            previous = current
        }
        out.append(source[i])
    }
    close(previous)
    return out.toString()
}

private fun noteFormatRanges(source: String): Pair<List<NoteFormatToken>, List<Triple<Int, Int, SpanStyle>>> {
    val active = mutableMapOf<String, MutableList<NoteFormatToken>>()
    val ranges = mutableListOf<Triple<Int, Int, SpanStyle>>()
    val recognized = mutableListOf<NoteFormatToken>()
    noteFormatTokens(source).forEach { token ->
        val stack = active.getOrPut(token.kind) { mutableListOf() }
        // A size opener always starts a new scope, even inside another size scope.
        // Symmetric emphasis markers close their latest matching scope.
        if (token.closing || (token.kind in listOf("bold", "underline", "italic") && stack.isNotEmpty())) {
            if (stack.isNotEmpty()) {
                val opener = stack.removeAt(stack.lastIndex)
                val style = opener.style
                if (style != null && opener.end < token.start) {
                    ranges.add(Triple(opener.end, token.start, style))
                }
                recognized.add(opener)
                recognized.add(token)
            }
        } else if (token.style != null) {
            stack.add(token)
        }
    }
    // Explicit legacy size/color tokens are reserved metadata, even when an
    // old edit left them unbalanced. Keep the stored source intact but hide
    // these orphan tokens in both the reader and editor. Unpaired emphasis
    // remains literal because underscores and asterisks can be intentional.
    val hiddenTokens = (recognized + noteFormatTokens(source).filter {
        it.kind == "size" || it.kind == "color" || it.kind == "highlight" || it.kind == "block"
    }).distinctBy { it.start }.sortedBy { it.start }
    return hiddenTokens to ranges.sortedWith(compareBy<Triple<Int, Int, SpanStyle>> { it.first }.thenByDescending { it.second })
}

/** Apply inline emphasis to the selected span without changing unrelated words. */
private fun wrapEditorSelection(value: TextFieldValue, marker: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    if (start == end) return value
    val selected = value.text.substring(start, end)
    val replacement = marker + selected + marker
    return value.copy(
        text = value.text.replaceRange(start, end, replacement),
        selection = TextRange(start + marker.length, start + marker.length + selected.length)
    )
}

/** Hide paired legacy markup while preserving selection/cursor mapping. */
private class NoteFormattingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val (tokens, ranges) = noteFormatRanges(source)
        val hidden = BooleanArray(source.length)
        tokens.forEach { token ->
            for (i in token.start until token.end) hidden[i] = true
        }
        // Preserve 1:1 cursor offsets to avoid Compose IME mapping crashes.
        val display = StringBuilder(source.length)
        for (i in source.indices) display.append(if (hidden[i]) '\u2060' else source[i])
        val styled = buildAnnotatedString {
            append(display.toString())
            ranges.forEach { (start, end, style) ->
                if (end > start) addStyle(style, start, end)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/** Convert a position in the markup-free reader back to the stored editor text. */
private fun sourceOffsetForFormattedText(source: String, visibleOffset: Int): Int {
    val (tokens, _) = noteFormatRanges(source)
    var sourceIndex = 0
    var displayedIndex = 0
    val target = visibleOffset.coerceAtLeast(0)
    for (token in tokens) {
        while (sourceIndex < token.start && displayedIndex < target) {
            sourceIndex++
            displayedIndex++
        }
        if (displayedIndex >= target) return sourceIndex.coerceIn(0, source.length)
        sourceIndex = token.end
    }
    while (sourceIndex < source.length && displayedIndex < target) {
        sourceIndex++
        displayedIndex++
    }
    return sourceIndex.coerceIn(0, source.length)
}

fun formattedNoteText(source: String): AnnotatedString = buildAnnotatedString {
    val (tokens, ranges) = noteFormatRanges(source)
    var cursor = 0
    val positions = IntArray(source.length + 1)
    tokens.forEach { token ->
        while (cursor < token.start) {
            positions[cursor] = length
            append(source[cursor])
            cursor++
        }
        while (cursor < token.end) {
            positions[cursor] = length
            cursor++
        }
    }
    while (cursor < source.length) {
        positions[cursor] = length
        append(source[cursor])
        cursor++
    }
    positions[source.length] = length
    ranges.forEach { (start, end, style) ->
        val a = positions[start.coerceIn(0, source.length)]
        val b = positions[end.coerceIn(0, source.length)]
        if (a >= 0 && b > a && b <= length) addStyle(style, a, b)
    }
}
@Composable
fun noteColor(tag: String): Color = when (tag) {
    "yellow" -> Color(0xFFFFF4CC)
    "blue" -> Color(0xFFDCEEFF)
    "pink" -> Color(0xFFFFE1E8)
    "purple" -> Color(0xFFEDE2FF)
    else -> Color(0xFFE5F2E3)
}

private fun normalizeArabicSearch(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
        .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u0640\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7-\\u06E8\\u06EA-\\u06ED]"), "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')

private fun matchesMaktabahSearch(value: String, query: String): Boolean =
    normalizeArabicSearch(value).contains(normalizeArabicSearch(query.trim()))

@Composable
fun LibraryScreen(modifier: Modifier = Modifier, onNavigate: (Int) -> Unit = {}) {
    var selectedCategory by remember { mutableStateOf("Kitab-Kitab") }
    var selectedEntry by remember { mutableStateOf<KalimahEntry?>(null) }
    var editingKalimah by remember { mutableStateOf<KalimahEntry?>(null) }
    var kalimahArabic by remember { mutableStateOf("") }
    var kalimahMeaning by remember { mutableStateOf("") }
    var kalimahTransliteration by remember { mutableStateOf("") }
    var kalimahReference by remember { mutableStateOf("") }
    var kalimahNote by remember { mutableStateOf("") }
    var showKalimahSettings by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val context = LocalContext.current
    val libraryPrefs = remember { context.getSharedPreferences("dinnote_library_edits", android.content.Context.MODE_PRIVATE) }
    val libraryDisplayPrefs = remember { context.getSharedPreferences("dinnote_display", android.content.Context.MODE_PRIVATE) }
    var libraryFontSize by remember { mutableFloatStateOf(libraryDisplayPrefs.getInt("font_size", 19).toFloat()) }
    val preferredBookLayout = libraryDisplayPrefs.getString("book_layout", "Arab di atas, Melayu di bawah") ?: "Arab di atas, Melayu di bawah"
    val showArabicInLibrary = preferredBookLayout != "Melayu sahaja"
    val showMalayInLibrary = preferredBookLayout != "Arab sahaja"
    val malayFirstInLibrary = preferredBookLayout == "Melayu di atas, Arab di bawah"
    var libraryRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(libraryPrefs) {
        val seedKey = "entry_Kitab-Kitab_builtin_arbaun_nawawi"
        if (!libraryPrefs.getBoolean("seed_arbaun_v1", false)) {
            val edit = libraryPrefs.edit()
            if (!libraryPrefs.contains(seedKey + "_title")) {
                edit.putString(seedKey + "_title", "الأربعون النووية")
                edit.putString(seedKey + "_body", "الأربعون النووية — الإمام النووي\nالمصدر: https://shamela.ws/book/12836/12 — الأحاديث ١–٤٢\n\nالحديث الأول\n«عن أمير المؤمنين أبي حفص عمر بن الخطاب رضي الله تعالى عنه قال: سمعت رسول الله صلى الله تعالى عليه وعلى آله وسلم يقول: إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى، فمن كانت هجرته إلى الله ورسوله فهجرته إلى الله ورسوله، ومن كانت هجرته لدنيا يصيبها أو امرأة ينكحها فهجرته إلى ما هاجر إليه» .\nرواه إماما المحدثين:\nأبو عبد الله محمد ابن إسماعيل بن إبراهيم بن المغيرة بن بردزبه البخاري، وأبو الحسين مسلم ابن الحجاج بن مسلم القشيري النيسابوري: في صحيحيهما اللذين هما أصح الكتب المصنفة.\nالحديث الثاني\n«عن عمر رضي الله تعالى عنه أيضاً قال: بينما نحن جلوس عند رسول الله صلى الله عليه وآله وسلم ذات يوم إذ طلع علينا رجل شديد بياض الثياب شديد سواد الشعر لا يرى عليه أثر السفر ولا يعرفه منا أحد، حتى جلس إلى النبي صلى الله عليه وآله وسلم فأسند ركبتيه إلى ركبتيه ووضع كفيه على فخذيه وقال: يا محمد أخبرني عن الإسلام،\nفقال رسول الله صلى الله عليه وآله وسلم: الإسلام أن تشهد أن لا إله إلا الله وأن محمداً رسول الله، وتقيم الصلاة، وتؤتي الزكاة، وتصوم رمضان، وتحج البيت إن استطعت إليه سبيلاً قال: صدقت، فعجبنا له يسأله ويصدقه.\nقال: فأخبرني عن الإيمان، قال أن تؤمن بالله وملائكته وكتبه ورسله واليوم الآخر، وتؤمن بالقدر خيره وشره، قال: صدقت، قال: فأخبرني عن الإحسان، قال أن تعبد الله كأنك تراه، فإن لم تكن تراه فإنه يراك،\nقال: فأخبرني عن الساعة، قال ما المسئول عنها بأعلم من السائل، قال: فأخبرني عن أماراتها، قال أن تلد الأمة ربتها، وأن ترى الحفاة العراة العالة رعاء الشاء يتطاولون في البنيان\nثم انطلق فلبثت ملياً ثم قال يا عمر أتدري من السائل؟ قلت: الله ورسوله أعلم، قال فإنه جبريل أتاكم يعلمكم دينكم» .\nرواه مسلم.\nالحديث الثالث\n«عن أبي عبد الرحمن عبد الله بن عمر بن الخطاب رضي الله تعالى عنهما قال: سمعت رسول الله صلى الله عليه وآله وسلم يقول: بني الإسلام على خمس: شهادة أن لا إله إلا الله وأن محمداً رسول الله، وإقام الصلاة، وإيتاء الزكاة، وحج البيت، وصوم رمضان» .\nرواه البخاري ومسلم.\nالحديث الرابع\n«عن أبي عبد الرحمن عبد الله بن مسعود رضي الله عنه قال: حدثنا رسول الله صلى الله عليه وآله وسلم وهو الصادق المصدوق: إن أحدكم يجمع خلقه في بطن أمه أربعين يوماً نطفة، ثم يكون علقة مثل ذلك، ثم يكون مضغة مثل ذلك، ثم يرسل إليه الملك فينفخ فيه الروح ويؤمر بأربع كلمات: بكتب رزقه، وأجله، وعمله، وشقي أو سعيد،\nفو الله الذي لا إله غيره إن أحدكم ليعمل بعمل أهل الجنة حتى ما يكون بينه وبينها إلا ذراع فيسبق عليه الكتاب فيعمل بعمل أهل النار فيدخلها، وإن أحدكم ليعمل بعمل أهل النار حتى ما يكون بينه وبينها إلا ذراع فيسبق عليه الكتاب فيعمل بعمل أهل الجنة فيدخلها» رواه البخاري ومسلم.\nالحديث الخامس\n«عن أم المؤمنين أم عبد الله عائشة رضي الله عنها قالت: قال رسول الله صلى الله عليه وآله وسلم: من أحدث في أمرنا هذا ما ليس منه فهو رد» .\nرواه البخاري ومسلم.\nوفي رواية لمسلم «من عمل عملاً ليس عليه أمرنا فهو رد» .\nالحديث السادس\n«عن أبي عبد الله النعمان بن بشير رضي الله عنهما قال: سمعت رسول الله صلى الله عليه وآله وسلم يقول: إن الحلال بين وإن الحرام بين وبينهما أمور مشتبهات لا يعلمهن كثير من الناس، فمن اتقى الشبهات فقد استبرأ لدينه وعرضه، ومن وقع في الشبهات وقع في\nالحرام كالراعي يرعى حول الحمى يوشك أن يرتع فيه، ألا وإن لكل ملك حمى، ألا وإن حمى الله محارمه، ألا وإن في الجسد مضغة إذا صلحت صلح الجسد كله وإذا فسدت فسد الجسد كله ألا وهي القلب» .\nرواه البخاري ومسلم.\nالحديث السابع\n«عن أبي رقية تميم بن أوس الداري رضي الله تعالى عنه أن النبي صلى الله عليه وآله وسلم قال الدين النصيحة.\nقلنا: لمن؟ قال لله، ولكتابه، ولرسوله، ولأئمة المسلمين وعامتهم» .\nرواه مسلم.\nالحديث الثامن\n«عن ابن عمر رضي الله تعالى عنهما أن رسول الله صلى الله تعالى عليه وعلى آله وسلم قال أمرت أن أقاتل الناس حتى يشهدوا أن لا إله إلا الله وأن محمداً رسول الله، ويقيموا الصلاة، ويؤتوا الزكاة: فإذا فعلوا ذلك عصموا مني دماءهم وأموالهم إلا بحق الإسلام وحسابهم على الله تعالى» .\nرواه البخاري ومسلم.\nالحديث التاسع\n«عن أبي هريرة عبد الرحمن بن صخر رضي الله تعالى عنه قال: سمعت رسول الله صلى الله عليه وآله وسلم يقول مانهيتكم عنه فاجتنبوه، وما أمرتكم به فأتوا منه ما استطعتم، فإنما أهلك الذين من قبلكم كثرة مسائلهم واختلافهم على أنبيائهم» .\nرواه البخاري ومسلم.\nالحديث العاشر\n«عن أبي هريرة رضي الله تعالى عنه قال: قال رسول الله صلى الله عليه وآله وسلم: إن الله تعالى طيب لا يقبل إلا طيباً، وإن الله أمر المؤمنين بما أمر به المرسلين فقال تعالى {يا أيها الرسل كلوا من الطيبات واعملوا صالحا} وقال تعالى {يا أيها الذين آمنوا كلوا من طيبات ما رزقناكم} ثم ذكر الرجل يطيل السفر أشعث أغبر يمد يديه إلى السماء: يا رب يا رب، ومطعمه حرام\nوملبسه حرام وغذي بالحرام فأنى يستجاب له» .\nرواه مسلم.\nالحديث الحادي عشر\n«عن أبي محمد الحسن بن علي بن أبي طالب سبط رسول الله صلى الله عليه وآله وسلم وريحانته رضي الله عنهما قال: حفظت من رسول الله صلى الله عليه وآله وسلم دع ما يريبك إلى ما لا يريبك» .\nرواه الترمذي والنسائي، وقال الترمذي: حديث حسن صحيح.\nالحديث الثانى عشر\n«عن أبي هريرة رضي الله تعالى عنه قال: قال رسول الله صلى الله عليه وآله وسلم من حسن إسلام المرء تركه ما لا يعنيه» .\nحديث حسن، رواه الترمذي وغيره وهكذا.\nالحديث الثالث عشر\n«عن أبي حمزة أنس بن مالك رضي الله تعالى عنه خادم رسول الله صلى الله عليه وآله وسلم عن النبي صلى الله عليه وآله وسلم قال لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه» .\nرواه البخاري ومسلم.\nالحديث الرابع عشر\n«عن ابن مسعود رضي الله تعالى عنه قال: قال رسول الله صلى الله عليه وآله وسلم لا يحل دم امرئ مسلم إلا بإحدى ثلاث: الثيب الزاني، والنفس بالنفس، والتارك لدينه المفارق للجماعة» .\nرواه البخاري ومسلم.\nالحديث الخامس عشر\n«عن أبي هريرة رضي الله تعالى عنه أن رسول الله صلى الله عليه وآله وسلم قال: من كان يؤمن بالله واليوم الآخر فليقل خيراً أو ليصمت، ومن كان يؤمن بالله واليوم الآخر فليكرم جاره، ومن كان يؤمن بالله واليوم الآخر فليكرم ضيفه» .\nرواه البخاري ومسلم.\nالحديث السادس عشر\n«عن أبي هريرة رضي الله تعالى عنه أن رجلاً قال للنبي صلى الله عليه وآله وسلم: أوصني، قال لا تغضب فردد مراراً، قال لا تغضب» .\nرواه البخاري.\nالحديث السابع عشر\n«عن أبي يعلى شداد بن أوس رضي الله تعالى عنه عن رسول الله صلى الله عليه وآله وسلم قال إن الله كتب الإحسان على كل شئ، فإذا قتلتم فأحسنوا القتلة وإذا ذبحتم فأحسنوا الذبحة، وليحد أحدكم شفرته وليرح ذبيحته» .\nرواه مسلم.\nالحديث الثامن عشر\n«عن أبي ذر جندب بن جنادة، وأبي عبد الرحمن معاذ بن جبل رضي الله تعالى عنهما عن رسول الله صلى الله عليه وآله وسلم قال: إتق الله حيثما كنت، وأتبع السيئة الحسنة تمحها، وخالق الناس بخلق حسن» .\nرواه الترمذي وقال: حديث حسن، وفي بعض النسخ: حسن صحيح.\nالحديث التاسع عشر\n«عن أبي العباس عبد الله بن عباس رضي الله تعالى عنهما قال: كنت خلف النبي صلى الله عليه وآله وسلم يوماً فقال يا غلام، إني أعلمك كلمات: إحفظ الله يحفظك، إحفظ الله تجده تجاهك، إذا سألت فاسأل الله، وإذا استعنت فاستعن بالله، واعلم أن الأمة لو اجتمعت على أن ينفعوك بشئ لم ينفعوك إلا بشئ قد كتبه الله لك،\nوإن اجتمعوا على أن يضروك بشئ لم يضروك إلا بشئ قد كتبه الله عليك، رفعت الأقلام وجفت الصحف» .\nرواه الترمذي وقال حديث حسن صحيح.\nوفي رواية غير الترمذي «إحفظ الله تجده أمامك، تعرف إلى الله في الرخاء يعرفك في الشدة، واعلم أن ما أخطأك لم يكن ليصيبك، وما أصابك لم يكن ليخطئك، واعلم أن النصر مع الصبر، وأن الفرج مع الكرب، وأن مع العسر يسراً» .\nالحديث العشرون\n«عن أبي مسعود عقبة بن عمرو الأنصاري البدري رضي الله عنه قال: قال رسول الله صلى الله عليه وآله وسلم: إن مما أدرك الناس من كلام النبوة الأولى: إذا لم تستح فاصنع ما شئت» .\nرواه البخاري.\nالحديث الحادي والعشرون\n«عن أبي عمرو - وقيل أبي عمرة - سفيان ابن عبد الله رضي الله عنه قال: قلت يا رسول الله قل لي في الإسلام قولاً لا أسال عنه أحداً غيرك، قال قل آمنت بالله، ثم استقم» .\nرواه مسلم.\nالحديث الثانى والعشرون\n«عن أبي عبد الله جابر بن عبد الله الأنصاري رضي الله عنهما أن رجلاً سأل رسول الله صلى الله عليه وآله وسلم فقال: أرأيت إذا صليت المكتوبات، وصمت رمضان، وأحللت الحلال، وحرمت الحرام، ولم أزد على ذلك شيئاً، أدخل الجنة؟ قال نعم» .\nرواه مسلم.\nومعنى حرمت الحرام: اجتنبته،\nومعنى أحللت الحلال: فعلته معتقداً حله.\nالحديث الثالث والعشرون\n«عن أبي مالك الحارث بن الأشعري رضي الله عنه قال: قال رسول الله صلى الله عليه وآله وسلم الطهور شطر الإيمان، والحمد لله تملأ الميزان، وسبحان الله والحمد لله تملآن - أو تملأ - ما بين\nالسماء والأرض، والصلاة نور، والصدقة برهان، والصبر ضياء، والقرآن حجة لك أو عليك، كل الناس يغدو: فبائع نفسه فمعتقها أو موبقها» .\nرواه مسلم.\nالحديث الرابع والعشرون\n«عن أبي ذر الغفاري رضي الله تعالى عنه عن النبي صلى الله تعالى عليه وآله وسلم فيما يرويه عن ربه عز وجل أنه قال يا عبادي، إني حرمت الظلم على نفسي وجعلته بينكم محرماً فلا تظالموا،\nيا عبادي، كلكم ضال إلا من هديته فاستهدوني أهدكم، يا عبادي، كلكم جائع إلا من أطعمته فاستطعموني أطعمكم، يا عبادي، كلكم عار إلا من كسوته فاستكسوني أكسكم: يا عبادي، إنكم تخطئون بالليل والنهار وأنا أغفر الذنوب جميعاً فاستغفروني أغفر لكم، ياعبادي، إنكم لن تبلغوا ضري فتضروني ولن تبلغوا نفعي فتنفعوني، يا عبادي، لو أن أولكم وآخركم وإنسكم وجنكم كانوا على أتقى قلب رجل واحد منكم ما زاد ذلك في ملكي شيئاً:\nيا عبادي، لو أن أولكم وآخركم وإنسكم وجنكم كانوا على أفجر قلب رجل واحد منكم ما نقص ذلك من ملكي شيئاً: يا عبادي، لو أن أولكم وآخركم وإنسكم وجنكم قاموا في صعيد واحد فسألوني فأعطيت كل واحد مسألته ما نقص ذلك مما عندي إلا كما ينقص المخيط إذا أدخل البحر، يا عبادي، إنما هي أعمالكم أحصيها لكم ثم أوفيكم إياها فمن وجد خيراً فليحمد الله ومن\nوجد غير ذلك فلا يلومن إلا نفسه» .\nرواه مسلم.\nالحديث الخامس والعشرون\n«عن أبي ذر رضي الله عنه أيضاً أن ناساً من أصحاب رسول الله صلى الله عليه وآله وسلم قالوا للنبي صلى الله تعالى وعليه وآله وسلم: يا رسول الله، ذهب أهل الدثور بالأجور: يصلون كما نصلي ويصومون كما نصوم، ويتصدقون بفضول أموالهم.\nقال: أوليس قد جعل الله لكم ما تصدقون: إن بكل تسبيحة صدقة، وكل تكبيرة صدقة، وكل\nتحميدة صدقة، وكل تهليلة صدقة، وأمر بمعروف صدقة، ونهي عن منكر صدقة، وفي بضع أحدكم صدقة، قالوا: يا رسول الله، أيأتي أحدنا شهوته ويكون له فيها أجر؟ قال أرأيتم لو وضعها في حرام أكان عليه وزر؟ فكذلك إذا وضعها في الحلال كان له أجر» .\nرواه مسلم.\nالحديث السادس والعشرون\n«عن أبي هريرة رضي الله تعالى عنه قال: قال رسول الله صلى الله عليه وآله وسلم كل سلامى من الناس عليه صدقة كل يوم تطلع فيه الشمس: تعدل بين اثنين صدقة، وتعين الرجل في دابته فتحمله عليها أو ترفع\nله عليها متاعه صدقة، والكلمة الطيبة صدقة، وبكل خطوة تمشيها إلى الصلاة صدقة، وتميط الأذى عن الطريق صدقة» .\nرواه البخاري ومسلم.\nالحديث السابع والعشرون\n«عن النواس بن سمعان رضي الله تعالى عنه عن النبي صلى الله عليه وعلى آله وسلم قال البر حسن الخلق، والإثم ما حاك في نفسك وكرهت أن يطلع عليه الناس» .\nرواه مسلم\nوعن وابصة بن معبد رضي الله تعالى عنه قال: «أتيت رسول الله صلى الله عليه وآله وسلم فقال: جئت تسأل عن البر؟ قلت: نعم، وقال: إستفت قلبك، البر ما\nاطمأنت إليه النفس واطمأن إليه القلب، والإثم ما حاك في النفس وتردد في الصدر وإن أفتاك الناس وأفتوك» .\nحديث حسن.\nرويناه في مسندي الإمامين أحمد بن حنبل والدرامي بإسناد حسن.\nالحديث الثامن والعشرون\n«عن أبي نجيح العرباض بن سارية رضي الله تعالى عنه قال: وعظنا رسول الله صلى الله عليه وآله وسلم موعظة وجلت منها القلوب وذرفت منها العيون، فقلنا: يا رسول الله كأنها موعظة مودع فأوصنا، قال أوصيكم بتقوى الله عز وجل والسمع والطاعة وإن تأمر عليكم عبد،\nفإنه من يعش منكم فسيري اختلافاً كثيراً، فعليكم بسنتي وسنة الخلفاء الراشدين المهديين عضوا عليها بالنواجذ، وإياكم ومحدثات الأمور فإن كل بدعة ضلالة» .\nرواه أبو داود والترمذي وقال: حديث حسن صحيح.\nالحديث التاسع والعشرون\n«عن معاذ بن جبل رضي الله عنه قال: قلت يا رسول الله أخبرني بعمل يدخلني الجنة ويباعدني عن النار، قال: لقد سألت عن عظيم وإنه ليسير على من يسره الله تعالى عليه: تعبد الله لا تشرك به شيئاً، وتقيم الصلاة، وتؤتي الزكاة، وتصوم رمضان، وتحج البيت ثم قال: ألا أدلك على أبواب الخير؟ : الصوم جنة، والصدقة تطفئ الخطيئة كما يطفئ الماء النار، وصلاة الرجل في جوف الليل\nثم تلا: {تتجافى جنوبهم عن المضاجع} {حتى إذا بلغ} {يعملون} ثم قال ألا أخبرك برأس الأمر وعموده وذروة سنامه؟ قلت: بلى يا رسول الله.\nقال رأس الأمر الإسلام، وعموده الصلاة، وذروة سنامه الجهاد\nثم قال: ألا أخبرك بملاك ذلك كله؟ قلت: بلى يا رسول الله، فأخذ بلسانه وقال كف عليك هذا قلت: يا نبي الله، وإنا لمؤاخذون بما نتكلم به؟ فقال ثكلتك أمك، وهل يكب الناس في النار على وجوههم - أو قال على مناخرهم - إلا حصائد ألسنتهم؟» .\nرواه الترمذي وقال: حديث حسن صحيح.\nالحديث الثلاثون\n«عن أبي ثعلبة الخشبي جرثوم بن ناشر رضي الله تعالى عنه عن رسول الله صلى الله عليه وسلم قال إن الله تعالى فرض فرائض فلا تضيعوها، وحد حدوداً فلا تعتدوها، وحرم أشياء فلا تنتهكوها،\nوسكت عن أشياء رحمة لكم غير نسيان فلا تبحثوا عنها» .\nحديث حسن رواه الدارقطني وغيره.\nالحديث الحادي والثلاثون\n«عن أبي العباس سهل بن سعد الساعدى رضي الله تعالى عنه قال جاء رجل إلى النبي صلى الله عليه وآله وسلم فقال: يا رسول الله، دلني على عمل إذا عملته أحبني الله وأحبني الناس: فقال إزهد في الدنيا يحبك الله، وازهد فيما عند الناس يحبك الناس» .\nحديث حسن، رواه ابن ماجة وغيره بأسانيد حسنة.\nالحديث الثاني والثلاثون\n«عن أبي سعيد سعد بن مالك بن سنان الخدري رضي الله تعالى عنه أن رسول الله صلى الله وعليه وآله وسلم قال: لا ضرر ولا ضرار» .\nحديث حسن، رواه ابن ماجة والدارقطني وغيرهما مسنداً.\nورواه مالك في الموطإ مرسلاً عن عمرو بن يحيى عن أبيه عن النبي صلى الله عليه وآله وسلم، فأسقط أبا سعيد، وله طرق يقوي بعضها بعضاً.\nالحديث الثالث والثلاثون\n«عن ابن عباس رضي الله عنهما أن رسول الله صلى الله عليه وآله وسلم قال: لو يعطى الناس بدعواهم لادعى رجال أموال قوم ودماءهم، لكن البينة على المدعى واليمين على من أنكر» .\nحديث حسن، رواه البيهقي وغيره هكذا، وبعضه في الصحيحين.\nالحديث الرابع والثلاثون\n«عن أبي سعيد الخدري رضي الله عنه قال: سمعت رسول الله صلى الله عليه وآله وسلم يقوم: من رأى منكم منكراً فليغيره بيده، فإن لم يستطع فبلسانه، فإن لم يستطع فبقلبه، وذلك أضعف الإيمان» .\nرواه مسلم.\nالحديث الخامس والثلاثون\n«عن أبي هريرة رضي الله عنه قال: قال رسول الله صلى الله عليه وآله وسلم لا تحاسدوا، ولا تناجشوا ولا تباغضوا، ولا تدابروا، ولا يبع بعضكم على بيع بعض، وكونوا عباد الله إخواناً، المسلم أخو المسلم لا يظلمه ولا يخذله ولا يكذبه ولا يحقره، التقوى ههنا - ويشير\nإلى صدور ثلاث مرات - بحسب امرئ من الشر أن يحقر أخاه المسلم، كل المسلم على المسلم حرام: دمه وماله وعرضه» .\nرواه مسلم.\nالحديث السادس والثلاثون\n«عن أبي هريرة رضي الله عنه عن النبي صلى الله عليه وآله وسلم قال من نفس عن مؤمن كربة من كرب الدنيا نفس الله عنه كربة من كرب يوم القيامة، ومن يسر على معسر يسر الله عليه في الدنيا والآخرة، ومن ستر مسلماً ستره الله في الدنيا والآخرة، والله في عون العبد ما كان العبد في عون أخيه، ومن سلك طريقاً يلتمس فيه علماً سهل الله له به طريقاً إلى الجنة، وما اجتمع قوم في بيت من بيوت الله يتلون كتاب الله ويتدارسونه بينهم إلا نزلت عليهم السكينة\nوغشيتهم الرحمة وحفتهم الملائكة وذكرهم الله فيمن عنده، ومن بطأ به عمله لم يسرع به نسبه» .\nرواه مسلم بهذا اللفظ.\nالحديث السابع والثلاثون\n«عن ابن عباس رضي الله عنهما عن رسول الله صلى الله عليه وآله وسلم فيما يرويه عن ربه تبارك وتعالى قال إن الله كتب الحسنات والسيئات ثم بين ذلك، فمن هم بحسنة فلم يعملها كتبها الله عنده حسنة كاملة، وإن هم بها فعملها كتبها الله عنده عشر حسنات إلى سبعمائة ضعف إلى أضعاف كثيرة، وإن هم بسيئة فلم يعملها كتبها الله عنده حسنة كاملة، وإن هم بها فعملها كتبها الله سيئة واحدة» .\nرواه البخاري ومسلم في صحيحيهما بهذه الحروف فانظر يا أخي وفقنا الله وإياك إلى عظيم لطف الله تعالى، وتأمل هذه الألفاظ، وقوله عنده إشارة إلى الإعتناء بها، وقوله كاملة للتأكيد وشدة الإعتناء بها، وقال في السيئة التي هم بها ثم تركها كتبها الله عنه حسنة كاملة فأكدها بـ (كاملة) وإن عملها كتبها سيئة واحدة، فأكد تقليلها\nبـ (واحدة) ولم يؤكدها بـ (كاملة) فلله الحمد والمنة، سبحانه لا نحصي ثناء عليه، وبالله التوفيق.\nالحديث الثامن والثلاثون\n«عن أبي هريرة رضي الله عنه قال: قال رسول الله صلى الله عليه وآله وسلم: إن الله تعالى قال: من عادى لي ولياً فقد آذنته بالحرب، وما تقرب إلى عبدي بشئ أحب إلي مما افترضته عليه، ولا يزال عبدي يتقرب إلي\nبالنوافل حتى أحبه، فإذا أحببته كنت سمعه الذي يسمع به وبصره الذي يبصر به ويده التي يبطش بها ورجله التي يمشي بها، ولئن سألني لأعطينه، ولئن استعاذني لأعيذنه» .\nرواه البخاري.\nالحديث التاسع والثلاثون\n«عن ابن عباس رضي الله عنهما أن رسول الله صلى الله عليه وآله وسلم قال إن الله تجاوز لي عن أمتى الخطأ والنسيان وما اسكترهوا عليه» .\nحديث حسن رواه ابن ماجة والبيهقي وغيرهما.\nالحديث الأربعون\n«عن ابن عمر رضي الله عنهما قال: أخذ رسول الله صلى الله عليه وآله وسلم بمنكبي فقال: كن في الدنيا كأنك غريب أو عابر سبيل» وكان ابن عمر رضي الله تعالى عنهما يقول: إذا أمسيت فلا تنتظر الصباح، وإذا أصبحت فلا تنتظر\nالمساء، وخذ من صحتك لمرضك، ومن حياتك لموتك.\nرواه البخاري.\nالحديث الحادي والأربعون\n«عن أبي محمد عبد الله بن عمرو بن العاص رضي الله عنهما قال: قال رسول الله صلى الله عليه وآله وسلم: لا يؤمن أحدكم حتى يكون هواه تبعاً لما جئت به» .\nحديث حسن صحيح، رويناه في كتاب الحجة بإسناد صحيح.\nالحديث الثاني والأربعون\n«عن أنس رضي الله عنه قال: سمعت رسول الله صلى الله عليه وآله وسلم يقول: قال الله تعالى يابن آدم، إنك ما دعوتني ورجوتني غفرت للك على ما كان منك ولا أبالي، يا ابن آدم لو بلغت ذنوبك عنان السماء ثم\nاستغفرتني غفرت لك، يا ابن آدم لو أتيتني بقراب الأرض خطايا ثم لقيتني لا تشرك بي شيئاً لأتيتك بقرابها مغفرة» .\nرواه الترمذي وقال: حديث حسن صحيح.")
            }
            edit.putBoolean("seed_arbaun_v1", true).apply()
            libraryRevision++
        }
    }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val builtinLibraryCategories = listOf("Ingatan", "Kitab-Kitab", "Fiqh Syafi‘i", "Usul Fiqh", "Hadis", "Tafsir", "Akidah", "Tasawuf", "Bahasa Arab", "Kalam Hikmah")
    val customLibraryCategories = remember(libraryRevision) {
        libraryPrefs.getString("custom_categories", "")!!.split("\n").filter { it.isNotBlank() }
    }
    val allLibraryCategories = builtinLibraryCategories + customLibraryCategories
    var libraryDeleteKey by remember { mutableStateOf<String?>(null) }
    var libraryTrashOpen by remember { mutableStateOf(false) }
    var editingLibraryKey by remember { mutableStateOf<String?>(null) }
    var libraryTitle by remember { mutableStateOf("") }
    var libraryBody by remember { mutableStateOf(TextFieldValue("")) }
    var translationDraft by remember { mutableStateOf("") }
    var translationStatus by remember { mutableStateOf("") }
    var translationBusy by remember { mutableStateOf(false) }
    val translatedNoteDatabase = remember { NoteDatabase(context).also { it.migrateOldSingleNote(context) } }

    fun openLibraryEditor(key: String, defaultTitle: String = "", defaultBody: String = "") {
        editingLibraryKey = key
        libraryTitle = libraryPrefs.getString(key + "_title", defaultTitle) ?: defaultTitle
        libraryBody = TextFieldValue(libraryPrefs.getString(key + "_body", defaultBody) ?: defaultBody)
        translationDraft = ""
        translationStatus = ""
    }
    fun saveLibraryEditor() {
        editingLibraryKey?.let { key ->
            libraryPrefs.edit().putString(key + "_title", libraryTitle).putString(key + "_body", libraryBody.text).apply()
            libraryRevision++
        }
    }
    LaunchedEffect(editingLibraryKey, libraryTitle, libraryBody.text) {
        if (editingLibraryKey != null) {
            kotlinx.coroutines.delay(700)
            saveLibraryEditor()
        }
    }
    val libraryItems = remember(libraryRevision, selectedCategory) {
        libraryPrefs.all.keys.filter { it.startsWith("entry_" + selectedCategory + "_") && it.endsWith("_title") }
            .map { it.removeSuffix("_title") }.sorted()
    }

    val displayedKalimah = remember(libraryRevision) {
        kalimahEntries.map { entry ->
            val prefix = "kalimah_edit_" + entry.title + "_"
            entry.copy(
                arabic = libraryPrefs.getString(prefix + "arabic", entry.arabic) ?: entry.arabic,
                meaning = libraryPrefs.getString(prefix + "meaning", entry.meaning) ?: entry.meaning,
                transliteration = libraryPrefs.getString(prefix + "transliteration", entry.transliteration) ?: entry.transliteration,
                reference = libraryPrefs.getString(prefix + "reference", entry.reference) ?: entry.reference,
                note = libraryPrefs.getString(prefix + "note", entry.note) ?: entry.note
            )
        }
    }
    fun editKalimah(entry: KalimahEntry) {
        editingKalimah = entry
        kalimahArabic = entry.arabic
        kalimahMeaning = entry.meaning
        kalimahTransliteration = entry.transliteration
        kalimahReference = entry.reference
        kalimahNote = entry.note
    }
    val filteredEntries = displayedKalimah.filter { entry ->
        matchesMaktabahSearch(entry.title, search) ||
            matchesMaktabahSearch(entry.transliteration, search) ||
            matchesMaktabahSearch(entry.meaning, search) ||
            matchesMaktabahSearch(entry.arabic, search)
    }

    Box(modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFB99A54)), shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF073C35), Color(0xFF096B57))))
                .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Maktabah DinNote", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Perpustakaan Digital Islam", fontSize = 12.sp, color = Color(0xFFE8CF91))
                    Box(Modifier.width(64.dp).height(2.dp).background(Color(0xFFD3B66F)))
                }
                TextButton(onClick = { libraryTrashOpen = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Tong Sampah", tint = Color(0xFFE8CF91), modifier = Modifier.size(21.dp))
                }
            }
        }


        var libraryCategoryExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { libraryCategoryExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFFCF4),
                border = BorderStroke(1.dp, Color(0xFFD3B66F))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("▤", fontSize = 19.sp, color = Color(0xFF075B49))
                    Spacer(Modifier.width(9.dp))
                    Text("Kategori: $selectedCategory", modifier = Modifier.weight(1f),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF20262B))

                    Spacer(Modifier.width(10.dp))
                    Text("⌄", fontSize = 20.sp, color = Color(0xFF20262B))
                }
            }
            DropdownMenu(expanded = libraryCategoryExpanded,
                onDismissRequest = { libraryCategoryExpanded = false }) {
                allLibraryCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category, fontSize = 14.sp) },
                        onClick = {
                            selectedCategory = category
                            search = ""
                            libraryCategoryExpanded = false
                        },
                        trailingIcon = if (category in customLibraryCategories) {
                            {
                                IconButton(onClick = {
                                    libraryCategoryExpanded = false
                                    categoryToDelete = category
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Padam kategori $category", tint = Color(0xFFB3261E))
                                }
                            }
                        } else null
                    )
                }

                DropdownMenuItem(text = { Text("+ Tambah kategori") }, onClick = {
                    libraryCategoryExpanded = false
                    newCategoryName = ""
                    showAddCategory = true
                })
            }
        }
        if (selectedCategory in customLibraryCategories) {
            TextButton(onClick = { categoryToRename = selectedCategory; newCategoryName = selectedCategory }) {
                Text("✎ Ubah nama kategori")
            }
        }

        when (selectedCategory) {
            "Ingatan" -> {
                Text("الكلمات الطيبة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = 22.sp)

                Text("${filteredEntries.size} kalimah tersedia", color = MaterialTheme.colorScheme.primary)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(filteredEntries, key = { it.title }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedEntry = entry },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFB7DCD7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    OutlinedButton(onClick = { editKalimah(entry) }, modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                                        Text("✎ Edit", fontSize = 12.sp, color = Color(0xFF087F83))
                                    }
                                    Spacer(Modifier.width(5.dp))
                                    OutlinedButton(onClick = { showKalimahSettings = true }, modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                                        Text("⚙", fontSize = 15.sp, color = Color(0xFF087F83))
                                    }
                                }
                                if (malayFirstInLibrary && showMalayInLibrary) {
                                    Text(formattedNoteText(entry.meaning), color = MaterialTheme.colorScheme.onSurfaceVariant)

                                }
                                if (showArabicInLibrary) Text(formattedNoteText(entry.arabic), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = libraryFontSize.sp)
                                if (showMalayInLibrary && !malayFirstInLibrary) {

                                    Text(formattedNoteText(entry.meaning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (showMalayInLibrary) Text(formattedNoteText(entry.transliteration), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Tekan untuk maklumat lanjut", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            }
                        }
                    }
                }
            }
            "Kitab-Kitab" -> {
                Text("Maktabah Kitab", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075B49))
                TurathStarterCard(search, onNavigate = onNavigate)
                TurathStarterCard(search, "turath/al_majmu.json", onNavigate)
                TurathStarterCard(search, "turath/al_ajurrumiyyah.json", onNavigate)
                TurathStarterCard(search, "turath/al_fatihah.json", onNavigate)
                IbnKathirCollection(search, onNavigate)
                TurathStarterCard(search, "turath/al_hikam.json", onNavigate)

                Button(onClick = { openLibraryEditor("entry_Kitab-Kitab_" + System.currentTimeMillis()) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075B49))) { Text("+ Tambah Kitab") }
                libraryItems.filter { matchesMaktabahSearch(libraryPrefs.getString(it + "_title", "") ?: "", search) ||
                    matchesMaktabahSearch(libraryPrefs.getString(it + "_body", "") ?: "", search) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Box {
                                    var menuOpen by remember(key) { mutableStateOf(false) }
                                    TextButton(onClick = { menuOpen = true }) { Text("⋮", fontSize = 22.sp, color = Color(0xFF075B59)) }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; openLibraryEditor(key) })
                                        DropdownMenuItem(text = { Text("Padam") }, onClick = { menuOpen = false; libraryDeleteKey = key })
                                    }
                                }
                            }
                            Text(formattedNoteText(libraryPrefs.getString(key + "_body", "") ?: ""), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            val decoration = remember(context) { runCatching { context.assets.open("maktabah_card.webp").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull() }
                            decoration?.let { bitmap ->
                                androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.End).width(116.dp).height(39.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                            }
                        }
                    }
                }
            }
            else -> {
                if (selectedCategory == "Kalam Hikmah") {
                    val banner = remember(context) {
                        runCatching { context.assets.open("kalam_hikmah_bg.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF9EC),
                        modifier = Modifier.fillMaxWidth().height(42.dp)) {
                        Box(Modifier.fillMaxSize()) {
                            banner?.let {
                                androidx.compose.foundation.Image(bitmap = it, contentDescription = null,
                                    modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
                            }
                            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("▤", fontSize = 25.sp, color = Color(0xFF087F83))
                                Spacer(Modifier.width(10.dp))
                                Text("Kalam Hikmah", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20332F))
                            }
                        }
                    }
                } else {
                    Text(selectedCategory, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075B59))
                }

                Button(onClick = { openLibraryEditor("entry_" + selectedCategory + "_" + System.currentTimeMillis()) }) { Text("+ Tambah Kandungan") }
                libraryItems.filter { matchesMaktabahSearch(libraryPrefs.getString(it + "_title", "") ?: "", search) ||
                    matchesMaktabahSearch(libraryPrefs.getString(it + "_body", "") ?: "", search) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC8A34D)), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7))) {
                        Column(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp), color = Color(0xFF075B59), border = BorderStroke(1.dp, Color(0xFFC8A34D))) {
                                    Text("▤", color = Color(0xFFFFE4A0), fontSize = 20.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF075B59), modifier = Modifier.weight(1f))
                                Box {
                                    var menuOpen by remember(key) { mutableStateOf(false) }
                                    TextButton(onClick = { menuOpen = true }) { Text("⋮", fontSize = 22.sp, color = Color(0xFF075B59)) }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; openLibraryEditor(key) })
                                        DropdownMenuItem(text = { Text("Padam") }, onClick = { menuOpen = false; libraryDeleteKey = key })
                                    }
                                }
                            }
                            Text(formattedNoteText(libraryPrefs.getString(key + "_body", "") ?: ""), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            val decoration = remember(context) { runCatching { context.assets.open("maktabah_card.webp").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull() }
                            decoration?.let { bitmap ->
                                androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.End).width(180.dp).height(52.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                            }
                            Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(Color(0xFF075B59), Color(0xFFD6AF58), Color(0xFF087F83)))))
                        }
                    }
                }
            }
        }
    }


    libraryDeleteKey?.let { key ->
        AlertDialog(
            onDismissRequest = { libraryDeleteKey = null },
            title = { Text("Padam kandungan?") },
            text = { Text("Kandungan akan dipindahkan ke Tong Sampah dan boleh dipulihkan.") },
            confirmButton = {
                TextButton(onClick = {
                    libraryPrefs.edit()
                        .putString("trash_" + key + "_title", libraryPrefs.getString(key + "_title", ""))
                        .putString("trash_" + key + "_body", libraryPrefs.getString(key + "_body", ""))
                        .remove(key + "_title").remove(key + "_body").apply()
                    libraryRevision++
                    libraryDeleteKey = null
                }) { Text("Padam", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { libraryDeleteKey = null }) { Text("Batal") } }
        )
    }
    if (libraryTrashOpen) {
        val trashKeys = remember(libraryRevision) {
            libraryPrefs.all.keys.filter { it.startsWith("trash_entry_") && it.endsWith("_title") }
                .map { it.removePrefix("trash_").removeSuffix("_title") }.sorted()
        }
        AlertDialog(
            onDismissRequest = { libraryTrashOpen = false },
            title = { Text("Tong Sampah") },
            text = {
                if (trashKeys.isEmpty()) Text("Tiada kandungan dipadam.")
                else LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(trashKeys, key = { it }) { key ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(libraryPrefs.getString("trash_" + key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                if (!libraryPrefs.contains(key + "_title")) {
                                    libraryPrefs.edit()
                                        .putString(key + "_title", libraryPrefs.getString("trash_" + key + "_title", ""))
                                        .putString(key + "_body", libraryPrefs.getString("trash_" + key + "_body", ""))
                                        .remove("trash_" + key + "_title").remove("trash_" + key + "_body").apply()
                                    libraryRevision++
                                }
                            }) { Text("Pulihkan") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { libraryTrashOpen = false }) { Text("Tutup") } }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Padam kategori?") },
            text = { Text("Kategori \""+ categoryToDelete.orEmpty() +"\" dan semua kandungannya akan dipadam secara kekal. Tindakan ini tidak boleh dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = categoryToDelete
                    if (target != null && target in customLibraryCategories) {
                        val editor = libraryPrefs.edit()
                        libraryPrefs.all.keys.filter { it.startsWith("entry_" + target + "_") }.forEach { editor.remove(it) }
                        editor.putString("custom_categories", customLibraryCategories.filterNot { it == target }.joinToString("\n"))
                        editor.apply()
                        if (selectedCategory == target) selectedCategory = "Kitab-Kitab"
                        libraryRevision++
                    }
                    categoryToDelete = null
                }) { Text("Padam", color = Color(0xFFB3261E)) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Batal") } }
        )
    }

    if (showAddCategory || categoryToRename != null) {
        AlertDialog(
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            onDismissRequest = { showAddCategory = false; categoryToRename = null },
            title = { Text(if (categoryToRename == null) "Tambah kategori" else "Ubah nama kategori") },
            text = {
                OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it },
                    label = { Text("Nama kategori") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    val old = categoryToRename
                    if (name.isNotEmpty() && (name == old || name !in allLibraryCategories)) {
                        val updated = if (old == null) customLibraryCategories + name
                            else customLibraryCategories.map { if (it == old) name else it }
                        if (old != null && old != name) {
                            libraryPrefs.all.keys.filter { it.startsWith("entry_" + old + "_") }.forEach { key ->
                                val newKey = "entry_" + name + "_" + key.removePrefix("entry_" + old + "_")
                                libraryPrefs.edit().putString(newKey, libraryPrefs.getString(key, "")).remove(key).apply()
                            }
                        }
                        libraryPrefs.edit().putString("custom_categories", updated.joinToString("\n")).apply()
                        selectedCategory = name
                        libraryRevision++
                        showAddCategory = false
                        categoryToRename = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false; categoryToRename = null }) { Text("Batal") }
            }
        )
    }

    if (editingLibraryKey != null) {
        LaunchedEffect(libraryTitle, libraryBody.text) {
            kotlinx.coroutines.delay(650)
            if (libraryTitle.isNotBlank() || libraryBody.text.isNotBlank()) saveLibraryEditor()
        }
        val libraryBodyScroll = rememberScrollState()
        var libraryBodyLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        var libraryBodyViewport by remember { mutableIntStateOf(0) }
        LaunchedEffect(libraryBody.selection, libraryBodyLayout, libraryBodyViewport, WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)) {
            val layout = libraryBodyLayout ?: return@LaunchedEffect
            if (libraryBodyViewport <= 0) return@LaunchedEffect
            val caret = runCatching { layout.getCursorRect(libraryBody.selection.end.coerceIn(0, libraryBody.text.length)) }.getOrNull() ?: return@LaunchedEffect
            val target = when {
                caret.bottom > libraryBodyScroll.value + libraryBodyViewport - 48f ->
                    (caret.bottom - libraryBodyViewport + 64f).toInt()
                caret.top < libraryBodyScroll.value + 16f ->
                    (caret.top - 24f).toInt()
                else -> libraryBodyScroll.value
            }
            if (target != libraryBodyScroll.value) {
                libraryBodyScroll.animateScrollTo(target.coerceIn(0, libraryBodyScroll.maxValue))
            }
        }
        // Inline editor shares the Activity Scaffold, so its bottom navigation
        // remains visible and the editor cannot cover Android's navigation area.
        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), color = Color.White) {
                Column(Modifier.fillMaxSize()) {
                    UnifiedEditorHeading("Catatan Maktabah",
                        onBack = { saveLibraryEditor(); editingLibraryKey = null },
                        onSave = { saveLibraryEditor(); editingLibraryKey = null })
                    var libraryToolsExpanded by remember { mutableStateOf(false) }
                    var libraryTranslationExpanded by remember { mutableStateOf(false) }
                    Surface(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 3.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(0.dp), color = Color.White) {
                        Box(Modifier.fillMaxSize()) {
                            // Keep the writing surface unobstructed; decoration belongs to reading cards.
                            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = libraryTitle, onValueChange = { libraryTitle = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold, color = Color(0xFF087F83),
                                        textDirection = TextDirection.ContentOrRtl),
                                    decorationBox = { inner ->
                                        Box {
                                            if (libraryTitle.isEmpty()) Text("Tajuk catatan",
                                                color = Color(0xFF087F83), fontSize = 18.sp)
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = libraryBody, onValueChange = { libraryBody = it },
                                    visualTransformation = remember { NoteFormattingTransformation() },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 4.dp)
                                        .onSizeChanged { libraryBodyViewport = it.height }
                                        .verticalScroll(libraryBodyScroll),
                                    onTextLayout = { libraryBodyLayout = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = libraryFontSize.sp,
                                        lineHeight = (libraryFontSize * libraryDisplayPrefs.getFloat("line_spacing", 1.6f).coerceIn(1.2f, 2.2f)).sp,
                                        color = Color(0xFF263B3E),
                                        textDirection = TextDirection.ContentOrRtl),
                                    decorationBox = { inner ->
                                        Box {
                                            if (libraryBody.text.isEmpty()) Text(
                                                "Sentuh di sini untuk menulis...",
                                                color = Color.Gray, fontSize = 16.sp)
                                            inner()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    UnifiedFocusToolbar(libraryBody, libraryFontSize,
                        onFormat = { libraryBody = wrapEditorSelection(libraryBody, it) },
                        onColor = { libraryBody = colorEditorSelection(libraryBody, it) },
                        onResize = { delta ->
                            if (libraryBody.selection.collapsed) {
                                libraryFontSize = (libraryFontSize + delta).coerceIn(14f, 30f)
                                libraryDisplayPrefs.edit().putInt("font_size", libraryFontSize.toInt()).apply()
                            } else libraryBody = resizeEditorTextSelection(libraryBody, delta, libraryFontSize.toInt())
                        })
                    SharedParagraphTools(libraryBody) { libraryBody = it }
                    Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick = { libraryToolsExpanded = !libraryToolsExpanded }) {
                            Text(if (libraryToolsExpanded) "Tutup alat ▴" else "⋯ Alat editor", fontSize = 12.sp)
                        }
                        TextButton(onClick = { libraryTranslationExpanded = !libraryTranslationExpanded }) {
                            Text(if (libraryTranslationExpanded) "Tutup terjemahan" else "Terjemahan", fontSize = 12.sp)
                        }
                    }
                    if (libraryToolsExpanded) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    var clearLibraryFormatConfirm by remember { mutableStateOf(false) }
                    TextButton(enabled = !libraryBody.selection.collapsed, onClick = {
                        clearLibraryFormatConfirm = true
                    }) { Text("Tx  Buang format", color = Color(0xFF087F83), fontSize = 13.sp) }
                    if (clearLibraryFormatConfirm) AlertDialog(
                        onDismissRequest = { clearLibraryFormatConfirm = false },
                        title = { Text("Buang format pilihan?") },
                        text = { Text("Isi petikan dikekalkan.") },
                        confirmButton = { TextButton(onClick = {
                            if (!libraryBody.selection.collapsed) libraryBody = clearEditorFormatting(libraryBody)
                            clearLibraryFormatConfirm = false
                        }) { Text("Buang format") } },
                        dismissButton = { TextButton(onClick = { clearLibraryFormatConfirm = false }) { Text("Batal") } }
                    )
                    Row(Modifier.wrapContentWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            enabled = !translationBusy && libraryBody.text.isNotBlank(),
                            onClick = {
                                val selection = libraryBody.selection
                                val selected = libraryBody.text.substring(selection.min.coerceIn(0, libraryBody.text.length), selection.max.coerceIn(0, libraryBody.text.length))
                                val source = if (selection.collapsed || selected.isBlank()) formattedNoteText(libraryBody.text).text
                                    else formattedNoteText(smartFormattedExcerpt(libraryBody.text, selection.min, selection.max)).text
                                libraryTranslationExpanded = true
                                translationBusy = true
                                translationStatus = "Menyediakan terjemahan (muat turun model jika perlu)..."
                                ArabicMalayTranslator.translate(source) { result ->
                                    translationBusy = false
                                    result.onSuccess {
                                        translationDraft = it
                                        translationStatus = "Terjemahan draf tersedia. Sila semak maksud fiqh."
                                    }.onFailure {
                                        translationStatus = "Terjemahan gagal: ${it.localizedMessage ?: "Sila cuba lagi"}"
                                    }
                                }
                            }
                        ) { Text("ع⇄A", fontSize = 14.sp) }
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            enabled = translationDraft.isNotBlank(),
                            onClick = {
                                val selection = libraryBody.selection
                                val selected = libraryBody.text.substring(selection.min.coerceIn(0, libraryBody.text.length), selection.max.coerceIn(0, libraryBody.text.length))
                                val source = selected.ifBlank { libraryBody.text }
                                val note = "Petikan Arab:\n" + source + "\n\nTerjemahan Melayu (draf):\n" + translationDraft + "\n\nSumber: " + libraryTitle.ifBlank { "Maktabah DinNote" }
                                translatedNoteDatabase.save(libraryTitle.ifBlank { "Petikan Maktabah" }, note, null)
                                translationStatus = "Petikan dan terjemahan disimpan dalam Nota."
                            }
                        ) { Text("▤+", fontSize = 14.sp) }
                    }

                        }
                    }
                    if (libraryTranslationExpanded) {
                        Column(Modifier.fillMaxWidth().heightIn(max = 120.dp).verticalScroll(rememberScrollState())) {
                    if (translationStatus.isNotBlank()) {
                        Text(translationStatus, modifier = Modifier.padding(horizontal = 14.dp), fontSize = 12.sp)
                    }
                    if (translationDraft.isNotBlank()) {
                        OutlinedTextField(
                            value = translationDraft,
                            onValueChange = { translationDraft = it },
                            label = { Text("Terjemahan Melayu — boleh sunting") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            maxLines = 4
                        )
                    }

                        }
                    }


                }
            }
    }
    } // End inline Maktabah overlay container

    editingKalimah?.let { entry ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { editingKalimah = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            var field by remember(entry.title) { mutableIntStateOf(0) }
            val values = listOf(kalimahArabic, kalimahMeaning, kalimahTransliteration, kalimahReference, kalimahNote)
            val labels = listOf("Teks Arab", "Terjemahan Melayu", "Transliterasi", "Rujukan", "Nota")
            Surface(Modifier.fillMaxSize().imePadding(), color = Color.White) {
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    UnifiedEditorHeading("Edit · " + entry.title, { editingKalimah = null }, {
                    val prefix = "kalimah_edit_" + entry.title + "_"
                    libraryPrefs.edit().putString(prefix + "arabic", kalimahArabic)
                        .putString(prefix + "meaning", kalimahMeaning)
                        .putString(prefix + "transliteration", kalimahTransliteration)
                        .putString(prefix + "reference", kalimahReference)
                        .putString(prefix + "note", kalimahNote).apply()
                    libraryRevision++
                    editingKalimah = null

                    })
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        labels.forEachIndexed { index, label ->
                            TextButton(onClick = { field = index }) {
                                Text(label, fontWeight = if (field == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    key(field) {
                        var value by remember { mutableStateOf(TextFieldValue(values[field])) }
                        var size by remember { mutableFloatStateOf(libraryFontSize) }
                        fun change(next: TextFieldValue) {
                            value = next
                            when (field) {
                                0 -> kalimahArabic = next.text
                                1 -> kalimahMeaning = next.text
                                2 -> kalimahTransliteration = next.text
                                3 -> kalimahReference = next.text
                                4 -> kalimahNote = next.text
                            }
                        }
                        androidx.compose.foundation.text.BasicTextField(value = value, onValueChange = { change(it) },
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()),
                            visualTransformation = remember { NoteFormattingTransformation() },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = size.sp, color = Color(0xFF263B3E),
                                textDirection = TextDirection.ContentOrRtl))
                        UnifiedFocusToolbar(value, size,
                            onFormat = { change(wrapEditorSelection(value, it)) },
                            onColor = { change(colorEditorSelection(value, it)) },
                            onResize = { delta ->
                                if (value.selection.collapsed) size = (size + delta).coerceIn(14f, 30f)
                                else change(resizeEditorTextSelection(value, delta, size.toInt()))
                            })
                        SharedParagraphTools(value, ::change)
                    }
                }
            }
        }
    }
    if (showKalimahSettings) {
        AlertDialog(
            onDismissRequest = { showKalimahSettings = false },
            title = { Text("Tetapan paparan kalimah") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saiz tulisan Arab: " + libraryFontSize.toInt() + " sp")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            libraryFontSize = (libraryFontSize - 2f).coerceAtLeast(14f)
                            libraryDisplayPrefs.edit().putInt("font_size", libraryFontSize.toInt()).apply()
                        }) { Text("A−") }
                        OutlinedButton(onClick = {
                            libraryFontSize = (libraryFontSize + 2f).coerceAtMost(30f)
                            libraryDisplayPrefs.edit().putInt("font_size", libraryFontSize.toInt()).apply()
                        }) { Text("A+") }
                    }
                    Text("Susunan Arab dan Melayu boleh diubah melalui Tetapan utama DinNote.", fontSize = 13.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showKalimahSettings = false }) { Text("Selesai") } }
        )
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            titleContentColor = Color(0xFF075B59),
            textContentColor = Color(0xFF263B3E),
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (malayFirstInLibrary && showMalayInLibrary) {
                        Text(formattedNoteText(entry.meaning))
                        UnifiedEditorDivider()
                    }
                    if (showArabicInLibrary) Text(formattedNoteText(entry.arabic), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = libraryFontSize.sp)
                    if (showMalayInLibrary && !malayFirstInLibrary) {
                        if (showArabicInLibrary) UnifiedEditorDivider()
                        Text(formattedNoteText(entry.meaning))
                    }
                    if (showMalayInLibrary) Text(formattedNoteText(entry.transliteration), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(formattedNoteText("Rujukan: ${entry.reference}"), fontSize = 13.sp)
                    Text(formattedNoteText(entry.note), fontSize = 13.sp)
                    IslamicCardFooter()
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { selectedEntry = null }, shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFB7DCD7))) { Text("Tutup", color = Color(0xFF087F83)) }
            }
        )
    }
}


@Composable
private fun UnifiedFocusToolbar(value: TextFieldValue, fontSize: Float, onFormat: (String) -> Unit, onColor: (Long) -> Unit, onResize: (Int) -> Unit) {
    val keyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    if (!keyboardVisible && value.selection.collapsed) return

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = { onFormat("**") },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) { Text("B", fontWeight = FontWeight.Bold) }
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = { onFormat("_") },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) { Text("I", fontStyle = FontStyle.Italic) }
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = { onFormat("++") },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) { Text("U", textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline) }
                        EditorColorPicker(onPick = onColor)
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = { onResize(-1) },
                            enabled = !value.selection.collapsed || fontSize > 14f,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) { Text("A−", color = Color(0xFF087F83)) }
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = {},
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) { Text("${selectedEditorFontSize(value, fontSize.toInt())}", color = Color(0xFF087F83)) }
                        OutlinedButton(shape = RoundedCornerShape(0.dp), 
                            onClick = { onResize(1) },
                            enabled = !value.selection.collapsed || fontSize < 30f,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) { Text("A+", color = Color(0xFF087F83)) }
                    }

}

private fun readSmartVault(context: android.content.Context): List<String> {
    val prefs = context.getSharedPreferences("dinnote_smart_vault", android.content.Context.MODE_PRIVATE)
    return runCatching {
        val array = org.json.JSONArray(prefs.getString("items", "[]") ?: "[]")
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}

private fun saveSmartVault(context: android.content.Context, block: String) {
    if (formattedNoteText(block).text.isBlank()) return
    val items = listOf(block) + readSmartVault(context).filter { it != block }
    val array = org.json.JSONArray()
    items.forEach { array.put(it) }
    context.getSharedPreferences("dinnote_smart_vault", android.content.Context.MODE_PRIVATE)
        .edit().putString("items", array.toString()).apply()
}

@Composable
private fun SmartMinbarDialog(material: String, onClose: () -> Unit) {
    val scroll = rememberScrollState()
    var size by remember { mutableStateOf(28f) }
    var speed by remember { mutableStateOf(30f) }
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(playing, speed) {
        while (playing) {
            kotlinx.coroutines.delay(50)
            if (scroll.value >= scroll.maxValue) { playing = false; break }
            scroll.scrollTo((scroll.value + (speed / 10f).toInt().coerceAtLeast(1)).coerceAtMost(scroll.maxValue))
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Smart Minbar", modifier = Modifier.weight(1f), color = Color(0xFF087F83), fontSize = 20.sp)
                    TextButton(onClick = onClose) { Text("Tutup") }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = { size = (size - 2f).coerceAtLeast(18f) }) { Text("A−") }
                    TextButton(onClick = { size = (size + 2f).coerceAtMost(48f) }) { Text("A+") }
                    TextButton(onClick = { playing = !playing }) { Text(if (playing) "Jeda" else "Auto-scroll") }
                }
                Text("Kelajuan", fontSize = 12.sp)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 10f..100f)
                HorizontalDivider()
                Text(formattedNoteText(material), modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(vertical = 12.dp),
                    fontSize = size.sp, lineHeight = (size * 1.65f).sp, color = Color(0xFF263B3E))
            }
        }
    }
}

@Composable
private fun SmartRecallDialog(items: List<String>, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("dinnote_smart_recall", android.content.Context.MODE_PRIVATE) }
    fun key(block: String): String = android.util.Base64.encodeToString(
        java.security.MessageDigest.getInstance("SHA-256").digest(block.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)
    val queue = remember(items) { items.filter { prefs.getLong(key(it), 0L) <= System.currentTimeMillis() } }
    var position by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Smart Recall · Murajaah") },
        text = {
            Column {
                if (position >= queue.size) {
                    Text(if (queue.isEmpty()) "Tiada petikan perlu diulang sekarang." else "Sesi murajaah selesai.")
                } else {
                    val block = queue[position]
                    Text("Petikan " + (position + 1) + " / " + queue.size, color = Color(0xFF087F83))
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        if (revealed) Text(formattedNoteText(block), fontSize = 22.sp, lineHeight = 34.sp)
                        else Text("Cuba ingat petikan ini: " + formattedNoteText(block).text.take(45) + "…", fontSize = 18.sp)
                    }
                    if (!revealed) TextButton(onClick = { revealed = true }) { Text("Lihat petikan penuh") }
                    else Row {
                        TextButton(onClick = {
                            prefs.edit().putLong(key(block), System.currentTimeMillis() + 10L * 60L * 1000L).apply()
                            position++; revealed = false
                        }) { Text("Ulang lagi") }
                        TextButton(onClick = {
                            prefs.edit().putLong(key(block), System.currentTimeMillis() + 24L * 60L * 60L * 1000L).apply()
                            position++; revealed = false
                        }) { Text("Sudah kuasai") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup") } })
}

@Composable
private fun SmartDualView(items: List<String>, onClose: () -> Unit, onInsert: (String) -> Unit) {
    var selected by remember { mutableStateOf(items.firstOrNull().orEmpty()) }
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val workspace = remember { context.getSharedPreferences("dinnote_workspace", android.content.Context.MODE_PRIVATE) }
    var draft by remember { mutableStateOf(workspace.getString("draft", "").orEmpty()) }
    var confirmClear by remember { mutableStateOf(false) }
    fun changeDraft(value: String) {
        draft = value
        workspace.edit().putString("draft", value).apply()
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false },
            title = { Text("Kosongkan catatan pengajian?") },
            text = { Text("Draf ruang pengajian akan dipadam. Nota yang sudah disimpan tidak terjejas.") },
            confirmButton = { TextButton(onClick = { changeDraft(""); confirmClear = false }) { Text("Kosongkan") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } })
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val wide = configuration.screenWidthDp >= 600 ||
        (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE &&
            configuration.screenWidthDp >= 480)
    val filtered = remember(items, query) {
        val term = normalizeArabicSearch(query.trim())
        if (term.isEmpty()) items else items.filter { normalizeArabicSearch(formattedNoteText(it).text).contains(term) }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Smart Dual View", Modifier.weight(1f), color = Color(0xFF087F83), fontSize = 20.sp)
                    TextButton(onClick = onClose) { Text("Tutup") }
                    TextButton(enabled = draft.isNotBlank(), onClick = { onInsert(draft); changeDraft(""); onClose() }) { Text("Tampal nota") }
                }
                HorizontalDivider()
                val sourcePanel: @Composable () -> Unit = {
                    Column(Modifier.fillMaxSize().padding(6.dp)) {
                        Text("Khazanah rujukan", color = Color(0xFF087F83))
                        OutlinedTextField(query, { query = it }, label = { Text("Cari petikan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        LazyColumn(Modifier.weight(1f)) {
                            items(filtered, key = { it }) { block ->
                                TextButton(onClick = { selected = block }) {
                                    Text(remember(block) { formattedNoteText(block) }, maxLines = 2)
                                }
                            }
                        }
                        HorizontalDivider()
                        Column(Modifier.weight(2f).verticalScroll(rememberScrollState())) {
                            Text(remember(selected) { formattedNoteText(selected) }, fontSize = 20.sp, lineHeight = 32.sp)
                        }
                        TextButton(enabled = selected.isNotBlank(), onClick = {
                            changeDraft(if (draft.isBlank()) selected else draft + "\n\n" + selected)
                        }) { Text("+ Petikan ke nota") }
                    }
                }
                val editorPanel: @Composable () -> Unit = {
                    Column(Modifier.fillMaxSize().padding(6.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Catatan pengajian", Modifier.weight(1f), color = Color(0xFF087F83))
                            TextButton(enabled = draft.isNotBlank(), onClick = { confirmClear = true }) { Text("Kosongkan") }
                        }
                        Text("Draf disimpan secara automatik", fontSize = 11.sp, color = Color(0xFF087F83))
                        OutlinedTextField(draft, { changeDraft(it) }, modifier = Modifier.fillMaxSize(),
                            placeholder = { Text("Tulis faedah dan huraian di sini…") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp, textDirection = TextDirection.ContentOrRtl),
                            visualTransformation = androidx.compose.ui.text.input.VisualTransformation { value ->
                                androidx.compose.ui.text.input.TransformedText(value, androidx.compose.ui.text.input.OffsetMapping.Identity)
                            })
                    }
                }
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Box(Modifier.weight(1f).fillMaxHeight()) { sourcePanel() }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFB7DCD7)))
                        Box(Modifier.weight(1f).fillMaxHeight()) { editorPanel() }
                    }
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { sourcePanel() }
                        HorizontalDivider()
                        Box(Modifier.weight(1f).fillMaxWidth()) { editorPanel() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartVaultDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items = remember { readSmartVault(context) }
    val index = remember { context.getSharedPreferences("dinnote_smart_index", android.content.Context.MODE_PRIVATE) }
    val blockKeys = remember(items) {
        items.associateWith { block ->
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(block.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
        }
    }
    fun tagKey(block: String): String = blockKeys.getValue(block)
    var tags by remember { mutableStateOf(items.associateWith { index.getString(tagKey(it), "") ?: "" }) }
    val referenceLabels = listOf("Kitab", "Pengarang", "Jilid", "Halaman", "Sumber tambahan")
    fun readReference(block: String): List<String> = referenceLabels.indices.map { field ->
        index.getString("ref_" + tagKey(block) + "_" + field, "") ?: ""
    }
    fun referenceText(fields: List<String>): String = fields.mapIndexedNotNull { field, value ->
        value.trim().takeIf { it.isNotEmpty() }?.let { referenceLabels[field] + ": " + it }
    }.joinToString(" · ")
    var references by remember { mutableStateOf(items.associateWith { readReference(it) }) }
    var refEditing by remember { mutableStateOf<String?>(null) }
    var refDraft by remember { mutableStateOf(List(referenceLabels.size) { "" }) }
    var dualOpen by remember { mutableStateOf(false) }
    var insightOpen by remember { mutableStateOf(false) }
    var recallOpen by remember { mutableStateOf(false) }
    var majlisMode by remember { mutableStateOf(false) }
    var majlisSelection by remember { mutableStateOf(emptyList<String>()) }
    var majlisTitle by remember { mutableStateOf("") }
    var minbarMaterial by remember { mutableStateOf<String?>(null) }
    fun assembleMajlis(): String {
        val sections = majlisSelection.mapIndexed { number, block ->
            val citation = referenceText(references[block].orEmpty())
            (number + 1).toString() + ".\n" + block + if (citation.isNotBlank()) "\n" + citation else ""
        }
        val title = majlisTitle.trim()
        return (if (title.isNotBlank()) title + "\n\n" else "") + sections.joinToString("\n\n")
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    val searchText = remember(items) { mutableMapOf<String, String>() }
    val filtered = remember(items, query, tags, references) {
        val term = normalizeArabicSearch(query.trim())
        if (term.isEmpty()) items else items.filter {
            normalizeArabicSearch(tags[it].orEmpty()).contains(term) ||
                normalizeArabicSearch(referenceText(references[it].orEmpty())).contains(term) ||
                searchText.getOrPut(it) { normalizeArabicSearch(formattedNoteText(it).text) }.contains(term)
        }
    }
    var relatedBlock by remember { mutableStateOf<String?>(null) }
    val themeSets = remember(tags) {
        tags.mapValues { (_, value) -> value.split(",")
            .map { it.trim().lowercase(java.util.Locale.ROOT) }.filter { it.isNotBlank() }.toSet() }
    }
    fun themeSet(block: String): Set<String> = themeSets[block].orEmpty()
    var graphOpen by remember { mutableStateOf(false) }
    var graphTheme by remember { mutableStateOf<String?>(null) }
    var atlasOpen by remember { mutableStateOf(false) }
    var atlasField by remember { mutableStateOf(0) }
    var atlasSelection by remember { mutableStateOf<String?>(null) }
    if (atlasOpen) {
        val groups = remember(references, atlasField) {
            items.mapNotNull { block ->
                references[block]?.getOrNull(atlasField)?.trim()?.takeIf { it.isNotEmpty() }?.let { it to block }
            }.groupBy({ it.first }, { it.second })
        }
        val selected = atlasSelection
        AlertDialog(onDismissRequest = { atlasOpen = false; atlasSelection = null },
            title = { Text("Smart Knowledge Atlas · " + (selected ?: referenceLabels[atlasField])) },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    Row {
                        TextButton(onClick = { atlasField = 0; atlasSelection = null }) { Text("Kitab") }
                        TextButton(onClick = { atlasField = 1; atlasSelection = null }) { Text("Pengarang") }
                    }
                    HorizontalDivider()
                    if (selected == null) {
                        Text("Jumlah sumber: " + groups.size, fontSize = 12.sp)
                        if (groups.isEmpty()) Text("Isikan maklumat kitab atau pengarang melalui Smart Reference.")
                        LazyColumn(Modifier.heightIn(max = 340.dp)) {
                            items(groups.entries.sortedByDescending { it.value.size }, key = { it.key }) { entry ->
                                TextButton(onClick = { atlasSelection = entry.key }, modifier = Modifier.fillMaxWidth()) {
                                    Text(entry.key, Modifier.weight(1f))
                                    Text(entry.value.size.toString())
                                }
                            }
                        }
                    } else {
                        LazyColumn(Modifier.heightIn(max = 340.dp)) {
                            items(groups[selected].orEmpty(), key = { it }) { block ->
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    TextButton(onClick = { atlasOpen = false; atlasSelection = null; relatedBlock = block }) {
                                        Text(formattedNoteText(block), maxLines = 4)
                                    }
                                    Text(referenceText(references[block].orEmpty()), fontSize = 12.sp)
                                    TextButton(onClick = { onInsert(block); onDismiss() }) { Text("Tampal petikan") }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (selected != null) atlasSelection = null else atlasOpen = false }) { Text("Kembali") } })
        return
    }
    if (graphOpen) {
        val themeIndex = remember(tags) {
            items.flatMap { block -> themeSet(block).map { it to block } }
                .groupBy({ it.first }, { it.second })
        }
        val active = graphTheme
        AlertDialog(onDismissRequest = { graphOpen = false; graphTheme = null },
            title = { Text(if (active == null) "Smart Knowledge Graph · Tema ilmu" else active) },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    if (active == null) {
                        Text("Pilih tema untuk meneroka jaringan petikan.", fontSize = 13.sp)
                        LazyColumn(Modifier.heightIn(max = 360.dp)) {
                            items(themeIndex.entries.sortedByDescending { it.value.size }, key = { it.key }) { entry ->
                                TextButton(onClick = { graphTheme = entry.key }, modifier = Modifier.fillMaxWidth()) {
                                    Text(entry.key, Modifier.weight(1f))
                                    Text(entry.value.size.toString())
                                }
                            }
                        }
                        if (themeIndex.isEmpty()) Text("Tambah tag tema pada petikan Smart Vault dahulu.")
                    } else {
                        val blocks = themeIndex[active].orEmpty()
                        val neighbors = blocks.flatMap { themeSet(it) }.filter { it != active }
                            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                        LazyColumn(Modifier.heightIn(max = 380.dp)) {
                            item {
                                val visible = neighbors.take(6)
                                BoxWithConstraints(Modifier.fillMaxWidth().height(230.dp)) {
                                    val w = maxWidth
                                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                        visible.forEachIndexed { n, _ ->
                                            val angle = 2.0 * Math.PI * n / visible.size - Math.PI / 2
                                            val target = androidx.compose.ui.geometry.Offset(
                                                center.x + (size.width / 2 - 42.dp.toPx()).coerceAtLeast(0f) * kotlin.math.cos(angle).toFloat(),
                                                center.y + 79.dp.toPx() * kotlin.math.sin(angle).toFloat())
                                            drawLine(Color(0xFF9FC8BD), center, target, strokeWidth = 2.dp.toPx())
                                        }
                                    }
                                    Surface(Modifier.align(androidx.compose.ui.Alignment.Center).width(88.dp),
                                        shape = RoundedCornerShape(22.dp), color = Color(0xFF087F83)) {
                                        Text(active, Modifier.padding(8.dp), color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
                                    }
                                    visible.forEachIndexed { n, neighbor ->
                                        val angle = 2.0 * Math.PI * n / visible.size - Math.PI / 2
                                        val x = ((w.value / 2 - 42).coerceAtLeast(0f) * kotlin.math.cos(angle)).toFloat().dp
                                        val y = (79 * kotlin.math.sin(angle)).toFloat().dp
                                        Surface(Modifier.align(androidx.compose.ui.Alignment.Center).offset(x, y).width(78.dp)
                                            .clickable { graphTheme = neighbor.key },
                                            shape = RoundedCornerShape(14.dp), color = Color(0xFFE4F2ED)) {
                                            Text(neighbor.key, Modifier.padding(6.dp), color = Color(0xFF075B59),
                                                textAlign = TextAlign.Center, fontSize = 12.sp, maxLines = 2)
                                        }
                                    }
                                }
                                Text("Tema bersambung", color = Color(0xFF087F83))
                            }
                            items(neighbors, key = { "theme:" + it.key }) { neighbor ->
                                TextButton(onClick = { graphTheme = neighbor.key }) { Text(neighbor.key + " · " + neighbor.value) }
                            }
                            item { HorizontalDivider(); Text("Petikan · " + blocks.size, color = Color(0xFF087F83)) }
                            items(blocks, key = { "block:" + it }) { block ->
                                TextButton(onClick = { graphOpen = false; graphTheme = null; relatedBlock = block }) {
                                    Text(formattedNoteText(block), maxLines = 4)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (active != null) graphTheme = null else graphOpen = false }) {
                Text(if (active != null) "Semua tema" else "Kembali")
            } })
        return
    }
    relatedBlock?.let { source ->
        val themes = themeSet(source)
        val related = remember(source, tags, references) {
            items.filter { it != source }.map { candidate ->
                val shared = themeSet(candidate).intersect(themes)
                val book = references[source]?.getOrNull(0).orEmpty().trim()
                val sameBook = book.isNotEmpty() && book.equals(references[candidate]?.getOrNull(0).orEmpty().trim(), ignoreCase = true)
                Triple(candidate, shared, sameBook)
            }.filter { it.second.isNotEmpty() || it.third }
                .sortedByDescending { it.second.size * 2 + if (it.third) 1 else 0 }
        }
        AlertDialog(onDismissRequest = { relatedBlock = null },
            title = { Text("Smart Knowledge · Petikan berkaitan") },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    Text(formattedNoteText(source), maxLines = 3)
                    HorizontalDivider()
                    Text("Hubungan: " + related.size, color = Color(0xFF087F83))
                    if (related.isEmpty()) Text("Belum ada petikan berkaitan. Tambah tema atau sumber kitab yang sama.")
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(related, key = { it.first }) { entry ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                TextButton(onClick = { relatedBlock = entry.first }) {
                                    Text(formattedNoteText(entry.first), maxLines = 4)
                                }
                                Text((entry.second.toList() + if (entry.third) listOf("Kitab sama") else emptyList()).joinToString(" · "), fontSize = 12.sp)
                                TextButton(onClick = { onInsert(entry.first); onDismiss() }) { Text("Tampal petikan") }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { relatedBlock = null }) { Text("Kembali ke Vault") } })
        return
    }
    if (dualOpen) {
        SmartDualView(items, onClose = { dualOpen = false }, onInsert = { onInsert(it); onDismiss() })
        return
    }
    if (insightOpen) {
        val recallPrefs = context.getSharedPreferences("dinnote_smart_recall", android.content.Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val due = items.count { recallPrefs.getLong(tagKey(it), 0L) <= now }
        val reviewed = items.count { recallPrefs.contains(tagKey(it)) }
        val themes = tags.values.flatMap { it.split(",").map { tag -> tag.trim() } }
            .filter { it.isNotEmpty() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val referenced = items.count { referenceText(references[it].orEmpty()).isNotBlank() }
        AlertDialog(onDismissRequest = { insightOpen = false },
            title = { Text("Smart Insight · Statistik Ilmu") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    listOf("Petikan tersimpan" to items.size, "Tema ilmu" to themes.size,
                        "Petikan dengan rujukan" to referenced, "Pernah dimurajaah" to reviewed,
                        "Murajaah tersedia" to due).forEach { (label, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(label, modifier = Modifier.weight(1f))
                            Text(count.toString(), color = Color(0xFF087F83), fontSize = 20.sp)
                        }
                        HorizontalDivider()
                    }
                    Text("Tema utama", modifier = Modifier.padding(top = 12.dp), color = Color(0xFF087F83))
                    themes.take(10).forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(entry.key, modifier = Modifier.weight(1f))
                            Text(entry.value.toString())
                        }
                    }
                    if (themes.isEmpty()) Text("Belum ada tema.")
                }
            },
            confirmButton = { TextButton(onClick = { insightOpen = false; recallOpen = true }) { Text("Murajaah") } },
            dismissButton = { TextButton(onClick = { insightOpen = false }) { Text("Tutup") } })
        return
    }
    if (recallOpen) {
        SmartRecallDialog(items, onClose = { recallOpen = false })
        return
    }
    minbarMaterial?.let { material ->
        SmartMinbarDialog(material, onClose = { minbarMaterial = null })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Vault · Khazanah Faedah") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    label = { Text("Cari teks, tema atau rujukan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { atlasSelection = null; atlasOpen = true }) {
                    Text("Smart Knowledge Atlas", color = Color(0xFF087F83))
                }
                TextButton(onClick = { graphTheme = null; graphOpen = true }) {
                    Text("Smart Knowledge Graph", color = Color(0xFF087F83))
                }
                TextButton(onClick = { dualOpen = true }) {
                    Text("Smart Dual View · Ruang Pengajian", color = Color(0xFF087F83))
                }
                TextButton(onClick = { insightOpen = true }) {
                    Text("Smart Insight · Statistik Ilmu", color = Color(0xFF087F83))
                }
                TextButton(onClick = { recallOpen = true }) {
                    Text("Smart Recall · Murajaah", color = Color(0xFF087F83))
                }
                TextButton(onClick = { majlisMode = !majlisMode; majlisSelection = emptyList() }) {
                    Text(if (majlisMode) "Batal Smart Majlis" else "Smart Majlis · Pilih petikan", color = Color(0xFF087F83))
                }
                if (majlisMode) {
                    OutlinedTextField(value = majlisTitle, onValueChange = { majlisTitle = it },
                        label = { Text("Tajuk kuliah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Dipilih: " + majlisSelection.size, fontSize = 12.sp)
                    TextButton(enabled = majlisSelection.isNotEmpty(), onClick = { minbarMaterial = assembleMajlis() }) {
                        Text("Baca dalam Smart Minbar")
                    }
                }
                Column {
                    if (filtered.isEmpty()) Text("Tiada petikan ditemui", modifier = Modifier.padding(12.dp))
                    Text("Paparan: " + filtered.size + " / " + items.size, fontSize = 12.sp)
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(filtered, key = { it }) { block ->
                        val preview = remember(block) { formattedNoteText(block) }
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            if (majlisMode) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Checkbox(checked = block in majlisSelection, onCheckedChange = { checked ->
                                        majlisSelection = if (checked) majlisSelection + block else majlisSelection.filter { it != block }
                                    })
                                    Text(preview, modifier = Modifier.weight(1f), maxLines = 4)
                                }
                            } else TextButton(onClick = { onInsert(block); onDismiss() }) {
                                Text(preview, maxLines = 4)
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                TextButton(onClick = { editing = block; draft = tags[block].orEmpty(); refEditing = null }) {
                                    Text("Tema / tag", color = Color(0xFF087F83))
                                }
                                Text(tags[block].orEmpty(), fontSize = 12.sp, maxLines = 2)
                            }
                            TextButton(onClick = { relatedBlock = block }) {
                                Text("Petikan berkaitan", color = Color(0xFF087F83))
                            }
                            val citation = referenceText(references[block].orEmpty())
                            if (citation.isNotBlank()) Text(citation, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                            Row {
                                TextButton(onClick = { refEditing = block; refDraft = readReference(block); editing = null }) {
                                    Text("Smart Reference", color = Color(0xFF087F83))
                                }
                                if (citation.isNotBlank()) TextButton(onClick = { onInsert(block + "\n" + citation); onDismiss() }) {
                                    Text("Tampal + rujukan")
                                }
                            }
                            if (refEditing == block) {
                                referenceLabels.forEachIndexed { field, label ->
                                    OutlinedTextField(value = refDraft[field], onValueChange = { input ->
                                        refDraft = refDraft.mapIndexed { k, old -> if (k == field) input else old }
                                    }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                }
                                Row {
                                    TextButton(onClick = {
                                        val editor = index.edit()
                                        refDraft.forEachIndexed { field, input ->
                                            editor.putString("ref_" + tagKey(block) + "_" + field, input.trim())
                                        }
                                        editor.apply()
                                        references = references + (block to refDraft.map { it.trim() })
                                        refEditing = null
                                    }) { Text("Simpan rujukan") }
                                    TextButton(onClick = { refEditing = null }) { Text("Batal") }
                                }
                            }
                            if (editing == block) {
                                OutlinedTextField(value = draft, onValueChange = { draft = it },
                                    label = { Text("Contoh: Fiqh, Solat") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth())
                                Row {
                                    TextButton(onClick = {
                                        val clean = draft.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
                                        index.edit().putString(tagKey(block), clean).apply()
                                        tags = tags + (block to clean)
                                        editing = null
                                    }) { Text("Simpan") }
                                    TextButton(onClick = { editing = null }) { Text("Batal") }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    }
                }
            }
        },
        confirmButton = {
            if (majlisMode) TextButton(enabled = majlisSelection.isNotEmpty(), onClick = {
                onInsert(assembleMajlis())
                onDismiss()
            }) { Text("Tampal bahan kuliah") }
            else TextButton(onClick = onDismiss) { Text("Tutup") }
        },
        dismissButton = { if (majlisMode) TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

// Shared paragraph selection for library and structured content editors.
private fun smartBlockColor(kind: String): Color = when (kind) {
    "quran" -> Color(0xFFE9F3E7)
    "hadith" -> Color(0xFFFAF0D8)
    "arabic" -> Color(0xFFE6F2EF)
    else -> Color(0xFFF2ECE1)
}

private fun insertIslamicEditorBlock(value: TextFieldValue, kind: String): TextFieldValue {
    val a = value.selection.min.coerceIn(0, value.text.length)
    val b = value.selection.max.coerceIn(a, value.text.length)
    val before = smartFormattedExcerpt(value.text, 0, a)
    val after = smartFormattedExcerpt(value.text, b, value.text.length)
    val selected = smartFormattedExcerpt(value.text, a, b)
    val content = selected.ifBlank { when (kind) {
        "quran" -> "Ayat al-Quran"
        "hadith" -> "Matan hadis"
        "arabic" -> "Teks Arab"
        else -> "Kalam hikmah"
    } }
    val opening = "\n{{block:$kind}}"
    val closing = "{{/block}}\n"
    val start = before.length + opening.length
    return TextFieldValue(before + opening + content + closing + after,
        selection = TextRange(start, start + content.length))
}

@Composable
private fun IslamicStructuredReader(source: String, fontSize: Float, onEdit: () -> Unit) {
    val pattern = Regex("""\{\{block:(quran|hadith|arabic|hikmah)\}\}([\s\S]*?)\{\{/block\}\}""")
    var cursor = 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pattern.findAll(source).forEach { match ->
            val plain = source.substring(cursor, match.range.first)
            if (plain.isNotBlank()) Text(formattedNoteText(plain), fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6f).sp, modifier = Modifier.fillMaxWidth())
            val kind = match.groupValues[1]
            val label = when (kind) {
                "quran" -> "Ayat al-Quran"
                "hadith" -> "Hadis"
                "arabic" -> "Teks Arab"
                else -> "Kalam Hikmah"
            }
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
                color = if (kind == "arabic") Color(0xFFFFF8E9) else smartBlockColor(kind),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD3A63B))) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF075D49),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD3A63B))
                    ) {
                        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp, color = Color.White,
                            fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.material3.HorizontalDivider(
                        thickness = 0.5.dp, color = Color(0xFFD8C9A7))
                    Text(formattedNoteText(match.groupValues[2]), modifier = Modifier.fillMaxWidth(),
                        fontSize = if (kind == "arabic") 18.sp else 20.sp,
                        lineHeight = if (kind == "arabic") 31.sp else 33.sp,
                        textAlign = TextAlign.Right,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f), thickness = 0.5.dp,
                            color = Color(0xFFD3A63B))
                        Text("✦", modifier = Modifier.padding(horizontal = 12.dp),
                            color = Color(0xFFB68A29), fontSize = 14.sp)
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f), thickness = 0.5.dp,
                            color = Color(0xFFD3A63B))
                    }
                }
            }
            cursor = match.range.last + 1
        }
        val tail = source.substring(cursor)
        if (tail.isNotBlank()) Text(formattedNoteText(tail), fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.6f).sp, modifier = Modifier.fillMaxWidth())
    }
}

private fun highlightEditorSelection(value: TextFieldValue, hex: String): TextFieldValue {
    val a = value.selection.min.coerceIn(0, value.text.length)
    val b = value.selection.max.coerceIn(a, value.text.length)
    if (a == b) return value
    val before = smartFormattedExcerpt(value.text, 0, a)
    val selected = smartFormattedExcerpt(value.text, a, b)
    val after = smartFormattedExcerpt(value.text, b, value.text.length)
    val marked = "{{highlight:$hex}}" + selected + "{{/highlight}}"
    return TextFieldValue(before + marked + after, selection = TextRange(before.length + marked.length))
}

@Composable
private fun SharedParagraphTools(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var vaultOpen by remember { mutableStateOf(false) }
    val source = value.text
    val cursor = value.selection.min.coerceIn(0, source.length)
    val start = if (cursor == 0) 0 else source.lastIndexOf('\n', cursor - 1) + 1
    val end = source.indexOf('\n', cursor).let { if (it < 0) source.length else it }
    val enabled = end > start && formattedNoteText(smartFormattedExcerpt(source, start, end)).text.isNotBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val transfer = context.getSharedPreferences("dinnote_smart_transfer", android.content.Context.MODE_PRIVATE)
    var hasTransfer by remember { mutableStateOf(transfer.contains("block")) }
    val excerpt = if (enabled) smartFormattedExcerpt(source, start, end) else ""
    fun duplicateBlock() {
        val before = smartFormattedExcerpt(source, 0, end)
        val after = smartFormattedExcerpt(source, end, source.length)
        val inserted = "\n" + excerpt
        onChange(TextFieldValue(before + inserted + after, selection = TextRange(before.length + inserted.length)))
    }
    fun moveBlock(up: Boolean) {
        if (up && start == 0 || !up && end == source.length) return
        val left = if (up) { if (start <= 1) 0 else source.lastIndexOf('\n', start - 2) + 1 } else start
        val right = if (up) end else source.indexOf('\n', end + 1).let { if (it < 0) source.length else it }
        val neighbour = if (up) smartFormattedExcerpt(source, left, start - 1) else smartFormattedExcerpt(source, end + 1, right)
        val before = smartFormattedExcerpt(source, 0, left)
        val after = smartFormattedExcerpt(source, right, source.length)
        val swapped = if (up) excerpt + "\n" + neighbour else neighbour + "\n" + excerpt
        val position = before.length + if (up) excerpt.length else swapped.length
        onChange(TextFieldValue(before + swapped + after, selection = TextRange(position)))
    }
    if (vaultOpen) SmartVaultDialog(onDismiss = { vaultOpen = false }, onInsert = { block ->
        val a = value.selection.min.coerceIn(0, source.length)
        val b = value.selection.max.coerceIn(a, source.length)
        val before = smartFormattedExcerpt(source, 0, a)
        val after = smartFormattedExcerpt(source, b, source.length)
        onChange(TextFieldValue(before + block + after, selection = TextRange(before.length + block.length)))
    })
    val selected = value.copy(selection = TextRange(start, end), composition = null)
    val target = if (value.selection.collapsed) selected else value
    Box(Modifier.wrapContentWidth()) {
        TextButton(onClick = { hasTransfer = transfer.contains("block"); expanded = true }) {
            Text("▤ Smart Block", color = if (enabled) Color(0xFF087F83) else Color.Gray, fontSize = 13.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Box al-Quran" to "quran", "Box Hadis" to "hadith", "Box Teks Arab" to "arabic", "Box Kalam Hikmah" to "hikmah").forEach { (label, kind) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    onChange(insertIslamicEditorBlock(value, kind))
                })
            }
            listOf("Highlight emas" to "#F5E3A0", "Highlight hijau" to "#D5ECDD").forEach { (label, hex) ->
                DropdownMenuItem(text = { Text(label) }, enabled = !target.selection.collapsed, onClick = {
                    expanded = false
                    onChange(highlightEditorSelection(target, hex))
                })
            }
            DropdownMenuItem(text = { Text("Pilih perenggan") }, enabled = enabled, onClick = {
                expanded = false
                onChange(selected)
            })
            DropdownMenuItem(text = { Text("Salin perenggan") }, enabled = enabled, onClick = {
                expanded = false
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DinNote", formattedNoteText(excerpt).text))
            })
            DropdownMenuItem(text = { Text("Salin dengan format DinNote") }, enabled = enabled, onClick = {
                expanded = false
                transfer.edit().putString("block", excerpt).apply()
                hasTransfer = true
                android.widget.Toast.makeText(context, "Blok disalin dengan format", android.widget.Toast.LENGTH_SHORT).show()
            })
            DropdownMenuItem(text = { Text("Tampal blok DinNote") }, enabled = hasTransfer, onClick = {
                expanded = false
                val block = transfer.getString("block", null)
                if (!block.isNullOrEmpty()) {
                    val a = value.selection.min.coerceIn(0, source.length)
                    val b = value.selection.max.coerceIn(a, source.length)
                    val before = smartFormattedExcerpt(source, 0, a)
                    val after = smartFormattedExcerpt(source, b, source.length)
                    onChange(TextFieldValue(before + block + after, selection = TextRange(before.length + block.length)))
                }
            })
            DropdownMenuItem(text = { Text("Kosongkan salinan DinNote") }, enabled = hasTransfer, onClick = {
                expanded = false
                transfer.edit().remove("block").apply()
                hasTransfer = false
            })
            DropdownMenuItem(text = { Text("Simpan ke Smart Vault") }, enabled = enabled, onClick = {
                expanded = false
                saveSmartVault(context, excerpt)
                android.widget.Toast.makeText(context, "Petikan disimpan", android.widget.Toast.LENGTH_SHORT).show()
            })
            DropdownMenuItem(text = { Text("Buka Smart Vault") }, onClick = {
                expanded = false
                vaultOpen = true
            })
            DropdownMenuItem(text = { Text("Duplikasi perenggan") }, enabled = enabled, onClick = {
                expanded = false
                duplicateBlock()
            })
            DropdownMenuItem(text = { Text("Pindah ke atas") }, enabled = enabled && start > 0, onClick = {
                expanded = false
                moveBlock(true)
            })
            DropdownMenuItem(text = { Text("Pindah ke bawah") }, enabled = enabled && end < source.length, onClick = {
                expanded = false
                moveBlock(false)
            })
            DropdownMenuItem(text = { Text("Warnakan perenggan") }, enabled = enabled, onClick = {
                expanded = false
                onChange(colorEditorSelection(selected, 0xFF087F83L))
            })
        }
    }
}

// Default content field for all structured Kalimah editors.
@Composable
private fun DefaultEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    arabic: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF087F83),
            unfocusedBorderColor = Color(0xFFD5DADD),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = (if (arabic) 21 else 16).sp,
            color = Color(0xFF263B3E),
            textDirection = if (arabic) TextDirection.Rtl else TextDirection.ContentOrLtr,
            textAlign = if (arabic) TextAlign.Right else TextAlign.Start
        )
    )
}

// Shared default editor controls: formatting markers are never inserted into content.
@Composable
private fun DefaultEditorFontControls(fontSize: Float, onChange: (Float) -> Unit, hasSelection: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("Saiz tulisan", fontSize = 12.sp, color = Color(0xFF53736F))
        Spacer(Modifier.width(8.dp))
        TextButton(
            modifier = Modifier.height(36.dp),
            onClick = { onChange((fontSize - 1f).coerceAtLeast(14f)) },
            enabled = hasSelection || fontSize > 14f
        ) {
            Text("A−", color = Color(0xFF087F83), fontSize = 16.sp)
        }
        Text("${fontSize.toInt()}", color = Color(0xFF075B59), fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold)
        TextButton(
            modifier = Modifier.height(36.dp),
            onClick = { onChange((fontSize + 1f).coerceAtMost(30f)) },
            enabled = hasSelection || fontSize < 30f
        ) {
            Text("A+", color = Color(0xFF087F83), fontSize = 16.sp)
        }
    }
}

@Composable
private fun UnifiedEditorHeading(title: String, onBack: () -> Unit, onSave: () -> Unit) {
    Surface(color = Color(0xFF006E70), shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
        Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 24.sp) }
            Text("✎  " + title, modifier = Modifier.weight(1f), fontSize = 15.sp,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onSave) { Text("✓", color = Color(0xFFFFD56A), fontSize = 27.sp) }
        }
    }
}

@Composable
private fun UnifiedEditorMosqueFooter(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(82.dp)) {
        val w = size.width
        val h = size.height
        val teal = Color(0xFF008F93)
        val gold = Color(0xFFD5AD55)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        drawRect(Color(0xFFEAF8F7).copy(alpha = 0.65f))
        for (i in 0..10) {
            val cx = w * (i + 0.5f) / 11f
            val half = w / 25f
            val peak = if (i % 3 == 0) h * 0.13f else h * 0.33f
            val arch = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - half, h)
                lineTo(cx - half, h * 0.57f)
                quadraticBezierTo(cx - half, peak + h * 0.08f, cx, peak)
                quadraticBezierTo(cx + half, peak + h * 0.08f, cx + half, h * 0.57f)
                lineTo(cx + half, h)
            }
            drawPath(arch, teal.copy(alpha = 0.26f), style = stroke)
        }
        val wave = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.84f)
            quadraticBezierTo(w * 0.23f, h * 0.57f, w * 0.48f, h * 0.83f)
            quadraticBezierTo(w * 0.72f, h * 1.02f, w, h * 0.68f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(wave, teal.copy(alpha = 0.68f))
        drawPath(wave, gold.copy(alpha = 0.85f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun UnifiedEditorDivider() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
        Text(" ۞ ", color = Color(0xFFC9A24A), fontSize = 21.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD8B66A), thickness = 1.dp)
    }
}

@Composable
private fun IslamicCardFooter() {
    // Shared compact Islamic ruler divider: 75% opacity, centered and flush to the card bottom.
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth().height(24.dp)
    ) {
        val w = size.width
        val h = size.height
        val teal = Color(0xFF008F93).copy(alpha = 0.75f)
        val gold = Color(0xFFD4A443).copy(alpha = 0.75f)
        val center = w / 2f
        val half = w * 0.23f
        val y = h - 1.dp.toPx()
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        drawLine(teal, androidx.compose.ui.geometry.Offset(center-half, y),
            androidx.compose.ui.geometry.Offset(center+half, y), strokeWidth = 1.dp.toPx())
        drawLine(gold, androidx.compose.ui.geometry.Offset(center-half*0.88f, y-4.dp.toPx()),
            androidx.compose.ui.geometry.Offset(center+half*0.88f, y-4.dp.toPx()),
            strokeWidth = 1.dp.toPx())
        for (i in -5..5) {
            val x = center + i * half / 6f
            val tickHeight = if (i % 2 == 0) 6.dp.toPx() else 3.dp.toPx()
            drawLine(teal, androidx.compose.ui.geometry.Offset(x,y),
                androidx.compose.ui.geometry.Offset(x,y-tickHeight), strokeWidth = 1.dp.toPx())
        }
        val diamond = androidx.compose.ui.graphics.Path().apply {
            moveTo(center, y-15.dp.toPx())
            lineTo(center+7.dp.toPx(), y-8.dp.toPx())
            lineTo(center, y-1.dp.toPx())
            lineTo(center-7.dp.toPx(), y-8.dp.toPx())
            close()
        }
        drawPath(diamond, gold, style = stroke)
        drawCircle(teal, radius = 2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(center,y-8.dp.toPx()))
    }
}

@Composable
private fun DinNoteFrontHeader() {
    val context = LocalContext.current
    val banner = remember(context) {
        runCatching {
            context.assets.open("dinnote_header.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (banner != null) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            val bannerRatio = banner.width.toFloat() / banner.height.toFloat()
            // Preserve the artwork while limiting vertical space on wide and unfolded displays.
            val bannerWidth = minOf(maxWidth, 540.dp, 112.dp * bannerRatio)
            androidx.compose.foundation.Image(
                bitmap = banner,
                contentDescription = "DinNote — Daripada catatan kepada khazanah ilmu.",
                contentScale = ContentScale.Fit,
                alpha = 1f,
                modifier = Modifier.width(bannerWidth)
                    .aspectRatio(bannerRatio)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    } else {
        DinNoteFrontHeaderFallback()
    }
}

@Composable
private fun DinNoteFrontHeaderFallback() {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFF003C35), Color(0xFF08675D), Color(0xFF00483F))))) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val gold = Color(0xFFE7C77E)
                val arch = Path().apply {
                    moveTo(0f, h * .76f)
                    cubicTo(w * .08f, h * .32f, w * .20f, h * .34f, w * .25f, h * .32f)
                    cubicTo(w * .30f, h * .12f, w * .43f, h * .16f, w * .50f, 2f)
                    cubicTo(w * .57f, h * .16f, w * .70f, h * .12f, w * .75f, h * .32f)
                    cubicTo(w * .80f, h * .34f, w * .92f, h * .32f, w, h * .62f)
                }
                drawPath(arch, Color(0xFF18A08A), style = Stroke(width = 7.dp.toPx()))
                drawPath(arch, gold, style = Stroke(width = 3.dp.toPx()))
                listOf(.11f, .89f).forEach { fraction ->
                    val x = w * fraction
                    val y = h * .58f
                    val r = 7.dp.toPx()
                    drawLine(gold, Offset(x, h * .39f), Offset(x, y), 1.dp.toPx())
                    drawCircle(gold.copy(alpha = .12f), r * 2.4f, Offset(x, y + r))
                    val lantern = Path().apply {
                        moveTo(x, y); lineTo(x + r, y + r)
                        lineTo(x + r * .65f, y + r * 2.2f)
                        lineTo(x, y + r * 2.8f)
                        lineTo(x - r * .65f, y + r * 2.2f)
                        lineTo(x - r, y + r); close()
                    }
                    drawPath(lantern, gold)
                }
            }

        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 36.dp, vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(R.drawable.ic_dinnote_header), contentDescription = "Logo DinNote", modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("DinNote", fontSize = 25.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color(0xFFFFF9E9))
                Text("Daripada catatan kepada khazanah ilmu.", fontSize = 10.sp, color = Color(0xFFFFF2D1), textAlign = TextAlign.Center)
                Text("──── ❖ ────", color = Color(0xFFE7C77E), fontSize = 9.sp)
            }
        }
    }
}


private val tafsirSurahMalay = "Al-Fatihah|Al-Baqarah|Ali Imran|An-Nisa|Al-Maidah|Al-Anam|Al-Araf|Al-Anfal|At-Taubah|Yunus|Hud|Yusuf|Ar-Rad|Ibrahim|Al-Hijr|An-Nahl|Al-Isra|Al-Kahfi|Maryam|Taha|Al-Anbiya|Al-Hajj|Al-Muminun|An-Nur|Al-Furqan|Asy-Syuara|An-Naml|Al-Qasas|Al-Ankabut|Ar-Rum|Luqman|As-Sajdah|Al-Ahzab|Saba|Fatir|Yasin|As-Saffat|Sad|Az-Zumar|Ghafir|Fussilat|Asy-Syura|Az-Zukhruf|Ad-Dukhan|Al-Jathiyah|Al-Ahqaf|Muhammad|Al-Fath|Al-Hujurat|Qaf|Az-Zariyat|At-Tur|An-Najm|Al-Qamar|Ar-Rahman|Al-Waqiah|Al-Hadid|Al-Mujadilah|Al-Hasyr|Al-Mumtahanah|As-Saff|Al-Jumuah|Al-Munafiqun|At-Taghabun|At-Talaq|At-Tahrim|Al-Mulk|Al-Qalam|Al-Haqqah|Al-Maarij|Nuh|Al-Jinn|Al-Muzzammil|Al-Muddaththir|Al-Qiyamah|Al-Insan|Al-Mursalat|An-Naba|An-Naziat|Abasa|At-Takwir|Al-Infitar|Al-Mutaffifin|Al-Insyiqaq|Al-Buruj|At-Tariq|Al-Ala|Al-Ghasyiyah|Al-Fajr|Al-Balad|Asy-Syams|Al-Lail|Ad-Duha|Asy-Syarh|At-Tin|Al-Alaq|Al-Qadr|Al-Bayyinah|Az-Zalzalah|Al-Adiyat|Al-Qariah|At-Takathur|Al-Asr|Al-Humazah|Al-Fil|Quraisy|Al-Maun|Al-Kawthar|Al-Kafirun|An-Nasr|Al-Masad|Al-Ikhlas|Al-Falaq|An-Nas".split('|')
private val tafsirSurahArabic = "الفاتحة|البقرة|آل عمران|النساء|المائدة|الأنعام|الأعراف|الأنفال|التوبة|يونس|هود|يوسف|الرعد|إبراهيم|الحجر|النحل|الإسراء|الكهف|مريم|طه|الأنبياء|الحج|المؤمنون|النور|الفرقان|الشعراء|النمل|القصص|العنكبوت|الروم|لقمان|السجدة|الأحزاب|سبأ|فاطر|يس|الصافات|ص|الزمر|غافر|فصلت|الشورى|الزخرف|الدخان|الجاثية|الأحقاف|محمد|الفتح|الحجرات|ق|الذاريات|الطور|النجم|القمر|الرحمن|الواقعة|الحديد|المجادلة|الحشر|الممتحنة|الصف|الجمعة|المنافقون|التغابن|الطلاق|التحريم|الملك|القلم|الحاقة|المعارج|نوح|الجن|المزمل|المدثر|القيامة|الإنسان|المرسلات|النبأ|النازعات|عبس|التكوير|الانفطار|المطففين|الانشقاق|البروج|الطارق|الأعلى|الغاشية|الفجر|البلد|الشمس|الليل|الضحى|الشرح|التين|العلق|القدر|البينة|الزلزلة|العاديات|القارعة|التكاثر|العصر|الهمزة|الفيل|قريش|الماعون|الكوثر|الكافرون|النصر|المسد|الإخلاص|الفلق|الناس".split('|')

@Composable
private fun IbnKathirCollection(search: String, onNavigate: (Int) -> Unit) {
    val context = LocalContext.current
    val available = remember(context) {
        context.assets.list("turath/ibn_kathir").orEmpty().mapNotNull { it.substringBefore(".").toIntOrNull() }
            .filter { it in 1..114 }.sorted()
    }
    if (available.isEmpty()) {
        TurathStarterCard(search, "turath/ibn_kathir_fatihah.json", onNavigate)
        TurathStarterCard(search, "turath/ibn_kathir_short.json", onNavigate)
        return
    }
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var chosen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    fun matches(n: Int, term: String): Boolean = term.isBlank() ||
        n.toString() == term.trim() || matchesMaktabahSearch(tafsirSurahMalay[n-1] + " " + tafsirSurahArabic[n-1], term)
    if (search.isNotBlank() && !matchesMaktabahSearch("Tafsir Ibn Kathir تفسير ابن كثير", search) && available.none { matches(it, search) }) return
    Card(onClick = { query = ""; expanded = true }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF4)),
        border = BorderStroke(1.dp, Color(0xFFD3B66F))) {
        Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(60.dp).height(82.dp).background(Color(0xFF124D40), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD3B66F), RoundedCornerShape(8.dp)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("ابْنُ كَثِيرٍ", color = Color(0xFFFFE8B0), fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            Column(Modifier.weight(1f)) {
                Text("تَفْسِيرُ الْقُرْآنِ الْعَظِيمِ", Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075B49))
                Text("Tafsir Ibn Kathir", fontWeight = FontWeight.Bold)
                Text("Imam Ibn Kathir • 774H", fontSize = 12.sp, color = Color(0xFF59685E))
                Text("${available.size}/114 surah • Arab offline", fontSize = 11.sp, color = Color(0xFF075B49))
            }
        }
    }
    if (expanded) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expanded = false; chosen = 0 },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = Color(0xFFFFFCF4)) {
                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color(0xFF124D40), shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFD3B66F))) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                TextButton(onClick = { if (chosen != 0) chosen = 0 else expanded = false }) { Text("‹ Kembali", color = Color(0xFFFFE8B0)) }
                                Text(if (chosen == 0) "تَفْسِيرُ ابْنِ كَثِيرٍ" else tafsirSurahArabic[chosen-1],
                                    Modifier.weight(1f), textAlign = TextAlign.End, color = Color.White, fontSize = 20.sp)
                            }
                            Text("Tafsir Ibn Kathir • ${available.size} surah", color = Color(0xFFFFE8B0), fontSize = 12.sp)
                        }
                    }
                    if (chosen != 0) {
                        key(chosen) { TurathStarterCard("", "turath/ibn_kathir/$chosen.json", onNavigate, openInitially = true) { chosen = 0 } }
                    } else {
                        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Cari nama atau nombor surah") })
                        Text("Terjemahan Melayu belum lengkap", fontSize = 11.sp, color = Color(0xFF59685E))
                        val filtered = available.filter { matches(it, query) }
                        if (filtered.isEmpty()) Text("Tiada surah sepadan.", fontSize = 13.sp)
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(filtered) { n ->
                                Card(onClick = { chosen = n }, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE4E8DF))) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(Modifier.size(36.dp).background(Color(0xFFE8F0E5), RoundedCornerShape(8.dp)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center) { Text(n.toString(), color = Color(0xFF075B49), fontSize = 12.sp) }
                                        Text(tafsirSurahMalay[n-1], Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(tafsirSurahArabic[n-1], fontSize = 19.sp, color = Color(0xFF075B49))
                                        Text("›", color = Color(0xFFA78945), fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TurathStarterCard(search: String, assetPath: String = "turath/arba_in_nawawiyyah.json", onNavigate: (Int) -> Unit = {}, openInitially: Boolean = false, onReaderClose: () -> Unit = {}) {
    val context = LocalContext.current
    val book = remember(context, assetPath) {
        runCatching { context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { org.json.JSONObject(it.readText()) } }.getOrNull()
    } ?: return
    val entries = remember(book) {
        val array = book.getJSONArray("entries")
        (0 until array.length()).map { array.getJSONObject(it) }
    }
    var opened by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(openInitially) }
    var selected by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(-1) }
    var language by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (assetPath.contains("ibn_kathir")) "Arab" else if (assetPath.contains("al_hikam")) "Hikmah" else "Bersebelahan") }
    val readerPrefs = remember(context) { context.getSharedPreferences("turath_reader", android.content.Context.MODE_PRIVATE) }
    var readerScale by remember(readerPrefs) { mutableStateOf(readerPrefs.getInt("reader_scale", 0).coerceIn(-4, 8)) }
    var lastRead by remember(assetPath) { mutableStateOf(readerPrefs.getInt(assetPath, -1)) }
    val visible = search.isBlank() || matchesMaktabahSearch(book.optString("titleArabic")+" "+book.optString("titleMalay"), search) ||
        entries.any { matchesMaktabahSearch(it.optString("arabic")+" "+it.optString("malay"),search) }
    if(visible && !openInitially) {
        val coverColor = when {
            assetPath.contains("al_hikam") -> Color(0xFF223F36)
            assetPath.contains("majmu") -> Color(0xFF722F35)
            assetPath.contains("fatihah") -> Color(0xFF16465D)
            else -> Color(0xFF075B49)
        }
        val coverTitle = when {
            assetPath.contains("al_hikam") -> "الْحِكَمُ"
            assetPath.contains("majmu") -> "الْمَجْمُوعُ"
            assetPath.contains("ibn_kathir") -> "ابْنُ كَثِيرٍ"
            assetPath.contains("fatihah") -> "الْفَاتِحَةُ"
            assetPath.contains("arba") -> "الْأَرْبَعُونَ"
            else -> book.optString("titleArabic")
        }
        Card(onClick = { opened = true; selected = -1 },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFD3B66F)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF4))) {
            Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(76.dp).height(102.dp)
                    .background(Brush.horizontalGradient(listOf(coverColor, coverColor, Color(0xFF142E30))), RoundedCornerShape(5.dp))
                    .border(2.dp, Color(0xFFB99A54), RoundedCornerShape(5.dp))
                    .padding(5.dp)
                    .border(1.dp, Color(0xFFE8CF91), RoundedCornerShape(3.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)) {
                        Text("❖", color = Color(0xFFE8CF91), fontSize = 13.sp)
                        Text(coverTitle, color = Color(0xFFFFF6DE), fontSize = 12.sp,
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Text("❖", color = Color(0xFFE8CF91), fontSize = 13.sp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(book.optString("titleArabic"), modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075B49))
                    Text(book.optString("titleMalay"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF263B30))
                    Text(if (book.has("expectedEntries")) "${entries.size} / ${book.optInt("expectedEntries")} hadis • Contoh awal" else "${entries.size} petikan • Koleksi awal",
                        fontSize = 11.sp, color = Color(0xFF59685E))
                    Text("Offline", fontSize = 10.sp, color = Color(0xFF075B49),
                        modifier = Modifier.background(Color(0xFFE8F0E5), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
    if(opened) {
        androidx.compose.ui.window.Dialog(onDismissRequest={opened=false; onReaderClose()},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false, decorFitsSystemWindows=false)) {
            Surface(modifier=Modifier.fillMaxSize().safeDrawingPadding(),color=Color.White) {
                Column(Modifier.fillMaxSize().padding(horizontal=18.dp, vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(selected !in entries.indices) {
                        var indexQuery by remember { mutableStateOf("") }
                        val visibleIndices = remember(entries, indexQuery) {
                            val term = normalizeArabicSearch(indexQuery.trim())
                            entries.indices.filter { i ->
                                term.isBlank() || normalizeArabicSearch(entries[i].optString("titleArabic")).contains(term) ||
                                    entries[i].optString("titleMalay").contains(indexQuery.trim(), ignoreCase = true)
                            }
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF124D40),
                            border = BorderStroke(1.dp, Color(0xFFD3B66F))) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    TextButton(onClick = { opened = false; onReaderClose() }) { Text("‹ Tutup", color = Color(0xFFE8CF91)) }
                                    Text(book.optString("titleArabic"), Modifier.weight(1f), textAlign = TextAlign.End,
                                        fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE8B0))
                                }
                                Text(book.optString("titleMalay"), Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                                    fontSize = 13.sp, color = Color.White)
                                Text("Indeks kandungan • ${entries.size} petikan", Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End, fontSize = 11.sp, color = Color(0xFFE8CF91))
                            }
                        }
                        OutlinedTextField(indexQuery, { indexQuery = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Cari bab atau tajuk...", fontSize = 13.sp) })
                        if (lastRead in entries.indices) {
                            TextButton(onClick = { selected = lastRead }) { Text("Sambung bacaan • ${lastRead + 1}", color = Color(0xFF075B49)) }
                        }
                        Text("KANDUNGAN KITAB  •  ${visibleIndices.size}", fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF075B49))
                        if (visibleIndices.isEmpty()) Text("Tiada tajuk sepadan.", color = Color.Gray, fontSize = 13.sp)
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(visibleIndices.size) { position ->
                                val index = visibleIndices[position]
                                val e = entries[index]
                                Card(onClick = { selected = index }, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE4E8DF)),
                                    colors = CardDefaults.cardColors(containerColor = if (index == lastRead) Color(0xFFE6EFE8) else Color(0xFFFFFCF4))) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Box(Modifier.size(34.dp).background(Color(0xFFE8F0E5), RoundedCornerShape(8.dp)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            Text((index + 1).toString().padStart(2, '0'), fontSize = 11.sp, color = Color(0xFF075B49))
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(e.optString("titleArabic"), Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                                                fontSize = 19.sp, lineHeight = 28.sp, color = Color(0xFF183F34))
                                            Text(e.optString("titleMalay"), fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF59685E))
                                        }
                                        Text("›", fontSize = 22.sp, color = Color(0xFFA78945))
                                    }
                                }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            TextButton(onClick = { selected = -1 }) { Text("‹ Senarai") }
                            Text(book.optString("titleArabic"), Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        val e=entries[selected]
                        androidx.compose.runtime.LaunchedEffect(selected, assetPath) {
                            lastRead = selected
                            readerPrefs.edit().putInt(assetPath, selected).apply()
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            (if (assetPath.contains("al_hikam")) listOf("Hikmah","Arab","Melayu","Bersebelahan") else listOf("Arab","Melayu","Bersebelahan")).forEach { mode ->
                                FilterChip(selected=language==mode,onClick={language=mode},label={Text(mode,fontSize=11.sp)})
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.End, verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            TextButton(enabled=readerScale > -4, onClick={ readerScale--; readerPrefs.edit().putInt("reader_scale", readerScale).apply() }) { Text("A−") }
                            TextButton(onClick={ readerScale=0; readerPrefs.edit().putInt("reader_scale", 0).apply() }) { Text("${20 + readerScale} sp") }
                            TextButton(enabled=readerScale < 8, onClick={ readerScale++; readerPrefs.edit().putInt("reader_scale", readerScale).apply() }) { Text("A+") }
                        }
                        key(selected) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(if (assetPath.contains("al_hikam")) "HIKMAH ${selected+1} / ${entries.size}" else "BACAAN ${selected+1} / ${entries.size}", fontSize = 11.sp, color = Color(0xFF075B49), fontWeight = FontWeight.Medium)
                                    Text(e.optString("titleMalay"),fontWeight=FontWeight.Bold,fontSize=17.sp,lineHeight=23.sp,color=Color(0xFF171717))
                                }
                                val arabicStyle = androidx.compose.ui.text.TextStyle(fontSize=(20+readerScale).sp,lineHeight=((20+readerScale)*1.6f).sp,textDirection=TextDirection.Rtl,textAlign=TextAlign.Right,color=MaterialTheme.colorScheme.onSurface)
                                val malayStyle = androidx.compose.ui.text.TextStyle(fontSize=(16+readerScale).sp,lineHeight=((16+readerScale)*1.5f).sp,textDirection=TextDirection.Ltr,color=Color(0xFF171717))
                                if (language == "Hikmah" && assetPath.contains("al_hikam")) {
                                    val parts = e.optString("malay").split("\n\nHURAIAN TEMATIK (bukan petikan syarah Ibn Ajibah):\n", limit = 2)
                                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF124D40), border = BorderStroke(1.dp, Color(0xFFD3B66F))) {
                                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("❖  MATAN HIKMAH  ❖", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFE8CF91), fontSize = 11.sp)
                                            ReferenceReaderText(text = AnnotatedString(e.optString("arabic")), modifier = Modifier.fillMaxWidth(), style = arabicStyle.copy(color = Color(0xFFFFF6DE), textAlign = TextAlign.Center))
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF5F7F0), border = BorderStroke(1.dp, Color(0xFFE4E8DF))) {
                                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("TERJEMAHAN MELAYU", color = Color(0xFF075B49), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            ReferenceReaderText(text = AnnotatedString(parts[0]), modifier = Modifier.fillMaxWidth(), style = malayStyle)
                                        }
                                    }
                                    if (parts.size > 1) Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFFCF4), border = BorderStroke(1.dp, Color(0xFFD3B66F))) {
                                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("HURAIAN TEMATIK", color = Color(0xFF075B49), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Bukan petikan syarah asal Ibn Ajibah", fontSize = 11.sp, color = Color(0xFF59685E))
                                            ReferenceReaderText(text = AnnotatedString(parts[1]), modifier = Modifier.fillMaxWidth(), style = malayStyle)
                                        }
                                    }
                                } else if(language=="Bersebelahan") {
                                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    if(maxWidth < 600.dp) {
                                        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                            ReferenceReaderText(text=AnnotatedString(e.optString("arabic")),modifier=Modifier.fillMaxWidth(),style=arabicStyle)
                                            HorizontalDivider(color=Color(0xFFE7E7E7))
                                            Text("Terjemahan Melayu • Draf",fontSize=12.sp,color=Color.Gray)
                                            ReferenceReaderText(text=AnnotatedString(e.optString("malay")),modifier=Modifier.fillMaxWidth(),style=malayStyle)
                                        }
                                    } else {
                                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                                Text("Melayu • Draf",fontSize=12.sp,fontWeight=FontWeight.Bold)
                                                ReferenceReaderText(text=AnnotatedString(e.optString("malay")),modifier=Modifier.fillMaxWidth(),style=malayStyle)
                                            }
                                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                                Text("النص العربي",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.End,fontSize=14.sp,fontWeight=FontWeight.Bold)
                                                ReferenceReaderText(text=AnnotatedString(e.optString("arabic")),modifier=Modifier.fillMaxWidth(),style=arabicStyle)
                                            }
                                        }
                                    }
                                }
                                    }
                                } else if(language=="Arab") {
                                    ReferenceReaderText(text=AnnotatedString(e.optString("arabic")),modifier=Modifier.fillMaxWidth(),style=arabicStyle)
                                } else {
                                    Text("Terjemahan Melayu • Draf",fontWeight=FontWeight.Bold)
                                    ReferenceReaderText(text=AnnotatedString(e.optString("malay")),modifier=Modifier.fillMaxWidth(),style=malayStyle)
                                }
                                if(e.has("page")) Text("Jilid ${e.optInt("volume")} • Halaman ${e.optInt("page")}",fontSize=12.sp)
                                val bookSource = book.optString("source")
                                if (bookSource.startsWith("https://")) {
                                    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookSource))) } }) { Text("Rujukan edisi sumber", color = Color(0xFF075B49)) }
                                }
                                val sources=e.optJSONArray("source")
                                if(sources!=null) for(i in 0 until sources.length()) {
                                    val url=sources.optString(i)
                                    TextButton(onClick={runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }}) { Text("Sumber asal "+(i+1)) }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            TextButton(enabled=selected>0,onClick={selected--}) { Text("Sebelum") }
                            Text("${selected+1} / ${entries.size}")
                            TextButton(enabled=selected<entries.lastIndex,onClick={selected++}) { Text("Seterusnya") }
                        }
                    }
                    HorizontalDivider(color=Color(0xFFE7E7E7))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                        listOf(Triple("Utama","⌂",0),Triple("Nota","✎",1),Triple("Maktabah","▤",2),Triple("Tetapan","⚙",4)).forEach { tab ->
                            Column(Modifier.weight(1f).clickable { opened=false; onReaderClose(); if(tab.third!=2) onNavigate(tab.third) }.padding(vertical=8.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(tab.second,fontSize=20.sp,color=if(tab.third==2) Color(0xFF087F83) else Color.DarkGray)
                                Text(tab.first,fontSize=11.sp,color=if(tab.third==2) Color(0xFF087F83) else Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
