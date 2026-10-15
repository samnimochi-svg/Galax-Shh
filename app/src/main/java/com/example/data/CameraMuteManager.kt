package com.example.data

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.tile.CameraMuteOffTileService
import com.example.tile.CameraMuteOnTileService
import com.example.tile.CameraMuteToggleTileService
import java.util.Collections

sealed class MuteResult {
    data class Success(
        val isMuted: Boolean,
        val cameraRestarted: Boolean = false,
        val systemVolumeMuted: Boolean = false,
        val systemVolumeRestored: Boolean = false
    ) : MuteResult()
    object EulaRequired : MuteResult()
    object PermissionRequired : MuteResult()
    data class Error(val message: String) : MuteResult()
}

object CameraHardwareTracker {
    private val inUseCameraIds = Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile
    private var isStarted = false

    fun start(context: Context) {
        if (isStarted) return
        synchronized(this) {
            if (isStarted) return
            try {
                val thread = HandlerThread("GalaxShhCameraTracker").apply { start() }
                val handler = Handler(thread.looper)
                val cm = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return

                cm.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
                    override fun onCameraAvailable(cameraId: String) {
                        inUseCameraIds.remove(cameraId)
                    }

                    override fun onCameraUnavailable(cameraId: String) {
                        inUseCameraIds.add(cameraId)
                    }
                }, handler)
                isStarted = true
            } catch (ignored: Throwable) {}
        }
    }

    fun isCameraInUse(context: Context): Boolean {
        start(context)
        return inUseCameraIds.isNotEmpty()
    }
}

data class SoundModeInfo(
    val ringerModeName: String,
    val isSilentOrVibrate: Boolean,
    val systemVolume: Int,
    val maxSystemVolume: Int
)

object CameraMuteManager {
    const val CSC_KEY = "csc_pref_camera_forced_shuttersound_key"
    const val ACTION_TOGGLE = "com.aistudio.galaxshh.action.TOGGLE"
    const val ACTION_MUTE_ON = "com.aistudio.galaxshh.action.MUTE_ON"
    const val ACTION_MUTE_OFF = "com.aistudio.galaxshh.action.MUTE_OFF"
    const val ACTION_MUTE_SYSTEM_ONLY = "com.aistudio.galaxshh.action.MUTE_SYSTEM_ONLY"

    val KNOWN_CAMERA_PACKAGES = setOf(
        "com.sec.android.app.camera",
        "com.samsung.android.camera",
        "com.sec.android.app.camera.shootingmode.panorama",
        "com.google.android.GoogleCamera",
        "com.android.camera",
        "com.android.camera2"
    )

    private const val CHANNEL_ID = "galax_shh_status_v3"
    private const val NOTIFICATION_ID = 1001
    const val CHANNEL_SETUP_ID = "galax_shh_setup"
    const val NOTIF_BATTERY_SETUP_ID = 2001
    const val NOTIF_A11Y_SETUP_ID = 2002
    private const val PREFS_NAME = "galax_shh_prefs"
    private const val KEY_EULA_ACCEPTED = "eula_accepted_v1"
    private const val KEY_AUTO_APPLY_BOOT = "auto_apply_on_boot"
    private const val KEY_PERSISTENT_NOTIF = "persistent_status_notification_v1"
    private const val KEY_SETUP_NOTIF_SENT = "setup_notifications_sent_v1"
    const val KEY_SAVED_SYSTEM_VOLUME = "saved_system_volume_before_mute"

