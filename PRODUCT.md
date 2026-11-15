# CHESS² / Arcane Chess Club

## Product and audience

A Vietnamese-first arcade chess web game built with Java/Spring Boot, JWT authentication, a static JavaScript frontend, Supabase PostgreSQL, Docker and Vercel deployment. Players can practice with a bot, invite friends to a public PvP room, or play locally on one device. It is an arcade variant with HP and tactical cards, not a full implementation of classical tournament chess.

## Existing features to preserve

- 8×8 board, click-to-move and mouse/touch dragging, automatic queen promotion, game outcomes based on king capture or HP depletion.
- Bot difficulty choices, numbered two-player PvP rooms, public invitations, persistent match journal, board flip and optional sound. PvP rooms expose both players' names and ELO; an unjoined room expires five minutes after creation.
- 100 HP, Gold wallet, purchased consumable cards, a persistent three-slot Deck, puzzles and leaderboard.
- Fifteen card effects cover healing, direct damage, freeze/protection, pawn movement, traps, rewind, portals, summon/revive/hide/teleport, mosquito damage and surrender. Server responses remain authoritative.
- Guest and registered accounts; login, logout, password-reset UI; profile with statistics, recent games and resume/view actions. Password-reset email delivery requires SMTP configuration.

No clocks, chat, online-friend presence, tournaments or classical checkmate/castling/en-passant should be presented as available features. PvP uses authenticated native WebSocket notifications with REST state recovery; no chess.com-level performance claim is made.

Security, data-integrity, recovery and scale targets are tracked separately in `docs/non-functional-requirements.md`. In particular, 99% uptime, sub-50 ms WebSocket latency and 10,000 CCU are acceptance targets, not verified capabilities of the current local build.

## Approved visual direction

The user asked for a project-specific mock-up after specifying a pixel-arcade direction inspired by Balatro/Gambonanza. The generated **Arcane Chess Club** desktop/mobile mock-up was accepted on 2026-11-07 with “Tôi thấy được đó”. Reference: `.impeccable/review/approved-mock.png`.

The approved direction combines midnight ink, forest felt, parchment, sage and antique gold; illustrated tactical cards; restrained pixel-gold frame ornament; an obvious square board; clear HP and turn indicators; Vietnamese-readable typography and convenient mobile navigation. The board and the current three-card hand should be visible together at the 390×844 reference viewport, with target instructions when a card is armed. Longer journals and secondary actions may scroll below.

## Implementation and verification boundary

The design pass updates the existing frontend and preserves gameplay/API behavior. Tests use a local in-memory H2 database, not the production CHESS database. Source changes in this pass have not been deployed.

The Impeccable launcher and detector could not run because engine 0.1.5 was unavailable in the restricted cache location. There is no generated concept seed, machine spec/diff score or completed automated build gate. The approved image, real browser captures, Playwright results and independent manual review are the design evidence. See `docs/arcane-club-design-verification.md` for recorded checks; `DESIGN.md` describes the implemented visual system.

## Local admin operations portal — 2026-11-13

The project now includes a separate operations surface at `/admin/`. It is intentionally absent from the player navigation and reuses the existing JWT session, `ADMIN` role checks and Spring Boot APIs. The portal provides an overview, account search, access locking, role/wallet/card management, shop price and availability controls, an authenticated activity log, and a safe system-status view that never exposes secrets or connection strings.

The demo profile seeds `admin` / `Admin@123456` in its isolated local database. Production credentials remain environment-driven; this local convenience credential must not be treated as a deployment default. The account is protected by the same backend RBAC as every other administrator, and a player token is explicitly forbidden from `/api/admin/**`.

This surface extends the approved Arcane Chess Club world in an **Operate** mode: forest and midnight operational panels, parchment text, restrained antique-gold status accents, a desktop sidebar, mobile top navigation and an inline account editor. It introduces no new frontend dependency and does not change the player-facing information architecture. Surface-specific rules are recorded in `.impeccable/surfaces/admin.md`.

Account locking is server-authoritative: a locked account cannot log in, its existing JWT stops authenticating on the next request, and new WebSocket handshakes are rejected. Administrators cannot lock their own current account. The audit trail records authenticated data-changing operations and login outcomes with actor, category, route, status and server time. It deliberately excludes passwords, tokens, request bodies, IP addresses and user-agent data.

Verification is local and scoped to this extension. The final suite passed 26/26 backend tests and 26/26 Playwright tests across desktop and Pixel 7 profiles. A real demo-profile login returned role `ADMIN` and successfully accessed the overview API. The current responsive audit-log capture is generated by the admin Playwright scenario; the independent finish review disposition remains **ship**. The mechanical detector was invoked once, but its output was truncated by the host and no report was produced, so no automated visual score is claimed. No commit, push, deployment or production database migration is part of this local pass.

## Inventory, Deck and persistent Move Log — 2026-11-14

The collection now separates owned inventory from the equipped Deck. Each account may equip up to three unique owned card types; an empty Deck is valid, and a newly purchased card fills the first open slot when possible. Creating or joining a game snapshots the appropriate Deck for each side. Later inventory changes apply to the next game and cannot alter an active match; the server rejects any card outside that match snapshot.

Every game now stores ordered `MOVE`, `CARD`, `EFFECT` and `RESIGN` events in `game_move_logs`. Each record includes a server timestamp, actor side/account where applicable, origin and target, captured piece and effect code. `GET /api/games/{id}/logs` is participant-only. The player UI loads all stored entries, exposes loading/empty/error states, and renders Vietnamese card, effect and captured-piece names with timestamps. This is an audit journal rather than an interactive replay player.

The UI remains within the approved Arcane Chess Club direction. Illustrated effects retain their artwork; effects without dedicated art use one honest neutral SVG card identity instead of borrowing another effect's image. Deck updates preserve keyboard focus. Latest local verification on 2026-11-15 passed 25/25 backend tests and 26/26 Playwright tests across desktop and Pixel 7, including the persistent journal, Deck flow and authenticated two-player WebSocket path. The independent Impeccable finish review disposition is **ship**.
