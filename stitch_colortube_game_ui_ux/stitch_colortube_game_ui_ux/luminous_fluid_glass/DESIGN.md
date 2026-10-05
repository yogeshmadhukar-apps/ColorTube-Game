---
name: Luminous Fluid & Glass
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#3b4949'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#6b7a7a'
  outline-variant: '#bacac9'
  surface-tint: '#006a6a'
  primary: '#006a6a'
  on-primary: '#ffffff'
  primary-container: '#00d2d3'
  on-primary-container: '#005556'
  inverse-primary: '#26dcdd'
  secondary: '#b9082c'
  on-secondary: '#ffffff'
  secondary-container: '#dd2d42'
  on-secondary-container: '#fffbff'
  tertiary: '#855400'
  on-tertiary: '#ffffff'
  tertiary-container: '#ffac32'
  on-tertiary-container: '#6c4300'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#56f9f9'
  primary-fixed-dim: '#26dcdd'
  on-primary-fixed: '#002020'
  on-primary-fixed-variant: '#004f50'
  secondary-fixed: '#ffdad9'
  secondary-fixed-dim: '#ffb3b2'
  on-secondary-fixed: '#410008'
  on-secondary-fixed-variant: '#920020'
  tertiary-fixed: '#ffddb7'
  tertiary-fixed-dim: '#ffb95d'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: '0'
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies a serene yet tactile mobile gaming experience—merging the architectural calm of ambient puzzle games with vibrant, tactile fluid mechanics. It is designed for casual players seeking mindful relaxation paired with instant kinetic delight. The aesthetic is clean, elevated, and unapologetically modern, rejecting chaotic arcade motifs in favor of frosted crystal, soft lighting, and high-clarity typography.

### Design Principles
- **Tactile Transparency:** Containers emulate heavy, polished laboratory glass with subtle caustic specular rim lights and frosted background refraction.
- **Vibrant Kinetic Accents:** The neutral canvas steps back, allowing hyper-saturated fluid hues and reward gold to command full interactive focus.
- **Soft Geometry:** Extreme rounded corners, sweeping pill contours, and organic forms invoke a sense of calm, approachability, and premium finish.
- **Clutterless Precision:** Menus, HUD indicators, and control elements maintain an airy, low-friction presence, ensuring gameplay remains the undisputed hero.

## Colors

The core color palette relies on an ultra-clean, neutral stage that lets prismatic liquid tokens shine with optical clarity. High-contrast typography in deep slate ensures effortless readability against soft off-white canvas tones, while vibrant game tokens deliver immediate perceptual categorization.

### Palette Architecture
- **Primary Accent (`#00D2D3`):** Radiant Cyan; used for focal interactive states, primary game tube selections, level progress indicators, and luminous glow highlights.
- **Secondary Accent (`#FF4757`):** Neon Coral; used for urgent game states, undo alerts, and warm liquid elements.
- **Tertiary Accent (`#FFA502`):** Sunburst Amber; used for dynamic energy indicators, coin counters, and golden reward surfaces alongside `#FFC312` and `#F79F1F`.
- **Neutral Baseline (`#0F172A`):** Deep Navy/Slate; defines dominant typography and sharp iconography, complemented by softened secondary text tones (`#475569`).
- **Canvas & Surface:** Light canvas is anchored on `#F8FAFC` and `#F1F5F9`, providing a porcelain-smooth backdrop. Dark theme transitions the foundation to `#0B0F19` with elevated card layers at `#131B2E`.

### Liquid Game Tokens
- Radiant Cyan: `#00D2D3`
- Neon Coral: `#FF4757`
- Electric Lime: `#2ED573`
- Royal Violet: `#5352ED`
- Rose Quartz: `#FF6B81`
- Sunburst Amber: `#FFA502`

## Typography

The type system balances structured geometric charisma with neutral utility. Plus Jakarta Sans drives headlines, numeric level designations, score counters, and badges, projecting a buoyant, sculpted quality. Inter serves all functional guidance, system messages, and secondary metadata, ensuring legible scanning on high-density mobile screens.

### Numerical Treatments
Level numbers and score counters use Plus Jakarta Sans in ExtraBold (`800`) with proportional tabular figures enabled. This guarantees smooth numeric transitions without horizontal jitter during coin tallies or level-clear animations.

## Layout & Spacing

Designed fundamentally for portrait mobile displays (9:16 aspect ratio), the layout model enforces strict thumb-friendly safe zones. Interactive touch targets are clustered in the lower 45% of the viewport, while game canvas elements (the tubes) claim the vertical center. Non-interactive status readouts and pause controls anchor the top HUD band.

