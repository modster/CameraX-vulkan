# Lumina Camera App — Glassmorphism HUD Redesign Plan

## Goal
Redesign the CameraXGuide Android app (Kotlin + Jetpack Compose + CameraX 1.6.0) with a glassmorphism, tech-noir, aerospace HUD aesthetic as defined in `design-system.md`. The result should feel like a precision optical instrument — a projected heads-up display rather than a conventional mobile UI.

---

## Step 1 — Theme & Color System Migration

**Files:** `app/src/main/java/com/plcoding/cameraxguide/ui/theme/Color.kt`, `Theme.kt`

- Replace all default Material3 seed colors with the custom dark palette from `design-system.md`.
- Define named color constants matching the design token naming: `SurfaceDim`, `SurfaceBright`, `NeonCyan`, `ElectricPurple`, `SlateBlue`, etc.
- Force `darkColorScheme` only — no light theme variant required.
- Map tokens to Material3 roles:
  - `primary` → `#e1fdff` (neon cyan text/icons)
  - `primaryContainer` → `#00f2ff`
  - `secondary` → `#ebb2ff` (electric purple)
  - `background` / `surface` → `#051424`
  - `surfaceVariant` → `#273647`
  - `outline` → `#849495`
- Apply `systemBarsBehavior` + `WindowCompat.setDecorFitsSystemWindows(false)` for edge-to-edge in `MainActivity.kt`.

---

## Step 2 — Typography Setup (Space Grotesk)

**Files:** `app/src/main/java/com/plcoding/cameraxguide/ui/theme/Type.kt`, `app/build.gradle.kts`

- Add Space Grotesk via Google Fonts Downloadable Fonts API (no bundling needed for API 26+, provide fallback for API 24-25).
- Define a custom `Typography` object with these text styles matching `design-system.md` tokens:
  - `headlineLarge` — 32sp, weight 700, letterSpacing -0.02em
  - `headlineMedium` — 24sp, weight 600
  - `titleMedium` (readout-lg) — 18sp, weight 500, letterSpacing 0.05em
  - `bodyMedium` — 16sp, weight 400
  - `labelSmall` (label-caps) — 12sp, weight 700, letterSpacing 0.1em, `textTransform = uppercase`
  - `labelXSmall` — 10sp, weight 400
- Use `FontFeatureSettings("tnum")` for all numerical readout text styles to ensure tabular lining.

---

## Step 3 — Glass Panel Composable Components

**New file:** `app/src/main/java/com/plcoding/cameraxguide/ui/components/GlassComponents.kt`

Create reusable glass-morphism composables:

### `GlassSurface`
```kotlin
@Composable
fun GlassSurface(modifier: Modifier, cornerRadius: Dp = 8.dp, content: @Composable () -> Unit)
```
- Background: `Color(0x14FFFFFF)` (8% white)
- Blur effect: Use `RenderEffect` (API 31+) with fallback semi-transparent fill for older APIs
- Border: 1px stroke `Color(0x26FFFFFF)` (15% white)
- Corner radius: configurable (8dp standard, 24dp for large overlays, 4dp for chips)

### `GlassButton`
```kotlin
@Composable
fun GlassButton(onClick: () -> Unit, label: String, modifier: Modifier)
```
- Wraps `GlassSurface` with `1dp` neon cyan border (`#00dbe7`)
- Pressed state: fill opacity increases to 20%, glow radius expands (use `Animatable`)
- Text: `labelSmall` style in `primary` color

### `NeonGlow` modifier extension
```kotlin
fun Modifier.neonGlow(color: Color, radius: Dp = 10.dp, alpha: Float = 0.5f): Modifier
```
- Draws a shadow/blur halo behind the composable using `drawBehind { drawCircle(...) }` or `graphicsLayer { renderEffect }`.

### `TechnicalChip`
```kotlin
@Composable
fun TechnicalChip(label: String, value: String)
```
- Pill-shaped `GlassSurface` (cornerRadius = `full` = 9999dp)
- `label-caps` style for the label, `readout-lg` for the value
- Neon cyan text with soft glow

---

## Step 4 — Camera UI Layout Redesign (MainActivity.kt)

**File:** `app/src/main/java/com/plcoding/cameraxguide/MainActivity.kt`

- Make preview truly edge-to-edge: `CameraPreview` fills the entire screen with `fillMaxSize()`, no padding.
- Overlay all controls using `Box` with `Alignment` — no opaque bottom bars.
- **Top bar (HUD header):** Row anchored to `TopCenter` inside the Box, padded 24dp from top (accounting for status bar insets). Contains:
  - App logo / mode label
  - `TechnicalChip` row showing current ISO, shutter speed, white balance
  - Flash toggle + settings icon as `GlassButton`s
- **Right side bar:** Column anchored to `CenterEnd`, padded 24dp from right. Contains:
  - Zoom level label (`readout-lg` style)
  - Vertical `PrecisionSlider` for zoom
  - EV exposure compensation slider
- **Bottom bar:** Column anchored to `BottomCenter`. Contains:
  - `ModeSegmentedControl` (Photo / Video / Pro) directly above the shutter
  - Shutter button row: thumbnail (opens gallery) — shutter — camera flip
  - Bottom inset padding via `WindowInsets.navigationBars`
- Remove BottomSheetScaffold from the main camera view (gallery moved to separate overlay).

---

