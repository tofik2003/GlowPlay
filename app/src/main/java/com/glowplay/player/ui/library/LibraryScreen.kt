package com.glowplay.player.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.glowplay.player.R
import com.glowplay.player.data.model.FolderItem
import com.glowplay.player.data.model.RecentItem
import com.glowplay.player.data.model.VideoItem
import com.glowplay.player.enhance.VideoCatalog
import com.glowplay.player.enhance.VideoSort
import com.glowplay.player.ui.components.GlowTitle
import com.glowplay.player.ui.components.NeonCard
import com.glowplay.player.ui.components.ResumeBar
import com.glowplay.player.util.FileSizeFormatter
import com.glowplay.player.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onQuery: (String) -> Unit,
    onSort: (VideoSort) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }

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
                        Text(stringResource(R.string.app_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    stringResource(R.string.tab_recent),
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) },
                    )
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                tab == 0 -> VideoGrid(state.videos, state, onOpenVideo)
                tab == 1 -> FolderList(state.folders, onOpenFolder)
                else -> RecentList(state.recents, state.videos, onOpenVideo)
            }
        }
    }
}

@Composable
private fun VideoGrid(
    videos: List<VideoItem>,
    state: LibraryUiState,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
) {
    if (videos.isEmpty()) {
        EmptyPane(stringResource(R.string.empty_library))
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
            VideoCard(video = video, progress = 0f) {
                onOpenVideo(video, VideoCatalog.playlistUris(state.videos, video))
            }
        }
    }
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
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(folder.name, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${folder.videoCount} • ${TimeFormatter.formatMs(folder.totalDurationMs)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onOpenVideo: (VideoItem, List<String>) -> Unit,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(4.dp),
                )
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
            Text(
                text = "${video.folderName.ifBlank { stringResource(R.string.unknown_folder) }} • ${FileSizeFormatter.format(video.sizeBytes)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            )
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
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
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
fun FolderDetailScreen(
    folder: FolderItem,
    videos: List<VideoItem>,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem, List<String>) -> Unit,
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
            TextButton(onClick = onBack) { Text("Back", color = MaterialTheme.colorScheme.primary) }
            Column {
                GlowTitle(folder.name)
                Text("${items.size} videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        VideoGrid(items, LibraryUiState(videos = items), onOpenVideo)
    }
}
