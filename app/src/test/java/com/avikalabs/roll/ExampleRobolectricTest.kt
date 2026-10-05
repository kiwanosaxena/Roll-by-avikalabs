package com.avikalabs.roll

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.avikalabs.roll.ui.components.DialMath
import com.avikalabs.roll.ui.components.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Roll", appName)
  }

  @Test
  fun `dial math angle and seconds snapping test`() {
    // 0 degrees -> 5s
    assertEquals(5, DialMath.angleToSnappedSeconds(0f))

    // 90 degrees -> 60s
    assertEquals(60, DialMath.angleToSnappedSeconds(90f))

    // 360 / 0 degrees max -> 3600s
    assertEquals(3600, DialMath.angleToSnappedSeconds(359.9f))
  }

  @Test
  fun `duration formatting test`() {
    assertEquals("45s", DurationFormatter.formatShort(45))
    assertEquals("1m", DurationFormatter.formatShort(60))
    assertEquals("1m 30s", DurationFormatter.formatShort(90))
    assertEquals("15m", DurationFormatter.formatShort(900))
    assertEquals("60m", DurationFormatter.formatShort(3600))
  }
}
