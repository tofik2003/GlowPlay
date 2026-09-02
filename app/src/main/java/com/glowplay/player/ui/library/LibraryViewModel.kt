package com.glowplay.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glowplay.player.GlowPlayApp
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.FavoritesStore
import com.glowplay.player.data.local.LibraryViewMode
import com.glowplay.player.data.local.PlaybackStore
import com.glowplay.player.data.local.UserPreferences
import com.glowplay.player.data.model.DurationFilter
import com.glowplay.player.data.model.FolderItem
import com.glowplay.player.data.model.RecentItem
import com.glowplay.player.data.model.VideoItem
import com.glowplay.player.data.repository.VideoRepository
import com.glowplay.player.enhance.VideoCatalog
import com.glowplay.player.enhance.VideoSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val videos: List<VideoItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val recents: List<RecentItem> = emptyList(),
    val favorites: List<VideoItem> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val query: String = "",
    val sort: VideoSort = VideoSort.DATE_NEW,
    val durationFilter: DurationFilter = DurationFilter.ANY,
    val loading: Boolean = true,
    val preferences: AppPreferences = AppPreferences(),
)

class LibraryViewModel(
    private val videos: VideoRepository,
    private val prefs: UserPreferences,
    private val playbackStore: PlaybackStore,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val durationFilter = MutableStateFlow(DurationFilter.ANY)

    private data class Base(
        val all: List<VideoItem>,
        val preferences: AppPreferences,
        val positions: Map<String, Long>,
        val query: String,
        val duration: DurationFilter,
    )

    private val base: kotlinx.coroutines.flow.Flow<Base> = combine(
        videos.observeVideos(),
        prefs.flow,
        playbackStore.positions,
        query,
        durationFilter,
    ) { all, preferences, positions, q, duration ->
        Base(all, preferences, positions, q, duration)
    }

    val state: StateFlow<LibraryUiState> = combine(
        base,
        favoritesStore.favorites,
    ) { b, favoriteKeys ->
        val searched = VideoCatalog.filter(b.all, b.query)
        val durationFiltered = VideoCatalog.filterByDuration(searched, b.duration)
        val sorted = VideoCatalog.sort(durationFiltered, b.preferences.sort)
        LibraryUiState(
            videos = sorted,
            folders = VideoCatalog.groupByFolder(sorted),
            recents = VideoCatalog.recents(b.all, b.positions),
            favorites = VideoCatalog.sort(VideoCatalog.favoritesOnly(b.all, favoriteKeys), b.preferences.sort),
            favoriteKeys = favoriteKeys,
            query = b.query,
            sort = b.preferences.sort,
            durationFilter = b.duration,
            loading = false,
            preferences = b.preferences,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQuery(value: String) {
        query.value = value
    }

    fun onSort(sort: VideoSort) {
        viewModelScope.launch { prefs.setSort(sort) }
    }

    fun onDurationFilter(filter: DurationFilter) {
        durationFilter.value = filter
    }

    fun onViewMode(mode: LibraryViewMode) {
        viewModelScope.launch { prefs.setViewMode(mode) }
    }

    fun onToggleFavorite(mediaKey: String) {
        viewModelScope.launch { favoritesStore.toggle(mediaKey) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlowPlayApp
                LibraryViewModel(app.videos, app.prefs, app.playbackStore, app.favoritesStore)
            }
        }
    }
}
