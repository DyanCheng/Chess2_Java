# Play surface: Arcane Chess Club

This surface implements the approved desktop/mobile match direction. It is an expression of the shared world in `DESIGN.md`, not a global composition requirement.

## Desktop and intermediate widths

The board and tactical panel sit side by side. Default columns are `minmax(0,1.05fr) minmax(360px,1fr)` with a 28px gap. At 1450px and above they become `minmax(0,1.1fr) minmax(420px,1fr)`. At 821–1190px the tactical column is 365px and the gap is 20px.

The board area uses `width:min(100%,max(380px,calc(100svh - 330px)))`, capped at 640px. The actual board uses an 8×8 grid and `aspect-ratio:1`; piece positions and drag geometry remain owned by `board.js`.

## Mobile

At 820px and below, the match stacks into one column with an 18px gap. The board and tactical panel are capped at 580px. The hand remains three equal columns.

At 540px and below, the match heading/mode tabs, redundant status line, board tip and tactical introduction are hidden. Mode selection remains in the lobby; turn text stays beside the player. The layout gap becomes 8px, board padding 8px and tactical panel padding 10px 8px. Compact player rows use 28px portraits and numeric HP.

The phone hand uses 3px card padding, an 8px gap, 11px names and 9px descriptions. Original duplicate names are hidden. Artwork uses a 1.35 aspect ratio with `background-size:300% 270%`; bottom-row effects retain bottom alignment. The armed-card instruction follows the hand with a minimum 44px height, 28px art and visible cancellation action. These compact values are specific to this surface, not the global body type scale.

## First viewport and interaction

At the approved 390×844 reference viewport, the board, current three-card hand and armed-card target instruction fit above the fixed bottom navigation. Longer inventories, journal, rules and secondary tools may scroll. Do not promise all six possible card types always fit in the first viewport.

Turn, HP and target instructions remain HTML. SVG pieces, interface icons and ornamental pseudo-elements do not intercept pointer input. Board flip reorders player strips and moves the turn label with its player row. Armed cards expose pressed state; unavailable cards are disabled; Escape cancels targeting.

## Evidence boundary

Reference: `.impeccable/review/approved-mock.png`. Actual captures: `.impeccable/review/desktop.png` and `.impeccable/review/mobile-viewport.png`. The independent review's final ship disposition covers four resolved findings: compact-phone first viewport, antique-gold trim, honest product record and shared SVG history/result symbols. The recorded suite passed 16/16; this documentation pass did not rerun it.

Engine 0.1.5 was unavailable. No engine state/spec/diff/design gate or detector result is asserted. Consult `docs/arcane-club-design-verification.md` for the current local verification record.

