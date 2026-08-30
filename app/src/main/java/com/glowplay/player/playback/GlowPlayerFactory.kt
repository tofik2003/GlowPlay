package com.glowplay.player.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import com.glowplay.player.enhance.GlowEffectCommand
import com.glowplay.player.enhance.GlowEffects

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
        return commands.map { command ->
            when (command.type) {
                GlowEffectCommand.Type.BRIGHTNESS -> Brightness(command.value)
                GlowEffectCommand.Type.CONTRAST -> Contrast(command.value)
                GlowEffectCommand.Type.SATURATION -> HslAdjustment.Builder()
                    .adjustSaturation(GlowEffects.saturationPercent(command.value))
                    .build()
                GlowEffectCommand.Type.WARMTH -> RgbAdjustment.Builder()
                    .setRedScale(GlowEffects.redScale(command.value))
                    .setGreenScale(1f)
                    .setBlueScale(GlowEffects.blueScale(command.value))
                    .build()
            }
        }
    }
}
