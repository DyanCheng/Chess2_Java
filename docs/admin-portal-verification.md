# Local admin portal verification — 2026-11-13

## Scope

This record covers the separate `/admin/` portal, local demo administrator seed, backend authorization and the account/shop/system controls. It does not certify a production deployment or the entire player surface.

## Verified behavior

- Demo login `admin` / `Admin@123456` returned player role `ADMIN`; its JWT accessed `/api/admin/overview` successfully.
- `/admin/` and its static assets load without authentication, while `/api/admin/**` rejects anonymous and ordinary player requests.
- Administrators can inspect metrics, search/filter users, update role, Gold and Gem, grant cards, and update shop price/availability through existing APIs.
- The status view transitions from healthy to persistently stale/unverified after a health request failure, then returns to healthy after recovery.
- The inline editor receives focus on open and restores it to the corresponding management control on close/save after rerender.
- Desktop and mobile captures preserve the Arcane Chess Club palette while keeping an operational, separate information architecture.

## Automated and visual evidence

- `mvn -f apps/server/pom.xml "-Djava.version=17" test`: **18 passed**.
- `npm run test:e2e`: **20 passed** across Desktop Chrome and Pixel 7 emulation.
- Capture-only Playwright run: **2 passed**; the temporary capture spec was removed afterward.
- Captures: `.impeccable/review/admin-desktop.png`, `admin-mobile.png`, `admin-account-editor.png`, `admin-stale-health.png`.
- Independent Impeccable finish review: **ship** after stale-status truth, group labelling and keyboard focus fixes were resolved.

The mechanical detector was run once, but the host truncated its output and it produced no report file. No detector score or whole-surface automated visual certification is claimed.

## Local-only boundary

No commit, push, deployment or production database mutation was performed. The demo credential exists only in `application-demo.yml`; production admin seeding remains controlled by environment variables.
