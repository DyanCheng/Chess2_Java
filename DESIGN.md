---
name: Arcane Chess Club
description: Vietnamese-first arcade chess in forest felt, parchment and antique gold.
colors:
  bg: "#171f21"
  bg-deep: "#101a19"
  surface: "#1a2d28"
  surface-raised: "#243b32"
  surface-hover: "#30463b"
  line: "#4d5a42"
  line-strong: "#8d835d"
  text: "#f2e8cb"
  text-muted: "#b7c0ae"
  brand: "#bbd99a"
  brand-strong: "#8dab70"
  gold: "#d7b779"
  danger: "#ed9b8b"
  board-light: "#ebdfbb"
  board-dark: "#8e9f7d"
  card-paper: "#e8dbb7"
  card-ink: "#243527"
typography:
  brand:
    fontFamily: "Silkscreen, sans-serif"
    fontSize: "25px"
    letterSpacing: "-.03em"
  headline:
    fontFamily: '"Be Vietnam Pro", "Segoe UI", sans-serif'
    fontSize: "30px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-.03em"
  title:
    fontFamily: '"Be Vietnam Pro", "Segoe UI", sans-serif'
    fontSize: "23px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "-.025em"
  body:
    fontFamily: '"Be Vietnam Pro", "Segoe UI", sans-serif'
    fontSize: "14px"
    lineHeight: 1.55
  card-title:
    fontFamily: '"Be Vietnam Pro", "Segoe UI", sans-serif'
    fontSize: "13px"
    fontWeight: 700
    lineHeight: 1.35
rounded:
  board: "3px"
  badge: "5px"
  control: "7px"
  field: "8px"
  card: "9px"
  panel: "12px"
spacing:
  small: "8px"
  medium: "12px"
  comfortable: "16px"
  panel: "20px"
  section: "24px"
  column: "28px"
components:
  button-primary:
    backgroundColor: "{colors.brand}"
    textColor: "#17291c"
    rounded: "{rounded.control}"
    padding: "0 16px"
  button-secondary:
    backgroundColor: "#1d3229"
    textColor: "{colors.text}"
    rounded: "{rounded.control}"
    padding: "0 16px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text-muted}"
    rounded: "{rounded.control}"
    padding: "0 16px"
  button-danger:
    backgroundColor: "#422a24"
    textColor: "#ffccba"
    rounded: "{rounded.control}"
    padding: "0 16px"
  input:
    backgroundColor: "{colors.bg-deep}"
    textColor: "{colors.text}"
    rounded: "{rounded.field}"
    padding: "0 11px"
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.panel}"
    padding: "22px"
  result-badge:
    backgroundColor: "#b7d49b0c"
    textColor: "{colors.brand}"
    rounded: "{rounded.badge}"
    padding: "5px 8px"
  navigation:
    textColor: "{colors.text}"
    rounded: "{rounded.control}"
    padding: "9px 12px"
  tactical-card:
    backgroundColor: "{colors.card-paper}"
    textColor: "{colors.card-ink}"
    rounded: "{rounded.card}"
    padding: "5px"
---

# Design System: Arcane Chess Club

## Overview

**Creative North Star: "Arcane Chess Club"**

An intimate arcade chess club built from midnight ink, forest felt, warm parchment and antique gold. Illustrated spell cards and restrained pixel ornament give the game its character; Vietnamese-readable text and a square, legible board keep the interface practical.

This is the implemented visual world selected through the user-approved desktop/mobile mock-up on 2026-11-07. It describes local source, not a production deployment. The game is an HP-and-card arcade variant; visual language must not imply unavailable tournament features.

**Key Characteristics:**

- Forest surfaces with parchment text and sage actions.
- Illustrated tactical cards with live HTML labels.
- Antique-gold transparent frame trim and locally authored SVG icons.
- Readable Vietnamese body type with a pixel wordmark.
- Compact responsive controls and explicit turn, HP and targeting states.

