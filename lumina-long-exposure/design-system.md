---
name: The Design System
colors:
  surface: '#051424'
  surface-dim: '#051424'
  surface-bright: '#2c3a4c'
  surface-container-lowest: '#010f1f'
  surface-container-low: '#0d1c2d'
  surface-container: '#122131'
  surface-container-high: '#1c2b3c'
  surface-container-highest: '#273647'
  on-surface: '#d4e4fa'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#d4e4fa'
  inverse-on-surface: '#233143'
  outline: '#849495'
  outline-variant: '#3a494b'
  surface-tint: '#00dbe7'
  primary: '#e1fdff'
  on-primary: '#00363a'
  primary-container: '#00f2ff'
  on-primary-container: '#006a71'
  inverse-primary: '#00696f'
  secondary: '#ebb2ff'
  on-secondary: '#520071'
  secondary-container: '#ce5dff'
  on-secondary-container: '#480064'
  tertiary: '#f7f6ff'
  on-tertiary: '#283044'
  tertiary-container: '#d3daf5'
  on-tertiary-container: '#575f75'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#74f5ff'
  primary-fixed-dim: '#00dbe7'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#f8d8ff'
  secondary-fixed-dim: '#ebb2ff'
  on-secondary-fixed: '#320047'
  on-secondary-fixed-variant: '#74009f'
  tertiary-fixed: '#dae2fd'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465c'
  background: '#051424'
  on-background: '#d4e4fa'
  surface-variant: '#273647'
typography:
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  readout-lg:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '500'
    lineHeight: '1'
    letterSpacing: 0.05em
  body-md:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-caps:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '700'
    lineHeight: '1'
    letterSpacing: 0.1em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 10px
    fontWeight: '400'
    lineHeight: '1'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  gutter: 16px
  margin-edge: 24px
  panel-padding: 12px
---

## Brand & Style

This design system is engineered to evoke the feeling of high-precision aerospace instrumentation and advanced optical technology. The brand personality is technical, expert-oriented, and immersive, targeting professional creators and tech enthusiasts who demand granular control over their imagery.

The aesthetic utilizes a sophisticated **Glassmorphism** style layered over a deep-space environment. It blends a "Tech-Noir" atmosphere with high-visibility neon accents. Visual depth is achieved through translucent panels and subtle background grid patterns that suggest an underlying digital architecture, creating an interface that feels like a head-up display (HUD) rather than a traditional mobile app.

## Colors

The palette is anchored by a deep, dark blue foundation that provides maximum contrast for the camera's viewfinder. 

*   **Primary (Neon Cyan):** Used for active states, critical focus indicators, and primary action buttons. It represents precision and clarity.
*   **Secondary (Electric Purple):** Used for creative modes, secondary adjustments, and decorative flourishes to add depth and a futuristic edge.
*   **Tertiary (Slate Blue):** Used for glass panel backgrounds and low-priority containers to distinguish UI layers from the pitch-black background.
*   **Glows:** Neon colors should employ a soft outer glow (0 0 10px) with 40-60% opacity to simulate light emission on the glass surfaces.

## Typography

The typography leverages **Space Grotesk** to achieve a clean, geometric, and cutting-edge look. The design system treats text as technical data. 

Headlines are bold and tight to convey authority. For camera readouts (ISO, Shutter, Aperture), use the "readout-lg" style with increased letter spacing to mimic monospaced hardware displays. Labels should frequently use uppercase transforms and wide tracking to maintain a technical "blueprint" aesthetic. All numerical data should be presented with tabular lining where possible to ensure alignment during real-time value changes.

## Layout & Spacing

The layout follows a **fluid grid** model optimized for edge-to-edge camera previews. Critical controls are anchored to the corners and sides to keep the center of the frame clear.

A 4px base unit governs all spatial relationships. UI panels use a 16px gutter between elements to maintain a technical, "exploded view" feel. Sidebars and control clusters are padded 24px from the screen edge to account for thumb ergonomics and device bezels. Subtle, low-opacity grid lines (10% opacity Neon Cyan) should be overlaid behind menus to reinforce the high-tech structural theme.

## Elevation & Depth

Elevation in this design system is conveyed through **Glassmorphism** and light emission rather than traditional shadows.

1.  **Base Layer:** The camera viewfinder or the deep dark blue background.
2.  **Middle Layer:** Glass panels with a 20px-40px backdrop blur and a semi-transparent fill (8% white or tertiary blue). These panels feature a 1px inner stroke in white (15% opacity) to define edges.
3.  **Top Layer:** Floating controls and active indicators. These elements emit a neon glow, creating a "Z-axis" depth that feels like the interface is projected toward the user.

Shadows are replaced by tinted "ambient glows" beneath active panels, using the primary or secondary color at 5% opacity with a large (40px) blur.

## Shapes

The design system uses a **Rounded** shape language to balance the aggressive technicality of the typography with a modern, premium feel. 

Standard UI panels and buttons use a 0.5rem (8px) corner radius. Larger container overlays use 1.5rem (24px) to create a softer frame around the camera content. Small indicators (like focus points or active chips) may use a "soft" 0.25rem radius for a precision-tool look. The interaction of rounded glass panels against the rigid grid pattern creates the distinct high-tech aesthetic.

## Components

*   **Glass Buttons:** Semi-transparent containers with 1px neon borders. On press, the background fill opacity increases, and the neon glow intensifies.
*   **Technical Chips:** Small, pill-shaped or slightly rounded readouts for metadata (e.g., "4K", "60FPS"). Use the `label-caps` typography style.
*   **Precision Sliders:** Thin tracks (2px) with a glowing circular thumb. The track "fills" with a neon gradient from Cyan to Purple as it increases.
*   **Segmented Controls:** Used for mode switching (Photo, Video, Pro). Active states are indicated by a neon underline or a glass-morphed "floating" background selector.
*   **HUD Overlays:** Crosshairs, histograms, and level indicators should use ultra-thin 1px lines in Neon Cyan with no fills to maximize visibility of the image behind them.
*   **Expansion Panels:** Vertically stacking glass drawers for advanced settings like White Balance or Focus Peaking, using subtle grid separators between rows.
