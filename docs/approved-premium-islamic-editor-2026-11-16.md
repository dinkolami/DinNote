# DinNote — approved premium Islamic editor visual style

Approved: 2026-11-16. **Design specification only; Android source implementation and APK build pending.**

## Final visual direction
- Use an elegant, readable typography style for the note editor, with a **compact** top heading: **Catatan Baharu**.
- Maintain a polished turquoise/teal and gold Islamic visual identity, with gold as a restrained accent.
- Top header: teal background, subtle Islamic geometric pattern, small decorative mosque-arch detail; never overlap Android status-bar clock or battery indicators.
- Compact toolbar above the editor: font and size, bold, italic, underline, text color, highlight, alignment and overflow. Keep Undo/Redo and Save accessible.
- Date/time and category dropdown in a compact separate row.
- Main editor: predominantly white, full-height writing canvas with thin turquoise and gold rounded border, subtle decorative top corners, small ornamental divider below **Tajuk**, and placeholder **Tulis catatan anda di sini...**.
- At the **bottom** of the editor canvas, use a faint mosque silhouette and restrained turquoise/gold flowing decorative trim; never cover typed content.
- Preserve keyboard/system insets, and automatically right-align Arabic RTL content.
- Continue the approved small default title font (suggested 18sp), with readable body text (suggested 16sp).
- Keep the overall layout clean and functional; decoration must not reduce writing space or obscure controls.

## Acceptance criteria
- Works on Android with both gesture and three-button navigation.
- Status bar clock, signal and battery percentage remain unobstructed.
- Add/edit/save notes work without crashes, including Arabic.
- Toolbar stays visible/accessible when keyboard is shown.
- Styling can be reused consistently across relevant DinNote editors.

Visual reference: latest approved DinNote editor mockup in the ChatGPT conversation dated 2026-11-16.

**This documentation commit records approval only; it does not claim the UI is implemented or that an APK exists.**
