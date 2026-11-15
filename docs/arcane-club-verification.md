# Arcane Club experience update

The playable flow is guest or registered account → lobby → bot/local/PvP → authoritative Java game API → persisted game/profile → updated board and history.

## Changes

- Persistent 64-square board and SVG piece layer. Mouse and touch use Pointer Events, pointer capture and one transform update per animation frame. Native image dragging is disabled.
- Immediate move preview, one mutation at a time, rollback on rejection, reconnect and resync after a lost response. Server owns legality, turn order, inventory and results.
- Warm ink/parchment/mint lobby, illustrated cards, move/capture transitions, optional sound, card effects, mobile layout and reduced-motion support.
- Profile shows server rating, XP, wallet and record, plus 20 recent games with resume/view controls.
- Numbered PvP rooms with six-digit join codes, share links, creator/opponent names and ELO, a visible five-minute countdown, automatic expiry, black orientation, turn restrictions, authenticated WebSocket updates and REST recovery that does not interrupt a drag.
- Waiting rooms use a dedicated share state instead of rendering an inactive board. Active matches use a compact match header, dominant board, small tactical rail, one board toolbar, and collapsed journal/rules; mobile hides the global bottom navigation during play.
- Hard bot evaluates the opponent's next reply. Frozen bot pieces cannot move. Card API checks the player's turn; protection follows the selected piece.
- Puzzle positions are visible and accept selection on the mini board.
- Inventory exposes owned quantities and a persistent three-slot Deck. A match snapshots each side's Deck when it is created or joined.
- The server persists ordered move, card, resolved-effect and resignation events with timestamps and capture data. The match journal reloads them from the database.

## Verification

Backend: `mvn -f apps/server/pom.xml -Djava.version=17 test` on local Microsoft JDK 17. Production and GitHub CI retain the repository's Java 21 configuration.

Browser: `npm ci`, `npx playwright install chromium`, `npm run test:e2e`.
The test server uses an isolated in-memory H2 database. It does not connect to Supabase or reuse production accounts.

Latest local result on 2026-11-15: 25/25 backend tests and 26/26 Playwright tests passed. The Impeccable layout detector returned no findings. Docker image `chess2-java:local` built successfully and its demo-profile smoke test created a numbered waiting room with creator ELO and expiry metadata.

Automated coverage:

1. Square geometry, persistent DOM, mouse drag and Chromium touch input, cancel, invalid drop, click moves, flip; drag image center stays within 2 CSS pixels of the pointer.
2. Delayed move response, duplicate prevention, rejection rollback and successful retry.
3. Hard bot response, profile/history, resume, resign, persisted result after reload.
4. Persistent journal reload after resume; more than 40 entries remain visible, including readable card, effect and captured-piece events.
5. Buy and consume Freeze, equip/remove/equip with restored keyboard focus, Deck count/snapshot, target cancellation, frozen-piece restriction and puzzle submission.
6. Two independent player contexts join by six-digit room code, see both usernames/ELO, exchange moves, enforce card turns and observe the winner; backend coverage rejects rooms older than five minutes.
7. Protection follows a moved piece; server accepts a move but the response is lost, then client resynchronizes.
8. Player-selected Black side, separate admin operations portal, registration/login/logout and reduced motion.

## Scope and limitations

- This is the existing arcade variant: capture the king or reduce enemy HP to zero; pawns auto-promote to queens. Classical checkmate, castling and en passant are not implemented.
- PvP uses authenticated native WebSocket notifications and retains a 2-second REST recovery poll. The hub is instance-local, so multi-instance deployment still requires shared pub/sub.
- Hard bot uses a shallow arcade search, not Stockfish.
- Move Log is persistent and auditable, but the UI does not yet provide interactive board replay controls.
- Rooms are public and joinable by six-digit code while empty, not private invitation-only rooms. Unjoined rooms expire five minutes after creation.
- Password reset delivery depends on existing SMTP configuration; this release does not configure email delivery.
- Browser automation covers desktop Chromium and emulated Pixel 7 with Chromium touch input; physical devices and Safari/Firefox are not yet verified.
