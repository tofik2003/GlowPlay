package com.glowplay.player.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.glowplay.player.enhance.GlowEffectCommand

object GlowPlayerFactory {
    fun create(context: Context, hardwareDecoder: Boolean = true): ExoPlayer {
        val renderers = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                if (hardwareDecoder) {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                } else {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                },
            )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 30_000, 1_000, 2_000)
            .build()
        val trackSelector = DefaultTrackSelector(context.applicationContext)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context.applicationContext, renderers)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }

    fun toMedia3Effects(commands: List<GlowEffectCommand>): List<Effect> {
        val matrix = GlowColorMatrix.fromCommands(commands) ?: return emptyList()
        return listOf(matrix)
    }
}
