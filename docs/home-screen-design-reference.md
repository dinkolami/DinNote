# DinNote — Approved Home Screen Design Reference

Reference: user-approved screenshot (17 November 2026). This document records the target design; it is not an implementation of the Android layout.

## Layout top to bottom

1. Islamic premium header: DinNote emblem and wordmark, turquoise/gold geometry, small mosque illustration.
2. Full-width rounded search field: `Cari nota, kitab dan rujukan`.
3. `Nota Dipinkan` section: show pinned notes, with compact Arabic and Malay content.
4. `Teroka DinNote`: two rows of three fully clickable rounded cards: first row **Nota | Maktabah | Carian**; second row **Penanda | Mazhab Syafi‘i | Tetapan**.
5. `Sambung Bacaan`: resume entry to Maktabah.
6. Compact bottom navigation: **Utama | Nota | Maktabah | Tetapan**; active tab highlighted turquoise.

## Visual styling

- White/cream background, turquoise outlines and accents, restrained gold highlights.
- Islamic architectural/geometric ornamentation, especially header and above bottom navigation.
- Thin clean borders, subtle shadows, compact card heights and consistent rounded corners.
- Arabic text right-aligned; legible Malay labels; avoid horizontal scrolling on Samsung Galaxy Z Fold6.
- Preserve existing functional navigation and note data; visual changes must not break editor/autosave.

## Implementation checklist

- [ ] Add properly licensed/original image assets for mosque, logo, and ornaments.
- [ ] Implement responsive Android layout matching the reference.
- [ ] Make each entire menu card clickable and wire to actual destinations.
- [ ] Connect pinned notes and resume-reading to real data.
- [ ] Build and test APK, including editor regression tests.
