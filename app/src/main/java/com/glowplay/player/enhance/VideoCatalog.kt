package com.glowplay.player.enhance

import com.glowplay.player.data.model.FolderItem
import com.glowplay.player.data.model.RecentItem
import com.glowplay.player.data.model.VideoItem

enum class VideoSort {
    DATE_NEW,
    DATE_OLD,
    NAME,
    DURATION,
    SIZE,
}

object VideoCatalog {
    fun groupByFolder(videos: List<VideoItem>): List<FolderItem> {
        return videos
            .groupBy { it.folderId to it.folderName.ifBlank { "Internal storage" } }
            .map { (key, items) ->
                FolderItem(
                    id = key.first,
                    name = key.second,
                    videoCount = items.size,
                    totalDurationMs = items.sumOf { it.durationMs },
                    coverUri = items.maxByOrNull { it.dateAddedEpochSec }?.uriString.orEmpty(),
                )
            }
            .sortedWith(compareByDescending<FolderItem> { it.videoCount }.thenBy { it.name.lowercase() })
    }

    fun filter(videos: List<VideoItem>, query: String): List<VideoItem> {
        val q = query.trim()
        if (q.isEmpty()) return videos
        return videos.filter { video ->
            video.name.contains(q, ignoreCase = true) ||
                video.folderName.contains(q, ignoreCase = true)
        }
    }

    fun sort(videos: List<VideoItem>, sort: VideoSort): List<VideoItem> = when (sort) {
        VideoSort.DATE_NEW -> videos.sortedByDescending { it.dateAddedEpochSec }
        VideoSort.DATE_OLD -> videos.sortedBy { it.dateAddedEpochSec }
        VideoSort.NAME -> videos.sortedBy { it.name.lowercase() }
        VideoSort.DURATION -> videos.sortedByDescending { it.durationMs }
        VideoSort.SIZE -> videos.sortedByDescending { it.sizeBytes }
    }

    fun recents(
        videos: List<VideoItem>,
        positions: Map<String, Long>,
        minPositionMs: Long = 5_000L,
        nearEndMs: Long = 8_000L,
    ): List<RecentItem> {
        if (positions.isEmpty()) return emptyList()
        val byKey = videos.associateBy { it.mediaKey }
        return positions.mapNotNull { (key, position) ->
            val video = byKey[key] ?: return@mapNotNull null
            if (position < minPositionMs) return@mapNotNull null
            if (video.durationMs > 0 && position >= video.durationMs - nearEndMs) return@mapNotNull null
            RecentItem(video, position)
        }.sortedByDescending { it.positionMs }
    }

    fun playlistUris(videos: List<VideoItem>, start: VideoItem): List<String> {
        val sameFolder = videos.filter { it.folderId == start.folderId }
        val ordered = sort(sameFolder, VideoSort.NAME)
        if (ordered.isEmpty()) return listOf(start.uriString)
        val index = ordered.indexOfFirst { it.id == start.id }.coerceAtLeast(0)
        return ordered.drop(index).map { it.uriString } + ordered.take(index).map { it.uriString }
    }
}
