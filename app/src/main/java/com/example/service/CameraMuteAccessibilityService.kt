package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * AccessibilityService that performs GLOBAL_ACTION_HOME.
 * Used to immediately exit foreground camera app to home screen when quick panel tile is clicked,
 * effectively applying camera shutter mute settings on Galaxy devices.
 */
class CameraMuteAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        Log.d(TAG, "CameraMuteAccessibilityService connected")
        try {
            CameraMuteStatusMonitorService.start(this)
            com.example.data.CameraMuteManager.updateStatusNotification(this)
            com.example.data.CameraMuteManager.updateTiles(this)
        } catch (_: Throwable) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event listening required - used strictly for GLOBAL_ACTION_HOME invocation
    }

    override fun onInterrupt() {
        Log.d(TAG, "CameraMuteAccessibilityService interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "CameraMuteAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "CameraMuteA11y"
        private var instance: WeakReference<CameraMuteAccessibilityService>? = null

        /**
         * Check if this service instance is currently bound and alive.
         */
        fun isRunning(): Boolean {
            return instance?.get() != null
        }

        /**
         * Check if user has enabled this Accessibility Service in system settings.
         */
        fun isServiceEnabledInSettings(context: Context): Boolean {
            try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)

                val myComponent = ComponentName(context, CameraMuteAccessibilityService::class.java).flattenToString()
                val myShortComponent = ComponentName(context, CameraMuteAccessibilityService::class.java).flattenToShortString()

                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    if (componentNameString.equals(myComponent, ignoreCase = true) ||
                        componentNameString.equals(myShortComponent, ignoreCase = true) ||
                        componentNameString.contains(CameraMuteAccessibilityService::class.java.simpleName)
                    ) {
                        return true
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to read accessibility settings", e)
            }
            return false
        }

        /**
         * Perform system-level home button press using Accessibility GLOBAL_ACTION_HOME.
         * Returns true if successfully triggered.
         */
        fun pressHome(): Boolean {
            val service = instance?.get() ?: return false
            return try {
                val success = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.d(TAG, "performGlobalAction(GLOBAL_ACTION_HOME) result=$success")
                success
            } catch (e: Throwable) {
                Log.w(TAG, "Error invoking GLOBAL_ACTION_HOME", e)
                false
            }
        }

        /**
         * Get Intent configured to navigate directly to this app's accessibility service setting screen.
         */
        fun getAccessibilitySettingsIntent(context: Context): Intent {
            val componentName = ComponentName(context, CameraMuteAccessibilityService::class.java).flattenToString()
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Direct deep-link parameters for Samsung One UI & Android 11+
                putExtra(":settings:fragment_args_key", componentName)
                val bundle = android.os.Bundle().apply {
                    putString(":settings:fragment_args_key", componentName)
                }
                putExtra(":settings:show_fragment_args", bundle)
            }
        }

        /**
         * Open Android system Accessibility settings screen.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = getAccessibilitySettingsIntent(context)
                context.startActivity(intent)
            } catch (e: Throwable) {
                try {
                    val fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (e2: Throwable) {
                    Log.w(TAG, "Failed to open accessibility settings", e2)
                }
            }
        }
    }
}