## Step 5 — Segmented Mode Selector

**New composable inside** `GlassComponents.kt` or a dedicated `ModeSelector.kt`:

```kotlin
@Composable
fun ModeSegmentedControl(
    modes: List<String>,
    selectedIndex: Int,
    onModeSelected: (Int) -> Unit
)
```
- Outer container: `GlassSurface` (cornerRadius 24dp, full-width pill shape)
- Active indicator: animated `Box` that slides under the selected label using `animateDpAsState` (move left offset)
- Active label: neon cyan with 1px bottom underline glow
- Inactive labels: `onSurfaceVariant` color
- Animation: `spring(stiffness = Spring.StiffnessMediumLow)`

---

## Step 6 — Technical Readout Chips & HUD Overlays

**New file:** `app/src/main/java/com/plcoding/cameraxguide/ui/components/HudOverlays.kt`

### `FocusCrosshair`
```kotlin
@Composable
fun FocusCrosshair(offset: Offset, visible: Boolean)
```
- Drawn via `Canvas` — four 1px neon cyan line segments forming a corner-bracket crosshair (not a full cross)
- Animated fade-in on tap, auto-fade after 2 seconds
- Size: 48dp × 48dp

### `LevelIndicator`
```kotlin
@Composable
fun LevelIndicator(rollDegrees: Float)
```
- Horizontal 1px cyan line with a center marker; line tilts using `rotate(rollDegrees)`
- Color shifts from cyan → error red when roll > 5°
- Data source: `SensorManager` (accelerometer), exposed via a new `SensorViewModel` or added to `MainViewModel`

### `Histogram` (optional / Pro mode only)
- `Canvas`-drawn bar chart using luminance data from the latest captured bitmap
- 64 buckets, rendered as 1px-wide cyan bars with 40% opacity

---

## Step 7 — Bottom Sheet Gallery Redesign

**File:** `app/src/main/java/com/plcoding/cameraxguide/PhotoBottomSheetContent.kt`

- Replace `BottomSheetScaffold` default styling with a custom `GlassSurface` container inside the sheet content.
- Sheet handle: 1px wide, 40dp long, neon cyan, centered.
- Grid items: rounded `GlassSurface` frames (8dp radius) around each thumbnail with a subtle cyan border on the selected/latest image.
- Sheet background: `Color(0xCC051424)` (80% opacity dark blue) + backdrop blur.
- "Gallery" label in `label-caps` style at top of sheet.

---

## Step 8 — Precision Slider Component

**Add to** `GlassComponents.kt`:

```kotlin
@Composable
fun PrecisionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    orientation: Orientation = Orientation.Vertical
)
```
- Track: 2px thin line, `outlineVariant` color
- Fill: neon gradient brush (Cyan → Purple) from min to current value using `Brush.linearGradient`
- Thumb: 12dp circle with inner fill and outer neon glow via `neonGlow()` modifier
- Support both vertical (zoom, EV) and horizontal (sliders in Pro mode)
- Interaction: `detectDragGestures` with debounced haptic feedback via `VibratorManager`

---

## Step 9 — Dependency Updates

**File:** `app/build.gradle.kts`

Add:
```kotlin
// Google Fonts
implementation("androidx.compose.ui:ui-text-google-fonts:1.7.x")

// Accompanist (if needed for blur < API 31)
implementation("com.google.accompanist:accompanist-placeholder-material3:0.36.0")

// Optional: Coil for async thumbnail loading in gallery
implementation("io.coil-kt:coil-compose:2.7.0")
```

---

## Step 10 — Pro Mode ViewModel Extension

**File:** `app/src/main/java/com/plcoding/cameraxguide/MainViewModel.kt`

Add state for:
- `cameraMode: StateFlow<CameraMode>` — enum: `PHOTO`, `VIDEO`, `PRO`
- `isoValue: StateFlow<Int>`, `shutterSpeed: StateFlow<Long>`, `evCompensation: StateFlow<Float>`
- `zoomRatio: StateFlow<Float>`
- Expose `setMode()`, `setISO()`, `setShutterSpeed()`, `setEV()`, `setZoom()` functions
- In `MainActivity.kt`, wire `Camera2Interop` extensions on the `ImageCapture` use case for manual ISO/shutter in Pro mode

---

## Implementation Order

1. Step 1 — Theme colors (foundation — everything depends on this)
2. Step 2 — Typography (needed before any UI work)
3. Step 3 — Glass components (shared building blocks)
4. Step 8 — Precision slider (needed for camera controls)
5. Step 4 — Camera UI layout (main screen)
6. Step 5 — Mode selector (part of camera UI)
7. Step 6 — HUD overlays (layered on top of camera UI)
8. Step 10 — ViewModel extension (Pro mode state)
9. Step 7 — Gallery redesign (secondary screen)
10. Step 9 — Dependency cleanup & verification

---

## Key Conventions to Follow

- `@OptIn(ExperimentalMaterial3Api::class)` on any composable using BottomSheet
- All blur effects must degrade gracefully on API < 31 (use semi-transparent fill fallback)
- Use `WindowInsets` APIs for edge-to-edge inset padding — no hardcoded status bar heights
- Bitmaps remain in-memory in `MainViewModel` (no disk persistence)
- `ContextCompat.getMainExecutor()` for all CameraX callbacks
- Always apply rotation matrix on captured images (`Matrix.postRotate()`)

