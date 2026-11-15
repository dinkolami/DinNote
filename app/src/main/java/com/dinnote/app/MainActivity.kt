
package com.dinnote.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDirection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DinNoteApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinNoteApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("dinnote", Context.MODE_PRIVATE)
    }

    var title by remember { mutableStateOf(prefs.getString("title", "") ?: "") }
    var note by remember { mutableStateOf(prefs.getString("note", "") ?: "") }
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DinNote", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "دفتر الملاحظات",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; saved = false },
                    label = { Text("Tajuk nota / عنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = bold,
                        onClick = { bold = !bold },
                        label = { Text("B Bold") }
                    )
                    FilterChip(
                        selected = italic,
                        onClick = { italic = !italic },
                        label = { Text("I Italic") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; saved = false },
                    label = { Text("Tulis nota Arab atau Rumi / اكتب هنا") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                    )
                )

                Button(
                    onClick = {
                        prefs.edit()
                            .putString("title", title)
                            .putString("note", note)
                            .apply()
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Nota")
                }

                if (saved) {
                    Text("Nota berjaya disimpan ✓")
                }

                Text(
                    "DinNote • Nota peribadi anda",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
