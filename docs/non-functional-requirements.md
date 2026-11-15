# Non-functional requirements and evidence

This document separates implemented controls from service-level targets. A target is not marked achieved without repeatable evidence from the production-like environment.

## Current status

| Area | Status | Evidence and boundary |
|---|---|---|
| Password security | Verified locally | Registered and guest passwords use Spring Security BCrypt. Tests prove the stored value is a BCrypt hash and matches only through `PasswordEncoder`. |
| REST authentication and authorization | Verified locally | JWT signature/expiry is checked for every business API; the user and current role are reloaded from the database. Admin APIs require `ROLE_ADMIN`. Public endpoints are limited to static assets, auth entry points and health. |
| Client-side tamper resistance | Verified for covered game flows | The server checks participant identity, turn, legal move, card ownership, Deck snapshot and target rules. Games use a pessimistic write lock before mutations. The client preview is never authoritative. |
| WebSocket JWT guard | Verified locally | `/ws/games` authenticates the HTTP Upgrade with the existing JWT through a WebSocket subprotocol, never a URL parameter. A valid user can subscribe only to a game where they are White or Black; invalid subscriptions close with policy-violation code 1008. |
| Request validation | Verified locally | Trust-boundary DTOs constrain usernames, email/password lengths, game types, sides, coordinates, card IDs, card targets, Deck size and admin mutations. Invalid payloads return HTTP 400 before service logic. |
| Transaction integrity | Verified locally | Registration, purchases, Deck updates, puzzle rewards and all game mutations run inside database transactions. A failed purchase test proves wallet and inventory remain consistent. Database constraints protect non-negative balances/quantities and HP ranges. |
| Maintainability | Implemented | Backend flow is Controller → Service → Repository/Entity. `ChessBoard` owns board rules; services own use cases and transaction boundaries; repositories own persistence. No speculative gateway abstraction is added. |
| State recovery | Verified locally | The browser retains JWT/guest identity, reloads games through authoritative REST state, resumes active games from profile and resynchronizes after a lost response. |
| Ping/Pong heartbeat | Implemented locally | Spring sends a control-frame Ping every 25 seconds, browsers reply with Pong automatically, and sessions silent for 60 seconds are closed. The browser reconnects with bounded exponential backoff and reloads authoritative REST state. |
| Database health | Healthy at audit time | Supabase project CHESS reported `ACTIVE_HEALTHY` and no health-advisor findings on 2026-11-14. This is a point-in-time check, not an uptime guarantee. |
| 99% uptime | Target, unverified | Measure successful health probes over a rolling 30-day window in production. Local health `UP` does not establish an availability SLO. |
| WebSocket latency below 50 ms | Target, unverified in production | Local Playwright verifies authenticated WebSocket delivery on desktop/mobile and requires the opponent board to update within 1.5 seconds even while Move Log is delayed by 3.5 seconds. Measure p50/p95/p99 on production-like regional infrastructure before claiming the 50 ms target. |
| 10,000 CCU | Target, unverified | The current Spring hub is instance-local and retains a 2-second REST recovery poll. Multi-instance scale requires shared pub/sub, connection budgets, rate limits and capacity sized to the Supabase/Vercel plans. Do not advertise 10,000 CCU before the staged load gate passes. |

## Database and scalability controls

- Runtime uses a bounded Hikari pool and Supabase pooling; migrations use a separate session-semantic connection.
- Flyway V9 adds covering indexes for the four foreign keys reported by Supabase Performance Advisor: `games.black_player_id`, `puzzle_attempts.puzzle_id`, `shop_items.card_id` and `user_cards.card_id`.
- The application should use a dedicated least-privilege Postgres login rather than the project owner role before production. Keep schema `chess` outside the Data API; if it is ever exposed, enable RLS and ownership policies first.
- Do not remove currently unused indexes based on a young/low-traffic database. Reassess with `pg_stat_statements` after representative load.
- Official references: [Supabase database connections](https://supabase.com/docs/guides/database/connecting-to-postgres), [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security), and [Database linter](https://supabase.com/docs/guides/database/database-linter).

## Acceptance gates for the unverified targets

1. Deploy a staging topology matching production regions, instance count, database tier and pool settings.
2. Replace the instance-local WebSocket signal with shared broker/pub-sub and verify reconnect/state recovery across separate application instances.
3. Run a gradual 10,000-user load profile including lobby reads, game joins, moves, card use, reconnects and disconnect storms. Record error rate, p50/p95/p99 latency, active database connections, lock waits, CPU and memory.
4. Accept the scale target only when the complete profile passes repeatedly without data-integrity failures. Accept the 99% SLO only after production monitoring supplies the defined rolling-window evidence.

No production database or deployment setting is changed by this local hardening pass.
