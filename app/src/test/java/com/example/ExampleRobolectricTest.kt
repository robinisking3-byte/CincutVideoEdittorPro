package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.TimelineController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CineCut", appName)
  }

  @Test
  fun `timeline controller initializes with tracks and can toggle play`() {
    val controller = TimelineController()
    val initialTracks = controller.tracks.value
    assertTrue("Timeline should have initialized tracks", initialTracks.isNotEmpty())

    assertFalse(controller.isPlaying.value)
    controller.togglePlayPause()
    assertTrue(controller.isPlaying.value)
    controller.togglePlayPause()
    assertFalse(controller.isPlaying.value)
  }

  @Test
  fun `timeline controller supports undo and redo`() {
    val controller = TimelineController()
    assertFalse(controller.canUndo())

    controller.addTextLayer("Test Title")
    assertTrue(controller.canUndo())

    controller.undo()
    assertTrue(controller.canRedo())

    controller.redo()
    assertFalse(controller.canRedo())
  }
}
