package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Galax-Shh", appName)
  }

  @Test
  fun `verify toggle tile resources exist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shhDrawable = context.getDrawable(R.drawable.ic_tile_shh)
    val soundOnDrawable = context.getDrawable(R.drawable.ic_tile_sound_on)
    org.junit.Assert.assertNotNull(shhDrawable)
    org.junit.Assert.assertNotNull(soundOnDrawable)
  }

  @Test
  fun `verify camera mute logic toggles correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    android.provider.Settings.System.putString(context.contentResolver, com.example.data.CameraMuteManager.CSC_KEY, "0")
    assertEquals(true, com.example.data.CameraMuteManager.isMuted(context))

    android.provider.Settings.System.putString(context.contentResolver, com.example.data.CameraMuteManager.CSC_KEY, "1")
    assertEquals(false, com.example.data.CameraMuteManager.isMuted(context))
  }

  @Test
  fun `verify galaxy camera package identification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    org.junit.Assert.assertTrue(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, "com.sec.android.app.camera"))
    org.junit.Assert.assertTrue(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, "com.samsung.android.camera"))

    // Non-camera packages must be false
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, "com.android.chrome"))
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, "com.sec.android.app.launcher"))
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, "com.google.android.youtube"))
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, context.packageName))
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isGalaxyCameraPackage(context, null))
  }

  @Test
  fun `verify camera foreground detection defaults to false`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val (isForeground, _) = com.example.data.CameraMuteManager.isCameraInForeground(context)
    org.junit.Assert.assertFalse(isForeground)
  }

  @Test
  fun `verify persistent notification preference and update status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.CameraMuteManager.setEulaAccepted(context, true)

    // Verify default enabled
    org.junit.Assert.assertTrue(com.example.data.CameraMuteManager.isPersistentNotificationEnabled(context))

    // Toggle persistent notification
    com.example.data.CameraMuteManager.setPersistentNotificationEnabled(context, false)
    org.junit.Assert.assertFalse(com.example.data.CameraMuteManager.isPersistentNotificationEnabled(context))

    com.example.data.CameraMuteManager.setPersistentNotificationEnabled(context, true)
    org.junit.Assert.assertTrue(com.example.data.CameraMuteManager.isPersistentNotificationEnabled(context))

    // Notification updates without throwing
    com.example.data.CameraMuteManager.updateStatusNotification(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val notifications = notificationManager.activeNotifications
    org.junit.Assert.assertTrue(notifications.any { it.id == 1001 })
  }

  @Test
  fun `verify external receiver handles mute system only action`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.CameraMuteManager.setEulaAccepted(context, true)

    val receiver = com.example.receiver.ExternalAutomationReceiver()
    val intent = android.content.Intent(com.example.data.CameraMuteManager.ACTION_MUTE_SYSTEM_ONLY)
    receiver.onReceive(context, intent)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val notifications = notificationManager.activeNotifications
    org.junit.Assert.assertTrue(notifications.any { it.id == 1001 })
  }
}
