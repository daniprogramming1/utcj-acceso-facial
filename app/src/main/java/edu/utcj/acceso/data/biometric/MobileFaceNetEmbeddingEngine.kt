package edu.utcj.acceso.data.biometric

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.face.Face
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor MobileFaceNet vía TensorFlow Lite.
 *
 * - Entrada: rostro alineado 112×112 RGB, normalizado a [-1, 1] como `(p - 127.5) / 128`.
 * - Salida: embedding 192-d (u otra dim del modelo), L2-normalizado.
 * - Modelo esperado: `assets/models/mobilefacenet.tflite`
 *   (obtenerlo con `scripts/download_mobilefacenet.sh`).
 *
 * Si el archivo no existe o falla la carga, [isModelLoaded] queda en false y el
 * módulo Hilt debe elegir el motor legado. Mensajes de log en español.
 */
@Singleton
class MobileFaceNetEmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : FaceEmbeddingEngine {

    companion object {
        private const val TAG = "MobileFaceNet"
        const val MODEL_ASSET_PATH = "models/mobilefacenet.tflite"
        const val INPUT_SIZE = 112
        /** Dimensión típica del MobileFaceNet comunitario (sirius-ai / MCarlomagno). */
        const val EXPECTED_EMBEDDING_DIM = 192
        /** Umbral coseno recomendado con embeddings L2 de MobileFaceNet. */
        const val RECOMMENDED_THRESHOLD = 0.60f
        private const val NUM_THREADS = 4
    }

    @Volatile
    private var interpreter: Interpreter? = null

    @Volatile
    var isModelLoaded: Boolean = false
        private set

    @Volatile
    private var outputDim: Int = EXPECTED_EMBEDDING_DIM

    override val embeddingDim: Int
        get() = outputDim

    override val isNeuralModel: Boolean = true

    init {
        tryLoadModel()
    }

    /**
     * Intenta (re)cargar el modelo desde assets. Seguro llamar varias veces.
     * @return true si el intérprete quedó listo.
     */
    fun tryLoadModel(): Boolean {
        if (interpreter != null) {
            isModelLoaded = true
            return true
        }
        return try {
            if (!assetExists(MODEL_ASSET_PATH)) {
                Log.w(
                    TAG,
                    "Modelo MobileFaceNet no encontrado en assets/$MODEL_ASSET_PATH. " +
                        "Ejecuta scripts/download_mobilefacenet.sh y vuelve a compilar. " +
                        "Se usará el motor facial legado (histograma)."
                )
                isModelLoaded = false
                return false
            }
            val model = loadModelFile(MODEL_ASSET_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
            }
            val interp = Interpreter(model, options)
            val outShape = interp.getOutputTensor(0).shape()
            outputDim = when {
                outShape.size >= 2 -> outShape[outShape.size - 1]
                outShape.size == 1 -> outShape[0]
                else -> EXPECTED_EMBEDDING_DIM
            }
            interpreter = interp
            isModelLoaded = true
            Log.i(
                TAG,
                "MobileFaceNet TFLite cargado ($MODEL_ASSET_PATH). " +
                    "Entrada ${INPUT_SIZE}×${INPUT_SIZE}, embedding ${outputDim}-d."
            )
            true
        } catch (e: Throwable) {
            // Incluye UnsatisfiedLinkError (biblioteca nativa de TFLite ausente para la ABI del equipo):
            // se usa el motor legado en lugar de cerrar la app. Los errores de la VM sí se propagan.
            if (e is VirtualMachineError) throw e
            Log.e(
                TAG,
                "Error al cargar MobileFaceNet desde assets/$MODEL_ASSET_PATH: ${e.message}. " +
                    "Se usará el motor facial legado.",
                e
            )
            interpreter = null
            isModelLoaded = false
            false
        }
    }

    override fun extractEmbedding(source: Bitmap, face: Face): FloatArray {
        val interp = interpreter
            ?: error(
                "MobileFaceNet no está cargado. Coloca el modelo en assets/$MODEL_ASSET_PATH " +
                    "o activa el motor legado en Configuración."
            )
        val aligned = FaceAlignment.alignAndCrop(source, face, outSize = INPUT_SIZE)
        val input = preprocess(aligned)
        val output = Array(1) { FloatArray(outputDim) }
        synchronized(interp) {
            interp.run(input, output)
        }
        return FaceAlignment.l2Normalize(output[0])
    }

    /**
     * Preprocesa RGB 112×112 a buffer float32 NHWC normalizado a [-1, 1].
     */
    fun preprocess(bitmap: Bitmap): ByteBuffer {
        val bmp = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) bitmap
        else Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bmp.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            buffer.putFloat((r - 127.5f) / 128.0f)
            buffer.putFloat((g - 127.5f) / 128.0f)
            buffer.putFloat((b - 127.5f) / 128.0f)
        }
        buffer.rewind()
        return buffer
    }

    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }

    private fun loadModelFile(assetPath: String): MappedByteBuffer {
        val fd = context.assets.openFd(assetPath)
        FileInputStream(fd.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    fun close() {
        synchronized(this) {
            interpreter?.close()
            interpreter = null
            isModelLoaded = false
        }
    }
}
