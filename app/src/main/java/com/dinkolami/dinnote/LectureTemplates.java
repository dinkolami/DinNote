package com.dinkolami.dinnote;

/**
 * Reusable lecture and tazkirah outlines. This is data only:
 * the existing DinNote editor remains the single editing surface.
 */
public final class LectureTemplates {
    private LectureTemplates() {}

    public static final String[] NAMES = {
        "Nota Kuliah", "Tazkirah Umum", "Tazkirah Khas",
        "Tazkirah Bertopik Khas", "Tazkirah Sambutan"
    };

    private static final String[][] SECTIONS = {
        {"Muqaddimah", "Pra Topik", "Isi 1", "Isi 2", "Isi 3", "Kesimpulan", "Penutup"},
        {"Pembukaan", "Pengenalan Ringkas", "Nasihat Utama", "Dalil / Hikmah", "Pengajaran", "Penutup"},
        {"Muqaddimah", "Tujuan Tazkirah", "Isi Utama", "Dalil dan Huraian", "Pengajaran Khusus", "Penutup"},
        {"Tajuk Khusus", "Muqaddimah", "Latar Belakang Topik", "Isi 1", "Isi 2", "Rumusan", "Penutup"},
        {"Pembukaan dan Salam", "Tujuan Majlis", "Keistimewaan Sambutan", "Dalil dan Pengajaran", "Pesanan kepada Hadirin", "Penutup dan Doa"}
    };

    public static String[] sections(int index) {
        if (index < 0 || index >= SECTIONS.length) throw new IllegalArgumentException("Unknown template");
        return SECTIONS[index].clone();
    }

    public static String initialText(int index) {
        StringBuilder text = new StringBuilder();
        for (String heading : sections(index)) {
            if (text.length() > 0) text.append("\n\n");
            text.append(heading).append("\n");
        }
        return text.toString();
    }

    /** A quarter-width rail is intended for expanded displays only. */
    public static boolean usePersistentRail(int availableWidthDp) {
        return availableWidthDp >= 600;
    }
}
