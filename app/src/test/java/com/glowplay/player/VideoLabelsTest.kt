package com.glowplay.player

import com.glowplay.player.util.VideoLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoLabelsTest {
    @Test
    fun resolutionBuckets() {
        assertEquals("4K", VideoLabels.resolutionLabel(3840, 2160))
        assertEquals("1080p", VideoLabels.resolutionLabel(1920, 1080))
        assertEquals("720p", VideoLabels.resolutionLabel(1280, 720))
        assertEquals("480p", VideoLabels.resolutionLabel(854, 480))
        assertEquals("SD", VideoLabels.resolutionLabel(320, 240))
        assertNull(VideoLabels.resolutionLabel(0, 0))
    }

    @Test
    fun formatExtraction() {
        assertEquals("MP4", VideoLabels.formatLabel("Movie.mp4"))
        assertEquals("MKV", VideoLabels.formatLabel("clip.final.mkv"))
        assertNull(VideoLabels.formatLabel("noextension"))
        assertNull(VideoLabels.formatLabel("trailing."))
    }
}
