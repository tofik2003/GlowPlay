package com.glowplay.player.playback

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram

/**
 * One-pass film post-processing program: unsharp-mask sharpening, a radial
 * vignette and animated film grain. It runs as a custom [BaseGlShaderProgram]
 * inside the Media3 GL effects pipeline, after the [GlowColorMatrix] grade.
 */
class GlowFilmShaderProgram(
    context: Context,
    useHdr: Boolean,
    private val sharpen: Float,
    private val vignette: Float,
    private val grain: Float,
) : BaseGlShaderProgram(
    /* useHighPrecisionColorComponents= */ useHdr,
    /* texturePoolCapacity= */ 1,
) {

    private val glProgram: GlProgram = GlProgram(
        context,
        "shaders/vertex_shader_transformation_es2.glsl",
        "shaders/glow_film_fragment.glsl",
    )

    init {
        // The film effect does not transform geometry; keep the quad and the
        // texture sampling matrix identity.
        glProgram.setFloatsUniform("uTransformationMatrix", GlUtil.create4x4IdentityMatrix())
        glProgram.setFloatsUniform("uTexTransformationMatrix", GlUtil.create4x4IdentityMatrix())
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        glProgram.setFloatsUniform(
            "uTexSize",
            floatArrayOf(inputWidth.toFloat(), inputHeight.toFloat()),
        )
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
            glProgram.setFloatUniform("uSharpen", sharpen)
            glProgram.setFloatUniform("uVignette", vignette)
            glProgram.setFloatUniform("uGrain", grain)
            glProgram.setFloatUniform("uPresentationTimeUs", presentationTimeUs.toFloat())
            glProgram.setBufferAttribute(
                "aFramePosition",
                NDC_SQUARE,
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, /* first= */ 0, /* count= */ 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        runCatching { glProgram.delete() }
    }

    private companion object {
        // Media3's NDC square in triangle-fan order: BL, TL, TR, BR.
        val NDC_SQUARE = floatArrayOf(
            -1f, -1f, 0f, 1f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 0f, 1f,
            1f, -1f, 0f, 1f,
        )
    }
}
