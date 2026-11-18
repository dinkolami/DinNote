package com.dinnote.app

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

data class SmartReferenceEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val aliases: List<String>,
    val description: String,
    val source: String = "",
    val details: Map<String, String> = emptyMap()
)

private val referenceFields = listOf("birthDate" to "Tarikh lahir", "birthPlace" to "Tempat lahir", "deathDate" to "Tarikh wafat", "deathPlace" to "Tempat wafat", "works" to "Karya utama", "teachers" to "Guru", "students" to "Murid", "author" to "Pengarang", "subject" to "Bidang", "location" to "Lokasi")
private fun fieldsFor(category: String) = when(category.lowercase(Locale.ROOT)) {
    "ulama", "tokoh islam" -> referenceFields.take(7)
    "kitab" -> referenceFields.filter { it.first in listOf("author","subject") }
    "tempat" -> referenceFields.filter { it.first == "location" }
    else -> emptyList()
}
private val defaultPopupFields = setOf("birthDate","birthPlace","deathDate","deathPlace","works","author","subject","location","description","source")

data class SmartReferenceMatch(val start: Int, val end: Int, val entry: SmartReferenceEntry)

/** Local reference catalogue; does not modify original note content. */
class SmartReferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("smart_reference", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    var popupMode: String
        get() = prefs.getString("popup_mode", "ringkas") ?: "ringkas"
        set(value) { prefs.edit().putString("popup_mode",value).apply() }
    var popupFields: Set<String>
        get() = prefs.getStringSet("popup_fields",defaultPopupFields)?.toSet() ?: defaultPopupFields
        set(value) { prefs.edit().putStringSet("popup_fields",value.toSet()).apply() }

    fun entries(): List<SmartReferenceEntry> {
        val raw = prefs.getString("entries", null) ?: return initialEntries()
        val saved = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val aliases = o.optJSONArray("aliases") ?: JSONArray()
                SmartReferenceEntry(o.getString("id"), o.getString("title"),
                    o.getString("category"), (0 until aliases.length()).map { aliases.getString(it) },
                    o.getString("description"), o.optString("source"),
                    o.optJSONObject("details")?.let { d -> d.keys().asSequence().associateWith { d.optString(it) } } ?: emptyMap())
            }
        }.getOrElse { return emptyList() }
        if (prefs.getInt("catalogue_version", 1) < 4) {
            val previousVersion = prefs.getInt("catalogue_version", 1)
            val oldIds = setOf("imam-shafii", "al-umm")
            val merged = saved + initialEntries().filter { item ->
                (item.id.startsWith("term-") || (previousVersion < 3 && item.id.startsWith("book-")) || (previousVersion < 2 && item.id !in oldIds)) && saved.none { it.id == item.id }
            }
            save(merged)
            return merged
        }
        return saved
    }

    fun save(entries: List<SmartReferenceEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(JSONObject().put("id", e.id).put("title", e.title)
                .put("category", e.category).put("aliases", JSONArray(e.aliases))
                .put("description", e.description).put("source", e.source).put("details", JSONObject(e.details)))
        }
        prefs.edit().putString("entries", array.toString()).putInt("catalogue_version", 4).apply()
    }

    fun upsert(entry: SmartReferenceEntry) {
        require(entry.title.isNotBlank() && entry.description.isNotBlank())
        save(entries().filterNot { it.id == entry.id } + entry)
    }

    fun delete(id: String) = save(entries().filterNot { it.id == id })

    private fun initialEntries() = listOf(
        SmartReferenceEntry("imam-shafii", "Imam al-Syafi‘i", "Ulama",
            listOf("Imam Syafie", "Imam al-Syafii", "الإمام الشافعي", "محمد بن إدريس الشافعي", "الشافعي"),
            "Muhammad bin Idris al-Syafi‘i (150–204H), pengasas mazhab Syafi‘i. Antara karya beliau ialah al-Umm dan al-Risalah.",
            "Siyar A‘lam al-Nubala’, biografi al-Syafi‘i", mapOf("birthDate" to "150H / 767M", "birthPlace" to "Gaza", "deathDate" to "204H / 820M", "deathPlace" to "Mesir", "works" to "الأم، الرسالة", "teachers" to "Imam Malik bin Anas", "students" to "Ahmad bin Hanbal")),
        SmartReferenceEntry("al-umm", "al-Umm", "Kitab", listOf("الأم", "كتاب الأم"),
            "Kitab fiqh karya Imam al-Syafi‘i.", details = mapOf("author" to "Imam al-Syafi‘i", "subject" to "Fiqh")),
        SmartReferenceEntry("abu-hanifa", "Imam Abu Hanifah", "Ulama", listOf("أبو حنيفة"), "Imam mazhab Hanafi.", details = mapOf("birthDate" to "80H", "birthPlace" to "Kufah", "deathDate" to "150H", "deathPlace" to "Baghdad")),
        SmartReferenceEntry("malik", "Imam Malik", "Ulama", listOf("مالك بن أنس", "الإمام مالك"), "Imam mazhab Maliki dan ahli hadis Madinah.", details = mapOf("birthDate" to "93H", "birthPlace" to "Madinah", "deathDate" to "179H", "deathPlace" to "Madinah", "works" to "الموطأ")),
        SmartReferenceEntry("ahmad", "Imam Ahmad bin Hanbal", "Ulama", listOf("أحمد بن حنبل", "الإمام أحمد"), "Imam mazhab Hanbali dan ahli hadis.", details = mapOf("birthDate" to "164H", "birthPlace" to "Baghdad", "deathDate" to "241H", "deathPlace" to "Baghdad", "works" to "المسند")),
        SmartReferenceEntry("nawawi", "Imam al-Nawawi", "Ulama", listOf("Imam Nawawi", "النووي", "الإمام النووي"), "Ulama besar mazhab Syafi‘i dan pensyarah hadis.", details = mapOf("birthDate" to "631H", "birthPlace" to "Nawa", "deathDate" to "676H", "deathPlace" to "Nawa", "works" to "المجموع، منهاج الطالبين، رياض الصالحين")),
        SmartReferenceEntry("rafii", "Imam al-Rafi‘i", "Ulama", listOf("Imam Rafii", "الرافعي", "عبد الكريم الرافعي"), "Antara tokoh utama pentarjihan mazhab Syafi‘i.", details = mapOf("birthDate" to "557H", "birthPlace" to "Qazvin", "deathDate" to "623H", "deathPlace" to "Qazvin", "works" to "العزيز شرح الوجيز، المحرر")),
        SmartReferenceEntry("ghazali", "Imam al-Ghazali", "Ulama", listOf("Imam Ghazali", "الغزالي", "أبو حامد الغزالي"), "Ulama fiqh Syafi‘i, usul fiqh dan tasawwuf.", details = mapOf("birthDate" to "450H", "birthPlace" to "Tus", "deathDate" to "505H", "deathPlace" to "Tus", "works" to "إحياء علوم الدين، المستصفى، الوسيط")),
        SmartReferenceEntry("juwayni", "Imam al-Juwayni", "Ulama", listOf("إمام الحرمين", "الجويني", "Imam al-Haramayn"), "Ulama usul fiqh dan guru Imam al-Ghazali.", details = mapOf("birthDate" to "419H", "deathDate" to "478H", "deathPlace" to "Nishapur", "works" to "نهاية المطلب، البرهان")),
        SmartReferenceEntry("muzani", "Imam al-Muzani", "Ulama", listOf("المزني", "إسماعيل المزني"), "Murid Imam al-Syafi‘i dan penyebar mazhabnya.", details = mapOf("birthDate" to "175H", "birthPlace" to "Mesir", "deathDate" to "264H", "deathPlace" to "Mesir", "works" to "مختصر المزني")),
        SmartReferenceEntry("mawardi", "Imam al-Mawardi", "Ulama", listOf("الماوردي", "Imam Mawardi"), "Faqih Syafi‘i dan penulis mengenai pemerintahan.", details = mapOf("birthDate" to "364H", "birthPlace" to "Basrah", "deathDate" to "450H", "deathPlace" to "Baghdad", "works" to "الحاوي الكبير، الأحكام السلطانية")),
        SmartReferenceEntry("shirazi", "Abu Ishaq al-Shirazi", "Ulama", listOf("أبو إسحاق الشيرازي", "الشيرازي"), "Faqih Syafi‘i dan pengajar di Madrasah Nizamiyyah.", details = mapOf("birthDate" to "393H", "birthPlace" to "Firuzabad", "deathDate" to "476H", "deathPlace" to "Baghdad", "works" to "المهذب، اللمع")),
        SmartReferenceEntry("ibn-hajar-haytami", "Ibn Hajar al-Haytami", "Ulama", listOf("ابن حجر الهيتمي", "ابن حجر المكي"), "Tokoh fiqh Syafi‘i mutaakhkhirin.", details = mapOf("birthDate" to "909H", "deathDate" to "974H", "deathPlace" to "Makkah", "works" to "تحفة المحتاج")),
        SmartReferenceEntry("ramli", "Syamsuddin al-Ramli", "Ulama", listOf("شمس الدين الرملي", "الإمام الرملي"), "Tokoh rujukan fiqh Syafi‘i mutaakhkhirin.", details = mapOf("birthDate" to "919H", "birthPlace" to "Mesir", "deathDate" to "1004H", "deathPlace" to "Mesir", "works" to "نهاية المحتاج")),
        SmartReferenceEntry("khatib-shirbini", "al-Khatib al-Syirbini", "Ulama", listOf("الخطيب الشربيني", "الشربيني"), "Ulama fiqh dan tafsir mazhab Syafi‘i.", details = mapOf("deathDate" to "977H", "works" to "مغني المحتاج")),
        SmartReferenceEntry("zakariya-ansari", "Syeikh Zakariyya al-Ansari", "Ulama", listOf("زكريا الأنصاري", "شيخ الإسلام زكريا الأنصاري"), "Ulama Syafi‘i dalam fiqh, hadis dan tasawwuf.", details = mapOf("birthDate" to "823H", "deathDate" to "926H", "deathPlace" to "Mesir", "works" to "أسنى المطالب، فتح الوهاب")),
        SmartReferenceEntry("bukhari", "Imam al-Bukhari", "Ulama", listOf("Imam Bukhari", "البخاري", "الإمام البخاري"), "Muhaddith dan penyusun Sahih al-Bukhari.", details = mapOf("birthDate" to "194H", "birthPlace" to "Bukhara", "deathDate" to "256H", "deathPlace" to "Khartank", "works" to "صحيح البخاري، الأدب المفرد")),
        SmartReferenceEntry("muslim", "Imam Muslim", "Ulama", listOf("مسلم بن الحجاج", "الإمام مسلم"), "Muhaddith dan penyusun Sahih Muslim; tahun lahir diperselisihkan.", details = mapOf("birthDate" to "204H", "birthPlace" to "Nishapur", "deathDate" to "261H", "deathPlace" to "Nishapur", "works" to "صحيح مسلم")),
        SmartReferenceEntry("abu-dawud", "Imam Abu Dawud", "Ulama", listOf("أبو داود السجستاني", "Imam Abu Daud"), "Penyusun salah satu kitab Sunan utama.", details = mapOf("birthDate" to "202H", "birthPlace" to "Sijistan", "deathDate" to "275H", "deathPlace" to "Basrah", "works" to "سنن أبي داود")),
        SmartReferenceEntry("tirmidhi", "Imam al-Tirmidhi", "Ulama", listOf("الترمذي", "Imam Tirmizi"), "Ahli hadis dan penyusun al-Jami‘.", details = mapOf("birthDate" to "209H", "deathDate" to "279H", "works" to "جامع الترمذي، الشمائل المحمدية")),
        SmartReferenceEntry("nasai", "Imam al-Nasa’i", "Ulama", listOf("النسائي", "Imam Nasai"), "Ahli hadis dan penyusun al-Sunan.", details = mapOf("birthDate" to "215H", "birthPlace" to "Nasa", "deathDate" to "303H", "works" to "سنن النسائي")),
        SmartReferenceEntry("ibn-majah", "Imam Ibn Majah", "Ulama", listOf("ابن ماجه", "Imam Ibnu Majah"), "Ahli hadis dan penyusun Sunan Ibn Majah.", details = mapOf("birthDate" to "209H", "birthPlace" to "Qazvin", "deathDate" to "273H", "deathPlace" to "Qazvin", "works" to "سنن ابن ماجه")),
        SmartReferenceEntry("ibn-hajar-asqalani", "Ibn Hajar al-Asqalani", "Ulama", listOf("ابن حجر العسقلاني", "الحافظ ابن حجر"), "Hafiz hadis dan faqih Syafi‘i.", details = mapOf("birthDate" to "773H", "birthPlace" to "Mesir", "deathDate" to "852H", "deathPlace" to "Mesir", "works" to "فتح الباري، تهذيب التهذيب، بلوغ المرام")),
        SmartReferenceEntry("suyuti", "Imam al-Suyuti", "Ulama", listOf("السيوطي", "جلال الدين السيوطي", "Imam Suyuti"), "Ulama ensiklopedik dalam hadis, tafsir dan fiqh.", details = mapOf("birthDate" to "849H", "birthPlace" to "Kaherah", "deathDate" to "911H", "deathPlace" to "Kaherah", "works" to "الإتقان، الجامع الصغير، الأشباه والنظائر")),
        SmartReferenceEntry("dhahabi", "Imam al-Dhahabi", "Ulama", listOf("الذهبي", "شمس الدين الذهبي"), "Sejarawan dan ahli kritik hadis.", details = mapOf("birthDate" to "673H", "birthPlace" to "Damascus", "deathDate" to "748H", "deathPlace" to "Damascus", "works" to "سير أعلام النبلاء، تاريخ الإسلام")),
        SmartReferenceEntry("qushayri", "Imam al-Qusyairi", "Ulama", listOf("القشيري", "عبد الكريم القشيري"), "Ulama tasawwuf, tafsir dan mazhab Syafi‘i.", details = mapOf("birthDate" to "376H", "deathDate" to "465H", "deathPlace" to "Nishapur", "works" to "الرسالة القشيرية، لطائف الإشارات")),
        SmartReferenceEntry("junayd", "Imam al-Junayd", "Ulama", listOf("الجنيد البغدادي", "الجنيد"), "Tokoh tasawwuf awal yang masyhur dengan penekanan terhadap syariat.", details = mapOf("deathDate" to "297H", "deathPlace" to "Baghdad")),
        SmartReferenceEntry("ibn-ata", "Ibn ‘Ata’illah al-Sakandari", "Ulama", listOf("ابن عطاء الله السكندري", "ابن عطاء الله", "Ibnu Athaillah"), "Ulama tasawwuf tariqat Syaziliyyah dan faqih Maliki.", details = mapOf("birthPlace" to "Iskandariah", "deathDate" to "709H", "deathPlace" to "Kaherah", "works" to "الحكم العطائية، لطائف المنن")),
        SmartReferenceEntry("abu-madyan", "Abu Madyan", "Ulama", listOf("أبو مدين الغوث", "أبو مدين"), "Tokoh tasawwuf Maghribi yang berpengaruh.", details = mapOf("deathDate" to "594H")),
        SmartReferenceEntry("hasan-basri", "Hasan al-Basri", "Ulama", listOf("الحسن البصري", "Hasan Basri"), "Tabi‘i yang terkenal dengan ilmu, zuhud dan nasihat.", details = mapOf("birthDate" to "21H", "birthPlace" to "Madinah", "deathDate" to "110H", "deathPlace" to "Basrah")),
        SmartReferenceEntry("yahya-muadh", "Yahya bin Mu‘adh", "Ulama", listOf("يحيى بن معاذ الرازي", "يحيى بن معاذ"), "Tokoh zuhud dan nasihat kerohanian.", details = mapOf("deathDate" to "258H", "deathPlace" to "Nishapur")),
        SmartReferenceEntry("rabi-adawiyya", "Rabi‘ah al-‘Adawiyyah", "Ulama", listOf("رابعة العدوية", "Rabiah al-Adawiyah"), "Tokoh zuhud awal; tarikh kelahiran dan wafat diperselisihkan.", details = mapOf("birthPlace" to "Basrah")),
        SmartReferenceEntry("book-risalah", "al-Risalah", "Kitab", listOf("الرسالة للشافعي", "رسالة الشافعي"), "Karya awal yang menghuraikan kaedah memahami dalil syarak.", details = mapOf("author" to "Imam al-Syafi‘i", "subject" to "Usul fiqh")),
        SmartReferenceEntry("book-majmu", "al-Majmu‘ Syarh al-Muhadhdhab", "Kitab", listOf("المجموع شرح المهذب", "مجموع النووي", "al-Majmu‘", "al-Majmu"), "Syarah kitab al-Muhadhdhab yang membincangkan dalil dan khilaf fiqh; karya asal tidak selesai semasa hayat pengarang.", details = mapOf("author" to "Imam al-Nawawi", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-minhaj", "Minhaj al-Talibin", "Kitab", listOf("منهاج الطالبين", "Minhaj al-Talibin"), "Matan rujukan fiqh Syafi‘i yang diringkaskan daripada al-Muharrar.", details = mapOf("author" to "Imam al-Nawawi", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-rawdah", "Rawdat al-Talibin", "Kitab", listOf("روضة الطالبين", "Rawdah al-Talibin"), "Ringkasan dan olahan karya al-Rafi‘i mengenai perbahasan mazhab.", details = mapOf("author" to "Imam al-Nawawi", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-tuhfah", "Tuhfat al-Muhtaj", "Kitab", listOf("تحفة المحتاج", "Tuhfah al-Muhtaj"), "Syarah Minhaj al-Talibin dan rujukan penting ulama Syafi‘i mutaakhkhirin.", details = mapOf("author" to "Ibn Hajar al-Haytami", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-nihayah", "Nihayat al-Muhtaj", "Kitab", listOf("نهاية المحتاج", "Nihayah al-Muhtaj"), "Syarah Minhaj al-Talibin yang menjadi rujukan utama mazhab.", details = mapOf("author" to "Syamsuddin al-Ramli", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-mughni", "Mughni al-Muhtaj", "Kitab", listOf("مغني المحتاج"), "Syarah Minhaj al-Talibin.", details = mapOf("author" to "al-Khatib al-Syirbini", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-muhadhdhab", "al-Muhadhdhab", "Kitab", listOf("المهذب في فقه الإمام الشافعي", "المهذب", "al-Muhazzab"), "Kitab fiqh yang menjadi asas kepada syarah al-Majmu‘.", details = mapOf("author" to "Abu Ishaq al-Shirazi", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-aziz", "al-‘Aziz Syarh al-Wajiz", "Kitab", listOf("العزيز شرح الوجيز", "فتح العزيز", "الشرح الكبير للرافعي"), "Syarah al-Wajiz karya al-Ghazali.", details = mapOf("author" to "Imam al-Rafi‘i", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-muharrar", "al-Muharrar", "Kitab", listOf("المحرر للرافعي"), "Matan fiqh yang menjadi asas Minhaj al-Talibin.", details = mapOf("author" to "Imam al-Rafi‘i", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-hawi", "al-Hawi al-Kabir", "Kitab", listOf("الحاوي الكبير"), "Ensiklopedia fiqh yang menghuraikan Mukhtasar al-Muzani.", details = mapOf("author" to "Imam al-Mawardi", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-muzani-book", "Mukhtasar al-Muzani", "Kitab", listOf("مختصر المزني"), "Ringkasan fiqh yang menyampaikan pandangan Imam al-Syafi‘i.", details = mapOf("author" to "Imam al-Muzani", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-asna", "Asna al-Matalib", "Kitab", listOf("أسنى المطالب"), "Syarah Rawd al-Talib.", details = mapOf("author" to "Zakariyya al-Ansari", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-fath-wahhab", "Fath al-Wahhab", "Kitab", listOf("فتح الوهاب بشرح منهج الطلاب", "فتح الوهاب"), "Syarah Manhaj al-Tullab.", details = mapOf("author" to "Zakariyya al-Ansari", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-i-anah", "I‘anat al-Talibin", "Kitab", listOf("إعانة الطالبين", "Ianah al-Talibin"), "Hasyiah terhadap Fath al-Mu‘in.", details = mapOf("author" to "Abu Bakr Syata al-Dimyati", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-fath-muin", "Fath al-Mu‘in", "Kitab", listOf("فتح المعين", "Fathul Muin"), "Syarah Qurrat al-‘Ayn yang masyhur di Alam Melayu.", details = mapOf("author" to "Zaynuddin al-Malibari", "subject" to "Fiqh Syafi‘i")),
        SmartReferenceEntry("book-bukhari-book", "Sahih al-Bukhari", "Kitab", listOf("صحيح البخاري", "Sahih Bukhari"), "Himpunan hadis sahih dengan susunan bab fiqh.", details = mapOf("author" to "Imam al-Bukhari", "subject" to "Hadis")),
        SmartReferenceEntry("book-muslim-book", "Sahih Muslim", "Kitab", listOf("صحيح مسلم"), "Himpunan hadis sahih dengan pengelompokan jalur riwayat.", details = mapOf("author" to "Imam Muslim", "subject" to "Hadis")),
        SmartReferenceEntry("book-abudawud-book", "Sunan Abi Dawud", "Kitab", listOf("سنن أبي داود", "Sunan Abu Daud"), "Himpunan hadis yang memberi tumpuan kepada hukum.", details = mapOf("author" to "Imam Abu Dawud", "subject" to "Hadis")),
        SmartReferenceEntry("book-tirmidhi-book", "Jami‘ al-Tirmidhi", "Kitab", listOf("جامع الترمذي", "سنن الترمذي"), "Himpunan hadis bersama penilaian dan perbahasan fiqh.", details = mapOf("author" to "Imam al-Tirmidhi", "subject" to "Hadis")),
        SmartReferenceEntry("book-nasai-book", "Sunan al-Nasa’i", "Kitab", listOf("سنن النسائي"), "Himpunan hadis dalam susunan bab.", details = mapOf("author" to "Imam al-Nasa’i", "subject" to "Hadis")),
        SmartReferenceEntry("book-majah-book", "Sunan Ibn Majah", "Kitab", listOf("سنن ابن ماجه"), "Salah satu daripada enam kitab hadis utama.", details = mapOf("author" to "Imam Ibn Majah", "subject" to "Hadis")),
        SmartReferenceEntry("book-fath-bari", "Fath al-Bari", "Kitab", listOf("فتح الباري", "Fathul Bari"), "Syarah Sahih al-Bukhari.", details = mapOf("author" to "Ibn Hajar al-Asqalani", "subject" to "Syarah hadis")),
        SmartReferenceEntry("book-riyad", "Riyad al-Salihin", "Kitab", listOf("رياض الصالحين", "Riyadhus Salihin"), "Himpunan ayat dan hadis berkaitan adab, ibadah dan akhlak.", details = mapOf("author" to "Imam al-Nawawi", "subject" to "Hadis dan akhlak")),
        SmartReferenceEntry("book-arba-in", "al-Arba‘in al-Nawawiyyah", "Kitab", listOf("الأربعون النووية", "الأربعين النووية", "Hadis 40 Nawawi"), "Koleksi 42 hadis asas agama.", details = mapOf("author" to "Imam al-Nawawi", "subject" to "Hadis")),
        SmartReferenceEntry("book-ihya", "Ihya’ ‘Ulum al-Din", "Kitab", listOf("إحياء علوم الدين", "Ihya Ulumuddin"), "Karya mengenai ibadah, adat, penyakit hati dan sifat terpuji.", details = mapOf("author" to "Imam al-Ghazali", "subject" to "Tasawwuf dan akhlak")),
        SmartReferenceEntry("book-hikam", "al-Hikam al-‘Ata’iyyah", "Kitab", listOf("الحكم العطائية", "Hikam Ibn Ataillah"), "Himpunan kalam hikmah mengenai perjalanan rohani dan kehambaan.", details = mapOf("author" to "Ibn ‘Ata’illah al-Sakandari", "subject" to "Tasawwuf")),
        SmartReferenceEntry("book-qushayri-book", "al-Risalah al-Qusyairiyyah", "Kitab", listOf("الرسالة القشيرية"), "Pengenalan tokoh, istilah dan adab ahli tasawwuf.", details = mapOf("author" to "Imam al-Qusyairi", "subject" to "Tasawwuf")),
        SmartReferenceEntry("book-hilyah", "Hilyat al-Awliya’", "Kitab", listOf("حلية الأولياء", "Hilyatul Awliya"), "Koleksi riwayat dan biografi tokoh zuhud.", details = mapOf("author" to "Abu Nu‘aym al-Isfahani", "subject" to "Biografi dan zuhud")),
        SmartReferenceEntry("book-siyar", "Siyar A‘lam al-Nubala’", "Kitab", listOf("سير أعلام النبلاء"), "Koleksi biografi tokoh Islam daripada pelbagai generasi.", details = mapOf("author" to "Imam al-Dhahabi", "subject" to "Biografi")),
        SmartReferenceEntry("book-mafatih", "Mafatih al-Ghayb", "Kitab", listOf("مفاتيح الغيب", "التفسير الكبير للرازي"), "Tafsir yang membincangkan bahasa, akidah dan persoalan rasional.", details = mapOf("author" to "Fakhruddin al-Razi", "subject" to "Tafsir")),
        SmartReferenceEntry("book-jalalayn", "Tafsir al-Jalalayn", "Kitab", listOf("تفسير الجلالين", "Tafsir Jalalain"), "Tafsir ringkas al-Quran yang disusun oleh dua orang ulama.", details = mapOf("author" to "Jalaluddin al-Mahalli dan Jalaluddin al-Suyuti", "subject" to "Tafsir")),
        SmartReferenceEntry("book-itqan", "al-Itqan fi ‘Ulum al-Quran", "Kitab", listOf("الإتقان في علوم القرآن"), "Karya ensiklopedik mengenai disiplin ilmu al-Quran.", details = mapOf("author" to "Imam al-Suyuti", "subject" to "Ulum al-Quran")),
        SmartReferenceEntry("book-mustasfa", "al-Mustasfa", "Kitab", listOf("المستصفى من علم الأصول", "المستصفى"), "Karya mengenai dalil, hukum dan kaedah istinbat.", details = mapOf("author" to "Imam al-Ghazali", "subject" to "Usul fiqh")),
        SmartReferenceEntry("term-fard", "Fardu", "Istilah", listOf("الفرض", "Fardhu"), "Tuntutan syarak yang mesti dilaksanakan; meninggalkannya tanpa uzur berdosa."),
        SmartReferenceEntry("term-wajib", "Wajib", "Istilah", listOf("الواجب"), "Tuntutan yang mesti dilakukan. Dalam mazhab Syafi‘i, pada umumnya sinonim dengan fardu."),
        SmartReferenceEntry("term-mandub", "Mandub", "Istilah", listOf("المندوب"), "Perbuatan yang dituntut tanpa tuntutan mengikat."),
        SmartReferenceEntry("term-makruh", "Makruh", "Istilah", listOf("المكروه"), "Perbuatan yang dituntut supaya ditinggalkan tanpa larangan mengikat."),
        SmartReferenceEntry("term-mubah", "Mubah", "Istilah", listOf("المباح"), "Perbuatan yang pada asalnya diberikan pilihan untuk dilakukan atau ditinggalkan."),
        SmartReferenceEntry("term-haram", "Haram", "Istilah", listOf("الحرام"), "Perbuatan yang dilarang oleh syarak secara mengikat."),
        SmartReferenceEntry("term-ijma", "Ijmak", "Istilah", listOf("الإجماع", "Ijma‘"), "Kesepakatan para mujtahid umat Islam selepas kewafatan Nabi SAW mengenai hukum syarak pada suatu zaman."),
        SmartReferenceEntry("term-qiyas", "Qiyas", "Istilah", listOf("القياس"), "Menghubungkan hukum suatu cabang kepada asal kerana berkongsi illah hukum."),
        SmartReferenceEntry("term-illah", "‘Illah hukum", "Istilah", listOf("العلة الشرعية", "علة الحكم"), "Sifat yang menjadi asas pengaitan sesuatu hukum dalam perbahasan usul fiqh."),
        SmartReferenceEntry("term-istishab", "Istishab", "Istilah", listOf("الاستصحاب"), "Mengekalkan hukum atau keadaan terdahulu sehingga terdapat dalil yang mengubahnya."),
        SmartReferenceEntry("term-ijtihad", "Ijtihad", "Istilah", listOf("الاجتهاد"), "Pengerahan kemampuan ilmiah oleh ahli yang layak untuk memperoleh hukum syarak."),
        SmartReferenceEntry("term-taqlid", "Taklid", "Istilah", listOf("التقليد"), "Mengikuti pandangan seseorang tanpa mengetahui dalil terperincinya."),
        SmartReferenceEntry("term-sahih", "Hadis sahih", "Istilah", listOf("الحديث الصحيح"), "Hadis bersanad bersambung, dengan perawi adil dan dabit, tanpa syaz dan illah yang mencacatkan."),
        SmartReferenceEntry("term-hasan", "Hadis hasan", "Istilah", listOf("الحديث الحسن"), "Hadis yang memenuhi syarat penerimaan dengan tahap ketepatan perawi lebih rendah daripada sahih, dalam definisi hasan li-dhatihi."),
        SmartReferenceEntry("term-daif", "Hadis daif", "Istilah", listOf("الحديث الضعيف", "Hadis dhaif"), "Hadis yang tidak memenuhi syarat hadis sahih atau hasan."),
        SmartReferenceEntry("term-mursal", "Hadis mursal", "Istilah", listOf("الحديث المرسل"), "Dalam penggunaan lazim ahli hadis, riwayat seorang tabi‘i yang disandarkan terus kepada Nabi SAW."),
        SmartReferenceEntry("term-mutawatir", "Hadis mutawatir", "Istilah", listOf("الحديث المتواتر"), "Riwayat melalui bilangan perawi yang menurut kebiasaan mustahil bersepakat untuk berdusta, dengan syarat lain yang ditetapkan ulama."),
        SmartReferenceEntry("term-marfu", "Hadis marfu‘", "Istilah", listOf("الحديث المرفوع"), "Riwayat yang disandarkan kepada Nabi SAW, sama ada perkataan, perbuatan, pengakuan atau sifat."),
        SmartReferenceEntry("term-mawquf", "Hadis mawquf", "Istilah", listOf("الحديث الموقوف"), "Riwayat yang disandarkan kepada sahabat."),
        SmartReferenceEntry("term-ikhlas", "Ikhlas", "Istilah", listOf("الإخلاص"), "Memurnikan niat dan tujuan amal kerana Allah."),
        SmartReferenceEntry("term-tawakkul", "Tawakal", "Istilah", listOf("التوكل", "Tawakkal"), "Pergantungan hati kepada Allah dengan mengambil sebab yang dibenarkan."),
        SmartReferenceEntry("term-zuhd", "Zuhud", "Istilah", listOf("الزهد"), "Tidak menjadikan kepentingan dunia sebagai tujuan utama hati."),
        SmartReferenceEntry("term-muraqabah", "Muraqabah", "Istilah", listOf("المراقبة"), "Kesedaran hati bahawa Allah sentiasa mengetahui keadaan dan amalan hamba."),
        SmartReferenceEntry("term-muhasabah", "Muhasabah", "Istilah", listOf("المحاسبة"), "Menilai dan memperbaiki diri serta amalan."),
        SmartReferenceEntry("term-riya", "Riyak", "Istilah", listOf("الرياء", "Riya‘"), "Melakukan atau memperlihatkan ibadah dengan tujuan memperoleh pandangan atau pujian manusia."),
        SmartReferenceEntry("term-ujub", "Ujub", "Istilah", listOf("العجب"), "Kagum terhadap diri atau amal sendiri sehingga mengabaikan kurniaan Allah."),
        SmartReferenceEntry("term-mubtada", "Mubtada’", "Istilah", listOf("المبتدأ"), "Isim yang biasanya berada pada permulaan jumlah ismiyyah dan dihukum marfu‘."),
        SmartReferenceEntry("term-khabar", "Khabar nahu", "Istilah", listOf("الخبر النحوي", "خبر المبتدأ"), "Unsur yang melengkapkan makna mubtada’."),
        SmartReferenceEntry("term-idgham", "Idgham", "Istilah", listOf("الإدغام"), "Memasukkan satu huruf ke dalam huruf berikutnya menurut kaedah bacaan."),
        SmartReferenceEntry("term-ikhfa", "Ikhfa’", "Istilah", listOf("الإخفاء"), "Menyembunyikan sebutan huruf pada kadar antara izhar dan idgham menurut kaedah tajwid."),
        SmartReferenceEntry("term-izhar", "Izhar", "Istilah", listOf("الإظهار"), "Menyebut huruf dengan jelas menurut kaedah tajwid."),
        SmartReferenceEntry("term-iqlab", "Iqlab", "Istilah", listOf("الإقلاب"), "Menukarkan bunyi nun sakinah atau tanwin kepada mim tersembunyi berserta dengung ketika bertemu ba."),
        SmartReferenceEntry("term-ghunnah", "Ghunnah", "Istilah", listOf("الغنة"), "Bunyi dengung yang keluar melalui rongga hidung."),
        SmartReferenceEntry("term-madd", "Mad tajwid", "Istilah", listOf("المد في التجويد"), "Memanjangkan suara pada huruf mad atau lin mengikut kaedah bacaan.")
    )
}

/** Accent-insensitive matching with offsets mapped back to the original text. */
object SmartReferenceMatcher {
    private data class Folded(val text: String, val starts: List<Int>, val ends: List<Int>)
    private fun fold(source: String): Folded {
        val text = StringBuilder()
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        var offset = 0
        while (offset < source.length) {
            val cp = source.codePointAt(offset)
            val end = offset + Character.charCount(cp)
            val part = Normalizer.normalize(String(Character.toChars(cp)), Normalizer.Form.NFD)
                .lowercase(Locale.ROOT)
            var added = false
            part.forEach { c ->
                val kind = Character.getType(c)
                if (kind != Character.NON_SPACING_MARK.toInt() &&
                    kind != Character.COMBINING_SPACING_MARK.toInt() &&
                    kind != Character.ENCLOSING_MARK.toInt() && c != 'ـ') {
                    text.append(c)
                    starts.add(offset)
                    ends.add(end)
                    added = true
                }
            }
            if (!added && ends.isNotEmpty()) ends[ends.lastIndex] = end
            offset = end
        }
        return Folded(text.toString(), starts, ends)
    }

    fun find(source: String, entries: List<SmartReferenceEntry>): List<SmartReferenceMatch> {
        val folded = fold(source)
        val candidates = mutableListOf<SmartReferenceMatch>()
        fun word(c: Char) = c.isLetterOrDigit() || c == '_'
        entries.forEach { entry ->
            (entry.aliases + entry.title).map { fold(it.trim()).text }.filter { it.isNotBlank() }.distinct().forEach { key ->
                var from = 0
                while (from < folded.text.length) {
                    val start = folded.text.indexOf(key, from)
                    if (start < 0) break
                    val end = start + key.length
                    if ((start == 0 || !word(folded.text[start - 1])) &&
                        (end == folded.text.length || !word(folded.text[end]))) {
                        candidates.add(SmartReferenceMatch(folded.starts[start], folded.ends[end - 1], entry))
                    }
                    from = start + 1
                }
            }
        }
        val result = mutableListOf<SmartReferenceMatch>()
        candidates.sortedWith(compareBy<SmartReferenceMatch> { it.start }.thenByDescending { it.end - it.start })
            .forEach { match -> if (result.lastOrNull()?.end?.let { it > match.start } != true) result.add(match) }
        return result
    }
}


@androidx.compose.runtime.Composable
fun SmartReferenceSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { SmartReferenceStore(context) }
    var enabled by remember { mutableStateOf(store.enabled) }
    var entries by remember { mutableStateOf(store.entries()) }
    var editing by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    var deleting by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    Text("Smart Reference — المرجع الذكي", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Aktifkan rujukan automatik")
        Switch(checked = enabled, onCheckedChange = { enabled = it; store.enabled = it })
    }
    var mode by remember { mutableStateOf(store.popupMode) }
    var selectedFields by remember { mutableStateOf(store.popupFields) }
    if (enabled) {
        Text("Jenis paparan popup")
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf("ringkas" to "Ringkas","lengkap" to "Lengkap","custom" to "Tersuai").forEach { (key,label) ->
                FilterChip(selected=mode==key,onClick={mode=key;store.popupMode=key},label={Text(label)})
            }
        }
        if(mode=="custom") (referenceFields + listOf("description" to "Biografi / keterangan","source" to "Sumber")).forEach { (key,label) ->
            Row { Checkbox(checked=key in selectedFields,onCheckedChange={checked ->
                selectedFields=if(checked) selectedFields+key else selectedFields-key;store.popupFields=selectedFields
            }); Text(label,Modifier.padding(top=12.dp)) }
        }
    }
    OutlinedButton(onClick = { editing = SmartReferenceEntry(title="",category="Ulama",aliases=emptyList(),description="") }) { Text("+ Tambah rujukan") }
    entries.forEach { entry ->
        Row(Modifier.fillMaxWidth()) {
            Text(entry.title, Modifier.weight(1f))
            TextButton(onClick={editing=entry}) { Text("Edit") }
            TextButton(onClick={deleting=entry}) { Text("Padam") }
        }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest={deleting=null},title={Text("Padam rujukan?")},text={Text(entry.title)},
            confirmButton={TextButton(onClick={store.delete(entry.id);entries=store.entries();deleting=null}){Text("Padam")}},
            dismissButton={TextButton(onClick={deleting=null}){Text("Batal")}})
    }
    editing?.let { entry ->
        var title by remember(entry.id) { mutableStateOf(entry.title) }
        var category by remember(entry.id) { mutableStateOf(entry.category) }
        var aliases by remember(entry.id) { mutableStateOf(entry.aliases.joinToString("\n")) }
        var description by remember(entry.id) { mutableStateOf(entry.description) }
        var source by remember(entry.id) { mutableStateOf(entry.source) }
        var details by remember(entry.id) { mutableStateOf(entry.details) }
        AlertDialog(onDismissRequest={editing=null},title={Text("Rujukan ilmu")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(title,{title=it},label={Text("Nama utama")})
                Text("Kategori")
                listOf("Ulama","Kitab","Istilah","Tempat","Tokoh Islam").chunked(3).forEach { group ->
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        group.forEach { value -> FilterChip(selected=category==value,onClick={category=value},label={Text(value)}) }
                    }
                }
                fieldsFor(category).forEach { (key,label) ->
                    OutlinedTextField(details[key].orEmpty(),{details=details+(key to it)},label={Text(label)})
                }
                OutlinedTextField(aliases,{aliases=it},label={Text("Alias — satu setiap baris")})
                OutlinedTextField(description,{description=it},label={Text("Keterangan")},minLines=3)
                OutlinedTextField(source,{source=it},label={Text("Sumber")})
            }},confirmButton={TextButton(enabled=title.isNotBlank() && description.isNotBlank(),onClick={
                store.upsert(entry.copy(title=title.trim(),category=category,aliases=aliases.lines().map{it.trim()}.filter{it.isNotEmpty()},description=description,source=source,details=details.filterValues { it.isNotBlank() }))
                entries=store.entries();editing=null
            }){Text("Simpan")}},dismissButton={TextButton(onClick={editing=null}){Text("Batal")}})
    }
}

@Composable
fun ReferenceReaderText(text: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current, onDoubleTap: ((Int)->Unit)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { SmartReferenceStore(context) }
    val matches = remember(text,store) { if(store.enabled) SmartReferenceMatcher.find(text.text,store.entries()) else emptyList() }
    val marked = remember(text,matches) { buildAnnotatedString {
        append(text)
        matches.forEach { addStyle(SpanStyle(color=Color(0xFF087F65),textDecoration=TextDecoration.Underline),it.start,it.end) }
    } }
    var selected by remember { mutableStateOf<SmartReferenceEntry?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val editAction by rememberUpdatedState(onDoubleTap)
    Text(marked,modifier=modifier.pointerInput(marked,matches,onDoubleTap != null) {
        detectTapGestures(onTap={p ->
            layout?.let { l -> val i=l.getOffsetForPosition(p)
                if(i in 0 until marked.length && l.getBoundingBox(i).contains(p)) selected=matches.firstOrNull { i >= it.start && i < it.end }?.entry
            }
        },onDoubleTap=if(onDoubleTap==null) null else {p -> layout?.let { editAction?.invoke(it.getOffsetForPosition(p)) }})
    },style=style,onTextLayout={layout=it})
    selected?.let { entry ->
        AlertDialog(onDismissRequest={selected=null},title={Text(entry.title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(entry.category,color=Color(0xFF075B59))
            val visible = when(store.popupMode) {
                "lengkap" -> (referenceFields.map { it.first } + listOf("description","source")).toSet()
                "custom" -> store.popupFields
                else -> defaultPopupFields
            }
            fieldsFor(entry.category).forEach { (key,label) ->
                val value = entry.details[key].orEmpty()
                if(key in visible && value.isNotBlank()) {
                    Text(label,style=MaterialTheme.typography.labelMedium,color=Color(0xFF075B59))
                    Text(value)
                }
            }
            if("description" in visible) Text(entry.description)
            if("source" in visible && entry.source.isNotBlank()) Text("Sumber: "+entry.source)
        }},confirmButton={TextButton(onClick={selected=null}){Text("Tutup")}})
    }
}
