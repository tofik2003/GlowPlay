package com.glowplay.player.ui.player

import android.app.Application
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.glowplay.player.GlowPlayApp
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.PlaybackStore
import com.glowplay.player.data.local.UserPreferences
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.GlowEffects
import com.glowplay.player.playback.GlowPlayerFactory
import com.glowplay.player.playback.PlayerHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectMode { FIT, FILL, ZOOM }

data class PlayerUiState(
    val title: String = "",
    val playing: Boolean = false,
    val buffering: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val locked: Boolean = false,
    val controlsVisible: Boolean = true,
    val enhanceOpen: Boolean = false,
    val eqOpen: Boolean = false,
    val preset: EnhancePreset = EnhancePreset.GLOW,
    val enhance: EnhanceSettings = EnhanceSettings(),
    val aspect: AspectMode = AspectMode.FIT,
    val audioTracks: List<String> = emptyList(),
    val textTracks: List<String> = emptyList(),
    val selectedAudio: Int = 0,
    val selectedText: Int = -1,
    val error: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val holdBoost: Boolean = false,
)

class PlayerViewModel(
    application: Application,
    private val prefs: UserPreferences,
    private val playbackStore: PlaybackStore,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    var player: ExoPlayer
        private set

    private var equalizer: Equalizer? = null
    private var mediaKey: String = ""
    private var playlist: List<String> = emptyList()
    private var progressJob: Job? = null
    private var hideJob: Job? = null
    private var lastAppliedSignature: String = ""

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(playing = isPlaying) }
            if (isPlaying) scheduleHide() else showControls()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    buffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = player.duration.coerceAtLeast(0L),
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                viewModelScope.launch { persistPosition(forceClear = true) }
                if (_state.value.preferences.autoplayNext) player.seekToNextMediaItem()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTracks(tracks)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) attachEqualizer(audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = error.message ?: "Playback error", buffering = false) }
        }
    }

    init {
        player = PlayerHolder.obtain(application, hardware = true)
        player.addListener(listener)
        viewModelScope.launch {
            prefs.flow.collect { preferences ->
                _state.update { it.copy(preferences = preferences) }
            }
        }
        progressJob = viewModelScope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        playing = player.isPlaying,
                    )
                }
                delay(200)
            }
        }
    }

    fun prepare(
        uri: Uri,
        title: String,
        key: String,
        playlistUris: List<String>,
        startIndex: Int,
    ) {
        val resolvedKey = key.ifBlank { uri.toString() }
        mediaKey = resolvedKey
        playlist = playlistUris.ifEmpty { listOf(uri.toString()) }
        val items = playlist.map { MediaItem.fromUri(it) }
        val index = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        player.setMediaItems(items, index, C.TIME_UNSET)
        viewModelScope.launch {
            val preferences = prefs.flow.first()
            val resume = if (preferences.rememberPosition) playbackStore.get(resolvedKey) else 0L
            if (resume > 1_000L) player.seekTo(resume)
            val preset = preferences.defaultPreset
            val enhance = preset.settingsOr(preferences.customEnhance)
            _state.update {
                it.copy(
                    title = title.ifBlank { uri.lastPathSegment ?: "GlowPlay" },
                    preset = preset,
                    enhance = enhance,
                    error = null,
                )
            }
            applyEnhance(enhance, preset)
            player.prepare()
            player.playWhenReady = true
            attachEqualizer()
            showControls()
        }
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, duration))
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed, holdBoost = false) }
    }

    fun setHoldBoost(active: Boolean) {
        if (!_state.value.preferences.longPressSpeed) return
        if (active) {
            player.setPlaybackSpeed(2f)
            _state.update { it.copy(holdBoost = true) }
        } else {
            player.setPlaybackSpeed(_state.value.speed)
            _state.update { it.copy(holdBoost = false) }
        }
    }

    fun toggleLock() {
        val locked = !_state.value.locked
        _state.update { it.copy(locked = locked, controlsVisible = !locked) }
    }

    fun cycleAspect(): AspectMode {
        val next = when (_state.value.aspect) {
            AspectMode.FIT -> AspectMode.FILL
            AspectMode.FILL -> AspectMode.ZOOM
            AspectMode.ZOOM -> AspectMode.FIT
        }
        _state.update { it.copy(aspect = next) }
        return next
    }

    fun showControls() {
        if (_state.value.locked) return
        _state.update { it.copy(controlsVisible = true) }
        scheduleHide()
    }

    fun toggleControls() {
        if (_state.value.locked) return
        if (_state.value.controlsVisible) {
            hideJob?.cancel()
            _state.update { it.copy(controlsVisible = false, enhanceOpen = false, eqOpen = false) }
        } else {
            showControls()
        }
    }

    fun setEnhanceOpen(open: Boolean) {
        _state.update { it.copy(enhanceOpen = open, eqOpen = false, controlsVisible = true) }
        hideJob?.cancel()
    }

    fun setEqOpen(open: Boolean) {
        _state.update { it.copy(eqOpen = open, enhanceOpen = false, controlsVisible = true) }
        hideJob?.cancel()
    }

    fun setPreset(preset: EnhancePreset) {
        val enhance = preset.settingsOr(_state.value.preferences.customEnhance)
        _state.update { it.copy(preset = preset, enhance = enhance) }
        applyEnhance(enhance, preset)
        viewModelScope.launch { prefs.setPreset(preset) }
    }

    fun updateEnhance(transform: (EnhanceSettings) -> EnhanceSettings) {
        val next = transform(_state.value.enhance).clamped().copy(enabled = true)
        _state.update { it.copy(preset = EnhancePreset.CUSTOM, enhance = next) }
        applyEnhance(next, EnhancePreset.CUSTOM)
        viewModelScope.launch { prefs.setCustomEnhance(next) }
    }

    fun selectAudio(index: Int) {
        val parameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
        player.trackSelectionParameters = parameters
        _state.update { it.copy(selectedAudio = index) }
    }

    fun selectText(index: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, index < 0)
            .build()
        _state.update { it.copy(selectedText = index) }
    }

    fun applyEqPreset(kind: EqKind) {
        val eq = equalizer ?: return
        runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            fun setAll(vararg levels: Int) {
                repeat(bands) { index ->
                    val level = (levels.getOrNull(index) ?: 0).toShort()
                    eq.setBandLevel(index.toShort(), level.coerceIn(range[0], range[1]))
                }
            }
            when (kind) {
                EqKind.FLAT -> setAll(0, 0, 0, 0, 0)
                EqKind.BASS -> setAll(900, 600, 100, -50, 0)
                EqKind.TREBLE -> setAll(-50, 0, 200, 700, 900)
                EqKind.VOICE -> setAll(-200, 100, 700, 500, 0)
                EqKind.MOVIE -> setAll(400, 200, 0, 250, 450)
            }
            eq.enabled = true
        }
    }

    fun volumeDelta(contextReady: AudioManager, delta: Int) {
        contextReady.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0,
        )
    }

    fun persist() {
        viewModelScope.launch {
            persistPosition(forceClear = false)
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        hideJob?.cancel()
        enhanceJob?.cancel()
        runCatching { player.removeListener(listener) }
        runCatching { equalizer?.release() }
        equalizer = null
        super.onCleared()
    }

    private var enhanceJob: Job? = null

    private fun applyEnhance(settings: EnhanceSettings, preset: EnhancePreset) {
        enhanceJob?.cancel()
        enhanceJob = viewModelScope.launch {
            delay(150)
            val active = if (preset == EnhancePreset.OFF) settings.copy(enabled = false) else settings
            val commands = GlowEffects.commands(active)
            val signature = preset.storageKey + commands.joinToString { "${it.type}:${it.value}" }
            if (signature == lastAppliedSignature) return@launch
            lastAppliedSignature = signature
            runCatching {
                player.setVideoEffects(GlowPlayerFactory.toMedia3Effects(commands))
            }
        }
    }

    private fun attachEqualizer(audioSessionId: Int = player.audioSessionId) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        }
    }

    private fun refreshTracks(tracks: Tracks) {
        val audio = mutableListOf<String>()
        val text = mutableListOf<String>()
        tracks.groups.forEach { group ->
            val type = group.type
            repeat(group.length) { i ->
                val format = group.getTrackFormat(i)
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                when (type) {
                    C.TRACK_TYPE_AUDIO -> audio += label
                    C.TRACK_TYPE_TEXT -> text += label
                }
            }
        }
        _state.update { it.copy(audioTracks = audio, textTracks = text) }
    }

    private suspend fun persistPosition(forceClear: Boolean) {
        if (mediaKey.isBlank()) return
        val pos = player.currentPosition
        val dur = player.duration
        if (forceClear || pos < 5_000L || (dur > 0 && pos >= dur - 8_000L)) {
            playbackStore.clear(mediaKey)
        } else {
            playbackStore.save(mediaKey, pos)
        }
    }

    private fun scheduleHide() {
        hideJob?.cancel()
        hideJob = viewModelScope.launch {
            delay(3_600)
            if (!_state.value.enhanceOpen && !_state.value.eqOpen && !_state.value.locked) {
                _state.update { it.copy(controlsVisible = false) }
            }
        }
    }

    enum class EqKind { FLAT, BASS, TREBLE, VOICE, MOVIE }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlowPlayApp
                PlayerViewModel(app, app.prefs, app.playbackStore)
            }
        }
    }
}
