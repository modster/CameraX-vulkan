# SOURCE DIRECTORY: com.plcoding.cameraxguide

## OVERVIEW
Core camera app implementation - single Activity with Compose UI.

## KEY FILES
| File | Role | Lines |
|------|------|-------|
| `MainActivity.kt` | Entry point, camera controls, permission handling | 202 |
| `MainViewModel.kt` | In-memory photo storage via StateFlow | 16 |
| `CameraPreview.kt` | PreviewView wrapper with lifecycle binding | 25 |
| `PhotoBottomSheetContent.kt` | Gallery grid (2 columns, staggered) | 52 |

## DEPENDENCY FLOW
```
MainActivity
    ├── CameraPreview (Compose wrapper)
    ├── MainViewModel (state holder)
    └── PhotoBottomSheetContent (gallery view)
```

## CAMERA FLOW
1. `MainActivity.onCreate()` → check permissions → request if missing
2. `LifecycleCameraController` created with IMAGE_CAPTURE | VIDEO_CAPTURE
3. `takePhoto()` → captures image → applies rotation matrix → updates ViewModel
4. ViewModel emits to StateFlow → gallery updates

## PATTERNS
- State hoisting: UI state in ViewModel, UI in Composable
- Lifecycle awareness: `bindToLifecycle(lifecycleOwner)` for camera
- Rotation fix: `Matrix.postRotate()` on captured bitmap

## GOTCHAS
- Images from CameraX need manual rotation (don't skip matrix)
- BottomSheetScaffold requires `ExperimentalMaterial3Api`
- PreviewView controller assignment must happen before `bindToLifecycle()`