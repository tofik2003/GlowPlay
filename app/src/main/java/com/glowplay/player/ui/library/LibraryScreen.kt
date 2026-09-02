package com.glowplay.player.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.glowplay.player.R
import com.glowplay.player.data.local.LibraryViewMode
import com.glowplay.player.data.model.DurationFilter
import com.glowplay.player.data.model.FolderItem
import com.glowplay.player.data.model.RecentItem
import com.glowplay.player.data.model.VideoItem
import com.glowplay.player.enhance.VideoCatalog
import com.glowplay.player.enhance.VideoSort
import com.glowplay.player.ui.components.GlowBadge
import com.glowplay.player.ui.components.GlowTitle
import com.glowplay.player.ui.components.NeonCard
import com.glowplay.player.ui.components.ResumeBar
import com.glowplay.player.ui.components.ShimmerBlock
import com.glowplay.player.util.FileSizeFormatter
import com.glowplay.player.util.TimeFormatter
import com.glowplay.player.util.VideoLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onQuery: (String) -> Unit,
    onSort: (VideoSort) -> Unit,
    onDurationFilter: (DurationFilter) -> Unit,
    onViewMode: (LibraryViewMode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    val viewMode = state.preferences.viewMode

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GlowTitle(text = stringResource(R.string.app_name))
                        Text(
                            stringResource(R.string.app_tagline),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(
                        onClick = { onViewMode(if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID) },
                    ) {
                        Icon(
                            if (viewMode == LibraryViewMode.GRID) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                            contentDescription = stringResource(R.string.toggle_view),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = stringResource(R.string.sort), tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            VideoSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(option)) },
                                    onClick = {
                                        sortMenu = false
                                        onSort(option)
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    placeholder = { Text(stringResource(R.string.search_videos)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                if (tab == 0) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DurationFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.durationFilter == filter,
                                onClick = { onDurationFilter(filter) },
                                label = { Text(durationFilterLabel(filter)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!permissionGranted) {
                PermissionPane(onRequestPermission)
                return@Column
            }
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[tab]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                listOf(
                    stringResource(R.string.tab_videos),
                    stringResource(R.string.tab_folders),
                    stringResource(R.string.tab_favorites),
                    stringResource(R.string.tab_recent),
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) },
                    )
                }
            }
            AnimatedContent(
                targetState = tab to state.loading,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "libraryTab",
            ) { (currentTab, loading) ->
                when {
                    loading -> LibraryLoadingGrid()
                    currentTab == 0 -> VideoGrid(state.videos, state, viewMode, onOpenVideo, onToggleFavorite)
                    currentTab == 1 -> FolderList(state.folders, onOpenFolder)
                    currentTab == 2 -> FavoritesGrid(state.favorites, state, viewMode, onOpenVideo, onToggleFavorite)
                    else -> RecentList(state.recents, state.videos, state.favoriteKeys, onOpenVideo, onToggleFavorite)
                }
            }
        }
    }
}

@Composable
private fun LibraryLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(168.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(8) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 13f),
            )
        }
    }
}