Source authority is the cascade `app.css → arcade.css → club.css` under `apps/web/assets/css/`, with `experience.js`, `visuals.js` and `render.js` under the adjacent `js/` directory. Frontmatter records reused implemented values; it is not a new CSS theme file. The sidecar holds extension metadata. Approved and actual captures live in `.impeccable/review/`; verification scope is recorded in `docs/arcane-club-design-verification.md`.

## Colors

The palette pairs quiet forest surfaces with warm paper and restrained metallic ornament.

### Primary

- **Sage action** (`brand`): primary buttons, active navigation, player-turn emphasis and friendly HP.
- **Deep sage** (`brand-strong`): inherited supporting identity treatment.

### Secondary

- **Antique gold** (`gold`): wordmark, frame edges, journal headings, links and focus outlines.
- **Parchment card / forest ink** (`card-paper`, `card-ink`): illustrated cards and their readable labels.

### Tertiary

- **Warm coral** (`danger`): form/error emphasis. Destructive buttons use their own darker component variant.
- Spell accents are effect-specific metadata in `experience.js`; they do not replace the global sage action color.

### Neutral

- **Midnight ink** (`bg`, `bg-deep`): page canvas and recessed fields.
- **Forest felt** (`surface`, `surface-raised`, `surface-hover`): containers and interactive surface states.
- **Moss / bronze lines** (`line`, `line-strong`): quiet dividers and control boundaries.
- **Warm parchment / muted sage text** (`text`, `text-muted`): main content and supporting copy.
- **Parchment / sage squares** (`board-light`, `board-dark`): the alternating board.

**The State Has Words Rule.** Pair state color with a label, numeric HP, selection outline or target instruction; color alone must not carry the game state.

Sidecar tonal ramps are synthesized preview metadata, not implemented CSS tokens or new approved palette choices.

## Typography

**Brand Font:** self-hosted Silkscreen, with sans-serif fallback; the supplied font file is weight 700. The inherited brand declaration requests 850, so the static supplied face is selected by browser matching. That request is not a recommended new font weight.

**Body Font:** self-hosted Be Vietnam Pro, with Segoe UI and sans-serif fallbacks; supplied files are 400, 600 and 700. Font licenses accompany the files in `assets/fonts/`.

The pixel voice belongs to the wordmark. Headings, instructions, navigation and card descriptions use Be Vietnam Pro so Vietnamese accents remain legible.

### Hierarchy

- **Headline:** the frontmatter role is the general page heading; lobby and authentication headlines are larger contextual compositions.
- **Title:** game panel heading; ordinary section headings inherit a nearby 24px step.
- **Body:** default content rhythm, with compact supporting copy at 12–13px.
- **Card title:** bold card names, separate from effect descriptions.
- **Brand:** scales to 23px at 820px and 21px at 540px.

The implementation has inherited synthetic/intermediate weight requests on some controls. Do not turn those requests into additional self-hosted font assets or a new weight scale.

**The Readable Spell Rule.** Keep card names, effect descriptions, counts and actions in HTML; artwork contains no UI text.

## Layout

The shared desktop shell is centered, capped at 1360px, with 24px side clearance and 24px top / 48px bottom padding. Repeated gaps use the frontmatter spacing vocabulary; individual dense controls use smaller local adjustments.

At 821–1190px the navigation becomes an in-flow row with icons. At 820px and below it becomes a fixed six-item bottom navigation with safe-area padding; the shell becomes `calc(100% - 28px)`, capped at 680px, with 100px bottom clearance. At 540px and below the shell becomes `calc(100% - 20px)` and the header reduces to 52px. Authentication, mode cards and paired content collapse to one column at 820px.

The match composition, exact board sizing and compact-phone hand treatment are surface decisions recorded in `.impeccable/surfaces/play.md`. Do not apply the match's hidden phone heading or three-column hand layout to unrelated screens.

## Elevation & Depth

Most containers and buttons are flat, differentiated by forest tones and thin boundaries. Soft shadows lift collectible cards and floating feedback; SVG chess pieces use subtle drop shadows. This is layered material, not a hard-offset shadow system.

### Shadow Vocabulary

