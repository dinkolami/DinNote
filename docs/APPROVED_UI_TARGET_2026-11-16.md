# DinNote approved UI target — 16 Nov 2026

Reference: five-screen DinNote concept image approved by product owner in ChatGPT conversation (16 Nov 2026). This is an implementation specification, NOT a claim that the APK matches it.

## Global visual system
- Clean modern Islamic UI; deep teal #006B70 and turquoise #008F91, white surfaces, restrained gold/green accents.
- Compact readable typography, thin light-gray card borders, modest rounded corners, clear spacing, subtle mosque/arch illustrations.
- Respect Android status and navigation bar safe insets. No overlap with clock, battery or system navigation.
- Use consistent iconography, card styling and touch targets across all screens.

## Screens to implement
1. **Utama**: teal greeting area with subdued Islamic mosque image; four prominent feature tiles Nota, Maktabah, Penanda, Kegemaran; smart-search row; daily reading card.
2. **Nota**: teal heading, compact category chips, white bordered note cards with timestamps and concise previews, floating add-note action; functional add/edit/read and autosave.
3. **Maktabah**: compact grid for Al-Quran, Hadis, Tafsir, Fiqah, Aqidah, Akhlak, Sirah, Lain-lain; selected book list below; user-created categories and editable content.
4. **Carian Pintar**: compact search field, filter chips, searchable notes and library, favorites and bookmarks, readable result previews.
5. **Bacaan Arab–Melayu**: full-page reading, Arabic right-aligned with RTL direction, Malay below, verse/navigation/bookmark/font controls.

## Navigation
Reference mockup depicts Utama / Nota / Maktabah / Carian / Profil. Existing app has four compact tabs Utama / Nota / Maktabah / Tetapan. Migrate deliberately, keeping Settings accessible and preserving saved data.

## Acceptance criteria
- Build debug APK successfully and upload artifact through GitHub Actions.
- Verify no crashes on add note, edit note, opening library content or switching tabs.
- Validate Arabic alignment and font size, small screens and keyboard behavior.
- Preserve existing notes, library data, favorites, and backup/restore compatibility.
- Only call it visually 100% complete after side-by-side on-device screenshots verify each screen.

## Implementation status
Design target approved; implementation and APK verification remain pending.
