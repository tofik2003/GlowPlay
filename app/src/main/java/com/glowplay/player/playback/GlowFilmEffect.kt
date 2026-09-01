package com.glowplay.player.playback

import android.content.Context
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/** Sharpen / vignette / film-grain amounts, each in [0, 1]. */
data class FilmFx(
    val sharpen: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
) {
    val isIdentity: Boolean
        get() = sharpen <= 0f && vignette <= 0f && grain <= 0f
}

/**
 * A [GlEffect] that applies the GlowFilmShaderProgram (unsharp-mask sharpening,
 * radial vignette and animated film grain) to every frame. It is intended to be
 * chained AFTER the [GlowColorMatrix] color grade in the effects list.
 */
class GlowFilmEffect(
    private val sharpen: Float,
    private val vignette: Float,
    private val grain: Float,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return try {
            GlowFilmShaderProgram(context, useHdr, sharpen, vignette, grain)
        } catch (e: Exception) {
            throw VideoFrameProcessingException(e)
        }
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        sharpen <= 0f && vignette <= 0f && grain <= 0f
}
