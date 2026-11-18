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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    androidx.compose.foundation.Canvas(Modifier.size(25.dp)) {
        val ink = Color(0xFF087F80)
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
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFFEFA),
        border = BorderStroke(1.dp, Color(0xFFD6E3DE)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DashboardLineIcon(symbol)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(25.dp).height(1.dp).background(Color(0xFFD3AE65)))
            Spacer(Modifier.height(5.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF183A30), maxLines = 2, lineHeight = 14.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF53736F),
                maxLines = 2, lineHeight = 12.sp, textAlign = TextAlign.Center)
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
        pinnedNotes.addAll(activeNotes.filter { it.pinned }.sortedByDescending { it.updatedAt })
        recentlyAddedNotes.clear()
        recentlyAddedNotes.addAll(activeNotes.sortedByDescending { it.id }.take(1))
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
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
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
                        Text("Nota Dipinkan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                val headerContext = LocalContext.current
                val headerLogo = remember(headerContext) {
                    runCatching {
                        headerContext.assets.open("dinnote_logo_header.png").use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
                if (headerLogo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = headerLogo,
                        contentDescription = "Logo DinNote asal",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(112.dp).height(38.dp)
                    )
                } else {
                    Text("DinNote", fontSize = 17.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                        color = Color(0xFF075B59))
                }
            }
            pinnedNotes.forEach { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPinnedNote(note.id) },
                    color = Color(0xFFFFFEFA),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFB9D6CE)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("✦", fontSize = 12.sp, color = Color(0xFFC49A48))
                        Text(note.title.ifBlank { "Tanpa tajuk" },
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF075B59), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("›", fontSize = 16.sp, color = Color(0xFF08766C))
                    }
                }
            }
        }
        if (recentlyAddedNotes.isNotEmpty()) {
            PremiumSectionHeading("Terakhir Ditambah", "✎")
            recentlyAddedNotes.forEach { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPinnedNote(note.id) },
                    color = Color(0xFFFFFEFA),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFB9D6CE)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("✎", fontSize = 12.sp, color = Color(0xFF08766C))
                        Text(note.title.ifBlank { "Tanpa tajuk" },
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF075B59), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("›", fontSize = 16.sp, color = Color(0xFF08766C))
                    }
                }
            }
        }
        reminder?.let { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expandedReminder = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFB9D6CE))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Ingatan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF08766C))
                    Text(formattedNoteText(entry.arabic), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right,
                        fontSize = 18.sp, color = Color(0xFF183A30))
                    Text(formattedNoteText(entry.meaning), fontSize = 13.sp, color = Color(0xFF52666A))
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
                Text("Teroka DinNote", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DinNoteDashboardTile("Nota", "Catatan & kuliah", "note",
                Modifier.weight(1f), onOpenNotes)
            DinNoteDashboardTile("Maktabah", "Kitab & rujukan", "library",
                Modifier.weight(1f), onOpenLibrary)
            DinNoteDashboardTile("Carian", "Arab & Melayu", "search",
                Modifier.weight(1f), onOpenSearch)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DinNoteDashboardTile("Penanda", "Kitab & bacaan", "bookmark",
                Modifier.weight(1f), onOpenLibrary)
            DinNoteDashboardTile("Mazhab Syafi‘i", "Kitab & hukum", "school",
                Modifier.weight(1f), onOpenLibrary)
            DinNoteDashboardTile("Tetapan", "Paparan & data", "settings",
                Modifier.weight(1f), onOpenSettings)
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
                Text("Teruskan ›", fontSize = 12.sp, color = Color.White)
            }
        }
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
    var renamedCategory by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
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

    val visibleNotes = remember(notes, showArchived, selectedCategory, search) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (showArchived) "Arkib Nota" else "Nota Saya", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "Nota" else "Arkib") }
                Button(onClick = {
                editingId = null
                title = ""
                body = TextFieldValue("")
                undoStack.clear(); redoStack.clear()
                selectedColor = "green"
                selectedCategory = if (selectedCategory == "Semua") "Lain-lain" else selectedCategory
                readingMode = false; showEditor = true
                }) { Text("+ Nota") }
            }
        }

        var notesCategoryExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            Surface(modifier = Modifier.fillMaxWidth().clickable { notesCategoryExpanded = true },
                shape = RoundedCornerShape(0.dp), color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFB7DCD7))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Kategori: $selectedCategory", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("⌄", fontSize = 20.sp, color = Color(0xFF087F8C))
                }
            }
            DropdownMenu(expanded = notesCategoryExpanded, onDismissRequest = { notesCategoryExpanded = false }) {
                (listOf("Semua") + allCategories).forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = {
                        selectedCategory = category
                        notesCategoryExpanded = false
                    })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("+ Tambah kategori") }, onClick = {
                    notesCategoryExpanded = false
                    showAddCategory = true
                })
                DropdownMenuItem(text = { Text("Urus kategori") }, onClick = {
                    notesCategoryExpanded = false
                    showManageCategories = true
                })
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari nota") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "${visibleNotes.size} nota" + if (showArchived) " dalam arkib" else " tersimpan",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        if (visibleNotes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✎", fontSize = 30.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(if (search.isBlank()) "Ruang nota anda masih kosong" else "Tiada nota ditemui", fontWeight = FontWeight.Bold)
                    Text(
                        if (search.isBlank()) "Mulakan dengan menulis nota pertama anda."
                        else "Cuba kata carian atau kategori yang lain.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val orderedNotes = visibleNotes.sortedWith(
                compareByDescending<DinNote> { it.pinned }.thenByDescending { it.updatedAt }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(orderedNotes.size) { index ->
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, Color(0xFFB7DCD7))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.category, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, maxLines = 1)
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                java.text.SimpleDateFormat("dd MMM", java.util.Locale("ms", "MY"))
                                    .format(java.util.Date(item.updatedAt)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                                Box {
                                    Text("⋮", modifier = Modifier.clickable { menuExpanded = true }, fontSize = 20.sp)
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
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
                                            text = { Text(if (showArchived) "Pulihkan" else "Arkib") },
                                            onClick = { database.setArchived(item.id, !item.archived); menuExpanded = false; refreshNotes() }
                                        )
                                        DropdownMenuItem(text = { Text("Padam") }, onClick = { noteToDelete = item; menuExpanded = false })
                                    }
                                }
                                }
                            }
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(item.title.ifBlank { "Tanpa tajuk" }, fontWeight = FontWeight.Bold, maxLines = 2)
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
            if (readingMode || persistNote()) showEditor = false
        }
        LaunchedEffect(title, body.text, selectedCategory, selectedColor, readingMode) {
            if (!readingMode && (editingId != null || title.isNotBlank() || body.text.isNotBlank())) {
                val saved = notes.firstOrNull { it.id == editingId }
                if (saved != null && saved.title == title.trim() && saved.body == body.text &&
                    saved.colorTag == selectedColor && saved.category == selectedCategory) {
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
                            var readingTextLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                            Text(
                                text = formattedNoteText(body.text),
                                modifier = Modifier.fillMaxWidth().pointerInput(body.text) {
                                    detectTapGestures(onDoubleTap = { position ->
                                        val layout = readingTextLayout
                                        if (layout != null) {
                                            val visibleOffset = layout.getOffsetForPosition(position)
                                            val sourceOffset = sourceOffsetForFormattedText(body.text, visibleOffset)
                                            body = body.copy(selection = TextRange(sourceOffset))
                                        }
                                        focusBodyAfterDoubleTap = true
                                        readingMode = false
                                    })
                                },
                                onTextLayout = { readingTextLayout = it },
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
                                listOf("Terjemah", "Tambah syarah", "Simpan petikan").forEach { action ->
                                    DropdownMenuItem(modifier = Modifier.height(48.dp), leadingIcon = { Text(when (action) { "Terjemah" -> "文"; "Tambah syarah" -> "✎"; else -> "❞" }, color = Color(0xFF087F83), fontSize = 20.sp) }, text = { Text(action, fontSize = 14.sp) }, onClick = {
                                        smartMenu = false
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
    val pattern = Regex("""\{\{size:(\d{1,2})\}\}|\{\{/size\}\}|\{\{color:(#[0-9A-Fa-f]{6})\}\}|\{\{/color\}\}|\*\*|\+\+|_""")
    return pattern.findAll(source).map { m ->
        val token = m.value
        val kind = when {
            token.startsWith("{{color:") || token == "{{/color}}" -> "color"
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
            else -> m.groups[1]?.value?.toIntOrNull()?.let { SpanStyle(fontSize = it.coerceIn(14, 30).sp) }
        }
        NoteFormatToken(m.range.first, m.range.last + 1, style, (token == "{{/size}}" || token == "{{/color}}"), kind)
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
        it.kind == "size" || it.kind == "color"
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
    var selectedCategory by remember { mutableStateOf("Ingatan") }
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
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    val builtinLibraryCategories = listOf("Ingatan", "Kitab-Kitab", "Fiqh Syafi‘i", "Usul Fiqh", "Hadis", "Tafsir", "Akidah", "Tasawuf", "Bahasa Arab", "Kalam Hikmah")
    val customLibraryCategories = remember(libraryRevision) {
        libraryPrefs.getString("custom_categories", "")!!.split("\n").filter { it.isNotBlank() }
    }
    val allLibraryCategories = builtinLibraryCategories + customLibraryCategories
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
        Text("Maktabah", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF075B59))


        var libraryCategoryExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { libraryCategoryExpanded = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFB7DCD7))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("▤", fontSize = 19.sp, color = Color(0xFF087F8C))
                    Spacer(Modifier.width(9.dp))
                    Text("Kategori: $selectedCategory", modifier = Modifier.weight(1f),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF20262B))
                    VerticalDivider(modifier = Modifier.height(23.dp), color = Color(0xFFB7C0C8))
                    Spacer(Modifier.width(10.dp))
                    Text("⌄", fontSize = 20.sp, color = Color(0xFF20262B))
                }
            }
            DropdownMenu(expanded = libraryCategoryExpanded,
                onDismissRequest = { libraryCategoryExpanded = false }) {
                allLibraryCategories.forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = {
                        selectedCategory = category
                        search = ""
                        libraryCategoryExpanded = false
                    })
                }
                HorizontalDivider()
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
                                    UnifiedEditorDivider()
                                }
                                if (showArabicInLibrary) Text(formattedNoteText(entry.arabic), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), fontSize = libraryFontSize.sp)
                                if (showMalayInLibrary && !malayFirstInLibrary) {
                                    if (showArabicInLibrary) UnifiedEditorDivider()
                                    Text(formattedNoteText(entry.meaning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (showMalayInLibrary) Text(formattedNoteText(entry.transliteration), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Tekan untuk maklumat lanjut", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                IslamicCardFooter()
                            }
                        }
                    }
                }
            }
            "Kitab-Kitab" -> {
                Text("📚 Maktabah Kitab", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Button(onClick = { openLibraryEditor("entry_Kitab-Kitab_" + System.currentTimeMillis()) }) { Text("+ Tambah Kitab") }
                libraryItems.filter { matchesMaktabahSearch(libraryPrefs.getString(it + "_title", "") ?: "", search) ||
                    matchesMaktabahSearch(libraryPrefs.getString(it + "_body", "") ?: "", search) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, border = BorderStroke(1.dp, Color(0xFFB7DCD7)), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text(formattedNoteText(libraryPrefs.getString(key + "_body", "") ?: ""), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            IslamicCardFooter()
                        }
                    }
                }
            }
            else -> {
                Text("✨ Kalam Hikmah", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Button(onClick = { openLibraryEditor("entry_" + selectedCategory + "_" + System.currentTimeMillis()) }) { Text("+ Tambah Kandungan") }
                libraryItems.filter { matchesMaktabahSearch(libraryPrefs.getString(it + "_title", "") ?: "", search) ||
                    matchesMaktabahSearch(libraryPrefs.getString(it + "_body", "") ?: "", search) }.forEach { key ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { openLibraryEditor(key) }, border = BorderStroke(1.dp, Color(0xFFB7DCD7)), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(libraryPrefs.getString(key + "_title", "Tanpa tajuk") ?: "Tanpa tajuk", fontWeight = FontWeight.Bold)
                            Text(formattedNoteText(libraryPrefs.getString(key + "_body", "") ?: ""), modifier = Modifier.fillMaxWidth(), style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr), maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            IslamicCardFooter()
                        }
                    }
                }
            }
        }
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
    Box(Modifier.wrapContentWidth()) {
        TextButton(onClick = { hasTransfer = transfer.contains("block"); expanded = true }) {
            Text("▤ Smart Block", color = if (enabled) Color(0xFF087F83) else Color.Gray, fontSize = 13.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
