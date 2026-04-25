# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-24
**Commit:** 72cb7d4
**Branch:** master

## OVERVIEW
Android camera app using CameraX and Jetpack Compose. Capture photos/videos with front/back camera switch. Material3 UI with bottom sheet gallery.

## STACK
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material3)
- **Camera:** CameraX 1.6.0
- **Architecture:** ViewModel + StateFlow
- **Min SDK:** 24 | **Target SDK:** 36

## STRUCTURE
```
.
├── app/
│   ├── build.gradle.kts          # App-level dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/plcoding/cameraxguide/
│       │   │   ├── MainActivity.kt       # Entry point, camera UI
│       │   │   ├── MainViewModel.kt      # Photo state management
│       │   │   ├── CameraPreview.kt      # CameraX PreviewView wrapper
│       │   │   ├── PhotoBottomSheetContent.kt  # Gallery grid
│       │   │   └── ui/theme/             # Compose theme (Color, Type, Theme)
│       │   ├── res/                      # Android resources
│       │   └── AndroidManifest.xml       # Permissions: CAMERA, RECORD_AUDIO
│       ├── test/                         # Unit tests
│       └── androidTest/                  # Instrumented tests
├── build.gradle.kts              # Project-level config
├── settings.gradle.kts           # Project name: CameraXGuide
└── lumina-long-exposure/         # Design references (HTML + screenshots), not part of Android runtime module
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Camera capture logic | `MainActivity.kt:153-185` | `takePhoto()` with rotation matrix |
| Camera preview setup | `CameraPreview.kt` | LifecycleCameraController binding |
| Photo gallery | `PhotoBottomSheetContent.kt` | LazyVerticalStaggeredGrid |
| State management | `MainViewModel.kt` | MutableStateFlow<List<Bitmap>> |
| Theme customization | `ui/theme/` | Material3 colors, typography |
| Permissions | `MainActivity.kt:187-194`, `AndroidManifest.xml` | CAMERA + RECORD_AUDIO |

## CONVENTIONS
- `OptIn(ExperimentalMaterial3Api::class)` required for BottomSheetScaffold
- CameraX permissions requested at runtime (not in manifest only)
- Manifest declares `android.hardware.camera` as optional (`android:required="false"`)
- Bitmaps stored in-memory via ViewModel StateFlow (no disk persistence)
- Use `ContextCompat.getMainExecutor()` for CameraX callbacks

## ANTI-PATTERNS (THIS PROJECT)
- **DO NOT** forget rotation matrix on captured images (images need rotation correction)
- **DO NOT** bind camera controller before lifecycle is ready (use `bindToLifecycle()`)
- **DO NOT** assume photos are compressed or persisted; `MainViewModel` stores full `Bitmap` objects in memory only

## COMMANDS
```bash
# Build (from project root)
./gradlew assembleDebug

# Run tests
./gradlew test

# Install debug
./gradlew installDebug
```

## NOTES
- Project name "CameraXGuide" is a tutorial reference (from plcoding)
- No persistent storage - photos only in-memory during session
- Single Activity architecture (no Fragments)
- TODO in `data_extraction_rules.xml` - backup rules incomplete