    fun isEulaAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_EULA_ACCEPTED, false)
    }

    fun setEulaAccepted(context: Context, accepted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EULA_ACCEPTED, accepted).apply()
    }

    fun isPersistentNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERSISTENT_NOTIF, true)
    }

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PERSISTENT_NOTIF, enabled).apply()
        if (enabled) {
            updateStatusNotification(context)
            com.example.service.CameraMuteStatusMonitorService.start(context)
        } else {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        }
    }

    fun muteSystemVolumeIfNeeded(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            if (currentVol >= 1) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val existingSaved = prefs.getInt(KEY_SAVED_SYSTEM_VOLUME, -1)
                if (existingSaved <= 0) {
                    prefs.edit().putInt(KEY_SAVED_SYSTEM_VOLUME, currentVol).apply()
                }
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun restoreSystemVolumeIfNeeded(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedVol = prefs.getInt(KEY_SAVED_SYSTEM_VOLUME, -1)
            if (savedVol > 0) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
                val targetVol = savedVol.coerceIn(1, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, targetVol, 0)
                prefs.edit().remove(KEY_SAVED_SYSTEM_VOLUME).apply()
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isMuted(context: Context): Boolean {
        return try {
            val value = Settings.System.getString(context.contentResolver, CSC_KEY)
            value == "0"
        } catch (e: Exception) {
            false
        }
    }

    fun isEffectivelyMuted(context: Context): Boolean {
        if (!isMuted(context)) return false
        val soundInfo = getSoundModeInfo(context)
        return soundInfo.isSilentOrVibrate || soundInfo.systemVolume == 0
    }

    fun getRawCscValue(context: Context): String? {
        return try {
            Settings.System.getString(context.contentResolver, CSC_KEY)
        } catch (_: Exception) {
            null
        }
    }

    fun getDefaultCameraPackage(context: Context): String? {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, 0)
            }
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    fun isGalaxyCameraPackage(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val defaultCamera = getDefaultCameraPackage(context)
        if (defaultCamera != null && packageName.equals(defaultCamera, ignoreCase = true)) {
            return true
        }
        if (packageName == "com.sec.android.app.camera" ||
            packageName == "com.samsung.android.camera" ||
            packageName.startsWith("com.sec.android.app.camera") ||
            packageName.startsWith("com.samsung.android.camera")
        ) {
            return true
        }
        return KNOWN_CAMERA_PACKAGES.contains(packageName)
    }

    fun isCameraInForeground(context: Context): Pair<Boolean, String?> {
        val defaultCameraPkg = getDefaultCameraPackage(context) ?: "com.sec.android.app.camera"

        // 1. Primary Check: Camera hardware sensor in-use (isolated background HandlerThread)
        if (CameraHardwareTracker.isCameraInUse(context)) {
            return true to defaultCameraPkg
        }

        // 2. Secondary Check: Real-time UsageEvents (detects most recently resumed foreground activity)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val time = System.currentTimeMillis()
                val events = usm?.queryEvents(time - 1000 * 8, time)
                if (events != null) {
                    val event = UsageEvents.Event()
                    var lastForegroundPkg: String? = null
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            val pkg = event.packageName
                            if (pkg != null && pkg != "com.android.systemui" && pkg != context.packageName) {
                                lastForegroundPkg = pkg
                            }
                        }
                    }
                    if (lastForegroundPkg != null) {
                        if (isGalaxyCameraPackage(context, lastForegroundPkg)) {
                            return true to lastForegroundPkg
                        } else {
                            // Another app (Chrome, SNS, Home Launcher, etc.) is in the foreground
                            return false to null
                        }
                    }
                }
            }
        } catch (ignored: Throwable) {}

        // 3. Tertiary Check: Running App Processes (strict foreground importance only)
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = am?.runningAppProcesses
            if (!processes.isNullOrEmpty()) {
                val foregroundProcs = processes.filter {
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName != "com.android.systemui" &&
                    it.processName != context.packageName
                }
                val otherForeground = foregroundProcs.any { !isGalaxyCameraPackage(context, it.processName) }
                if (otherForeground) {
                    return false to null
                }
                val cameraForeground = foregroundProcs.find { isGalaxyCameraPackage(context, it.processName) }
                if (cameraForeground != null) {
                    return true to cameraForeground.processName
                }
            }
        } catch (ignored: Throwable) {}

        return false to null
    }

    fun restartCameraIfInForeground(context: Context): Boolean {
        val (isForeground, _) = isCameraInForeground(context)
        return isForeground
    }

    fun navigateToHome(context: Context): Boolean {
        // 1. Accessibility Service: GLOBAL_ACTION_HOME (True native home button action)
        if (com.example.service.CameraMuteAccessibilityService.pressHome()) {
            return true
        }

        // 2. Fallback: Intent-based Home action
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context is TileService) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        val options = ActivityOptions.makeBasic().apply {
                            setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            homeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            options.toBundle()
                        )
                        context.startActivityAndCollapse(pendingIntent)
                        return true
                    } catch (_: Throwable) {}
                }
                @Suppress("DEPRECATION")
                context.startActivityAndCollapse(homeIntent)
                return true
            } else {
                context.startActivity(homeIntent)
                return true
            }
        } catch (_: Throwable) {}
        return false
    }

    fun navigateHome(context: Context): Boolean = navigateToHome(context)

    fun setMute(
        context: Context,
        mute: Boolean,
        autoRestartCamera: Boolean = true,
        syncSystemVolume: Boolean = true
    ): MuteResult {
        if (!isEulaAccepted(context)) {
            return MuteResult.EulaRequired
        }
        val targetValue = if (mute) "0" else "1"
        return try {
            val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.canWrite(context)
            } else {
                true
            }

            val currentValue = Settings.System.getString(context.contentResolver, CSC_KEY)
            val alreadySet = (currentValue == targetValue)
            val success = if (alreadySet) true else Settings.System.putString(context.contentResolver, CSC_KEY, targetValue)

            if (success) {
                updateTiles(context)

                var sysVolMuted = false
                var sysVolRestored = false
                if (syncSystemVolume) {
                    if (mute) {
                        sysVolMuted = muteSystemVolumeIfNeeded(context)
                    } else {
                        sysVolRestored = restoreSystemVolumeIfNeeded(context)
                    }
                }

                updateStatusNotification(context)

                val cameraRestarted = try {
                    if (autoRestartCamera) {
                        restartCameraIfInForeground(context)
                    } else {
                        false
                    }
                } catch (_: Throwable) {
                    false
                }
                MuteResult.Success(
                    isMuted = mute,
                    cameraRestarted = cameraRestarted,
                    systemVolumeMuted = sysVolMuted,
                    systemVolumeRestored = sysVolRestored
                )
            } else {
                if (!canWrite) {
                    MuteResult.PermissionRequired
                } else {
                    MuteResult.Error(context.getString(R.string.error_settings_update_failed))
                }
            }
        } catch (e: SecurityException) {
            MuteResult.PermissionRequired
        } catch (e: Throwable) {
            MuteResult.Error(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    fun toggleMute(context: Context): MuteResult {
        val currentlyMuted = isEffectivelyMuted(context)
        return setMute(context, !currentlyMuted)
    }

    fun applyAction(context: Context, action: String?): MuteResult {
        return when (action) {
            ACTION_MUTE_ON -> setMute(context, true)
            ACTION_MUTE_OFF -> setMute(context, false)
            ACTION_TOGGLE -> toggleMute(context)
            else -> toggleMute(context)
        }
    }

    fun canModifySystemSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    fun getSoundModeInfo(context: Context): SoundModeInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        val (modeName, isSilentOrVib) = when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> context.getString(R.string.sound_mode_silent) to true
            AudioManager.RINGER_MODE_VIBRATE -> context.getString(R.string.sound_mode_vibrate) to true
            else -> context.getString(R.string.sound_mode_normal) to false
        }
        val systemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
        return SoundModeInfo(
            ringerModeName = modeName,
            isSilentOrVibrate = isSilentOrVib,
            systemVolume = systemVol,
            maxSystemVolume = maxVol
        )
    }

    fun triggerHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(20, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Display an extra large, highly visible toast with the app icon when camera mute is applied.
     * Guaranteed to display reliably on the main thread with fallback to standard toast.
     */
    fun showMuteAppliedBigToast(
        context: Context,
        mainTitle: String? = null,
        subMessage: String = ""
    ) {
        val appContext = context.applicationContext
        val resolvedTitle = if (mainTitle.isNullOrBlank()) {
            appContext.getString(R.string.toast_mute_applied_title)
        } else {
            mainTitle
        }
        Handler(Looper.getMainLooper()).post {
            try {
                val toast = Toast(appContext)
                val density = appContext.resources.displayMetrics.density
                fun dp(dpVal: Int): Int = (dpVal * density).toInt()

                // Root card container
                val rootLayout = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    val padH = dp(28)
                    val padV = dp(22)
                    setPadding(padH, padV, padH, padV)

                    // Card background with rounded corners & One UI blue accent border
                    val cardBg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(20).toFloat()
                        setColor(Color.parseColor("#F2181824")) // Deep dark translucent
                        setStroke(dp(2), Color.parseColor("#4D3B82F6")) // One UI Blue accent border
                    }
                    background = cardBg
                    elevation = dp(10).toFloat()
                }

                // 1. Extra Large App Icon (84dp x 84dp)
                val iconSize = dp(84)
                val iconView = ImageView(appContext).apply {
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        bottomMargin = dp(14)
                    }
                    setImageResource(R.drawable.app_icon_shh)
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, dp(18).toFloat())
                        }
                    }
                    clipToOutline = true
                }
                rootLayout.addView(iconView)

                // 2. Bold Emphasis Title
                val titleView = TextView(appContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = if (subMessage.isNotEmpty()) dp(6) else 0
                    }
                    text = if (resolvedTitle.startsWith("🤫")) resolvedTitle else "🤫 $resolvedTitle"
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }
                rootLayout.addView(titleView)

                // 3. Subtitle / Details (if any)
                if (subMessage.isNotEmpty()) {
                    val subtitleView = TextView(appContext).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        text = subMessage
                        textSize = 13f
                        setTextColor(Color.parseColor("#D1D5DB"))
                        gravity = Gravity.CENTER
                    }
                    rootLayout.addView(subtitleView)
                }

                toast.view = rootLayout
                toast.duration = Toast.LENGTH_SHORT
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            } catch (e: Throwable) {
                // Reliable fallback for standard text toast
                try {
                    val fallbackText = (if (resolvedTitle.startsWith("🤫")) resolvedTitle else "🤫 $resolvedTitle") +
                            if (subMessage.isNotEmpty()) "\n$subMessage" else ""
                    val fallbackToast = Toast.makeText(appContext, fallbackText, Toast.LENGTH_SHORT)
                    fallbackToast.setGravity(Gravity.CENTER, 0, 0)
                    fallbackToast.show()
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * General toast display.
     * If message indicates camera mute applied, automatically upgrades to the Extra Large Icon Toast.
     */
    fun showToast(context: Context, message: String) {
        val appContext = context.applicationContext
        val isMuteToast = message.contains("カメラ消音") ||
                message.contains("Camera Mute") ||
                message.contains("카메라 무음") ||
                message.contains(appContext.getString(R.string.toast_mute_applied_title)) ||
                message.contains(appContext.getString(R.string.status_muted_active))

        if (isMuteToast) {
            var mainTitle = appContext.getString(R.string.toast_mute_applied_title)
            var subMsg = ""
            if (message.contains(" - ")) {
                val parts = message.split(" - ", limit = 2)
                mainTitle = parts[0].trim()
                subMsg = parts[1].trim()
            } else if (message.contains(" (") && message.contains(")")) {
                val startIdx = message.indexOf(" (")
                mainTitle = message.substring(0, startIdx).trim()
                subMsg = message.substring(startIdx).trim()
            } else {
                mainTitle = message
            }
            showMuteAppliedBigToast(appContext, mainTitle, subMsg)
            return
        }

        // Standard text toast with formatting (displayed normally at bottom)
        val formattedMsg = when {
            message.startsWith("🤫") || message.startsWith("🔔") || message.startsWith("⚠️") || message.startsWith("❌") -> message
            message.contains("通常") || message.contains("Sound Restored") || message.contains("シャッター音") || message.contains("Shutter Sound") || message.contains("셔터음") ||
                    message.contains(appContext.getString(R.string.toast_sound_restored_title)) ||
                    message.contains(appContext.getString(R.string.status_sound_active)) -> "🔔 $message"
            else -> message
        }
        Handler(Looper.getMainLooper()).post {
            try {
                val toast = Toast.makeText(appContext, formattedMsg, Toast.LENGTH_SHORT)
                toast.show()
            } catch (_: Throwable) {
                try {
                    Toast.makeText(context, formattedMsg, Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {}
            }
        }
    }

    data class StatusNotificationInfo(
        val title: String,
        val content: String,
        val subText: String,
        val iconRes: Int
    )

    fun updateStatusNotification(context: Context) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (!isEulaAccepted(appContext)) {
            return
        }

        val persistentEnabled = isPersistentNotificationEnabled(appContext)
        if (!persistentEnabled) {
            val isShowing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID }
                } catch (_: Throwable) {
                    false
                }
            } else {
                false
            }
            if (!isShowing) return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = appContext.getString(R.string.notification_channel_desc)
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canWrite = canModifySystemSettings(appContext)
        val isMutedInCsc = isMuted(appContext)
        val effectivelyMuted = isEffectivelyMuted(appContext)
        val soundInfo = getSoundModeInfo(appContext)

        val notifInfo = when {
            !canWrite -> {
                StatusNotificationInfo(
                    appContext.getString(R.string.notification_permission_required_title),
                    appContext.getString(R.string.notification_permission_required_desc),
                    appContext.getString(R.string.permission_card_title),
                    R.drawable.ic_tile_shh
                )
            }
            effectivelyMuted -> {
                val modeDesc = if (soundInfo.isSilentOrVibrate) {
                    "[${soundInfo.ringerModeName}]"
                } else {
                    "[${appContext.getString(R.string.toast_sys_vol_muted)}]"
                }
                StatusNotificationInfo(
                    appContext.getString(R.string.notification_status_muted_title),
                    "${appContext.getString(R.string.notification_status_muted_desc)} $modeDesc\n${appContext.getString(R.string.notification_status_muted_detail)}",
                    appContext.getString(R.string.notification_status_muted_subtext),
                    R.drawable.ic_tile_shh
                )
            }
            isMutedInCsc && !soundInfo.isSilentOrVibrate && soundInfo.systemVolume > 0 -> {
                StatusNotificationInfo(
                    String.format(appContext.getString(R.string.notification_status_warning_vol_title), soundInfo.systemVolume),
                    "${String.format(appContext.getString(R.string.notification_status_warning_vol_desc), soundInfo.systemVolume)}\n${appContext.getString(R.string.sound_status_warning_vol)}",
                    appContext.getString(R.string.notification_status_warning_subtext),
                    R.drawable.ic_tile_sound_on
                )
            }
            else -> {
                StatusNotificationInfo(
                    appContext.getString(R.string.notification_status_sound_on_title),
                    "${appContext.getString(R.string.notification_status_sound_on_desc)}\n${appContext.getString(R.string.notification_status_sound_on_detail)}",
                    appContext.getString(R.string.notification_status_sound_on_subtext),
                    R.drawable.ic_tile_sound_on
                )
            }
        }

        val notifBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(notifInfo.iconRes)
            .setContentTitle(notifInfo.title)
            .setContentText(notifInfo.content)
            .setSubText(notifInfo.subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifInfo.content))
            .setContentIntent(pendingIntent)
            .setOngoing(persistentEnabled)
            .setAutoCancel(!persistentEnabled)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        if (canWrite) {
            when {
                effectivelyMuted -> {
                    val soundOnIntent = Intent(ACTION_MUTE_OFF).apply {
                        setClass(appContext, com.example.receiver.ExternalAutomationReceiver::class.java)
                    }
                    val piSoundOn = PendingIntent.getBroadcast(
                        appContext,
                        101,
                        soundOnIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notifBuilder.addAction(
                        R.drawable.ic_tile_sound_on,
                        appContext.getString(R.string.notification_action_restore_sound),
                        piSoundOn
                    )
                }
                isMutedInCsc && !soundInfo.isSilentOrVibrate && soundInfo.systemVolume > 0 -> {
                    val muteVolIntent = Intent(ACTION_MUTE_SYSTEM_ONLY).apply {
                        setClass(appContext, com.example.receiver.ExternalAutomationReceiver::class.java)
                    }
                    val piMuteVol = PendingIntent.getBroadcast(
                        appContext,
                        102,
                        muteVolIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notifBuilder.addAction(
                        R.drawable.ic_tile_shh,
                        appContext.getString(R.string.notification_action_mute_vol),
                        piMuteVol
                    )

                    val soundOnIntent = Intent(ACTION_MUTE_OFF).apply {
                        setClass(appContext, com.example.receiver.ExternalAutomationReceiver::class.java)
                    }
                    val piSoundOn = PendingIntent.getBroadcast(
                        appContext,
                        103,
                        soundOnIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notifBuilder.addAction(
                        R.drawable.ic_tile_sound_on,
                        appContext.getString(R.string.notification_action_restore_sound),
                        piSoundOn
                    )
                }
                else -> {
                    val muteOnIntent = Intent(ACTION_MUTE_ON).apply {
                        setClass(appContext, com.example.receiver.ExternalAutomationReceiver::class.java)
                    }
                    val piMuteOn = PendingIntent.getBroadcast(
                        appContext,
                        104,
                        muteOnIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notifBuilder.addAction(
                        R.drawable.ic_tile_shh,
                        appContext.getString(R.string.notification_action_apply_mute),
                        piMuteOn
                    )
                }
            }
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, notifBuilder.build())
        } catch (_: Throwable) {}
    }

    fun showStatusNotification(context: Context, result: MuteResult? = null) {
        updateStatusNotification(context)
    }

    fun updateTiles(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, CameraMuteToggleTileService::class.java)
                )
                TileService.requestListeningState(
                    context,
                    ComponentName(context, CameraMuteOnTileService::class.java)
                )
                TileService.requestListeningState(
                    context,
                    ComponentName(context, CameraMuteOffTileService::class.java)
                )
            } catch (ignored: Throwable) {}
        }
    }

    fun isAutoApplyOnBoot(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_APPLY_BOOT, false)
    }

    fun setAutoApplyOnBoot(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_APPLY_BOOT, enabled).apply()
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    fun getRequestIgnoreBatteryOptimizationIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        return Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            context.startActivity(getRequestIgnoreBatteryOptimizationIntent(context))
        } catch (_: Throwable) {
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (_: Throwable) {
                try {
                    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(appDetails)
                } catch (_: Throwable) {}
            }
        }
    }

    fun checkAndSendSetupNotifications(context: Context, force: Boolean = false) {
        if (!isEulaAccepted(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadySent = prefs.getBoolean(KEY_SETUP_NOTIF_SENT, false)
        if (alreadySent && !force) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val setupChannel = NotificationChannel(
                CHANNEL_SETUP_ID,
                context.getString(R.string.notification_channel_setup_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_setup_desc)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(setupChannel)
        }

        // 1. Battery Optimization Notification (prevent deep sleep)
        if (!isIgnoringBatteryOptimizations(context)) {
            val batteryIntent = getRequestIgnoreBatteryOptimizationIntent(context)
            val pendingBattery = PendingIntent.getActivity(
                context,
                NOTIF_BATTERY_SETUP_ID,
                batteryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val batteryNotification = NotificationCompat.Builder(context, CHANNEL_SETUP_ID)
                .setSmallIcon(R.drawable.ic_tile_shh)
                .setContentTitle(context.getString(R.string.notif_battery_setup_title))
                .setContentText(context.getString(R.string.notif_battery_setup_desc))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_battery_setup_desc)))
                .setContentIntent(pendingBattery)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(NOTIF_BATTERY_SETUP_ID, batteryNotification)
        } else {
            notificationManager.cancel(NOTIF_BATTERY_SETUP_ID)
        }

        // 2. Accessibility Service Notification (for home button integration)
        if (!com.example.service.CameraMuteAccessibilityService.isServiceEnabledInSettings(context)) {
            val a11yIntent = com.example.service.CameraMuteAccessibilityService.getAccessibilitySettingsIntent(context)
            val pendingA11y = PendingIntent.getActivity(
                context,
                NOTIF_A11Y_SETUP_ID,
                a11yIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val a11yNotification = NotificationCompat.Builder(context, CHANNEL_SETUP_ID)
                .setSmallIcon(R.drawable.ic_tile_shh)
                .setContentTitle(context.getString(R.string.notif_a11y_setup_title))
                .setContentText(context.getString(R.string.notif_a11y_setup_desc))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_a11y_setup_desc)))
                .setContentIntent(pendingA11y)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(NOTIF_A11Y_SETUP_ID, a11yNotification)
        } else {
            notificationManager.cancel(NOTIF_A11Y_SETUP_ID)
        }

        prefs.edit().putBoolean(KEY_SETUP_NOTIF_SENT, true).apply()
    }

    fun dismissBatterySetupNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIF_BATTERY_SETUP_ID)
    }

    fun dismissA11ySetupNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIF_A11Y_SETUP_ID)
    }

    /**
     * Restores all camera shutter sounds and system volumes to default (unmuted / normal 1),
     * and disables automatic mute on boot. Used prior to uninstallation or for full reset.
     */
    fun restoreAllToDefaults(context: Context): Boolean {
        setAutoApplyOnBoot(context, false)
        val result = setMute(context, mute = false, autoRestartCamera = false, syncSystemVolume = true)
        return result is MuteResult.Success
    }

    /**
     * Launches the system uninstallation confirmation screen for this application.
     */
    fun launchUninstall(context: Context) {
        try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(uninstallIntent)
        } catch (_: Throwable) {
            try {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
            } catch (_: Throwable) {}
        }
    }
}

