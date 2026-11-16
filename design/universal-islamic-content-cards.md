# DinNote — Universal Islamic Content Card Standard

Approved 2026-11-16. Applies automatically to ALL existing and future user-created content and categories, including Utama, Nota, Maktabah, Kitab-Kitab, Kalam Hikmah and Kalimah Tayyibah. Not restricted to Basmalah.

## Visual standard
- White/off-white background, thin turquoise outline, modest rounded corners (not oversized).
- Compact, readable spacing and content-dependent height; avoid excess empty space.
- Arabic passages automatically right-aligned with RTL text direction, while Malay text uses normal LTR alignment.
- Teal and subtle gold Islamic architectural mosque/arch ornament along the bottom AFTER the text; scale responsively without covering text.
- Consistent small minimalist icons, clean separators, restrained typography, and compact bottom navigation: Utama, Nota, Maktabah, Tetapan.
- Detail dialogs use matching border and corners and a compact outlined Tutup button.
- New categories and content inherit this style by default; use reusable shared components/theme rather than hardcoding the Basmalah entry.

## Implementation acceptance criteria
1. Every existing content card and newly added item uses the shared style automatically.
2. Arabic is RTL/right-aligned, including mixed Arabic/Malay content.
3. Ornament remains below content, never overlaps long text, and scrolls correctly.
4. Responsive on small screens; accessible text sizes; no clipped controls.
5. User content and autosave functionality remain unchanged.

This commit records the approved design specification, not an implemented Android UI or built APK.
