package com.glowplay.player.ui.player

import android.app.Application
import android.media.AudioManager
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.glowplay.player.GlowPlayApp
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.PlaybackStore
import com.glowplay.player.data.local.UserPreferences
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.GlowEffects
import com.glowplay.player.playback.AudioEffects
import com.glowplay.player.playback.EqKind
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
    val loudness: Int = 0,
    val bass: Int = 0,
    val surround: Int = 0,
    val tracksOpen: Boolean = false,
    val subtitleSize: Float = 1f,
    val subtitlePosition: Float = 0.08f,
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

    private val audioEffects = AudioEffects()
    private var currentTracks: Tracks? = null
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
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) attachAudioEffects(audioSessionId)
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
                    loudness = preferences.loudness,
                    bass = preferences.bassBoost,
                    surround = preferences.surround,
                    subtitleSize = preferences.subtitleSize,
                    subtitlePosition = preferences.subtitlePosition,
                    error = null,
                )
            }
            applyEnhance(enhance, preset)
            player.prepare()
            player.playWhenReady = true
            attachAudioEffects()
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
            _state.update { it.copy(controlsVisible = false, enhanceOpen = false, eqOpen = false, tracksOpen = false) }
        } else {
            showControls()
        }
    }

    fun setEnhanceOpen(open: Boolean) {
        _state.update { it.copy(enhanceOpen = open, eqOpen = false, tracksOpen = false, controlsVisible = true) }
        hideJob?.cancel()
    }

    fun setEqOpen(open: Boolean) {
        _state.update { it.copy(eqOpen = open, enhanceOpen = false, tracksOpen = false, controlsVisible = true) }
        hideJob?.cancel()
    }

    fun setTracksOpen(open: Boolean) {
        _state.update { it.copy(tracksOpen = open, enhanceOpen = false, eqOpen = false, controlsVisible = true) }
        hideJob?.cancel()
    }

    fun setSubtitleSize(size: Float) {
        val value = size.coerceIn(0.5f, 2f)
        _state.update { it.copy(subtitleSize = value) }
        viewModelScope.launch { prefs.setSubtitleSize(value) }
    }

    fun setSubtitlePosition(position: Float) {
        val value = position.coerceIn(0f, 0.5f)
        _state.update { it.copy(subtitlePosition = value) }
        viewModelScope.launch { prefs.setSubtitlePosition(value) }
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
        selectTrack(C.TRACK_TYPE_AUDIO, index) { selected ->
            _state.update { it.copy(selectedAudio = selected) }
        }
    }

    fun selectText(index: Int) {
        selectTrack(C.TRACK_TYPE_TEXT, index) { selected ->
            _state.update { it.copy(selectedText = selected) }
        }
    }

    /**
     * Selects the [targetIndex]-th supported track of [type], or disables the
     * type entirely when [targetIndex] is negative (subtitles "Off").
     */
    private fun selectTrack(type: Int, targetIndex: Int, onSelected: (Int) -> Unit) {
        val tracks = currentTracks ?: return
        val builder = player.trackSelectionParameters.buildUpon()
        if (targetIndex < 0) {
            builder.setTrackTypeDisabled(type, true)
            player.trackSelectionParameters = builder.build()
            onSelected(-1)
            return
        }
        var seen = 0
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                if (seen == targetIndex) {
                    builder.setOverrideForType(
                        TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex),
                    )
                    player.trackSelectionParameters = builder.build()
                    onSelected(targetIndex)
                    return
                }
                seen++
            }
        }
    }

    fun applyEqPreset(kind: EqKind) {
        audioEffects.applyEq(kind)
    }

    fun setLoudness(mb: Int) {
        val value = mb.coerceIn(0, 2000)
        _state.update { it.copy(loudness = value) }
        audioEffects.setLoudness(value)
        viewModelScope.launch { prefs.setLoudness(value) }
    }

    fun setBass(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        _state.update { it.copy(bass = value) }
        audioEffects.setBass(value)
        viewModelScope.launch { prefs.setBass(value) }
    }

    fun setSurround(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        _state.update { it.copy(surround = value) }
        audioEffects.setSurround(value)
        viewModelScope.launch { prefs.setSurround(value) }
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
        audioEffects.release()
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

    private fun attachAudioEffects(audioSessionId: Int = player.audioSessionId) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        audioEffects.attach(audioSessionId)
        applyAudioEnhance()
    }

    private fun applyAudioEnhance() {
        val s = _state.value
        audioEffects.setLoudness(s.loudness)
        audioEffects.setBass(s.bass)
        audioEffects.setSurround(s.surround)
    }

    private fun refreshTracks(tracks: Tracks) {
        currentTracks = tracks
        val audio = mutableListOf<String>()
        val text = mutableListOf<String>()
        var selectedAudio = 0
        var selectedText = -1
        var audioSeen = 0
        var textSeen = 0
        tracks.groups.forEach { group ->
            val type = group.type
            val isAudio = type == C.TRACK_TYPE_AUDIO
            val isText = type == C.TRACK_TYPE_TEXT
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                when (type) {
                    C.TRACK_TYPE_AUDIO -> audio += label
                    C.TRACK_TYPE_TEXT -> text += label
                }
            }
            if (group.isSelected) {
                if (isAudio) selectedAudio = audioSeen
                if (isText) selectedText = textSeen
            }
            if (isAudio) audioSeen = audio.size
            if (isText) textSeen = text.size
        }
        _state.update {
            it.copy(
                audioTracks = audio,
                textTracks = text,
                selectedAudio = selectedAudio,
                selectedText = selectedText,
            )
        }
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
            if (!_state.value.enhanceOpen && !_state.value.eqOpen && !_state.value.tracksOpen && !_state.value.locked) {
                _state.update { it.copy(controlsVisible = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlowPlayApp
                PlayerViewModel(app, app.prefs, app.playbackStore)
            }
        }
    }
}
