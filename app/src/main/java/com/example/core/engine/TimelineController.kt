package com.example.core.engine

import com.example.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TimelineController(
    initialProject: Project = Project(title = "Cinematic Odyssey")
) {
    // History stack for Undo / Redo
    private val undoStack = mutableListOf<TimelineSnapshot>()
    private val redoStack = mutableListOf<TimelineSnapshot>()
    private val maxHistorySteps = 30

    data class TimelineSnapshot(
        val tracks: List<Track>,
        val playheadMs: Long,
        val durationMs: Long
    )

    private val _projectState = MutableStateFlow(initialProject)
    val projectState: StateFlow<Project> = _projectState.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(defaultInitialTracks())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedTrackId = MutableStateFlow<String?>("track_video_main")
    val selectedTrackId: StateFlow<String?> = _selectedTrackId.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f) // 0.5x to 4.0x
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _snappingEnabled = MutableStateFlow(true)
    val snappingEnabled: StateFlow<Boolean> = _snappingEnabled.asStateFlow()

    init {
        // Select first clip if exists
        val firstClip = _tracks.value.firstOrNull { it.clips.isNotEmpty() }?.clips?.firstOrNull()
        _selectedClipId.value = firstClip?.id
    }

    private fun saveSnapshot() {
        undoStack.add(
            TimelineSnapshot(
                tracks = _tracks.value.map { it.copy(clips = it.clips.map { c -> c.copy() }) },
                playheadMs = _playheadMs.value,
                durationMs = _projectState.value.durationMs
            )
        )
        if (undoStack.size > maxHistorySteps) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentSnapshot = TimelineSnapshot(_tracks.value, _playheadMs.value, _projectState.value.durationMs)
        redoStack.add(currentSnapshot)

        val previous = undoStack.removeAt(undoStack.lastIndex)
        _tracks.value = previous.tracks
        _playheadMs.value = previous.playheadMs
        _projectState.value = _projectState.value.copy(durationMs = previous.durationMs, updatedAt = System.currentTimeMillis())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentSnapshot = TimelineSnapshot(_tracks.value, _playheadMs.value, _projectState.value.durationMs)
        undoStack.add(currentSnapshot)

        val next = redoStack.removeAt(redoStack.lastIndex)
        _tracks.value = next.tracks
        _playheadMs.value = next.playheadMs
        _projectState.value = _projectState.value.copy(durationMs = next.durationMs, updatedAt = System.currentTimeMillis())
    }

    fun setPlayhead(timeMs: Long) {
        val maxDuration = _projectState.value.durationMs
        var targetTime = timeMs.coerceIn(0L, maxDuration)

        // Snapping logic if enabled
        if (_snappingEnabled.value) {
            val snapThresholdMs = 120L
            val snapPoints = getSnapPoints()
            val closest = snapPoints.firstOrNull { Math.abs(it - targetTime) <= snapThresholdMs }
            if (closest != null) {
                targetTime = closest
            }
        }
        _playheadMs.value = targetTime
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun stepFrame(forward: Boolean) {
        val frameMs = 1000L / _projectState.value.frameRate.coerceAtLeast(24)
        val delta = if (forward) frameMs else -frameMs
        setPlayhead(_playheadMs.value + delta)
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.4f, 4.0f)
    }

    fun toggleSnapping() {
        _snappingEnabled.value = !_snappingEnabled.value
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        if (clipId != null) {
            val parentTrack = _tracks.value.find { track -> track.clips.any { it.id == clipId } }
            if (parentTrack != null) {
                _selectedTrackId.value = parentTrack.id
            }
        }
    }

    fun getSelectedClip(): Clip? {
        val id = _selectedClipId.value ?: return null
        return _tracks.value.flatMap { it.clips }.find { it.id == id }
    }

    // ================= CLIP EDITING OPERATIONS ================= //

    fun splitClipAtPlayhead() {
        val currentPlayhead = _playheadMs.value
        val clip = getSelectedClip() ?: return

        // Verify playhead is inside clip bounds
        val clipStart = clip.startMs
        val clipEnd = clip.startMs + clip.durationMs
        if (currentPlayhead <= clipStart + 100L || currentPlayhead >= clipEnd - 100L) {
            return // Too close to edge to split
        }

        saveSnapshot()

        val splitOffset = currentPlayhead - clipStart
        val firstClip = clip.copy(
            id = UUID.randomUUID().toString(),
            durationMs = splitOffset,
            trimOutMs = clip.trimInMs + (splitOffset * clip.speed).toLong()
        )
        val secondClip = clip.copy(
            id = UUID.randomUUID().toString(),
            startMs = currentPlayhead,
            durationMs = clip.durationMs - splitOffset,
            trimInMs = clip.trimInMs + (splitOffset * clip.speed).toLong()
        )

        _tracks.value = _tracks.value.map { track ->
            if (track.clips.any { it.id == clip.id }) {
                val newClips = track.clips.flatMap { existing ->
                    if (existing.id == clip.id) listOf(firstClip, secondClip) else listOf(existing)
                }
                track.copy(clips = newClips)
            } else {
                track
            }
        }

        _selectedClipId.value = secondClip.id
        recalculateTotalDuration()
    }

    fun trimClipStart(clipId: String, newStartMs: Long) {
        saveSnapshot()
        _tracks.value = _tracks.value.map { track ->
            track.copy(clips = track.clips.map { clip ->
                if (clip.id == clipId) {
                    val delta = (newStartMs - clip.startMs).coerceAtLeast(0L)
                    val newDuration = (clip.durationMs - delta).coerceAtLeast(200L)
                    clip.copy(
                        startMs = newStartMs,
                        durationMs = newDuration,
                        trimInMs = clip.trimInMs + (delta * clip.speed).toLong()
                    )
                } else clip
            })
        }
        recalculateTotalDuration()
    }

    fun trimClipEnd(clipId: String, newDurationMs: Long) {
        saveSnapshot()
        _tracks.value = _tracks.value.map { track ->
            track.copy(clips = track.clips.map { clip ->
                if (clip.id == clipId) {
                    val safeDuration = newDurationMs.coerceAtLeast(200L)
                    clip.copy(
                        durationMs = safeDuration,
                        trimOutMs = clip.trimInMs + (safeDuration * clip.speed).toLong()
                    )
                } else clip
            })
        }
        recalculateTotalDuration()
    }

    fun deleteSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        saveSnapshot()
        _tracks.value = _tracks.value.map { track ->
            track.copy(clips = track.clips.filter { it.id != clipId })
        }
        _selectedClipId.value = null
        recalculateTotalDuration()
    }

    fun rippleDeleteSelectedClip() {
        val clip = getSelectedClip() ?: return
        val clipDuration = clip.durationMs
        val clipStart = clip.startMs
        saveSnapshot()

        _tracks.value = _tracks.value.map { track ->
            val filtered = track.clips.filter { it.id != clip.id }
            val shifted = filtered.map { other ->
                if (other.startMs >= clipStart + clipDuration) {
                    other.copy(startMs = other.startMs - clipDuration)
                } else other
            }
            track.copy(clips = shifted)
        }
        _selectedClipId.value = null
        recalculateTotalDuration()
    }

    fun duplicateSelectedClip() {
        val clip = getSelectedClip() ?: return
        saveSnapshot()
        val duplicated = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} (Copy)",
            startMs = clip.startMs + clip.durationMs + 100L
        )
        _tracks.value = _tracks.value.map { track ->
            if (track.clips.any { it.id == clip.id }) {
                track.copy(clips = track.clips + duplicated)
            } else track
        }
        _selectedClipId.value = duplicated.id
        recalculateTotalDuration()
    }

    fun changeClipSpeed(speed: Float) {
        val clip = getSelectedClip() ?: return
        saveSnapshot()
        val ratio = clip.speed / speed
        val newDuration = (clip.durationMs * ratio).toLong().coerceAtLeast(100L)

        updateClip(clip.copy(speed = speed, durationMs = newDuration))
        recalculateTotalDuration()
    }

    fun updateClip(updatedClip: Clip) {
        _tracks.value = _tracks.value.map { track ->
            track.copy(clips = track.clips.map { if (it.id == updatedClip.id) updatedClip else it })
        }
    }

    fun updateColorGrading(grading: ColorGrading) {
        val clip = getSelectedClip() ?: return
        updateClip(clip.copy(colorGrading = grading))
    }

    fun updateChromaKey(chroma: ChromaKey) {
        val clip = getSelectedClip() ?: return
        updateClip(clip.copy(chromaKey = chroma))
    }

    fun setTransition(type: TransitionType, durationMs: Long = 500L) {
        val clip = getSelectedClip() ?: return
        saveSnapshot()
        updateClip(clip.copy(transitionIn = type, transitionDurationMs = durationMs))
    }

    fun addKeyframe(property: String, value: Float) {
        val clip = getSelectedClip() ?: return
        saveSnapshot()
        val currentOffset = (_playheadMs.value - clip.startMs).coerceIn(0L, clip.durationMs)
        val newKeyframe = Keyframe(property = property, timeMs = currentOffset, value = value)
        val filtered = clip.keyframes.filterNot { it.property == property && Math.abs(it.timeMs - currentOffset) < 50L }
        updateClip(clip.copy(keyframes = (filtered + newKeyframe).sortedBy { it.timeMs }))
    }

    fun removeKeyframe(keyframeId: String) {
        val clip = getSelectedClip() ?: return
        saveSnapshot()
        updateClip(clip.copy(keyframes = clip.keyframes.filterNot { it.id == keyframeId }))
    }

    fun addTextLayer(text: String, font: String = "Cinematic Sans", color: Long = 0xFFFFFFFF) {
        saveSnapshot()
        val overlay = TextOverlay(
            text = text,
            fontName = font,
            textColor = color,
            fontSizeSp = 32f,
            positionY = 0.8f
        )
        val newClip = Clip(
            trackId = "track_text",
            name = "Title: $text",
            startMs = _playheadMs.value,
            durationMs = 3500L,
            textOverlay = overlay
        )
        _tracks.value = _tracks.value.map { track ->
            if (track.type == TrackType.TEXT) {
                track.copy(clips = track.clips + newClip)
            } else track
        }
        _selectedClipId.value = newClip.id
        recalculateTotalDuration()
    }

    fun addClipToTrack(trackType: TrackType, name: String, durationMs: Long = 4000L, mediaUri: String = "") {
        saveSnapshot()
        val targetTrack = _tracks.value.firstOrNull { it.type == trackType } ?: _tracks.value.first()
        val lastClipEnd = targetTrack.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
        val startTime = maxOf(_playheadMs.value, lastClipEnd)

        val newClip = Clip(
            trackId = targetTrack.id,
            name = name,
            mediaUri = mediaUri,
            startMs = startTime,
            durationMs = durationMs,
            trimOutMs = durationMs
        )
        _tracks.value = _tracks.value.map { track ->
            if (track.id == targetTrack.id) track.copy(clips = track.clips + newClip) else track
        }
        _selectedClipId.value = newClip.id
        recalculateTotalDuration()
    }

    fun toggleTrackMute(trackId: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
        }
    }

    fun toggleTrackLock(trackId: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isLocked = !it.isLocked) else it
        }
    }

    fun toggleTrackVisibility(trackId: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isVisible = !it.isVisible) else it
        }
    }

    private fun getSnapPoints(): List<Long> {
        val points = mutableListOf(0L, _projectState.value.durationMs)
        for (track in _tracks.value) {
            for (clip in track.clips) {
                points.add(clip.startMs)
                points.add(clip.startMs + clip.durationMs)
            }
        }
        return points.distinct().sorted()
    }

    private fun recalculateTotalDuration() {
        val maxClipEnd = _tracks.value.flatMap { it.clips }.maxOfOrNull { it.startMs + it.durationMs } ?: 5000L
        val total = maxOf(maxClipEnd + 1000L, 8000L)
        _projectState.value = _projectState.value.copy(durationMs = total, updatedAt = System.currentTimeMillis())
    }

    companion object {
        fun defaultInitialTracks(): List<Track> {
            val mainVideoTrackId = "track_video_main"
            val overlayVideoTrackId = "track_video_overlay"
            val textTrackId = "track_text"
            val audioTrackId = "track_audio"

            return listOf(
                Track(
                    id = textTrackId,
                    name = "Titles & Captions",
                    type = TrackType.TEXT,
                    clips = listOf(
                        Clip(
                            id = "clip_text_1",
                            trackId = textTrackId,
                            name = "CINECUT REEL",
                            startMs = 500L,
                            durationMs = 4000L,
                            textOverlay = TextOverlay(
                                text = "CINECUT ORIGINALS",
                                fontSizeSp = 30f,
                                textColor = 0xFFFFD700,
                                positionY = 0.82f,
                                animation = "Fade In"
                            )
                        )
                    )
                ),
                Track(
                    id = overlayVideoTrackId,
                    name = "B-Roll Overlay",
                    type = TrackType.VIDEO,
                    clips = listOf(
                        Clip(
                            id = "clip_broll_1",
                            trackId = overlayVideoTrackId,
                            name = "Cyber City Neon.mp4",
                            startMs = 3500L,
                            durationMs = 4500L,
                            opacity = 0.9f,
                            scale = 0.95f,
                            transitionIn = TransitionType.CROSS_DISSOLVE,
                            colorGrading = ColorGrading(lutFilter = "Cyberpunk", saturation = 1.3f)
                        )
                    )
                ),
                Track(
                    id = mainVideoTrackId,
                    name = "Main Video",
                    type = TrackType.VIDEO,
                    clips = listOf(
                        Clip(
                            id = "clip_main_1",
                            trackId = mainVideoTrackId,
                            name = "Golden Hour Drone.mp4",
                            startMs = 0L,
                            durationMs = 4500L,
                            transitionIn = TransitionType.FADE_BLACK,
                            colorGrading = ColorGrading(lutFilter = "Golden Hour", exposure = 0.2f, saturation = 1.15f)
                        ),
                        Clip(
                            id = "clip_main_2",
                            trackId = mainVideoTrackId,
                            name = "Cinematic Portrait 4K.mp4",
                            startMs = 4500L,
                            durationMs = 5500L,
                            transitionIn = TransitionType.CROSS_DISSOLVE,
                            colorGrading = ColorGrading(lutFilter = "Cinematic Teal & Orange", contrast = 1.2f)
                        ),
                        Clip(
                            id = "clip_main_3",
                            trackId = mainVideoTrackId,
                            name = "Mountain Horizon.mp4",
                            startMs = 10000L,
                            durationMs = 4000L,
                            colorGrading = ColorGrading(lutFilter = "Vintage 16mm", temperature = 0.15f)
                        )
                    )
                ),
                Track(
                    id = audioTrackId,
                    name = "Master Score",
                    type = TrackType.AUDIO,
                    clips = listOf(
                        Clip(
                            id = "clip_audio_1",
                            trackId = audioTrackId,
                            name = "Epic Horizon Beat 128BPM.wav",
                            startMs = 0L,
                            durationMs = 14000L,
                            volume = 0.85f
                        )
                    )
                )
            )
        }
    }
}