- **Ambient feedback:** `0 14px 32px #0003`, the inherited `--shadow` used by toast feedback.
- **Card at rest:** `0 6px 14px #0003`.
- **Card hover:** `0 12px 20px #0004`.
- **Card burst:** `0 20px 45px #0006`.
- **Board piece:** `drop-shadow(0 2px 1px #0003)`; dragging increases the lift.

Motion describes actions: buttons transition over 160ms; card hover over 180ms; HP width over 350ms using `cubic-bezier(.16,1,.3,1)`. Page entry is a 160ms opacity change. The lobby knight floats over 6s; the thinking dot pulses over 1s; successful card use may show a 1000ms burst.

Reduced-motion preference disables continuous/decorative animations, reduces CSS animation and transition duration to .01ms, uses immediate scrolling and skips the JavaScript card burst. Preserve these CSS and JavaScript checks together.

## Shapes

Rounded forest panels, gently rounded controls and parchment cards provide the base silhouette. The square board stays geometrically independent of piece art. Small icons are outlined SVG with rounded caps/joins; chess pieces retain the existing SVG set.

`assets/art/arcane-frame.png` supplies genuine transparent antique-gold ornament. CSS nine-slices it at 16%, using an 18px border image for board, game and profile containers, and 12px for cards. Decoration uses `pointer-events:none`, sits in a pseudo-element and never changes the board's interactive geometry. The source uses a restrained sepia filter.

The frame and card atlas are separate generated production assets. Exact prompts and source references belong in `assets/art/PROVENANCE.md` and PNG iTXt metadata; do not treat the approved UI mock-up as a production image crop.

## Components

### Buttons

Primary sage actions, dark secondary buttons, quiet ghost buttons and warm destructive buttons share the control radius and compact horizontal padding. General buttons have a 44px minimum height; explicit compact variants and contextual actions can differ. Hover raises ordinary buttons by 1px, active press lowers by 1px, and focus-visible uses a gold 3px outline with 3px offset. Disabled states reduce opacity; action availability remains controlled by existing state.

### Inputs / Fields

Dark recessed fields have bronze boundaries, an 8px radius, 43px minimum height and gold caret. Focus changes the border to sage and adds the inherited translucent mint halo. Keep visible labels or an accessible name. Error text uses the warm coral role.

### Cards / Containers

Forest panels use thin moss borders and a 12px radius without a general shadow. Tactical cards use paper, dark text, effect-colored borders and the dedicated soft card shadow. Armed cards have a pale-blue 2px outline with a 3px offset and expose `aria-pressed`; disabled cards remain visibly unavailable.

The six-tile atlas `assets/art/arcane-cards.png` uses three columns and two rows with `background-size:300% 200%`: Heal, Freeze, Double Damage / Invulnerable, Extra Queen, Surrender. The same art is reused in the lobby, collection and hand. Phone hand cropping is surface-specific.

### Navigation

Desktop navigation is a centered header row; intermediate widths expose icons in flow; mobile uses the fixed bottom row described above. Active routes use sage text and a translucent sage surface; desktop adds a fine inset border. SVG icons come from `visuals.js`, not icon fonts or Unicode substitutes.

### Status and Feedback

HP combines a bar and numeric label. Turn status uses text and optional thinking animation. Card targeting includes an inline instruction and cancellation action. Result badges use muted sage; match-result and history symbols use the shared SVG set. The journal describes session moves and does not imply an analysis engine.

## Do's and Don'ts

### Do:

- **Do** preserve the forest, parchment, sage and antique-gold material relationship.
- **Do** keep Vietnamese text in Be Vietnam Pro and reserve Silkscreen for the wordmark.
- **Do** reuse the atlas, transparent nine-slice trim and shared SVG icon paths.
- **Do** keep numeric HP, turn text, target instructions and visible keyboard focus.
- **Do** respect reduced motion in CSS and JavaScript.

### Don't:

- **Don't** bake card text or controls into artwork.
- **Don't** let ornament capture input or alter square board geometry.
- **Don't** replace shared interface SVGs with glyph icons.
- **Don't** promote phone match density into a global type or layout rule.
- **Don't** imply unavailable game features or automated design-validation results.

