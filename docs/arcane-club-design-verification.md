# Arcane Chess Club design implementation — 2026-11-07

The user approved the desktop/mobile mock-up and its forest, parchment, gold and illustrated pixel-card direction. This change applies that direction to the static Java frontend, primarily the match screen, shared navigation and card collection. Lobby and profile inherit the same palette. No deployment, production database change or gameplay rule change was made in this design pass.

## Changes

- Replaced the prior violet `club.css` theme with forest/ink tokens, parchment cards and a compact match layout.
- Added six original card illustrations as a reusable atlas; all names, effect descriptions, counts and actions remain HTML rather than baked-in image text.
- Self-hosted Be Vietnam Pro for Vietnamese and Silkscreen for the wordmark, with font licenses included.
- Added side labels, legible HP values, inline targeting guidance and active card state. Existing SVG chess pieces and persistent pointer-driven board remain intact.
- Added fixed mobile navigation and kept the hand in three columns. The mobile match avoids redundant mode controls; returning to the lobby still exposes every mode.
- Reused the same card art in the collection and lobby. Added a shared, small SVG interface icon set.

## Verification

`npm run test:e2e -- --reporter=line`: **16 passed**, desktop Chromium and Pixel 7 emulation, against the real Spring Boot API and isolated in-memory H2 database. Final complete run after independent-review fixes: 56.0 seconds.

Coverage: registration/login/logout; guest session; mouse and CDP touch dragging; pointer cancellation; exact square-board geometry; flip; click moves; rejection rollback and duplicate-submit prevention; bot reply; profile/history persistence; purchasing and using Freeze/Arsenal; puzzle solve; two-client PvP and wrong-turn restriction; recovery from a lost server response; reduced motion; artwork/font decoding; keyboard card activation and cancellation; responsive hand and bottom navigation.

The first two new design tests used a guest with 250 Gold for a 450 Gold purchase plan. They were corrected to register a user with the existing 500 Gold starting balance. No production economy or seed data was changed to satisfy tests.

Actual captures are in `.impeccable/review/`: desktop at 1440×1000, mobile at 390×844 CSS pixels, plus full-page captures and the approved mock-up. Supporting regression tests use the standard Desktop Chrome / Pixel 7 profiles.

The Impeccable engine/detector could not run because engine 0.1.5 was not installed and its default cache location was unavailable under sandbox permissions. Visual comparison therefore uses actual browser captures and independent review, without a claimed machine fidelity score.

## Independent review

Initial manual disposition: **fix**. Four items were requested: fit the board and three-card hand into the mobile viewport, restore gold corner ornament, persist the actual approved direction honestly, and replace inconsistent history/result glyph icons.

The correction batch moved the turn badge into the player row, removed redundant mobile match headings, compacted the illustrated hand and target prompt, added an original native-alpha nine-slice frame, recorded `PRODUCT.md`, and reused SVG icons. New geometry assertions verify that all three card bounds and the selected-card instruction end above the navigation at 390×844. PNG prompts are embedded as iTXt metadata; the trim's center is fully transparent.

The same independent reviewer rescored all four findings **resolved**, with disposition **ship**, scoped to the listed fixes. No whole-surface automated fidelity certification is claimed.

## Scope limits

- A static image cannot establish equivalence to chess.com interaction performance. Drag behavior is checked; no cross-device FPS benchmark is claimed.
- Gameplay remains the existing arcade variant, with existing PvP polling and backend rules.
- These are local-source changes. The deployed production site has not been updated by this pass.
- The artwork PNG is shared by six cards; it is approximately 2.7 MB. Future delivery optimization can generate a smaller encoding without changing the design.
