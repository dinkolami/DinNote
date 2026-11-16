# DinNote — Premium Islamic Home Screen (approved mockup)

Design reference: mockup approved in ChatGPT on 2026-11-17. This is a design specification, not an implemented Android screen.

## Layout (preserve existing order)
1. DinNote logo with a small, low-profile teal/gold mosque ornament at top right.
2. Compact rounded search bar: “Cari nota, kitab dan rujukan”.
3. “Nota Dipinkan” heading with thin gold rule and pinned note preview.
4. “Teroka DinNote” with a 3×2 grid: Maktabah, Nota, Carian, Penanda, Mazhab Syafi‘i, Tetapan.
5. “Sambung Bacaan” compact teal-tinted card with Teruskan action.
6. Compact bottom navigation: Utama, Nota, Maktabah, Tetapan.

## Visual style
- Cream background (#FFF9F0) with extremely faint Islamic geometry.
- Teal (#117E82), gold (#D5AB48), white cards, dark green text (#244B48).
- Thin teal outlines, subtle shadows, consistent rounded corners.
- Consistent icon sizes and stroke weights; generous touch targets.
- Keep mosque ornament small, low, and decorative rather than dominating the header.
- Arabic note excerpts must retain right-to-left direction and legible script.

## Implementation notes
- Preserve navigation behavior, pinned notes, search, and reading progress.
- Use native Android resources and layouts; do not use the mockup screenshot as a static full-screen background.
- Verify on narrow and foldable Android screens, then run the APK build workflow.
