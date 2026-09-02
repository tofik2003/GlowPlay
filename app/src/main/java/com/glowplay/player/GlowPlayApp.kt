package com.glowplay.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.glowplay.player.data.local.FavoritesStore
import com.glowplay.player.data.local.PlaybackStore
import com.glowplay.player.data.local.UserPreferences
import com.glowplay.player.data.repository.VideoRepository

class GlowPlayApp : Application(), ImageLoaderFactory {
    lateinit var videos: VideoRepository
        private set
    lateinit var prefs: UserPreferences
        private set
    lateinit var playbackStore: PlaybackStore
        private set
    lateinit var favoritesStore: FavoritesStore
        private set

    override fun onCreate() {
        super.onCreate()
        videos = VideoRepository(this)
        prefs = UserPreferences(this)
        playbackStore = PlaybackStore(this)
        favoritesStore = FavoritesStore(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
}
