package com.glowplay.player

import com.glowplay.player.data.model.DurationFilter
import com.glowplay.player.data.model.VideoItem
import com.glowplay.player.enhance.VideoCatalog
import com.glowplay.player.enhance.VideoSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCatalogTest {
    private val movies = VideoItem(1, "content://1", "Dune.mp4", 120_000, 50, 1920, 1080, 200, 10, "Movies")
    private val older = VideoItem(2, "content://2", "Alien.mp4", 90_000, 80, 1280, 720, 100, 10, "Movies")
    private val clip = VideoItem(3, "content://3", "Cat.mp4", 8_000, 10, 640, 360, 300, 20, "Camera")

    @Test
    fun groupsFoldersByCountThenName() {
        val folders = VideoCatalog.groupByFolder(listOf(movies, older, clip))
        assertEquals(2, folders.size)
        assertEquals("Movies", folders.first().name)
        assertEquals(2, folders.first().videoCount)
        assertEquals(210_000L, folders.first().totalDurationMs)
    }

    @Test
    fun filterIsCaseInsensitive() {
        val result = VideoCatalog.filter(listOf(movies, clip), "dune")
        assertEquals(listOf(movies), result)
    }

    @Test
    fun sortByNameAndSize() {
        val byName = VideoCatalog.sort(listOf(movies, older), VideoSort.NAME)
        assertEquals("Alien.mp4", byName.first().name)
        val bySize = VideoCatalog.sort(listOf(movies, older), VideoSort.SIZE)
        assertEquals("Alien.mp4", bySize.first().name)
    }

    @Test
    fun recentsIgnoreTinyAndFinished() {
        val positions = mapOf(
            movies.mediaKey to 12_000L,
            older.mediaKey to 1_000L,
            clip.mediaKey to 7_500L,
        )
        val recents = VideoCatalog.recents(listOf(movies, older, clip), positions)
        assertEquals(1, recents.size)
        assertEquals("Dune.mp4", recents.first().video.name)
    }

    @Test
    fun playlistStartsAtSelectedVideoThenWrapsFolder() {
        val playlist = VideoCatalog.playlistUris(listOf(movies, older, clip), movies)
        assertEquals("content://1", playlist.first())
        assertTrue(playlist.contains("content://2"))
        assertEquals(2, playlist.size)
    }

    @Test
    fun durationFilterBucketsCorrectly() {
        val feature = VideoItem(4, "content://4", "Epic.mp4", 40 * 60_000L, 400, 1920, 1080, 50, 10, "Movies")
        val all = listOf(movies, older, clip, feature)

        val short = VideoCatalog.filterByDuration(all, DurationFilter.SHORT)
        assertEquals(setOf(movies, older, clip), short.toSet())

        val long = VideoCatalog.filterByDuration(all, DurationFilter.LONG)
        assertEquals(listOf(feature), long)

        val any = VideoCatalog.filterByDuration(all, DurationFilter.ANY)
        assertEquals(4, any.size)
    }

    @Test
    fun favoritesOnlyKeepsMatchingKeys() {
        val favorites = VideoCatalog.favoritesOnly(listOf(movies, older, clip), setOf(clip.mediaKey))
        assertEquals(listOf(clip), favorites)
        assertTrue(VideoCatalog.favoritesOnly(listOf(movies), emptySet()).isEmpty())
    }
}
