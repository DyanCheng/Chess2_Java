# Play surface: Arcane Chess Club

This surface implements the approved desktop/mobile match direction. It is an expression of the shared world in `DESIGN.md`, not a global composition requirement.

## Desktop and intermediate widths

The board is the primary task surface and the tactical rail is a compact supporting tool. Default columns are `minmax(0,1.55fr) minmax(300px,.65fr)` with a 20px gap. At 1450px and above the tactical column is fixed at 340px. At 821–1190px it is 280px with a 16px gap. Mode selection exists only in the lobby; the match header contains the match identity, live status and one leave/cancel action.

The board area uses `width:min(100%,max(480px,calc(100svh - 200px)))`, capped at 720px. The actual board uses an 8×8 grid and `aspect-ratio:1`; piece positions and drag geometry remain owned by `board.js`.

Desktop cards are compact horizontal rows with a 58px illustration, name, single-line effect and count. The journal and rules are disclosure sections closed by default. Flip, sound and resign form one toolbar below the board. The rail has a quiet boundary rather than the same ornamental weight as the board.

## PvP rooms

`WAITING` is a separate state, not a disabled match board. It shows one centered room card with the six-digit number, owner/ELO, five-minute countdown and copy action; the board, empty Deck, journal and rules are hidden until a second player joins. The raw link field is not shown because the copy action and room code already carry the task. The lobby list identifies the creator by username and ELO, and both player strips show real usernames and ratings after joining. A waiting room is no longer joinable after its deadline. Status, code and recovery text use words rather than color alone.

## Mobile

At 820px and below, the match stacks into one column with an 18px gap. The board and tactical panel are capped at 580px. The hand remains three equal columns. The global bottom navigation is hidden while the play route is active; a compact match header keeps the explicit leave/cancel action available.

At 540px and below, the match heading/mode tabs, redundant status line, board tip and tactical introduction are hidden. Mode selection remains in the lobby; turn text stays beside the player. The layout gap becomes 8px, board padding 8px and tactical panel padding 10px 8px. Compact player rows use 28px portraits and numeric HP.

The phone hand uses 3px card padding, an 8px gap and 11px names. Original names and repeated descriptions are hidden at 540px and below; the targeting instruction still explains the armed card. Artwork uses a compact 1.7 aspect ratio with `background-size:300% 230%`; bottom-row effects retain bottom alignment. The armed-card instruction follows the hand with a minimum 44px height, 28px art and visible cancellation action. These compact values are specific to this surface, not the global body type scale.

## First viewport and interaction

At the approved 390×844 reference viewport, the board, current three-card hand and armed-card target instruction fit above the fixed bottom navigation. Longer inventories, journal, rules and secondary tools may scroll. Do not promise all six possible card types always fit in the first viewport.

Turn, HP and target instructions remain HTML. SVG pieces, interface icons and ornamental pseudo-elements do not intercept pointer input. Board flip reorders player strips and moves the turn label with its player row. Armed cards expose pressed state; unavailable cards are disabled; Escape cancels targeting.

## Evidence boundary

Reference: `.impeccable/review/approved-mock.png`. Actual captures: `.impeccable/review/desktop.png` and `.impeccable/review/mobile-viewport.png`. The independent review's final ship disposition covers four resolved findings: compact-phone first viewport, antique-gold trim, honest product record and shared SVG history/result symbols. The recorded suite passed 16/16; this documentation pass did not rerun it.

Engine 0.1.5 was unavailable. No engine state/spec/diff/design gate or detector result is asserted. Consult `docs/arcane-club-design-verification.md` for the current local verification record.

