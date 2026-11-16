# Approved DinNote note editor — 2026-11-16

Status: UI mockup approved. **Implementation in Android code pending.**

## Layout
- Single-line, compact **Nota baharu** title at the top, smaller than the previous two-line heading.
- Top actions: Back, Undo, Redo, note/list action and Save/check.
- Second compact row: date/time and editable category dropdown (e.g. Lain-lain).
- Formatting toolbar **above** the text editor: font family/style, size, bold, italic, underline, text colour, highlight, alignment, overflow.
- Main writing area fills the available screen and has a thin turquoise rounded outer border; placeholders **Tajuk** and **Tulis nota di sini...**.
- Keep clean white backgrounds and modest spacing; avoid wasting vertical space.

## Behaviour to implement and test
- Note creation, editing and saving must work; no crash on Add note.
- Auto-save edits where supported; ensure undo/redo is functional.
- Arabic text automatically right-aligns and uses RTL direction while Malay/Latin remains LTR.
- Use keyboard and system insets so toolbar and editor are not hidden behind Android navigation.
- Reuse consistent editor in notes and editable Maktabah/kitab content where applicable.

Approved visual reference: DinNote editor mockup in ChatGPT conversation on 2026-11-16. This documentation commit does **not** represent a compiled or tested APK.
