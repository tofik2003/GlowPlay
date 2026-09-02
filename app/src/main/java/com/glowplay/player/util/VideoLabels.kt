package com.glowplay.player.util

/**
 * Small, pure formatting helpers for the badges shown on video cards
 * (resolution class + container format). Kept separate from [VideoItem] so
 * they stay trivially unit-testable.
 */
object VideoLabels {
    /** Maps a raw pixel height to the familiar marketing resolution label. */
    fun resolutionLabel(width: Int, height: Int): String? {
        val longest = maxOf(width, height)
        if (longest <= 0) return null
        return when {
            longest >= 3840 -> "4K"
            longest >= 2560 -> "1440p"
            longest >= 1920 -> "1080p"
            longest >= 1280 -> "720p"
            longest >= 854 -> "480p"
            longest >= 640 -> "360p"
            else -> "SD"
        }
    }

    /** Extracts an uppercase container/extension badge (e.g. "MP4", "MKV"). */
    fun formatLabel(displayName: String): String? {
        val dot = displayName.lastIndexOf('.')
        if (dot < 0 || dot == displayName.lastIndex) return null
        val ext = displayName.substring(dot + 1).trim()
        if (ext.isEmpty() || ext.length > 5) return null
        return ext.uppercase()
    }
}
