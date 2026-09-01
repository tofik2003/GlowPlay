package com.glowplay.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.glowplay.player.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoRepository(context: Context) {
    private val appContext = context.applicationContext

    fun observeVideos(): Flow<List<VideoItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { trySend(queryVideos()) }
            }
        }
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }
        send(queryVideos())
        awaitClose {
            runCatching { appContext.contentResolver.unregisterContentObserver(observer) }
        }
    }.distinctUntilChanged()

    suspend fun queryVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val items = ArrayList<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        try {
            appContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sort,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    items += VideoItem(
                        id = id,
                        uriString = uri.toString(),
                        name = cursor.getString(nameCol) ?: "Video",
                        durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                        sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                        width = cursor.getInt(wCol).coerceAtLeast(0),
                        height = cursor.getInt(hCol).coerceAtLeast(0),
                        dateAddedEpochSec = cursor.getLong(dateCol),
                        folderId = cursor.getLong(bucketIdCol),
                        folderName = cursor.getString(bucketNameCol).orEmpty(),
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }
        items
    }

    fun parseUri(value: String): Uri = Uri.parse(value)
}
