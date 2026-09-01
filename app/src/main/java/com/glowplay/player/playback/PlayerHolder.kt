package com.glowplay.player.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerHolder {
    @Volatile
    private var exo: ExoPlayer? = null

    fun player(): ExoPlayer? = exo

    @Synchronized
    fun obtain(context: Context, hardware: Boolean): ExoPlayer {
        exo?.let { return it }
        return GlowPlayerFactory.create(context, hardware).also { exo = it }
    }

    @Synchronized
    fun release() {
        exo?.release()
        exo = null
    }
}
