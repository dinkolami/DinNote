package com.dinnote.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DinLibraryItem(
    val title: String,
    val author: String,
    val arabic: String,
    val explanation: String,
    val reference: String
)

private val kitabItems = listOf(
    DinLibraryItem(
        "Al-Majmu‘ Syarh al-Muhadhdhab",
        "Imam al-Nawawi (mazhab Syafi‘i)",
        "المجموع شرح المهذب",
        "Rujukan fiqh perbandingan yang menghimpunkan perbahasan hukum, dalil dan nukilan ulama. Gunakan carian jilid dan bab untuk menyemak isu tertentu.",
        "Al-Nawawi, al-Majmu‘ Syarh al-Muhadhdhab."
    ),
    DinLibraryItem(
        "Minhaj al-Talibin",
        "Imam al-Nawawi",
        "منهاج الطالبين وعمدة المفتين",
        "Matn penting dalam fiqh Syafi‘i. Nota pengajian hendaklah membezakan teks matn, syarah dan tarjih mu‘tamad.",
        "Al-Nawawi, Minhaj al-Talibin."
    ),
    DinLibraryItem(
        "Tuhfat al-Muhtaj",
        "Ibn Hajar al-Haytami",
        "تحفة المحتاج بشرح المنهاج",
        "Antara syarah utama Minhaj al-Talibin yang banyak dirujuk dalam penentuan pendapat mu‘tamad mazhab Syafi‘i.",
        "Ibn Hajar al-Haytami, Tuhfat al-Muhtaj."
    ),
    DinLibraryItem(
        "Nihayat al-Muhtaj",
        "Shams al-Din al-Ramli",
        "نهاية المحتاج إلى شرح المنهاج",
        "Syarah Minhaj al-Talibin yang menjadi rujukan penting dalam fiqh Syafi‘i. Bandingkan nukilan dengan Tuhfat al-Muhtaj apabila perlu.",
        "Al-Ramli, Nihayat al-Muhtaj."
    ),
    DinLibraryItem(
        "Fath al-Mu‘in",
        "Zayn al-Din al-Malibari",
        "فتح المعين بشرح قرة العين",
        "Kitab fiqh Syafi‘i yang disusun secara ringkas dan lazim dipelajari bersama syarah I‘anat al-Talibin.",
        "Zayn al-Din al-Malibari, Fath al-Mu‘in."
    )
)

