# Supabase connections on Vercel

Production logs on 2026-11-07 showed `EMAXCONNSESSION: max clients reached in session mode ... pool_size: 15` during simultaneous application starts. The functional smoke test passed, but some new instances failed to start.

Enable `SUPABASE_TRANSACTION_POOLING=true` on Vercel. The existing secret `SUPABASE_DB_URL` stays unchanged:

- Runtime Hikari connections use the same shared-pooler host and credentials, port 6543, and `prepareThreshold=0`. Encoded credentials are preserved byte-for-byte.
- Each runtime instance holds at most two connections, with zero minimum idle, 30-second idle timeout and two-minute maximum lifetime.
- Flyway uses its own short-lived connection through the original session URL; optionally override it with `SUPABASE_MIGRATION_DB_URL`. Migration checks and locking remain enabled.
- Local H2 and development configurations stay unchanged unless transaction pooling is explicitly enabled.
- Do not put URLs or passwords in logs. No database password rotation, data deletion or schema reset is required.

References: [Supabase connection modes](https://supabase.com/docs/guides/database/connecting-to-postgres), [Spring Boot dedicated Flyway datasource](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway).

This removes the session-mode bottleneck for runtime queries; it does not remove the project's overall database connection and compute limits.
