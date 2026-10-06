package edu.utcj.acceso.data.biometric

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * School-project face embedding pipeline.
 *
 * ## Approach (documented limitation)
 * Without a licensed FaceNet / MobileFaceNet ONNX model, this engine builds a
 * pragmatic feature vector from:
 * 1. ML Kit face detection landmarks (eyes, nose, mouth) for alignment
 * 2. Aligned face crop → downsampled grayscale intensity histogram (16×16 = 256 bins normalized)
 * 3. Landmark-relative geometry ratios (eye distance, nose offset, mouth width, etc.)
 *
 * The resulting ~280-dim L2-normalized float vector supports cosine matching for demos
 * and coursework. It is **not** production-grade biometric template quality.
 *
 * ## Plug-in point for a real model
 * Replace [extractEmbedding] body (or provide an alternate [FaceEmbeddingEngine] binding
 * in Hilt) with ONNX Runtime / TFLite inference that outputs a 128/512-d FaceNet embedding.
 * Keep [FaceMatcher] and storage encryption unchanged.
 */
@Singleton
class FaceEmbeddingEngine @Inject constructor() {

    companion object {
        const val HIST_SIZE = 16
        const val GEOMETRY_DIMS = 12
        const val EMBEDDING_DIM = HIST_SIZE * HIST_SIZE + GEOMETRY_DIMS // 268
        private const val CROP_MARGIN = 0.25f
    }

    /**
     * Extract embedding from a face bitmap already roughly cropped, plus ML Kit [Face].
     * @return L2-normalized float array of size [EMBEDDING_DIM]
     */
    fun extractEmbedding(source: Bitmap, face: Face): FloatArray {
        val aligned = alignAndCrop(source, face)
        val hist = grayscaleHistogram(aligned, HIST_SIZE)
        val geom = geometryFeatures(face, source.width, source.height)
        val raw = FloatArray(EMBEDDING_DIM)
        System.arraycopy(hist, 0, raw, 0, hist.size)
        System.arraycopy(geom, 0, raw, hist.size, geom.size)
        return l2Normalize(raw)
    }

    /** Interface-friendly overload when landmarks are already extracted. */
    fun extractEmbedding(alignedFace: Bitmap, geometry: FloatArray): FloatArray {
        require(geometry.size == GEOMETRY_DIMS)
        val hist = grayscaleHistogram(alignedFace, HIST_SIZE)
        val raw = FloatArray(EMBEDDING_DIM)
        System.arraycopy(hist, 0, raw, 0, hist.size)
        System.arraycopy(geometry, 0, raw, hist.size, geometry.size)
        return l2Normalize(raw)
    }

    fun alignAndCrop(source: Bitmap, face: Face): Bitmap {
        val box = face.boundingBox
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        val marginX = (box.width() * CROP_MARGIN).toInt()
        val marginY = (box.height() * CROP_MARGIN).toInt()
        val left = max(0, box.left - marginX)
        val top = max(0, box.top - marginY)
        val right = min(source.width, box.right + marginX)
        val bottom = min(source.height, box.bottom + marginY)
        val w = max(1, right - left)
        val h = max(1, bottom - top)
        var crop = Bitmap.createBitmap(source, left, top, w, h)

        if (leftEye != null && rightEye != null) {
            val angle = Math.toDegrees(
                atan2(
                    (rightEye.y - leftEye.y).toDouble(),
                    (rightEye.x - leftEye.x).toDouble()
                )
            ).toFloat()
            crop = rotateBitmap(crop, -angle)
        }
        // Normalize to fixed size for histogram stability
        return Bitmap.createScaledBitmap(crop, 128, 128, true)
    }

    fun geometryFeatures(face: Face, imgW: Int, imgH: Int): FloatArray {
        fun pt(type: Int): PointF? = face.getLandmark(type)?.position
        val le = pt(FaceLandmark.LEFT_EYE)
        val re = pt(FaceLandmark.RIGHT_EYE)
        val nose = pt(FaceLandmark.NOSE_BASE)
        val ml = pt(FaceLandmark.MOUTH_LEFT)
        val mr = pt(FaceLandmark.MOUTH_RIGHT)
        val mb = pt(FaceLandmark.MOUTH_BOTTOM)

        val eyeDist = if (le != null && re != null) hypot(re.x - le.x, re.y - le.y) else 1f
        val mouthW = if (ml != null && mr != null) hypot(mr.x - ml.x, mr.y - ml.y) else 0f
        val box = face.boundingBox
        val bw = box.width().toFloat().coerceAtLeast(1f)
        val bh = box.height().toFloat().coerceAtLeast(1f)

        return floatArrayOf(
            eyeDist / bw,
            mouthW / bw,
            (nose?.x?.minus(box.left) ?: 0f) / bw,
            (nose?.y?.minus(box.top) ?: 0f) / bh,
            (le?.x?.minus(box.left) ?: 0f) / bw,
            (le?.y?.minus(box.top) ?: 0f) / bh,
            (re?.x?.minus(box.left) ?: 0f) / bw,
            (re?.y?.minus(box.top) ?: 0f) / bh,
            face.headEulerAngleY / 90f,
            face.headEulerAngleZ / 90f,
            face.headEulerAngleX / 90f,
            (box.width().toFloat() / imgW.coerceAtLeast(1))
        )
    }

    private fun grayscaleHistogram(bmp: Bitmap, size: Int): FloatArray {
        val scaled = if (bmp.width == size && bmp.height == size) bmp
        else Bitmap.createScaledBitmap(bmp, size, size, true)
        val hist = FloatArray(size * size)
        var sum = 0f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val c = scaled.getPixel(x, y)
                val g = (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f
                hist[y * size + x] = g
                sum += g
            }
        }
        if (sum > 0f) {
            for (i in hist.indices) hist[i] /= sum
        }
        return hist
    }

    private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        if (kotlin.math.abs(degrees) < 0.5f) return src
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun l2Normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm < 1e-8f) return v
        return FloatArray(v.size) { i -> v[i] / norm }
    }
}
