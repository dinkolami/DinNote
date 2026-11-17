# DinNote Islamic Premium — Emerald & Gold assets

Approved design: ornate emerald and gold frontpage (not the later minimalist alternative).

## Image files

Place these binary WebP files in `app/src/main/assets/`:

- `islamic_premium_background.webp` — decorative Islamic background with mosque, lantern and book
- `frontpage_design_reference.webp` — approved reference composition for the homepage

These files are supplied in `DinNote_Islamic_Premium_Assets.zip` in the ChatGPT conversation, but **are not yet uploaded to this repository**. Do not claim they are included in an APK until the files have been uploaded and the build has passed.

## Integration

Keep search, notes, reminders and navigation as real accessible Compose controls, not as part of a static image. Use decorative images behind controls only. Keep the existing user's selected background preference and alpha setting. Respect Android system insets and preserve the existing signing key, package name and database.

The reference screenshot is a visual specification only: do not display it as a static full-screen app UI.
