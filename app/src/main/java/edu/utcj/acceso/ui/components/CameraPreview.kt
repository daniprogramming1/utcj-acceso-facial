package edu.utcj.acceso.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Cámara frontal con CameraX. Entrega cada frame como Bitmap (rotado y en espejo)
 * al callback [onFrame]. Los frames viven solo en memoria: NUNCA se guardan en disco.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrame: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    val bmp = try { image.toBitmapRotated() } catch (_: Exception) { null }
                    image.close()
                    if (bmp != null) onFrame(bmp)
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                    // Sin cámara frontal disponible (emulador sin cámara, etc.)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

/** Convierte YUV_420_888 → NV21 → JPEG → Bitmap, aplica rotación y espejo. */
fun ImageProxy.toBitmapRotated(): Bitmap? {
    if (format != ImageFormat.YUV_420_888 || planes.size < 3) return null
    val nv21 = yuv420888ToNv21(this)
    val out = ByteArrayOutputStream()
    YuvImage(nv21, ImageFormat.NV21, width, height, null)
        .compressToJpeg(Rect(0, 0, width, height), 85, out)
    val bytes = out.toByteArray()
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val matrix = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        postScale(-1f, 1f) // espejo para cámara frontal
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

/** Conversión que respeta rowStride/pixelStride de cada plano. */
private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val w = image.width
    val h = image.height
    val nv21 = ByteArray(w * h + 2 * (w / 2) * (h / 2))
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuf = yPlane.buffer
    var pos = 0
    for (row in 0 until h) {
        yBuf.position(row * yPlane.rowStride)
        yBuf.get(nv21, pos, w)
        pos += w
    }
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer
    val chromaH = h / 2
    val chromaW = w / 2
    for (row in 0 until chromaH) {
        for (col in 0 until chromaW) {
            val vIdx = row * vPlane.rowStride + col * vPlane.pixelStride
            val uIdx = row * uPlane.rowStride + col * uPlane.pixelStride
            nv21[pos++] = vBuf.get(vIdx)
            nv21[pos++] = uBuf.get(uIdx)
        }
    }
    return nv21
}
