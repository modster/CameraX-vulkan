## Plan: Persist CameraX captures to collection

Replace the in-memory Bitmap flow with a persisted photo pipeline: capture to MediaStore, store metadata for fast list rendering, and render thumbnails via URI-based image loading in Compose.

### High-level approach
- File handling: Save JPEG via MediaStore with IS_PENDING, RELATIVE_PATH, DATE_TAKEN; return CapturedPhoto(uri, displayName, timestamp)
- Storage: PhotoRepository + MediaStorePhotoDataSource (savePhoto, loadPhotos, deletePhoto); MainViewModel exposes StateFlow<List<CapturedPhoto>>; load persisted list on app start
- Permissions: CAMERA always runtime; READ_MEDIA_IMAGES (API 33+); READ_EXTERNAL_STORAGE (API 32-)
- Presentation: PhotoBottomSheetContent consumes List<CapturedPhoto>; Coil for URI thumbnail rendering; metadata-driven labels

### Implementation order
1. Add CapturedPhoto model + repository interfaces
2. Implement savePhoto() in MediaStore; wire takePhoto() to call it
3. Change MainViewModel state to persisted photos; add initial loadPhotos()
4. Update PhotoBottomSheetContent to render from Uri
5. Add permission gates for read/query path
6. Verify: take photos -> force close -> reopen -> collection persists

### Migration strategy
Dual-write (current bitmap + persisted uri) first, then remove bitmap state once verified.

