@file:OptIn(ExperimentalMaterial3Api::class)

package com.plcoding.cameraxguide

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plcoding.cameraxguide.data.MediaStorePhotoDataSource
import com.plcoding.cameraxguide.data.PhotoRepositoryImpl
import com.plcoding.cameraxguide.model.ExposureBlendUiMode
import com.plcoding.cameraxguide.ui.theme.AppShapes
import com.plcoding.cameraxguide.ui.theme.AppSpacing
import com.plcoding.cameraxguide.ui.theme.CameraXGuideTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions(),
                0
            )
        }
        setContent {
            CameraXGuideTheme {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val lifecycleOwner = LocalLifecycleOwner.current
                var iso by remember { mutableFloatStateOf(1600f) }
                var shutter by remember { mutableFloatStateOf(15f) }
                var whiteBalance by remember { mutableFloatStateOf(5600f) }
                var evBias by remember { mutableFloatStateOf(-0.3f) }
                var selectedBlendMode by remember { mutableStateOf<ExposureBlendUiMode>(ExposureBlendUiMode.Screen) }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                CameraController.VIDEO_CAPTURE or
                                CameraController.IMAGE_ANALYSIS
                        )
                        imageAnalysisBackpressureStrategy =
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    }
                }
                val photoRepository = remember {
                    PhotoRepositoryImpl(
                        dataSource = MediaStorePhotoDataSource(contentResolver)
                    )
                }
                val viewModel = viewModel<MainViewModel>(
                    factory = MainViewModel.factory(photoRepository)
                )
                val bitmaps by viewModel.bitmaps.collectAsState()
                val photos by viewModel.photos.collectAsState()
                val isLongExposureActive by viewModel.isLongExposureActive.collectAsState()
                val liveAccumulatedFrame by viewModel.liveAccumulatedFrame.collectAsState()
                val exposureDurationMs by viewModel.exposureDurationMs.collectAsState()

                // Set up the ImageAnalysis analyser on a dedicated background thread.
                // Each camera frame is forwarded to the ViewModel which decides whether to
                // accumulate it (GPU-pipeline-style) based on the long-exposure active flag.
                val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                DisposableEffect(controller, viewModel) {
                    controller.setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        // Scale down to a manageable resolution while preserving the
                        // camera's native aspect ratio to avoid distortion.
                        val targetMaxDim = 960
                        val scaleFactor = targetMaxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                        val scaledW = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                        val scaledH = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, false)
                        bitmap.recycle()
                        viewModel.onFrameAnalyzed(scaled)
                        scaled.recycle()
                        imageProxy.close()
                    }
                    onDispose {
                        controller.clearImageAnalysisAnalyzer()
                        // shutdownNow() immediately stops queued tasks and frees the thread.
                        analysisExecutor.shutdownNow()
                    }
                }

                LaunchedEffect(Unit) {
                    if (hasGalleryReadPermission()) {
                        viewModel.loadPhotos()
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME && hasGalleryReadPermission()) {
                            viewModel.loadPhotos()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            photos = photos,
                            fallbackBitmaps = bitmaps,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Long-exposure ghost overlay — shows accumulated light integration
                        // in real-time so the user can decide when to stop the exposure.
                        // Opacity is ~85% so the live camera feed is still visible beneath.
                        liveAccumulatedFrame?.let { ghost ->
                            Image(
                                bitmap = ghost.asImageBitmap(),
                                contentDescription = "Live long-exposure accumulation",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                    ),
                                alpha = 0.85f
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                        )

                        CameraTopBar(
                            onSwitchCamera = {
                                controller.cameraSelector = if (
                                    controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                                ) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            }
                        )

                        TechnicalReadoutStrip(
                            iso = iso.toInt(),
                            shutter = shutter,
                            isCapturing = isLongExposureActive,
                            exposureDurationMs = exposureDurationMs,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 88.dp)
                        )

                        HistogramPanel(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = AppSpacing.Gutter, top = 88.dp)
                        )

                        MetadataReadoutPanel(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = AppSpacing.Gutter, top = 88.dp)
                        )

                        HudCrosshair(
                            modifier = Modifier.align(Alignment.Center)
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = AppSpacing.Gutter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ManualControlsDrawer(
                                iso = iso,
                                onIsoChange = { iso = it },
                                shutter = shutter,
                                onShutterChange = { shutter = it },
                                whiteBalance = whiteBalance,
                                onWhiteBalanceChange = { whiteBalance = it },
                                evBias = evBias,
                                onEvBiasChange = { evBias = it },
                                selectedBlendMode = selectedBlendMode,
                                onBlendModeSelected = { selectedBlendMode = it }
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.Gutter))
                            BottomCaptureNav(
                                isCapturing = isLongExposureActive,
                                onOpenGallery = {
                                    scope.launch { scaffoldState.bottomSheetState.expand() }
                                },
                                onCapture = {
                                    if (isLongExposureActive) {
                                        viewModel.stopLongExposure()
                                    } else {
                                        viewModel.startLongExposure(selectedBlendMode)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasGalleryReadPermission(): Boolean {
        val permission = mediaReadPermission() ?: return true
        return ContextCompat.checkSelfPermission(applicationContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): Array<String> {
        return mediaReadPermission()?.let { READ_BASE_PERMISSIONS + it } ?: READ_BASE_PERMISSIONS
    }

    private fun mediaReadPermission(): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Manifest.permission.READ_EXTERNAL_STORAGE
            else -> null
        }
    }

    companion object {
        private val READ_BASE_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

@Composable
private fun CameraTopBar(
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.MarginEdge, vertical = AppSpacing.Gutter),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PHOTON",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Unit)) {
            GlassIconButton(
                onClick = onSwitchCamera,
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch camera"
            )
            GlassIconButton(
                onClick = {},
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
private fun TechnicalReadoutStrip(
    iso: Int,
    shutter: Float,
    isCapturing: Boolean,
    exposureDurationMs: Long,
    modifier: Modifier = Modifier
) {
    val elapsedLabel = if (isCapturing) {
        val totalSecs = exposureDurationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val tenths = (exposureDurationMs % 1000) / 100
        "${mins}:${secs.toString().padStart(2, '0')}.${tenths}"
    } else {
        "STANDBY"
    }
    val recColor by animateColorAsState(
        targetValue = if (isCapturing) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "rec_color"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
            .padding(horizontal = AppSpacing.Gutter, vertical = AppSpacing.Unit),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = if (isCapturing) "Recording" else "Standby",
            tint = recColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = elapsedLabel,
            color = recColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text("ISO $iso", color = MaterialTheme.colorScheme.primaryContainer, style = MaterialTheme.typography.titleMedium)
        Text("SS ${"%.1f".format(shutter)}s", color = MaterialTheme.colorScheme.primaryContainer, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HudCrosshair(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 59.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
    }
}

@Composable
private fun HistogramPanel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(AppShapes.Md)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.Md)
            .padding(AppSpacing.Unit)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(0.2f, 0.35f, 0.45f, 0.8f, 0.6f, 0.4f, 0.25f, 0.15f).forEach { bar ->
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = (48f * bar).dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            )
        }
    }
}

@Composable
private fun MetadataReadoutPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Unit)) {
        MetadataChip("F-STOP", "f/1.8")
        MetadataChip("FOCUS", "INF")
        MetadataChip("CODEC", "RAW")
    }
}

