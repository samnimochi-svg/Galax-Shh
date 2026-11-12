package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.data.CameraMuteManager

/**
 * Background monitoring service that observes system volume, ringer mode,
 * and Samsung CSC shutter sound system setting changes in real time,
 * keeping the status notification and Quick Settings tiles synchronized.
 */
class CameraMuteStatusMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isReceiverRegistered = false
    private var isObserverRegistered = false

    private val soundChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Sound/volume broadcast received: ${intent?.action}")
            updateState()
        }
    }

    private val cscContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d(TAG, "CSC Setting change detected via ContentObserver")
            updateState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerMonitoring()
        updateState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerMonitoring()
        updateState()
        return START_STICKY
    }

    private fun registerMonitoring() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                @Suppress("DEPRECATION")
                addAction("android.media.VOLUME_CHANGED_ACTION")
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(soundChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(soundChangeReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register soundChangeReceiver", e)
            }
        }

        if (!isObserverRegistered) {
            try {
                val uri = Settings.System.getUriFor(CameraMuteManager.CSC_KEY)
                contentResolver.registerContentObserver(uri, false, cscContentObserver)
                isObserverRegistered = true
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register cscContentObserver", e)
            }
        }
    }

    private fun updateState() {
        try {
            CameraMuteManager.updateStatusNotification(this)
            CameraMuteManager.updateTiles(this)
        } catch (e: Throwable) {
            Log.w(TAG, "Error updating status notification or tiles", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(soundChangeReceiver)
            } catch (_: Throwable) {}
            isReceiverRegistered = false
        }
        if (isObserverRegistered) {
            try {
                contentResolver.unregisterContentObserver(cscContentObserver)
            } catch (_: Throwable) {}
            isObserverRegistered = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CameraMuteMonitorSvc"

        fun start(context: Context) {
            try {
                val intent = Intent(context, CameraMuteStatusMonitorService::class.java)
                context.startService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to start CameraMuteStatusMonitorService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, CameraMuteStatusMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to stop CameraMuteStatusMonitorService", e)
            }
        }
    }
}
