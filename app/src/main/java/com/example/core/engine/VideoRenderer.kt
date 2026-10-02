package com.example.core.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.core.model.Clip
import com.example.core.model.ColorGrading
import com.example.core.model.Track
import com.example.core.model.TrackType

object VideoRenderer {

    /**
     * Compute current active clips at playhead timeMs across all visible tracks
     */
    fun getActiveClips(tracks: List<Track>, playheadMs: Long): List<Clip> {
        val active = mutableListOf<Clip>()
        // Process tracks in rendering order (Bottom to top: Video Main -> Overlay -> Text)
        for (track in tracks) {
            if (!track.isVisible) continue
            for (clip in track.clips) {
                if (playheadMs >= clip.startMs && playheadMs < clip.startMs + clip.durationMs) {
                    active.add(clip)
                }
            }
        }
        return active
    }

    /**
     * Build an Android ColorMatrix based on exposure, contrast, saturation, temperature, and LUT filter
     */
    fun createColorFilter(grading: ColorGrading): ColorFilter {
        val matrix = ColorMatrix()

        // Saturation
        matrix.setToSaturation(grading.saturation.coerceIn(0f, 3f))

        // Contrast and Exposure adjustment
        val c = grading.contrast.coerceIn(0.2f, 2.5f)
        val b = grading.exposure * 40f // brightness offset

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.timesAssign(contrastMatrix)

        // LUT / Stylized color tints
        when (grading.lutFilter) {
            "Cinematic Gold" -> {
                val gold = ColorMatrix(
                    floatArrayOf(
                        1.28f, 0f, 0f, 0f, 22f,
                        0f, 1.12f, 0f, 0f, 12f,
                        0f, 0f, 0.78f, 0f, -18f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(gold)
            }
            "Teal & Orange", "Cinematic Teal & Orange" -> {
                val tealOrange = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 0.85f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(tealOrange)
            }
            "Cyberpunk Neon", "Cyberpunk" -> {
                val cyber = ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0.2f, 0f, 20f,
                        0f, 0.9f, 0.1f, 0f, -5f,
                        0.3f, 0f, 1.4f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(cyber)
            }
            "Noir B&W", "Noir" -> {
                matrix.setToSaturation(0f)
                val noir = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -25f,
                        0f, 1.35f, 0f, 0f, -25f,
                        0f, 0f, 1.35f, 0f, -25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(noir)
            }
            "Warm Sunset", "Golden Hour" -> {
                val golden = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, 25f,
                        0f, 1.15f, 0f, 0f, 15f,
                        0f, 0f, 0.8f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(golden)
            }
            "Retro VHS" -> {
                val vhs = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0.05f, 0.05f, 0f, 10f,
                        0.05f, 1.1f, 0f, 0f, 5f,
                        0.1f, 0f, 0.85f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(vhs)
            }
            "Emerald Film" -> {
                val emerald = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, -10f,
                        0f, 1.25f, 0f, 0f, 15f,
                        0f, 0f, 0.95f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(emerald)
            }
            "Tokyo Night" -> {
                val tokyo = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0f, 0.15f, 0f, -15f,
                        0f, 0.9f, 0.1f, 0f, -10f,
                        0.2f, 0.1f, 1.4f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(tokyo)
            }
            "Pastel Dream" -> {
                val pastel = ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.05f, 0.05f, 0f, 35f,
                        0.05f, 0.95f, 0.05f, 0f, 30f,
                        0.05f, 0.05f, 1.05f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(pastel)
            }
            "Bleach Bypass" -> {
                matrix.setToSaturation(0.45f)
                val bleach = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -10f,
                        0f, 1.3f, 0f, 0f, -10f,
                        0f, 0f, 1.3f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(bleach)
            }
            "Lomo Chrome" -> {
                val lomo = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, 15f,
                        0f, 1.15f, 0f, 0f, -5f,
                        0f, 0f, 1.25f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(lomo)
            }
            "Cold Ice" -> {
                val cold = ColorMatrix(
                    floatArrayOf(
                        0.8f, 0f, 0f, 0f, -15f,
                        0f, 0.95f, 0f, 0f, 5f,
                        0f, 0f, 1.35f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(cold)
            }
            "Vintage Sepia", "Vintage 16mm" -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0.15f, 0.05f, 0f, 20f,
                        0.1f, 1.05f, 0.05f, 0f, 12f,
                        0f, 0.05f, 0.75f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.timesAssign(sepia)
            }
        }

        return ColorFilter.colorMatrix(matrix)
    }

    /**
     * Compute interpolated keyframe value for a property at the given clip offset time
     */
    fun interpolateKeyframe(clip: Clip, property: String, currentOffsetMs: Long, defaultValue: Float): Float {
        val propertyFrames = clip.keyframes.filter { it.property == property }.sortedBy { it.timeMs }
        if (propertyFrames.isEmpty()) return defaultValue
        if (currentOffsetMs <= propertyFrames.first().timeMs) return propertyFrames.first().value
        if (currentOffsetMs >= propertyFrames.last().timeMs) return propertyFrames.last().value

        for (i in 0 until propertyFrames.size - 1) {
            val kf1 = propertyFrames[i]
            val kf2 = propertyFrames[i + 1]
            if (currentOffsetMs in kf1.timeMs..kf2.timeMs) {
                val t = (currentOffsetMs - kf1.timeMs).toFloat() / (kf2.timeMs - kf1.timeMs).coerceAtLeast(1L)
                return kf1.value + (kf2.value - kf1.value) * t
            }
        }
        return defaultValue
    }
}
