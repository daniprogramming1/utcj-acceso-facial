package edu.utcj.acceso.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.utcj.acceso.data.biometric.FaceEmbeddingEngine
import edu.utcj.acceso.data.biometric.LegacyHistogramEmbeddingEngine
import edu.utcj.acceso.data.biometric.MobileFaceNetEmbeddingEngine
import edu.utcj.acceso.data.repository.SettingsRepository
import javax.inject.Singleton

/**
 * Elige el motor facial en tiempo de arranque:
 * 1. Si Configuración pide motor legado → histograma.
 * 2. Si MobileFaceNet TFLite está en assets → red neuronal.
 * 3. Si no → histograma con aviso en log (español).
 */
@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {
    private const val TAG = "BiometricModule"

    @Provides
    @Singleton
    fun provideFaceEmbeddingEngine(
        settings: SettingsRepository,
        mobile: MobileFaceNetEmbeddingEngine,
        legacy: LegacyHistogramEmbeddingEngine
    ): FaceEmbeddingEngine {
        if (settings.useLegacyFaceEmbedding()) {
            Log.i(
                TAG,
                "Motor facial legado forzado por configuración " +
                    "(use_legacy_face_embedding=true). Umbral sugerido: " +
                    "${LegacyHistogramEmbeddingEngine.RECOMMENDED_THRESHOLD}."
            )
            return legacy
        }
        if (mobile.isModelLoaded || mobile.tryLoadModel()) {
            Log.i(
                TAG,
                "Usando MobileFaceNet TFLite (${mobile.embeddingDim}-d). " +
                    "Umbral sugerido: ${MobileFaceNetEmbeddingEngine.RECOMMENDED_THRESHOLD}. " +
                    "IMPORTANTE: tras cambiar de modelo, los alumnos deben volver a registrarse."
            )
            return mobile
        }
        Log.w(
            TAG,
            "Modelo MobileFaceNet ausente (assets/models/mobilefacenet.tflite). " +
                "Ejecuta scripts/download_mobilefacenet.sh. " +
                "Usando motor facial legado (histograma 268-d) hasta que el modelo esté disponible."
        )
        return legacy
    }
}
