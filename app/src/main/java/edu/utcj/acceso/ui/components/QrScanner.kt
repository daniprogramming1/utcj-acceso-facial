package edu.utcj.acceso.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Cámara con CameraX que lee códigos QR con ML Kit (en el dispositivo, sin red).
 * Los cuadros viven solo en memoria y nunca se guardan. [onCode] se llama en el hilo principal.
 *
 * @param front usa la cámara frontal (kiosco en pedestal); por defecto la trasera (guardia).
 * @param paused deja de entregar códigos (p. ej. mientras se muestra un resultado).
 */
@Composable
fun QrScannerCamera(
    modifier: Modifier = Modifier,
    front: Boolean = false,
    paused: Boolean = false,
    onCode: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentPaused by rememberUpdatedState(paused)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, QrAnalyzer(scanner, { currentPaused }) { currentOnCode(it) })
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                }.recoverCatching {
                    // Equipo sin la cámara pedida: usar la otra.
                    val other = if (front) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, other, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private class QrAnalyzer(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val paused: () -> Boolean,
    private val onCode: (String) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: androidx.camera.core.ImageProxy) {
        val media = proxy.image
        if (media == null || paused()) {
            proxy.close()
            return
        }
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                codes.firstNotNullOfOrNull { it.rawValue }?.let { if (!paused()) onCode(it) }
            }
            .addOnCompleteListener { proxy.close() }
    }
}

/**
 * Marco de escaneo: oscurece alrededor, esquinas redondeadas y una línea animada que recorre
 * el recuadro. [accent] cambia a verde/rojo al leer un código.
 */
@Composable
fun ScanFrameOverlay(
    modifier: Modifier = Modifier,
    accent: Color = BrandConfig.palette.primaryBright,
    animate: Boolean = true,
    frameFraction: Float = 0.68f
) {
    val color by animateColorAsState(accent, label = "scanAccent")
    val line = if (animate && !AppTheme.reducedMotion) {
        val t = rememberInfiniteTransition(label = "scanLine")
        t.animateFloat(0.06f, 0.94f, infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Reverse), label = "y").value
    } else 0.5f
    Canvas(modifier.fillMaxSize()) {
        val side = min(size.width, size.height) * frameFraction
        val tl = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val r = side * 0.08f
        val hole = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
            addRoundRect(RoundRect(tl.x, tl.y, tl.x + side, tl.y + side, CornerRadius(r)))
        }
        drawPath(hole, Color.Black.copy(alpha = 0.5f))
        // Esquinas
        val len = side * 0.18f
        val sw = 5.dp.toPx()
        val corners = listOf(
            tl to Offset(1f, 1f),
            Offset(tl.x + side, tl.y) to Offset(-1f, 1f),
            Offset(tl.x, tl.y + side) to Offset(1f, -1f),
            Offset(tl.x + side, tl.y + side) to Offset(-1f, -1f)
        )
        corners.forEach { (p, d) ->
            drawLine(color, p, Offset(p.x + d.x * len, p.y), sw, StrokeCap.Round)
            drawLine(color, p, Offset(p.x, p.y + d.y * len), sw, StrokeCap.Round)
        }
        // Línea de escaneo con estela
        val y = tl.y + side * line
        val inset = side * 0.06f
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.28f)), startY = y - side * 0.12f, endY = y),
            topLeft = Offset(tl.x + inset, y - side * 0.12f),
            size = Size(side - inset * 2, side * 0.12f)
        )
        drawLine(color, Offset(tl.x + inset, y), Offset(tl.x + side - inset, y), 3.dp.toPx(), StrokeCap.Round)
        drawRoundRect(color.copy(alpha = 0.25f), tl, Size(side, side), CornerRadius(r), style = Stroke(1.dp.toPx()))
    }
}

/** Chip de guía sobre la cámara (estilo «vidrio oscuro»). */
@Composable
fun GuidanceChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.QrCodeScanner,
    tone: Tone = Tone.Info,
    large: Boolean = false
) {
    val accent = when (tone) {
        Tone.Success -> AppTheme.extended.success
        Tone.Warning -> AppTheme.extended.warning
        Tone.Danger -> AppTheme.extended.danger
        else -> BrandConfig.palette.primaryBright
    }
    Row(
        modifier
            .background(Color.Black.copy(alpha = 0.62f), AppShapes.pill)
            .border(1.dp, Color.White.copy(alpha = 0.12f), AppShapes.pill)
            .padding(horizontal = if (large) 24.dp else 16.dp, vertical = if (large) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(if (large) 30.dp else 20.dp))
        Text(
            text,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 2
        )
    }
}

/** Sustituto visual de la cámara (previews, capturas de pantalla, emulador sin cámara). */
@Composable
fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF3A4A5F), Color(0xFF1B2636), Color(0xFF0E1622))))
    )
}

/** Solicita el permiso de cámara con una pantalla explicativa antes de mostrar [content]. */
@Composable
fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    if (granted) {
        content()
    } else {
        val fg = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
        Box(modifier.fillMaxSize().padding(Spacing.xxl), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 420.dp)) {
                IconBadge(Icons.Rounded.CameraAlt, Tone.Brand, size = 64.dp)
                Spacer(Modifier.height(Spacing.lg))
                Text("Necesitamos acceso a la cámara", style = MaterialTheme.typography.titleLarge, color = fg, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    if (denied) "Permiso denegado. Puedes habilitarlo en Ajustes › Apps › ${BrandConfig.APP_NAME} › Permisos."
                    else "La cámara se usa solo para leer códigos QR. Las imágenes nunca se guardan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("Permitir cámara", onClick = { launcher.launch(Manifest.permission.CAMERA) }, icon = Icons.Rounded.CameraAlt)
            }
        }
    }
}
