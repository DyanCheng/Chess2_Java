# Admin surface: Arcane operations room

This local-only surface extends the shared Arcane Chess Club world in Operate mode. It manages real application data through the existing Spring Boot admin APIs; it is not part of the player navigation and must not expose database secrets.

## Information architecture

`/admin/` has five focused destinations: overview, accounts, shop, activity log and system. The overview prioritizes six operating counts, current service truth and accounts that need attention. Account actions stay in an inline editor above the table so administrators retain context. Locking requires a short operational reason, unlocking is explicit, and the signed-in administrator cannot lock their own account. Destructive account deletion, password viewing and secret editing are deliberately absent.

The portal reuses the player session storage format, but entry requires a JWT whose server-authoritative role is `ADMIN`. A restored session is rechecked through `/api/auth/me`; `/api/admin/**` remains the security boundary.

## Desktop and intermediate widths

The default shell uses a sticky 244px sidebar and a flexible workspace capped at 1100px. At 1100px and below, the sidebar becomes 210px, the six metrics wrap to three columns, the overview stacks and the account editor becomes two columns. Tables retain horizontal scrolling instead of compressing operational values into unreadable cards.

## Mobile

At 780px and below, navigation becomes a sticky five-item top row and the action bar sits immediately beneath it. Metrics use two columns; overview, system and editor grids stack. At 520px and below, paired fields become one column. The final result/action table column remains sticky so essential controls and outcomes stay visible while wide operational tables scroll. The page remains document-scrollable; no fixed player navigation or game decoration is inherited.

## Status truth and accessibility

Health information distinguishes verified current state, a failed refresh and previously fetched data. Published Actuator group names are labelled “Đã công bố”; they are never presented as successful liveness/readiness checks. Environment/profile labels are explicitly configuration. A failed refresh keeps a persistent stale badge and marks API/database state unverified until recovery.

Opening an account editor moves keyboard focus to its heading. Closing or successfully saving restores focus to the matching management button, including after the table rerenders. Buttons and fields retain visible focus, labels and reduced-motion behavior from the shared system.

The activity log supports text, category and result filters. It shows server time, actor, action, route and HTTP outcome without exposing authentication material, request contents or network fingerprints. A selected account also shows its recent activity inside the editor so access decisions keep their immediate context.

## Evidence boundary

Primary captures remain `.impeccable/review/admin-desktop.png` and `admin-mobile.png`; the latest Playwright admin scenario additionally captures the responsive activity log. The final manual review disposition is **ship**, scoped to the separate portal and its operational controls. Maven passed 26/26 tests and Playwright passed 26/26 desktop/mobile tests. Detector output was unavailable after host truncation; no automated visual fidelity score is asserted.
