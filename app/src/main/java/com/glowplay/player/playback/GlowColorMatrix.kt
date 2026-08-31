package com.glowplay.player.playback

import androidx.media3.effect.RgbMatrix
import com.glowplay.player.enhance.GlowEffectCommand
import com.glowplay.player.enhance.GlowEffects

/**
 * A single-pass 4x4 color matrix (column-major, homogeneous RGBA) that fuses
 * brightness, contrast, saturation and warmth into one GPU pass.
 *
 * This replaces the previous chain of four separate Media3 effects
 * (`Brightness` -> `Contrast` -> `HslAdjustment` -> `RgbAdjustment`), which ran
 * four shader passes per frame. One matrix means less color drift from chained
 * rounding, fewer shader switches and lower GPU cost, while keeping the same
 * slider semantics (brightness is an additive offset, contrast pivots around
 * mid-gray, saturation is luma-weighted, warmth scales red up / blue down).
 */
class GlowColorMatrix private constructor(
    private val matrix: FloatArray,
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix

    companion object {
        private val IDENTITY = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }

        /** Returns a fused matrix for the given commands, or null when there is nothing to do. */
        fun fromCommands(commands: List<GlowEffectCommand>): GlowColorMatrix? {
            if (commands.isEmpty()) return null
            var brightness = 0f
            var contrast = 0f
            var saturation = 0f
            var warmth = 0f
            for (command in commands) {
                when (command.type) {
                    GlowEffectCommand.Type.BRIGHTNESS -> brightness = command.value
                    GlowEffectCommand.Type.CONTRAST -> contrast = command.value
                    GlowEffectCommand.Type.SATURATION -> saturation = command.value
                    GlowEffectCommand.Type.WARMTH -> warmth = command.value
                }
            }

            // Effects are applied in the order brightness -> contrast ->
            // saturation -> warmth, so brightness must be rightmost in the
            // product M = W * S * C * B.
            var m = IDENTITY.copyOf()
            if (brightness != 0f) m = multiply(brightnessMatrix(brightness), m)
            if (contrast != 0f) m = multiply(contrastMatrix(contrast), m)
            if (saturation != 0f) m = multiply(saturationMatrix(saturation), m)
            if (warmth != 0f) m = multiply(warmthMatrix(warmth), m)
            return GlowColorMatrix(m)
        }

        // All matrices below are column-major: element (row, col) lives at col * 4 + row.
        // The fourth column holds the additive constants (applied when alpha = 1).

        private fun brightnessMatrix(b: Float): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            b, b, b, 1f,
        )

        private fun contrastMatrix(c: Float): FloatArray {
            val factor = (1f + c) / (1.0001f - c)
            val pivot = (1f - factor) * 0.5f
            return floatArrayOf(
                factor, 0f, 0f, 0f,
                0f, factor, 0f, 0f,
                0f, 0f, factor, 0f,
                pivot, pivot, pivot, 1f,
            )
        }

        private fun saturationMatrix(s: Float): FloatArray {
            val scale = 1f + s
            val inv = 1f - scale
            // Rec. 709 luma coefficients.
            val lr = 0.2126f
            val lg = 0.7152f
            val lb = 0.0722f
            return floatArrayOf(
                lr * inv + scale, lg * inv, lb * inv, 0f,
                lr * inv, lg * inv + scale, lb * inv, 0f,
                lr * inv, lg * inv, lb * inv + scale, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        private fun warmthMatrix(w: Float): FloatArray {
            val r = GlowEffects.redScale(w)
            val b = GlowEffects.blueScale(w)
            return floatArrayOf(
                r, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, b, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        /** Computes the column-major product `a * b`. */
        private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
            val out = FloatArray(16)
            for (col in 0 until 4) {
                for (row in 0 until 4) {
                    var sum = 0f
                    for (k in 0 until 4) {
                        sum += a[k * 4 + row] * b[col * 4 + k]
                    }
                    out[col * 4 + row] = sum
                }
            }
            return out
        }
    }
}
