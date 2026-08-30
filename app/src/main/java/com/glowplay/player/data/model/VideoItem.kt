package com.glowplay.player.data.model

data class VideoItem(
    val id: Long,
    val uriString: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedEpochSec: Long,
    val folderId: Long,
    val folderName: String,
) {
    val mediaKey: String get() = if (id > 0L) id.toString() else uriString
}

data class FolderItem(
    val id: Long,
    val name: String,
    val videoCount: Int,
    val totalDurationMs: Long,
    val coverUri: String,
)

data class RecentItem(
    val video: VideoItem,
    val positionMs: Long,
)