@Composable
private fun VideoGrid(
    videos: List<VideoItem>,
    state: LibraryUiState,
    viewMode: LibraryViewMode,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (videos.isEmpty()) {
        EmptyPane(stringResource(R.string.empty_library))
        return
    }
    if (viewMode == LibraryViewMode.LIST) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(videos, key = { it.mediaKey }) { video ->
                VideoCard(
                    video = video,
                    progress = 0f,
                    listMode = true,
                    favorite = video.mediaKey in state.favoriteKeys,
                    onToggleFavorite = { onToggleFavorite(video.mediaKey) },
                ) {
                    onOpenVideo(video, VideoCatalog.playlistUris(state.videos, video))
                }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(168.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(videos, key = { it.mediaKey }) { video ->
            VideoCard(
                video = video,
                progress = 0f,
                favorite = video.mediaKey in state.favoriteKeys,
                onToggleFavorite = { onToggleFavorite(video.mediaKey) },
            ) {
                onOpenVideo(video, VideoCatalog.playlistUris(state.videos, video))
            }
        }
    }
}

@Composable
private fun FavoritesGrid(
    favorites: List<VideoItem>,
    state: LibraryUiState,
    viewMode: LibraryViewMode,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (favorites.isEmpty()) {
        EmptyPane(stringResource(R.string.empty_favorites))
        return
    }
    VideoGrid(favorites, state.copy(videos = favorites), viewMode, onOpenVideo, onToggleFavorite)
}

@Composable
private fun FolderList(folders: List<FolderItem>, onOpenFolder: (FolderItem) -> Unit) {
    if (folders.isEmpty()) {
        EmptyPane(stringResource(R.string.empty_library))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(folders, key = { it.id }) { folder ->
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFolder(folder) },
                glow = MaterialTheme.colorScheme.secondary,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(folder.name, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${folder.videoCount} • ${TimeFormatter.formatMs(folder.totalDurationMs)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentList(
    recents: List<RecentItem>,
    all: List<VideoItem>,
    favoriteKeys: Set<String>,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (recents.isEmpty()) {
        EmptyPane(stringResource(R.string.empty_recent))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recents, key = { it.video.mediaKey }) { recent ->
            VideoCard(
                video = recent.video,
                progress = TimeFormatter.progress(recent.positionMs, recent.video.durationMs),
                listMode = true,
                favorite = recent.video.mediaKey in favoriteKeys,
                onToggleFavorite = { onToggleFavorite(recent.video.mediaKey) },
            ) {
                onOpenVideo(recent.video, VideoCatalog.playlistUris(all, recent.video))
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoItem,
    progress: Float,
    listMode: Boolean = false,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    NeonCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (listMode) 16f / 7f else 16f / 10f)
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(video.uriString))
                        .videoFrameMillis(1_200)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.cd_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    VideoLabels.resolutionLabel(video.width, video.height)?.let {
                        GlowBadge(it, containerColor = Color.Black.copy(alpha = 0.55f), contentColor = Color.White)
                    }
                }
                Text(
                    text = TimeFormatter.formatMs(video.durationMs),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                        .padding(6.dp),
                )
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .padding(2.dp),
                    ) {
                        Icon(
                            if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite),
                            tint = if (favorite) MaterialTheme.colorScheme.secondary else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (progress > 0f) {
                    ResumeBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                text = video.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${video.folderName.ifBlank { stringResource(R.string.unknown_folder) }} • ${FileSizeFormatter.format(video.sizeBytes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                VideoLabels.formatLabel(video.name)?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionPane(onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NeonCard(modifier = Modifier.padding(24.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                GlowTitle(stringResource(R.string.permission_title))
                Text(
                    stringResource(R.string.permission_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                )
                Button(onClick = onRequestPermission) { Text(stringResource(R.string.grant_permission)) }
                TextButton(
                    onClick = {
                        launcher.launch(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.open_settings)) }
            }
        }
    }
}

@Composable
private fun EmptyPane(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun sortLabel(sort: VideoSort): String = when (sort) {
    VideoSort.DATE_NEW -> stringResource(R.string.sort_date_new)
    VideoSort.DATE_OLD -> stringResource(R.string.sort_date_old)
    VideoSort.NAME -> stringResource(R.string.sort_name)
    VideoSort.DURATION -> stringResource(R.string.sort_duration)
    VideoSort.SIZE -> stringResource(R.string.sort_size)
}

@Composable
private fun durationFilterLabel(filter: DurationFilter): String = when (filter) {
    DurationFilter.ANY -> stringResource(R.string.duration_any)
    DurationFilter.SHORT -> stringResource(R.string.duration_short)
    DurationFilter.MEDIUM -> stringResource(R.string.duration_medium)
    DurationFilter.LONG -> stringResource(R.string.duration_long)
}

@Composable
fun FolderDetailScreen(
    folder: FolderItem,
    videos: List<VideoItem>,
    favoriteKeys: Set<String>,
    viewMode: LibraryViewMode,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val items = videos.filter { it.folderId == folder.id }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) }
            Column {
                GlowTitle(folder.name)
                Text(
                    stringResource(R.string.videos_count, items.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        VideoGrid(
            items,
            LibraryUiState(videos = items, favoriteKeys = favoriteKeys),
            viewMode,
            onOpenVideo,
            onToggleFavorite,
        )
    }
}
