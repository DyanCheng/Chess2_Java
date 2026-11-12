# CHESS² / Arcane Chess Club

## Product and audience

A Vietnamese-first arcade chess web game built with Java/Spring Boot, JWT authentication, a static JavaScript frontend, Supabase PostgreSQL, Docker and Vercel deployment. Players can practice with a bot, invite friends to a public PvP room, or play locally on one device. It is an arcade variant with HP and tactical cards, not a full implementation of classical tournament chess.

## Existing features to preserve

- 8×8 board, click-to-move and mouse/touch dragging, automatic queen promotion, game outcomes based on king capture or HP depletion.
- Bot difficulty choices, two-player public room invitations, session move journal, board flip and optional sound.
- 100 HP, Gold wallet, purchased consumable cards, puzzles and leaderboard.
- Cards: heal 20 HP, strike for 25 HP, freeze an opponent piece for a turn, protect a piece for two turns, exchange 80% HP for a queen, and surrender. Server responses remain authoritative.
- Guest and registered accounts; login, logout, password-reset UI; profile with statistics, recent games and resume/view actions. Password-reset email delivery requires SMTP configuration.

No clocks, chat, online-friend presence, tournaments or classical checkmate/castling/en-passant should be presented as available features. PvP currently uses polling; no WebSocket or chess.com-level performance claim is made.

## Approved visual direction

The user asked for a project-specific mock-up after specifying a pixel-arcade direction inspired by Balatro/Gambonanza. The generated **Arcane Chess Club** desktop/mobile mock-up was accepted on 2026-11-07 with “Tôi thấy được đó”. Reference: `.impeccable/review/approved-mock.png`.

The approved direction combines midnight ink, forest felt, parchment, sage and antique gold; illustrated tactical cards; restrained pixel-gold frame ornament; an obvious square board; clear HP and turn indicators; Vietnamese-readable typography and convenient mobile navigation. The board and the current three-card hand should be visible together at the 390×844 reference viewport, with target instructions when a card is armed. Longer journals and secondary actions may scroll below.

## Implementation and verification boundary

The design pass updates the existing frontend and preserves gameplay/API behavior. Tests use a local in-memory H2 database, not the production CHESS database. Source changes in this pass have not been deployed.

The Impeccable launcher and detector could not run because engine 0.1.5 was unavailable in the restricted cache location. There is no generated concept seed, machine spec/diff score or completed automated build gate. The approved image, real browser captures, Playwright results and independent manual review are the design evidence. See `docs/arcane-club-design-verification.md` for recorded checks; `DESIGN.md` describes the implemented visual system.
