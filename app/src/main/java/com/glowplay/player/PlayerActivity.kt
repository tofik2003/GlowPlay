package com.glowplay.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.glowplay.player.ui.player.PlayerScreen
import com.glowplay.player.ui.player.PlayerViewModel
import com.glowplay.player.ui.player.rotateActivity
import com.glowplay.player.ui.theme.GlowPlayTheme

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerViewModel: PlayerViewModel
    private var inPip = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playerViewModel = ViewModelProvider(this, PlayerViewModel.Factory)[PlayerViewModel::class.java]
        updateSystemBars()
        handleIntent(intent)
        setContent {
            val state by playerViewModel.state.collectAsStateWithLifecycle()
            GlowPlayTheme {
                PlayerScreen(
                    state = state,
                    playerViewFactory = {
                        PlayerView(this).apply {
                            player = playerViewModel.player
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    onBack = { finish() },
                    onTogglePlay = playerViewModel::togglePlay,
                    onSeek = playerViewModel::seekTo,
                    onSeekBy = playerViewModel::seekBy,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onToggleLock = playerViewModel::toggleLock,
                    onToggleControls = playerViewModel::toggleControls,
                    onShowControls = playerViewModel::showControls,
                    onCycleAspect = { playerViewModel.cycleAspect() },
                    onSpeed = playerViewModel::setSpeed,
                    onHoldBoost = playerViewModel::setHoldBoost,
                    onPreset = playerViewModel::setPreset,
                    onEnhance = playerViewModel::updateEnhance,
                    onEnhanceOpen = playerViewModel::setEnhanceOpen,
                    onEqOpen = playerViewModel::setEqOpen,
                    onEqPreset = playerViewModel::applyEqPreset,
                    onLoudness = playerViewModel::setLoudness,
                    onBass = playerViewModel::setBass,
                    onSurround = playerViewModel::setSurround,
                    onTracksOpen = playerViewModel::setTracksOpen,
                    onSelectAudio = playerViewModel::selectAudio,
                    onSelectText = playerViewModel::selectText,
                    onSubtitleSize = playerViewModel::setSubtitleSize,
                    onSubtitlePosition = playerViewModel::setSubtitlePosition,
                    onPip = { enterPip() },
                    onRotate = { rotateActivity(this) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerViewModel.state.value.preferences.pipOnLeave && playerViewModel.state.value.playing) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            playerViewModel.setEnhanceOpen(false)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBars()
    }

    private fun updateSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val immersive = playerViewModel.state.value.preferences.immersiveLandscape &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.persist()
        if (!inPip && !isChangingConfigurations) {
            playerViewModel.player.pause()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data
            ?: intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
            ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
        val key = intent.getStringExtra(EXTRA_KEY).orEmpty().ifBlank { uri.toString() }
        val playlist = intent.getStringArrayListExtra(EXTRA_PLAYLIST)?.filterNotNull().orEmpty()
        val index = playlist.indexOf(uri.toString()).coerceAtLeast(0)
        playerViewModel.prepare(uri, title, key, playlist, index)
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        val sourceRect = Rect()
        window.decorView.getGlobalVisibleRect(sourceRect)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setSourceRectHint(sourceRect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }
        enterPictureInPictureMode(builder.build())
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_KEY = "extra_key"
        const val EXTRA_PLAYLIST = "extra_playlist"

        fun intent(
            context: Context,
            uri: String,
            title: String,
            key: String,
            playlist: List<String>,
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                data = Uri.parse(uri)
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_KEY, key)
                putStringArrayListExtra(EXTRA_PLAYLIST, ArrayList(playlist))
            }
        }
    }
}
