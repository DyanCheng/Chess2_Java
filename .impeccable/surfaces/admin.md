# Admin surface: Arcane operations room

This local-only surface extends the shared Arcane Chess Club world in Operate mode. It manages real application data through the existing Spring Boot admin APIs; it is not part of the player navigation and must not expose database secrets.

## Information architecture

`/admin/` has four focused destinations: overview, accounts, shop and system. The overview prioritizes five operating counts, current service truth and accounts that need attention. Account actions stay in an inline editor above the table so administrators retain context. Destructive account deletion, password viewing and secret editing are deliberately absent.

The portal reuses the player session storage format, but entry requires a JWT whose server-authoritative role is `ADMIN`. A restored session is rechecked through `/api/auth/me`; `/api/admin/**` remains the security boundary.

## Desktop and intermediate widths

The default shell uses a sticky 244px sidebar and a flexible workspace capped at 1100px. At 1100px and below, the sidebar becomes 210px, the five metrics wrap to three columns, the overview stacks and the three-part editor becomes two columns. Tables retain horizontal scrolling instead of compressing operational values into unreadable cards.

## Mobile

At 780px and below, navigation becomes a sticky four-item top row and the action bar sits immediately beneath it. Metrics use two columns; overview, system and editor grids stack. At 520px and below, paired fields become one column and the final odd metric spans the row. The page remains document-scrollable; no fixed player navigation or game decoration is inherited.

## Status truth and accessibility

Health information distinguishes verified current state, a failed refresh and previously fetched data. Published Actuator group names are labelled “Đã công bố”; they are never presented as successful liveness/readiness checks. Environment/profile labels are explicitly configuration. A failed refresh keeps a persistent stale badge and marks API/database state unverified until recovery.

Opening an account editor moves keyboard focus to its heading. Closing or successfully saving restores focus to the matching management button, including after the table rerenders. Buttons and fields retain visible focus, labels and reduced-motion behavior from the shared system.

## Evidence boundary

Primary captures: `.impeccable/review/admin-desktop.png` and `admin-mobile.png`. Supporting captures: `admin-account-editor.png` and `admin-stale-health.png`. The final manual review disposition is **ship**, scoped to the separate portal and its three reviewed operational fixes. Maven passed 18/18 tests and Playwright passed 20/20 desktop/mobile tests. Detector output was unavailable after host truncation; no automated visual fidelity score is asserted.
