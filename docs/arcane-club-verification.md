# Arcane Club experience update

The playable flow is guest or registered account → lobby → bot/local/PvP → authoritative Java game API → persisted game/profile → updated board and history.

## Changes

- Persistent 64-square board and SVG piece layer. Mouse and touch use Pointer Events, pointer capture and one transform update per animation frame. Native image dragging is disabled.
- Immediate move preview, one mutation at a time, rollback on rejection, reconnect and resync after a lost response. Server owns legality, turn order, inventory and results.
- Warm ink/parchment/mint lobby, illustrated cards, move/capture transitions, optional sound, card effects, mobile layout and reduced-motion support.
- Profile shows server rating, XP, wallet and record, plus 20 recent games with resume/view controls.
- Public PvP room links, automatic black orientation, turn restrictions, polling that does not interrupt a drag, result and rematch controls.
- Hard bot evaluates the opponent's next reply. Frozen bot pieces cannot move. Card API checks the player's turn; protection follows the selected piece.
- Puzzle positions are visible and accept selection on the mini board.

## Verification

Backend: `mvn -f backend/pom.xml -Djava.version=17 test` on local Microsoft JDK 17. Production and GitHub CI retain the repository's Java 21 configuration. Local pre-existing Java 25 configuration changes are not part of this release.

Browser: `npm ci`, `npx playwright install chromium`, `npm run test:e2e`.
The test server uses an isolated in-memory H2 database. It does not connect to Supabase or reuse production accounts.

Automated coverage:

1. Square geometry, persistent DOM, mouse drag and Chromium touch input, cancel, invalid drop, click moves, flip; drag image center stays within 2 CSS pixels of the pointer.
2. Delayed move response, duplicate prevention, rejection rollback and successful retry.
3. Hard bot response, profile/history, resume, resign, persisted result after reload.
4. Buy and consume Freeze, target cancellation, frozen piece restriction, puzzle submission.
5. Two independent player contexts join by link, exchange moves, enforce card turns and observe the winner.
6. Protection follows a moved piece; server accepts a move but the response is lost, then client resynchronizes.
7. Register, logout and login; credentials do not appear in the URL; reduced motion is honored.

## Scope and limitations

- This is the existing arcade variant: capture the king or reduce enemy HP to zero; pawns auto-promote to queens. Classical checkmate, castling and en passant are not implemented.
- PvP still polls every 1.8 seconds. Own-piece dragging is local and immediate; remote moves depend on polling and network latency.
- Hard bot uses a shallow arcade search, not Stockfish.
- Move journal is session-only; stored history contains positions/results, not a full move replay.
- Rooms are public and joinable while empty, not private invitation-only rooms.
- Password reset delivery depends on existing SMTP configuration; this release does not configure email delivery.
- Browser automation covers desktop Chromium and emulated Pixel 7 with Chromium touch input; physical devices and Safari/Firefox are not yet verified.