### Spatial Rhythm
- **Grid Structure:** A 4-column dynamic portrait grid with `1rem` gutters and `1.25rem` outer canvas padding.
- **Tube Stage:** A flexible, centered stage adapting dynamically from 3 to 6 tubes horizontally, maintaining a minimum `0.75rem` touch separation between vessel hitboxes.
- **Vertical Hierarchy:**
  - Header HUD: 56px height, containing profile, level title, and currency pill.
  - Interactive Action Row: Positioned 32px above the bottom screen home indicator, housing Undo, Reset, and Hint action triggers.

## Elevation & Depth

Depth is established through translucent physical glassmorphism rather than muddy drop shadows. Surfaces feel like polished laboratory glass cylinders suspended above a warm, ambient canvas.

### Surface Tiers
- **Tier 0 (Canvas):** Smooth ambient background (`#F8FAFC`) carrying a subtle diagonal radial sheen.
- **Tier 1 (Glass Containers & Tube Barrels):** Translucent backdrop blur (`backdrop-filter: blur(16px)`), filled with `rgba(255, 255, 255, 0.65)` in light mode, bounded by a 1.5px semi-transparent specular rim stroke (`rgba(255, 255, 255, 0.8)` on top, fading to `rgba(15, 23, 42, 0.08)` on bottom).
- **Tier 2 (Floating Overlays & Dialogs):** Deep frosted shield (`rgba(255, 255, 255, 0.85)`) layered over a soft ambient shadow: `0 20px 40px -15px rgba(15, 23, 42, 0.12)`.
- **Tier 3 (Tactile Primary CTAs):** 3D-extruded volumetric buttons utilizing vibrant gradients (Radiant Cyan into Deep Teal) paired with a solid bottom tactile rim (`box-shadow: 0 6px 0 #0097A7, 0 12px 20px rgba(0, 210, 211, 0.35)`). On press, elements depress vertically by 4px with corresponding shadow reduction.

## Shapes

The design system embraces an extreme pill-shaped philosophy (Level 3), eliminating sharp corners to reinforce safety, fluidity, and playfulness.

### Geometry Guidelines
- **Tubes & Vials:** The base of every game tube is fully rounded into a seamless semi-circular bowl (`border-bottom-left-radius: 9999px`, `border-bottom-right-radius: 9999px`), with a gently flared, polished lip at the rim.
- **Buttons & Chips:** Always rendered as full pills (`rounded-full`), providing a natural finger-pad target.
- **Cards & Modals:** Large structural containers leverage `rounded-xl` (2.5rem / 40px) to maintain continuous curvature consistent with handheld hardware corners.

## Components

### Buttons & CTAs
- **Primary CTA:** Full pill button featuring a gradient fill from `#00D2D3` to `#00A8A8`. Features bold white Plus Jakarta Sans text, an inner top highlight border (`rgba(255, 255, 255, 0.5)`), and a physical 3D drop-base (`4px` offset in `#008B8B`).
- **Secondary / Utility Buttons:** Circular frosted glass buttons (48x48px minimum touch target) for Undo, Restart, and Add Tube actions. Glass backdrop with deep slate iconography (`#1E293B`).
- **Reward Buttons:** Gradient of Sunburst Amber (`#FFA502`) to Golden Honey (`#F79F1F`) with a tactile bottom ledge (`#D68000`).

### Badges & Currency Chips
- **HUD Level Badge:** Pill-shaped frosted capsule with a hairline border (`1px solid rgba(255, 255, 255, 0.8)`). Displays level status in slate typography with a mini cyan dot indicator.
- **Currency Counter:** Pill chip containing a 3D gold coin glyph on the leading edge, tabular numerical balance in `label-lg`, and an elevated mini plus icon on the trailing edge.

### Game Tubes (Vessels)
- **Construction:** Double-layer CSS/Canvas architecture. The background layer holds the frosted inner refraction, the mid layer holds colored fluid blocks with organic liquid surface tension curves, and the foreground layer applies vertical glossy glass highlights and caustic reflections.
- **Active State:** Selected tubes lift upward by `12px` via smooth spring easing, accompanied by a subtle Radiant Cyan outer ambient bloom (`box-shadow: 0 0 24px rgba(0, 210, 211, 0.45)`).

### Dialogs & Modal Sheets
- **Victory Modal:** Centered floating sheet (`rounded-xl` / 32px padding) using frosted high-opacity glass, crowned with a Sunburst Amber starburst burst, big numerical bonus counters, and stacked pill action buttons.
- **Pause & Settings:** Bottom-anchored sheet featuring large touch-toggle switches with pill tracks and floating circular thumbs.

### Inputs & Toggles
- **Toggles (Haptics, Sound):** Pill track (`52px x 30px`) in soft slate gray (`#E2E8F0`), transitioning to Radiant Cyan (`#00D2D3`) when active. Pure white elevated thumb with a micro-drop shadow.