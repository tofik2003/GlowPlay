package com.glowplay.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/**
 * Equalizer presets applied to the player's audio session.
 *
 * The band curves are index-based (the device decides band center frequencies),
 * matching the original GlowPlay equalizer. The two newest presets approximate a
 * "dialogue clarity" and a "noise reducer" curve — playback-side cleanup, since
 * Android's NoiseSuppressor/AcousticEchoCanceler only act on the microphone
 * capture path and cannot be attached to a player's output.
 */
enum class EqKind { FLAT, BASS, TREBLE, VOICE, MOVIE, DIALOGUE, NOISE_REDUCE }

/**
 * Owns the optional audio effects attached to the player's audio session id.
 * Every effect is optional: devices can lack support, so each creation and each
 * mutation is guarded and non-fatal. Releasing releases all of them.
 */
class AudioEffects {

    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun attach(sessionId: Int) {
        release()
        runCatching { equalizer = Equalizer(0, sessionId).apply { enabled = true } }
        runCatching { loudness = LoudnessEnhancer(sessionId).apply { enabled = false } }
        runCatching { bassBoost = BassBoost(0, sessionId).apply { enabled = false } }
        runCatching { virtualizer = Virtualizer(0, sessionId).apply { enabled = false } }
    }

    fun applyEq(kind: EqKind) {
        val eq = equalizer ?: return
        runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            fun setAll(vararg levels: Int) {
                repeat(bands) { index ->
                    val level = (levels.getOrNull(index) ?: 0).toShort()
                    eq.setBandLevel(index.toShort(), level.coerceIn(range[0], range[1]))
                }
            }
            when (kind) {
                EqKind.FLAT -> setAll(0, 0, 0, 0, 0)
                EqKind.BASS -> setAll(900, 600, 100, -50, 0)
                EqKind.TREBLE -> setAll(-50, 0, 200, 700, 900)
                EqKind.VOICE -> setAll(-200, 100, 700, 500, 0)
                EqKind.MOVIE -> setAll(400, 200, 0, 250, 450)
                EqKind.DIALOGUE -> setAll(-400, -150, 650, 850, 350)
                EqKind.NOISE_REDUCE -> setAll(-600, -200, 250, 100, -450)
            }
            eq.enabled = true
        }
    }

    /** [targetGainMb] is a LoudnessEnhancer target gain in millibels (0..2000). */
    fun setLoudness(targetGainMb: Int) {
        val fx = loudness ?: return
        runCatching {
            val clamped = targetGainMb.coerceIn(0, 2000)
            fx.setTargetGain(clamped)
            fx.enabled = clamped > 0
        }
    }

    /** [strength] is a BassBoost strength in permille (0..1000). */
    fun setBass(strength: Int) {
        val fx = bassBoost ?: return
        runCatching {
            val clamped = strength.coerceIn(0, 1000)
            fx.setStrength(clamped.toShort())
            fx.enabled = clamped > 0
        }
    }

    /** [strength] is a Virtualizer (surround) strength in permille (0..1000). */
    fun setSurround(strength: Int) {
        val fx = virtualizer ?: return
        runCatching {
            val clamped = strength.coerceIn(0, 1000)
            fx.setStrength(clamped.toShort())
            fx.enabled = clamped > 0
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { loudness?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        loudness = null
        bassBoost = null
        virtualizer = null
    }
}
