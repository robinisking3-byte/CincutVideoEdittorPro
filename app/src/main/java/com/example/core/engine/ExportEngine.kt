package com.example.core.engine

import android.content.Context
import com.example.core.model.ExportJob
import com.example.core.model.ExportResolution
import com.example.core.model.ExportSettings
import com.example.core.model.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class ExportEngine(private val context: Context) {

    private val _currentJob = MutableStateFlow<ExportJob?>(null)
    val currentJob: StateFlow<ExportJob?> = _currentJob.asStateFlow()

    private val _exportHistory = MutableStateFlow<List<ExportJob>>(emptyList())
    val exportHistory: StateFlow<List<ExportJob>> = _exportHistory.asStateFlow()

    private var exportCoroutineJob: Job? = null

    fun startExport(
        project: Project,
        settings: ExportSettings,
        scope: CoroutineScope
    ) {
        if (_currentJob.value?.status == "Exporting") return

        val exportId = UUID.randomUUID().toString()
        val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val fileName = "${project.title.replace(" ", "_")}_${settings.resolution.label.replace(" ", "_")}_$exportId.mp4"
        val outputFile = File(exportDir, fileName)

        val job = ExportJob(
            id = exportId,
            projectId = project.id,
            projectTitle = project.title,
            outputPath = outputFile.absolutePath,
            progress = 0.0f,
            status = "Exporting",
            timestamp = System.currentTimeMillis()
        )
        _currentJob.value = job

        exportCoroutineJob = scope.launch(Dispatchers.Default) {
            try {
                val totalSteps = 100
                val durationMs = project.durationMs
                val frameDelay = when (settings.resolution) {
                    ExportResolution.HD_720P -> 30L
                    ExportResolution.FHD_1080P -> 50L
                    ExportResolution.QHD_1440P -> 80L
                    ExportResolution.UHD_4K -> 120L
                }

                for (step in 1..totalSteps) {
                    if (!isActive) throw CancellationException("Export cancelled by user")
                    delay(frameDelay)
                    val progress = step / 100f
                    _currentJob.value = _currentJob.value?.copy(progress = progress)
                }

                // Simulate final muxed file write
                outputFile.writeText("CineCut Rendered Video: ${project.title}, Resolution: ${settings.resolution.label}")
                val finalJob = _currentJob.value?.copy(
                    progress = 1.0f,
                    status = "Completed",
                    fileSizeBytes = (durationMs * settings.bitrateMbps * 125L).toLong()
                )
                _currentJob.value = finalJob
                if (finalJob != null) {
                    _exportHistory.value = listOf(finalJob) + _exportHistory.value
                }
            } catch (e: CancellationException) {
                val cancelled = _currentJob.value?.copy(status = "Cancelled")
                _currentJob.value = cancelled
                if (cancelled != null) {
                    _exportHistory.value = listOf(cancelled) + _exportHistory.value
                }
            } catch (e: Exception) {
                val failed = _currentJob.value?.copy(status = "Failed")
                _currentJob.value = failed
            }
        }
    }

    fun cancelExport() {
        exportCoroutineJob?.cancel()
    }

    fun clearCurrentJob() {
        _currentJob.value = null
    }
}