@Composable
private fun MetadataChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(AppShapes.Default)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), AppShapes.Default)
            .padding(horizontal = AppSpacing.PanelPadding, vertical = AppSpacing.Unit)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primaryContainer)
    }
}

@Composable
private fun ManualControlsDrawer(
    iso: Float,
    onIsoChange: (Float) -> Unit,
    shutter: Float,
    onShutterChange: (Float) -> Unit,
    whiteBalance: Float,
    onWhiteBalanceChange: (Float) -> Unit,
    evBias: Float,
    onEvBiasChange: (Float) -> Unit,
    selectedBlendMode: ExposureBlendUiMode,
    onBlendModeSelected: (ExposureBlendUiMode) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = AppSpacing.Gutter)
            .clip(AppShapes.Xl)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.Xl)
            .padding(AppSpacing.Gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Unit)
    ) {
        LabeledSlider("ISO SENSITIVITY", iso.toInt().toString(), iso, 100f..12800f, onIsoChange)
        LabeledSlider("SHUTTER SPEED", "${"%.1f".format(shutter)}s", shutter, 0f..30f, onShutterChange)
        LabeledSlider("WHITE BALANCE", "${whiteBalance.toInt()}K", whiteBalance, 2000f..10000f, onWhiteBalanceChange)
        LabeledSlider("EV BIAS", "${"%.1f".format(evBias)}", evBias, -3f..3f, onEvBiasChange)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BLEND MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .clip(AppShapes.Default)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ExposureBlendUiMode.all.forEach { mode ->
                    val selected = mode == selectedBlendMode
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                        shape = AppShapes.Sm,
                        onClick = { onBlendModeSelected(mode) }
                    ) {
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(horizontal = AppSpacing.PanelPadding, vertical = AppSpacing.Unit),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primaryContainer)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun BottomCaptureNav(
    isCapturing: Boolean,
    onOpenGallery: () -> Unit,
    onCapture: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.Gutter)
            .clip(AppShapes.Xl)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), AppShapes.Xl)
            .padding(horizontal = AppSpacing.Gutter, vertical = AppSpacing.Unit),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(onClick = {}, imageVector = Icons.Default.Photo, contentDescription = "Mode")
        GlassIconButton(onClick = onOpenGallery, imageVector = Icons.Default.Photo, contentDescription = "Open gallery")

        CaptureButton(isCapturing = isCapturing, onClick = onCapture)

        GlassIconButton(onClick = {}, imageVector = Icons.Default.Cameraswitch, contentDescription = "Effects")
        GlassIconButton(onClick = {}, imageVector = Icons.Default.Settings, contentDescription = "Analytics")
    }
}

/**
 * The central capture/stop button.
 *
 * - Idle state: cyan camera icon — tap to begin long-exposure.
 * - Capturing state: pulsing red stop icon — tap to end exposure and save.
 */
@Composable
private fun CaptureButton(isCapturing: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "capture_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCapturing) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isCapturing) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primary,
        label = "button_color"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isCapturing) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimary,
        label = "icon_color"
    )
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        modifier = Modifier
            .size(74.dp)
            .scale(pulseScale)
    ) {
        Icon(
            imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.PhotoCamera,
            contentDescription = if (isCapturing) "Stop long exposure" else "Start long exposure",
            tint = iconColor
        )
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = CircleShape
            )
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
