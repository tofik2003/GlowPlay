package com.glowplay.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glowplay.player.GlowPlayApp
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.PlaybackStore
import com.glowplay.player.data.local.UserPreferences
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
    val query: String = "",
    val sort: VideoSort = VideoSort.DATE_NEW,
    val loading: Boolean = true,
    val preferences: AppPreferences = AppPreferences(),
)

class LibraryViewModel(
    private val videos: VideoRepository,
    private val prefs: UserPreferences,
    private val playbackStore: PlaybackStore,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<LibraryUiState> = combine(
        videos.observeVideos(),
        prefs.flow,
        playbackStore.positions,
        query,
    ) { all, preferences, positions, q ->
        val filtered = VideoCatalog.sort(VideoCatalog.filter(all, q), preferences.sort)
        LibraryUiState(
            videos = filtered,
            folders = VideoCatalog.groupByFolder(filtered),
            recents = VideoCatalog.recents(all, positions),
            query = q,
            sort = preferences.sort,
            loading = false,
            preferences = preferences,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQuery(value: String) {
        query.value = value
    }

    fun onSort(sort: VideoSort) {
        viewModelScope.launch { prefs.setSort(sort) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlowPlayApp
                LibraryViewModel(app.videos, app.prefs, app.playbackStore)
            }
        }
    }
}
