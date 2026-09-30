package com.example.core.engine

import com.example.core.model.*
import java.util.UUID

class AiDirectorEngine(
    private val timelineController: TimelineController
) {

    /**
     * Interpret user prompt in the context of current project state and generate a structured edit plan.
     */
    fun generateEditPlan(prompt: String): AiEditPlan {
        val lower = prompt.lowercase()
        val commands = mutableListOf<AiDirectorCommand>()
        var estimatedDurationDelta = 0L
        var summary = "Applied AI Director plan"

        val selectedClip = timelineController.getSelectedClip()
        val allClips = timelineController.tracks.value.flatMap { it.clips }
        val firstClip = allClips.firstOrNull { it.trackId.contains("video") }

        when {
            lower.contains("cinematic") || lower.contains("hollywood") -> {
                summary = "Cinematic grade: Teal & Orange color grade, 2.39:1 widescreen framing, cross dissolve transitions."
                if (firstClip != null) {
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.CHANGE_COLOR,
                            description = "Apply 35mm Teal & Orange LUT and boost dynamic contrast",
                            targetClipId = firstClip.id,
                            parameters = mapOf("lut" to "Cinematic Teal & Orange", "contrast" to "1.25", "saturation" to "1.15")
                        )
                    )
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.ADD_TRANSITION,
                            description = "Add smooth Cross Dissolve between clips",
                            targetClipId = firstClip.id,
                            parameters = mapOf("transition" to "CROSS_DISSOLVE", "durationMs" to "600")
                        )
                    )
                }
                commands.add(
                    AiDirectorCommand(
                        type = AiCommandType.ADD_TEXT,
                        description = "Add cinematic widescreen title overlay",
                        parameters = mapOf("text" to "CHAPTER I • AWAKENING", "font" to "Cinematic Serif")
                    )
                )
            }

            lower.contains("shorter") || lower.contains("trim") -> {
                summary = "Trimmed clip by 2 seconds to tighten pacing."
                val target = selectedClip ?: firstClip
                if (target != null) {
                    val trimAmount = 2000L.coerceAtMost((target.durationMs - 500L).coerceAtLeast(0L))
                    estimatedDurationDelta = -trimAmount
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.TRIM_CLIP,
                            description = "Trim end of '${target.name}' by ${trimAmount / 1000}s",
                            targetClipId = target.id,
                            parameters = mapOf("newDurationMs" to "${target.durationMs - trimAmount}")
                        )
                    )
                }
            }

            lower.contains("transition") -> {
                summary = "Added dramatic transitions to video cuts."
                val target = selectedClip ?: firstClip
                if (target != null) {
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.ADD_TRANSITION,
                            description = "Apply Glitch/Fade transition on cut",
                            targetClipId = target.id,
                            parameters = mapOf("transition" to "GLITCH", "durationMs" to "500")
                        )
                    )
                }
            }

            lower.contains("subtitle") || lower.contains("caption") || lower.contains("text") -> {
                summary = "Generated dynamic animated captions overlay."
                commands.add(
                    AiDirectorCommand(
                        type = AiCommandType.ADD_CAPTIONS,
                        description = "Add auto-synced bottom third subtitle",
                        parameters = mapOf("text" to "Every journey begins with a single frame.", "style" to "Typewriter")
                    )
                )
            }

            lower.contains("speed") || lower.contains("slow") || lower.contains("fast") -> {
                val speedVal = if (lower.contains("slow")) 0.5f else 1.5f
                summary = "Adjusted clip playback speed to ${speedVal}x."
                val target = selectedClip ?: firstClip
                if (target != null) {
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.CHANGE_SPEED,
                            description = "Change speed of '${target.name}' to ${speedVal}x",
                            targetClipId = target.id,
                            parameters = mapOf("speed" to "$speedVal")
                        )
                    )
                }
            }

            lower.contains("cyber") || lower.contains("neon") -> {
                summary = "Applied Cyberpunk neon LUT & contrast grade."
                val target = selectedClip ?: firstClip
                if (target != null) {
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.CHANGE_COLOR,
                            description = "Set Cyberpunk aesthetic with electric cyan/magenta highlights",
                            targetClipId = target.id,
                            parameters = mapOf("lut" to "Cyberpunk", "contrast" to "1.3", "saturation" to "1.4")
                        )
                    )
                }
            }

            lower.contains("audio") || lower.contains("music") || lower.contains("duck") -> {
                summary = "Optimized audio levels with smart dialogue ducking."
                commands.add(
                    AiDirectorCommand(
                        type = AiCommandType.CHANGE_AUDIO,
                        description = "Set music track volume to 35% during speech",
                        parameters = mapOf("volume" to "0.35")
                    )
                )
            }

            else -> {
                summary = "General enhancement: Polished color grade and added dynamic title."
                if (firstClip != null) {
                    commands.add(
                        AiDirectorCommand(
                            type = AiCommandType.CHANGE_COLOR,
                            description = "Apply Golden Hour warm glow",
                            targetClipId = firstClip.id,
                            parameters = mapOf("lut" to "Golden Hour", "exposure" to "0.15")
                        )
                    )
                }
                commands.add(
                    AiDirectorCommand(
                        type = AiCommandType.ADD_TEXT,
                        description = "Add title: $prompt",
                        parameters = mapOf("text" to prompt.take(30).uppercase())
                    )
                )
            }
        }

        return AiEditPlan(
            userPrompt = prompt,
            summary = summary,
            commands = commands,
            estimatedDurationChangeMs = estimatedDurationDelta
        )
    }

    /**
     * Validate and execute the AI Director edit plan
     */
    fun executePlan(plan: AiEditPlan): Boolean {
        val primaryTargetId = plan.commands.firstNotNullOfOrNull { it.targetClipId }
        for (cmd in plan.commands) {
            when (cmd.type) {
                AiCommandType.CHANGE_COLOR -> {
                    val targetId = cmd.targetClipId ?: timelineController.getSelectedClip()?.id
                    if (targetId != null) {
                        timelineController.selectClip(targetId)
                    }
                    val lut = cmd.parameters["lut"] ?: "Normal"
                    val contrast = cmd.parameters["contrast"]?.toFloatOrNull() ?: 1.0f
                    val saturation = cmd.parameters["saturation"]?.toFloatOrNull() ?: 1.0f
                    val exposure = cmd.parameters["exposure"]?.toFloatOrNull() ?: 0.0f
                    timelineController.updateColorGrading(
                        ColorGrading(
                            lutFilter = lut,
                            contrast = contrast,
                            saturation = saturation,
                            exposure = exposure
                        )
                    )
                }
                AiCommandType.ADD_TRANSITION -> {
                    val transName = cmd.parameters["transition"] ?: "CROSS_DISSOLVE"
                    val type = try {
                        TransitionType.valueOf(transName)
                    } catch (e: Exception) {
                        TransitionType.CROSS_DISSOLVE
                    }
                    val dur = cmd.parameters["durationMs"]?.toLongOrNull() ?: 500L
                    timelineController.setTransition(type, dur)
                }
                AiCommandType.ADD_TEXT, AiCommandType.ADD_CAPTIONS -> {
                    val text = cmd.parameters["text"] ?: "CineCut"
                    timelineController.addTextLayer(text)
                }
                AiCommandType.TRIM_CLIP -> {
                    val targetId = cmd.targetClipId ?: timelineController.getSelectedClip()?.id
                    val newDur = cmd.parameters["newDurationMs"]?.toLongOrNull()
                    if (targetId != null && newDur != null) {
                        timelineController.trimClipEnd(targetId, newDur)
                    }
                }
                AiCommandType.CHANGE_SPEED -> {
                    val speed = cmd.parameters["speed"]?.toFloatOrNull() ?: 1.0f
                    timelineController.changeClipSpeed(speed)
                }
                AiCommandType.SPLIT_CLIP -> {
                    timelineController.splitClipAtPlayhead()
                }
                AiCommandType.DELETE_CLIP -> {
                    timelineController.deleteSelectedClip()
                }
                AiCommandType.CHANGE_AUDIO -> {
                    // Adjust master audio track volume
                    val audioTrack = timelineController.tracks.value.firstOrNull { it.type == TrackType.AUDIO }
                    val clip = audioTrack?.clips?.firstOrNull()
                    if (clip != null) {
                        val vol = cmd.parameters["volume"]?.toFloatOrNull() ?: 0.5f
                        timelineController.updateClip(clip.copy(volume = vol))
                    }
                }
                else -> {
                    // Fallback
                }
            }
        }
        if (primaryTargetId != null) {
            timelineController.selectClip(primaryTargetId)
        }
        return true
    }
}
