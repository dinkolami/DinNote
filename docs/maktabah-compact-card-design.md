# Maktabah DinNote — compact book cards (approved option 2)

Design reference: approved option 2 in conversation, 18 Nov 2026.

## Implementation requirements

- Retain existing book-cover icons (green, maroon, navy), green/gold palette, and offline badges.
- Reduce book-cover dimensions and card vertical padding; avoid unused vertical whitespace.
- Place Arabic book title aligned to the right near the Malay/Latin book name, without a large empty region between.
- Display a small Arabic secondary label at the lower-right of each book card, where the grey-marked empty area was shown in the screenshot. Use actual book metadata, not a decorative hard-coded label.
- Keep RTL for Arabic, preserve diacritics where available, and allow long text to wrap without clipping.
- Preserve readable tap targets and support narrow/foldable screens, landscape, and Android navigation insets.
- No new book-cover asset is required; reuse existing assets.

This is a design specification only. It does not change the Android UI until implemented in application source.
