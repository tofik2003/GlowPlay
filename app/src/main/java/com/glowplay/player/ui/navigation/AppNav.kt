package com.glowplay.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glowplay.player.PlayerActivity
import com.glowplay.player.data.model.FolderItem
import com.glowplay.player.ui.library.FolderDetailScreen
import com.glowplay.player.ui.library.LibraryScreen
import com.glowplay.player.ui.library.LibraryViewModel
import com.glowplay.player.ui.settings.SettingsScreen
import com.glowplay.player.ui.settings.SettingsViewModel

object Routes {
    const val Library = "library"
    const val Settings = "settings"
    const val Folder = "folder/{id}/{name}"
    fun folder(id: Long, name: String) = "folder/$id/${android.net.Uri.encode(name)}"
}

@Composable
fun GlowPlayNav(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    val nav = rememberNavController()
    val libraryVm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    NavHost(navController = nav, startDestination = Routes.Library) {
        composable(Routes.Library) {
            LibraryScreen(
                state = libraryState,
                permissionGranted = permissionGranted,
                onRequestPermission = onRequestPermission,
                onQuery = libraryVm::onQuery,
                onSort = libraryVm::onSort,
                onOpenSettings = { nav.navigate(Routes.Settings) },
                onOpenVideo = { video, playlist ->
                    context.startActivity(
                        PlayerActivity.intent(
                            context = context,
                            uri = video.uriString,
                            title = video.name,
                            key = video.mediaKey,
                            playlist = playlist,
                        ),
                    )
                },
                onOpenFolder = { folder: FolderItem ->
                    nav.navigate(Routes.folder(folder.id, folder.name))
                },
            )
        }
        composable(
            route = Routes.Folder,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val name = entry.arguments?.getString("name").orEmpty()
            val folder = libraryState.folders.firstOrNull { it.id == id }
                ?: FolderItem(id, name, 0, 0, "")
            FolderDetailScreen(
                folder = folder,
                videos = libraryState.videos,
                onBack = { nav.popBackStack() },
                onOpenVideo = { video, playlist ->
                    context.startActivity(
                        PlayerActivity.intent(
                            context = context,
                            uri = video.uriString,
                            title = video.name,
                            key = video.mediaKey,
                            playlist = playlist,
                        ),
                    )
                },
            )
        }
        composable(Routes.Settings) {
            val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            val settings by settingsVm.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = settings,
                onBack = { nav.popBackStack() },
                onRemember = settingsVm::setRemember,
                onAutoplay = settingsVm::setAutoplay,
                onGestures = settingsVm::setGestures,
                onHoldSpeed = settingsVm::setHoldSpeed,
                onHardware = settingsVm::setHardware,
                onPip = settingsVm::setPip,
                onPreset = settingsVm::setPreset,
                onThemeMode = settingsVm::setThemeMode,
                onSeekStep = settingsVm::setSeekStep,
                onHoldSpeedValue = settingsVm::setHoldSpeedValue,
            )
        }
    }
}