private val hikmahItems = listOf(
    DinLibraryItem(
        "Tanda bergantung kepada amal",
        "Ibn ‘Ata’illah al-Sakandari",
        "مِنْ عَلَامَةِ الِاعْتِمَادِ عَلَى الْعَمَلِ نُقْصَانُ الرَّجَاءِ عِنْدَ وُجُودِ الزَّلَلِ",
        "Antara tanda seseorang terlalu bersandar kepada amalnya ialah harapannya kepada Allah menjadi lemah ketika melakukan kesalahan. Seorang hamba tetap perlu beramal, namun pergantungannya hendaklah kepada rahmat Allah.",
        "Ibn ‘Ata’illah, al-Hikam al-‘Ata’iyyah, Hikmah pertama."
    ),
    DinLibraryItem(
        "Jangan berputus asa ketika berdoa",
        "Ibn ‘Ata’illah al-Sakandari",
        "لَا يَكُنْ تَأَخُّرُ أَمَدِ الْعَطَاءِ مَعَ الْإِلْحَاحِ فِي الدُّعَاءِ مُوجِبًا لِيَأْسِكَ",
        "Janganlah kelewatan datangnya pemberian, sedangkan engkau terus berdoa, menjadi sebab engkau berputus asa. Doa ialah ibadah; penentuan waktu dan bentuk pemberian adalah hikmah Allah.",
        "Ibn ‘Ata’illah, al-Hikam al-‘Ata’iyyah."
    ),
    DinLibraryItem(
        "Nilai amalan bergantung kepada keikhlasan",
        "Peringatan daripada nas al-Quran",
        "وَمَا أُمِرُوا إِلَّا لِيَعْبُدُوا اللَّهَ مُخْلِصِينَ لَهُ الدِّينَ",
        "Amalan ibadah dituntut agar dilakukan dengan ikhlas kerana Allah, bukan semata-mata untuk pujian atau pandangan manusia.",
        "Al-Quran, Surah al-Bayyinah 98:5."
    ),
    DinLibraryItem(
        "Bersama kesukaran ada kemudahan",
        "Nas al-Quran",
        "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
        "Kesusahan bukan keadaan yang kekal. Ayat ini mengajar orang beriman supaya terus berharap, bersabar dan berusaha.",
        "Al-Quran, Surah al-Insyirah 94:5."
    ),
    DinLibraryItem(
        "Memelihara kasih sayang sesama saudara",
        "Kata-kata yang dinukilkan daripada ‘Umar ibn al-Khattab",
        "إِنَّ مِمَّا يُصَفِّي لَكَ وُدَّ أَخِيكَ أَنْ تَبْدَأَهُ بِالسَّلَامِ إِذَا لَقِيتَهُ، وَأَنْ تَدْعُوَهُ بِأَحَبِّ الْأَسْمَاءِ إِلَيْهِ، وَأَنْ تُوَسِّعَ لَهُ فِي الْمَجْلِسِ",
        "Antara adab yang mengeratkan persaudaraan ialah memulakan salam, memanggil saudara dengan nama yang disukainya dan memberi ruang kepadanya dalam majlis.",
        "Dinukilkan oleh Kalemtayeb pada halaman utama (kalemtayeb.com/item). Nisbah kata-kata ini kepada ‘Umar perlu disemak pada sumber asal."
    )
)

@Composable
fun LibraryScreen2(modifier: Modifier = Modifier) {
    var category by remember { mutableStateOf("Kalimah Tayyibah") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DinLibraryItem?>(null) }

    val kalimahMatches = kalimahEntries.filter {
        it.title.contains(search, true) || it.transliteration.contains(search, true) ||
            it.meaning.contains(search, true) || it.arabic.contains(search)
    }
    val libraryMatches = when (category) {
        "Kitab-Kitab" -> kitabItems.filter {
            it.title.contains(search, true) || it.author.contains(search, true) ||
                it.explanation.contains(search, true) || it.arabic.contains(search)
        }
        "Kalam Hikmah" -> hikmahItems.filter {
            it.title.contains(search, true) || it.author.contains(search, true) ||
                it.explanation.contains(search, true) || it.arabic.contains(search)
        }
        else -> emptyList()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perpustakaan DinNote", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Koleksi terbina dalam aplikasi dan boleh dibaca tanpa internet.")
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
        if (category == "Kalimah Tayyibah") {
            Text("الكلمات الطيبة", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            Text("${kalimahMatches.size} kandungan", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(kalimahMatches, key = { it.title }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = DinLibraryItem(entry.title, "Kalimah Tayyibah", entry.arabic,
                                "${entry.transliteration}\n${entry.meaning}\n${entry.note}", entry.reference)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(entry.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(entry.arabic, fontSize = 24.sp)
                            Text(entry.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            Text(if (category == "Kitab-Kitab") "مكتبة الكتب" else "كلام الحكمة", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            Text("${libraryMatches.size} kandungan offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(libraryMatches, key = { it.title }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selected = entry },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(entry.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(entry.arabic, fontSize = 22.sp)
                            Text(entry.author, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(entry.explanation, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Baca lanjut  →", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Text(
            "Sumber rujukan dicatat pada setiap entri. Kandungan tambahan boleh dimasukkan dalam kemas kini aplikasi.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(entry.arabic, fontSize = 23.sp)
                    Text(entry.author, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Text(entry.explanation)
                    Text("Rujukan: ${entry.reference}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Tutup") }
            }
        )
    }
}
