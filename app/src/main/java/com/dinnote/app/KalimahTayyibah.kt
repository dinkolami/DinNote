package com.dinnote.app

data class KalimahEntry(
    val title: String,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val reference: String,
    val note: String
)

val kalimahEntries = listOf(
    KalimahEntry(
        "Basmalah",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "Bismillahir-Rahmanir-Rahim",
        "Dengan nama Allah Yang Maha Pemurah lagi Maha Penyayang.",
        "Surah al-Fatihah, 1:1; Surah an-Naml, 27:30",
        "Dibaca ketika memulakan perkara yang baik."
    ),
    KalimahEntry(
        "Hamdalah",
        "الْحَمْدُ لِلَّهِ",
        "Alhamdulillah",
        "Segala puji bagi Allah.",
        "Surah al-Fatihah, 1:2",
        "Ungkapan memuji dan mensyukuri nikmat Allah."
    ),
    KalimahEntry(
        "Tasbih",
        "سُبْحَانَ اللَّهِ",
        "Subhanallah",
        "Maha Suci Allah.",
        "Surah al-Isra', 17:44",
        "Menyucikan Allah daripada segala kekurangan."
    ),
    KalimahEntry(
        "Takbir",
        "اللَّهُ أَكْبَرُ",
        "Allahu Akbar",
        "Allah Maha Besar.",
        "Antara zikir yang disyariatkan dalam al-Quran dan hadis.",
        "Mengagungkan Allah dan mengakui kebesaran-Nya."
    ),
    KalimahEntry(
        "Tahlil",
        "لَا إِلَٰهَ إِلَّا اللَّهُ",
        "La ilaha illallah",
        "Tiada Tuhan yang berhak disembah melainkan Allah.",
        "Surah Muhammad, 47:19",
        "Kalimah tauhid yang menjadi asas keimanan."
    ),
    KalimahEntry(
        "Hauqalah",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        "La hawla wa la quwwata illa billah",
        "Tiada daya dan kekuatan melainkan dengan pertolongan Allah.",
        "Sahih al-Bukhari dan Sahih Muslim",
        "Disebut sebagai salah satu perbendaharaan syurga dalam hadis sahih."
    ),
    KalimahEntry(
        "Istighfar",
        "أَسْتَغْفِرُ اللَّهَ",
        "Astaghfirullah",
        "Aku memohon keampunan kepada Allah.",
        "Surah Nuh, 71:10",
        "Memohon ampun dan kembali bertaubat kepada Allah."
    ),
    KalimahEntry(
        "Selawat",
        "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
        "Allahumma salli 'ala Muhammad",
        "Ya Allah, limpahkanlah selawat ke atas Nabi Muhammad.",
        "Surah al-Ahzab, 33:56",
        "Doa memohon Allah melimpahkan selawat ke atas Rasulullah SAW."
    ),
    KalimahEntry(
        "In sya Allah",
        "إِنْ شَاءَ اللَّهُ",
        "In sya Allah",
        "Jika Allah menghendaki.",
        "Surah al-Kahfi, 18:23–24",
        "Diucapkan ketika menyatakan hasrat melakukan sesuatu pada masa akan datang."
    )
)
