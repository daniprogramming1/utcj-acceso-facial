package edu.utcj.acceso.data.biometric

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face

/**
 * Contrato del motor de embeddings faciales.
 *
 * Implementaciones:
 * - [MobileFaceNetEmbeddingEngine]: TFLite MobileFaceNet (112×112 → 192-d), preferido.
 * - [LegacyHistogramEmbeddingEngine]: histograma + geometría (268-d), respaldo escolar.
 *
 * ## Migración
 * Los embeddings de distintos motores **no son compatibles**. Tras cambiar de modelo
 * (o al activar/desactivar el legado), los alumnos **deben volver a registrarse**.
 * Room guarda bytes de longitud variable; no hace falta migrar el esquema.
 */
interface FaceEmbeddingEngine {
    /** Dimensión del vector L2-normalizado que produce este motor. */
    val embeddingDim: Int

    /** true si usa red neuronal (MobileFaceNet); false si es el histograma legado. */
    val isNeuralModel: Boolean

    /**
     * Extrae embedding a partir del bitmap de cámara y el [Face] de ML Kit
     * (alineación por landmarks + inferencia o histograma).
     * @return vector L2-normalizado de tamaño [embeddingDim]
     */
    fun extractEmbedding(source: Bitmap, face: Face): FloatArray
}
