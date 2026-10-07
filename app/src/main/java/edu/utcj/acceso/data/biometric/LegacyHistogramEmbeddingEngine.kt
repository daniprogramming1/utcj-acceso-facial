package edu.utcj.acceso.data.biometric

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor facial escolar (legado): histograma 16×16 en escala de grises + 12 rasgos
 * geométricos de landmarks ML Kit → vector 268-d L2-normalizado.
 *
 * No alcanza la robustez de MobileFaceNet; se conserva como respaldo cuando el
 * modelo TFLite no está en assets o cuando [SettingsRepository.useLegacyFaceEmbedding]
 * está activo.
 */
@Singleton
class LegacyHistogramEmbeddingEngine @Inject constructor() : FaceEmbeddingEngine {

    companion object {
        private const val TAG = "LegacyFaceEngine"
        const val HIST_SIZE = 16
        const val EMBEDDING_DIM = HIST_SIZE * HIST_SIZE + FaceAlignment.GEOMETRY_DIMS // 268
        /** Umbral coseno recomendado para este motor (más estricto que MobileFaceNet). */
        const val RECOMMENDED_THRESHOLD = 0.72f
    }

    override val embeddingDim: Int = EMBEDDING_DIM
    override val isNeuralModel: Boolean = false

    init {
        Log.i(TAG, "Motor facial legado (histograma 268-d) listo.")
    }

    override fun extractEmbedding(source: Bitmap, face: Face): FloatArray {
        val aligned = FaceAlignment.alignAndCrop(source, face, outSize = 128)
        val hist = FaceAlignment.grayscaleHistogram(aligned, HIST_SIZE)
        val geom = FaceAlignment.geometryFeatures(face, source.width, source.height)
        val raw = FloatArray(EMBEDDING_DIM)
        System.arraycopy(hist, 0, raw, 0, hist.size)
        System.arraycopy(geom, 0, raw, hist.size, geom.size)
        return FaceAlignment.l2Normalize(raw)
    }

    /** Sobrecarga de prueba / herramientas cuando ya hay rostro alineado. */
    fun extractEmbedding(alignedFace: Bitmap, geometry: FloatArray): FloatArray {
        require(geometry.size == FaceAlignment.GEOMETRY_DIMS)
        val hist = FaceAlignment.grayscaleHistogram(alignedFace, HIST_SIZE)
        val raw = FloatArray(EMBEDDING_DIM)
        System.arraycopy(hist, 0, raw, 0, hist.size)
        System.arraycopy(geometry, 0, raw, hist.size, geometry.size)
        return FaceAlignment.l2Normalize(raw)
    }
}
