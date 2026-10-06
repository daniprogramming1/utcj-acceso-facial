package edu.utcj.acceso.data.biometric

import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Optional simple liveness: blink detection and/or head turn.
 * Toggle via SettingsRepository.livenessEnabled.
 */
@Singleton
class LivenessChecker @Inject constructor() {

    enum class Challenge { NONE, BLINK, TURN_LEFT, TURN_RIGHT }

    data class LivenessState(
        val challenge: Challenge = Challenge.BLINK,
        val completed: Boolean = false,
        val guidanceEs: String = "Parpadea para confirmar presencia",
        val sawEyesOpen: Boolean = false,
        val sawBlink: Boolean = false,
        val sawTurn: Boolean = false
    )

    fun nextChallenge(preferHeadTurn: Boolean = false): Challenge =
        if (preferHeadTurn) Challenge.TURN_LEFT else Challenge.BLINK

    fun guidance(challenge: Challenge): String = when (challenge) {
        Challenge.NONE -> ""
        Challenge.BLINK -> "Parpadea para confirmar presencia"
        Challenge.TURN_LEFT -> "Gira ligeramente la cabeza a la izquierda"
        Challenge.TURN_RIGHT -> "Gira ligeramente la cabeza a la derecha"
    }

    fun update(state: LivenessState, face: Face): LivenessState {
        return when (state.challenge) {
            Challenge.NONE -> state.copy(completed = true)
            Challenge.BLINK -> {
                val left = face.leftEyeOpenProbability ?: 1f
                val right = face.rightEyeOpenProbability ?: 1f
                val open = left > 0.6f && right > 0.6f
                val closed = left < 0.3f && right < 0.3f
                val sawOpen = state.sawEyesOpen || open
                val sawBlink = state.sawBlink || (sawOpen && closed)
                val done = sawBlink && open
                state.copy(
                    sawEyesOpen = sawOpen,
                    sawBlink = sawBlink,
                    completed = done,
                    guidanceEs = if (done) "Presencia confirmada" else guidance(Challenge.BLINK)
                )
            }
            Challenge.TURN_LEFT -> {
                val turned = face.headEulerAngleY < -12f
                val back = abs(face.headEulerAngleY) < 8f
                val saw = state.sawTurn || turned
                val done = saw && back
                state.copy(
                    sawTurn = saw,
                    completed = done,
                    guidanceEs = if (done) "Presencia confirmada" else guidance(Challenge.TURN_LEFT)
                )
            }
            Challenge.TURN_RIGHT -> {
                val turned = face.headEulerAngleY > 12f
                val back = abs(face.headEulerAngleY) < 8f
                val saw = state.sawTurn || turned
                val done = saw && back
                state.copy(
                    sawTurn = saw,
                    completed = done,
                    guidanceEs = if (done) "Presencia confirmada" else guidance(Challenge.TURN_RIGHT)
                )
            }
        }
    }
}
