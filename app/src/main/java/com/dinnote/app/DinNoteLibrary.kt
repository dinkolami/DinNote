package com.dinnote.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DinLibraryItem(
    val id: String,
    val category: String,
    val title: String,
    val author: String,
    val arabic: String,
    val transliteration: String = "",
    val explanation: String,
    val reference: String,
    val note: String = ""
)

private fun defaultLibraryItems(): List<DinLibraryItem> {
    val kalimah = kalimahEntries.map { entry ->
        DinLibraryItem(
            id = "kalimah_${entry.title}", category = "Kalimah Tayyibah",
            title = entry.title, author = "Kalimah Tayyibah", arabic = entry.arabic,
            transliteration = entry.transliteration, explanation = entry.meaning,
            reference = entry.reference, note = entry.note
        )
    }
    val kitab = listOf(
        DinLibraryItem("kitab_majmu", "Kitab-Kitab", "Al-Majmu‘ Syarh al-Muhadhdhab", "Imam al-Nawawi (mazhab Syafi‘i)", "المجموع شرح المهذب", explanation = "Rujukan fiqh perbandingan yang menghimpunkan perbahasan hukum, dalil dan nukilan ulama. Gunakan carian jilid dan bab untuk menyemak isu tertentu.", reference = "Al-Nawawi, al-Majmu‘ Syarh al-Muhadhdhab."),
        DinLibraryItem("kitab_minhaj", "Kitab-Kitab", "Minhaj al-Talibin", "Imam al-Nawawi", "منهاج الطالبين وعمدة المفتين", explanation = "Matn penting dalam fiqh Syafi‘i. Nota pengajian hendaklah membezakan teks matn, syarah dan tarjih mu‘tamad.", reference = "Al-Nawawi, Minhaj al-Talibin."),
        DinLibraryItem("kitab_tuhfah", "Kitab-Kitab", "Tuhfat al-Muhtaj", "Ibn Hajar al-Haytami", "تحفة المحتاج بشرح المنهاج", explanation = "Antara syarah utama Minhaj al-Talibin yang banyak dirujuk dalam penentuan pendapat mu‘tamad mazhab Syafi‘i.", reference = "Ibn Hajar al-Haytami, Tuhfat al-Muhtaj."),
        DinLibraryItem("kitab_nihayah", "Kitab-Kitab", "Nihayat al-Muhtaj", "Shams al-Din al-Ramli", "نهاية المحتاج إلى شرح المنهاج", explanation = "Syarah Minhaj al-Talibin yang menjadi rujukan penting dalam fiqh Syafi‘i. Bandingkan nukilan dengan Tuhfat al-Muhtaj apabila perlu.", reference = "Al-Ramli, Nihayat al-Muhtaj."),
        DinLibraryItem("kitab_fath", "Kitab-Kitab", "Fath al-Mu‘in", "Zayn al-Din al-Malibari", "فتح المعين بشرح قرة العين", explanation = "Kitab fiqh Syafi‘i yang disusun secara ringkas dan lazim dipelajari bersama syarah I‘anat al-Talibin.", reference = "Zayn al-Din al-Malibari, Fath al-Mu‘in.")
    )
    val hikmah = listOf(
        DinLibraryItem("hikmah_1", "Kalam Hikmah", "Tanda bergantung kepada amal", "Ibn ‘Ata’illah al-Sakandari", "مِنْ عَلَامَةِ الِاعْتِمَادِ عَلَى الْعَمَلِ نُقْصَانُ الرَّجَاءِ عِنْدَ وُجُودِ الزَّلَلِ", explanation = "Antara tanda seseorang terlalu bersandar kepada amalnya ialah harapannya kepada Allah menjadi lemah ketika melakukan kesalahan. Seorang hamba tetap perlu beramal, namun pergantungannya hendaklah kepada rahmat Allah.", reference = "Ibn ‘Ata’illah, al-Hikam al-‘Ata’iyyah, Hikmah pertama."),
        DinLibraryItem("hikmah_2", "Kalam Hikmah", "Jangan berputus asa ketika berdoa", "Ibn ‘Ata’illah al-Sakandari", "لَا يَكُنْ تَأَخُّرُ أَمَدِ الْعَطَاءِ مَعَ الْإِلْحَاحِ فِي الدُّعَاءِ مُوجِبًا لِيَأْسِكَ", explanation = "Janganlah kelewatan datangnya pemberian, sedangkan engkau terus berdoa, menjadi sebab engkau berputus asa. Doa ialah ibadah; penentuan waktu dan bentuk pemberian adalah hikmah Allah.", reference = "Ibn ‘Ata’illah, al-Hikam al-‘Ata’iyyah."),
        DinLibraryItem("hikmah_3", "Kalam Hikmah", "Nilai amalan bergantung kepada keikhlasan", "Peringatan daripada nas al-Quran", "وَمَا أُمِرُوا إِلَّا لِيَعْبُدُوا اللَّهَ مُخْلِصِينَ لَهُ الدِّينَ", explanation = "Amalan ibadah dituntut agar dilakukan dengan ikhlas kerana Allah, bukan semata-mata untuk pujian atau pandangan manusia.", reference = "Al-Quran, Surah al-Bayyinah 98:5."),
        DinLibraryItem("hikmah_4", "Kalam Hikmah", "Bersama kesukaran ada kemudahan", "Nas al-Quran", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", explanation = "Kesusahan bukan keadaan yang kekal. Ayat ini mengajar orang beriman supaya terus berharap, bersabar dan berusaha.", reference = "Al-Quran, Surah al-Insyirah 94:5."),
        DinLibraryItem("hikmah_5", "Kalam Hikmah", "Memelihara kasih sayang sesama saudara", "Kata-kata yang dinukilkan daripada ‘Umar ibn al-Khattab", "إِنَّ مِمَّا يُصَفِّي لَكَ وُدَّ أَخِيكَ أَنْ تَبْدَأَهُ بِالسَّلَامِ إِذَا لَقِيتَهُ، وَأَنْ تَدْعُوَهُ بِأَحَبِّ الْأَسْمَاءِ إِلَيْهِ، وَأَنْ تُوَسِّعَ لَهُ فِي الْمَجْلِسِ", explanation = "Antara adab yang mengeratkan persaudaraan ialah memulakan salam, memanggil saudara dengan nama yang disukainya dan memberi ruang kepadanya dalam majlis.", reference = "Dinukilkan oleh Kalemtayeb pada halaman utama (kalemtayeb.com/item). Nisbah kata-kata ini kepada ‘Umar perlu disemak pada sumber asal.")
    )
    return kalimah + kitab + hikmah
}

private fun loadSavedItem(context: Context, item: DinLibraryItem): DinLibraryItem {
    val prefs = context.getSharedPreferences("dinnote_library_edits", Context.MODE_PRIVATE)
    return item.copy(
        title = prefs.getString("${item.id}.title", item.title) ?: item.title,
        author = prefs.getString("${item.id}.author", item.author) ?: item.author,
        arabic = prefs.getString("${item.id}.arabic", item.arabic) ?: item.arabic,
        transliteration = prefs.getString("${item.id}.transliteration", item.transliteration) ?: item.transliteration,
        explanation = prefs.getString("${item.id}.explanation", item.explanation) ?: item.explanation,
        reference = prefs.getString("${item.id}.reference", item.reference) ?: item.reference,
        note = prefs.getString("${item.id}.note", item.note) ?: item.note
    )
}

@Composable
fun LibraryScreen2(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entries = remember {
        mutableStateListOf<DinLibraryItem>().apply {
            addAll(defaultLibraryItems().map { loadSavedItem(context, it) })
        }
    }
    var category by remember { mutableStateOf("Kalimah Tayyibah") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DinLibraryItem?>(null) }
    var editing by remember { mutableStateOf<DinLibraryItem?>(null) }

    fun updateItem(updated: DinLibraryItem) {
        val index = entries.indexOfFirst { it.id == updated.id }
        if (index >= 0) entries[index] = updated
        context.getSharedPreferences("dinnote_library_edits", Context.MODE_PRIVATE).edit()
            .putString("${updated.id}.title", updated.title)
            .putString("${updated.id}.author", updated.author)
            .putString("${updated.id}.arabic", updated.arabic)
            .putString("${updated.id}.transliteration", updated.transliteration)
            .putString("${updated.id}.explanation", updated.explanation)
            .putString("${updated.id}.reference", updated.reference)
            .putString("${updated.id}.note", updated.note)
            .apply()
        editing = updated
    }

    val filtered = entries.filter { item ->
        item.category == category && (
            item.title.contains(search, true) || item.author.contains(search, true) ||
                item.arabic.contains(search) || item.transliteration.contains(search, true) ||
                item.explanation.contains(search, true) || item.reference.contains(search, true)
            )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perpustakaan DinNote", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Koleksi offline. Kandungan boleh diedit terus dan disimpan secara automatik.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Kalimah Tayyibah", "Kitab-Kitab", "Kalam Hikmah").forEach {
                FilterChip(
                    selected = category == it,
                    onClick = { category = it; search = "" },
                    label = { Text(it, fontSize = 11.sp) }
                )
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari dalam $category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            when (category) {
                "Kalimah Tayyibah" -> "الكلمات الطيبة"
                "Kitab-Kitab" -> "مكتبة الكتب"
                else -> "كلام الحكمة"
            },
            fontSize = 22.sp, color = MaterialTheme.colorScheme.primary
        )
        Text("${filtered.size} kandungan  •  Tekan ikon pensel untuk edit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { entry ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { editing = entry }) { Text("✎ Edit") }
                        }
                        Text(entry.arabic, fontSize = 23.sp)
                        if (entry.transliteration.isNotBlank()) Text(entry.transliteration, fontWeight = FontWeight.Medium)
                        if (entry.author.isNotBlank()) Text(entry.author, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(entry.explanation, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { selected = entry }) { Text("Baca lanjut") }
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp).then(Arrangement.spacedBy(0.dp))) {
                    Text(entry.arabic, fontSize = 23.sp)
                    if (entry.transliteration.isNotBlank()) Text(entry.transliteration)
                    if (entry.author.isNotBlank()) Text(entry.author, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Text(entry.explanation)
                    if (entry.note.isNotBlank()) Text(entry.note)
                    Text("Rujukan: ${entry.reference}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } },
            dismissButton = { TextButton(onClick = { editing = entry; selected = null }) { Text("Edit") } }
        )
    }

    editing?.let { item ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit ${item.category}") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("Perubahan disimpan automatik setiap kali anda menaip.", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    OutlinedTextField(item.title, { updateItem(item.copy(title = it)) }, label = { Text("Tajuk") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(item.arabic, { updateItem(item.copy(arabic = it)) }, label = { Text("Teks Arab") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    if (item.category == "Kalimah Tayyibah") {
                        OutlinedTextField(item.transliteration, { updateItem(item.copy(transliteration = it)) }, label = { Text("Bacaan Rumi") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(item.author, { updateItem(item.copy(author = it)) }, label = { Text("Pengarang / Sumber") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(item.explanation, { updateItem(item.copy(explanation = it)) }, label = { Text("Maksud / Huraian / Terjemahan") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(item.note, { updateItem(item.copy(note = it)) }, label = { Text("Nota tambahan") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(item.reference, { updateItem(item.copy(reference = it)) }, label = { Text("Rujukan") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text("✓ Disimpan automatik pada telefon ini", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("Selesai") } }
        )
    }
}
