# DinNote — Approved compact UI design (2026-11-16)

Status: **Design approved; implementation pending.** This document records the approved UI requirements. It does not claim the Android layouts or APK have been changed.

## Status bar and Islamic decoration
- Keep the teal/gold Islamic geometric decoration.
- Place it **below the Android status bar**, preferably near the lower edge of the content area, never over the clock, connectivity indicators, battery icon or battery percentage.
- Respect Android status bar safe-area insets; do not draw obstructing content over system indicators.

## Bottom navigation
- Four tabs: **Utama**, **Nota**, **Maktabah**, **Tetapan**.
- Compact white rounded bar, thin turquoise outline, subtle separators, turquoise highlight for active tab.
- Place the app navigation **above** the Android system navigation bar, with correct WindowInsets.navigationBars padding; never overlap Android Back/Home/Recents controls.
- Ensure notes and Maktabah content remain visible above the app navigation.

## Note search
- Use a smaller, low-height, compact **Cari nota** search field with a thin turquoise outline.
- Keep a separate compact filter control.
- Avoid wasting vertical space and retain a clean uncluttered look.

## Acceptance checks
1. On three-button navigation, no overlap between DinNote tabs and Android Back/Home/Recents.
2. On gesture navigation, no overlap with the gesture area.
3. Android clock, network and battery percentage are fully visible.
4. Nota and Maktabah remain usable with keyboard shown and hidden.
5. Compact search and filter remain functional.

Reference: User-approved UI mockup in ChatGPT conversation dated 2026-11-16.
