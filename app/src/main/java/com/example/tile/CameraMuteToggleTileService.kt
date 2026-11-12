package com.example.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.CameraMuteManager
import com.example.data.MuteResult

@RequiresApi(Build.VERSION_CODES.N)
class CameraMuteToggleTileService : TileService() {

    private var isReceiverRegistered = false
    private val soundChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTileState()
            CameraMuteManager.updateStatusNotification(this@CameraMuteToggleTileService)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        com.example.data.CameraHardwareTracker.start(this)
        com.example.service.CameraMuteStatusMonitorService.start(this)
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
            } catch (_: Throwable) {}
        }
        updateTileState()
        CameraMuteManager.updateStatusNotification(this)
    }

    override fun onStopListening() {
        super.onStopListening()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(soundChangeReceiver)
            } catch (_: Throwable) {}
            isReceiverRegistered = false
        }
    }

    override fun onClick() {
        super.onClick()
        val result = CameraMuteManager.toggleMute(this)
        when (result) {
            is MuteResult.Success -> {
                val stateMsg = if (result.isMuted) getString(R.string.toast_mute_applied_title) else getString(R.string.toast_sound_restored_title)
                val volMsg = when {
                    result.systemVolumeMuted -> getString(R.string.toast_sys_vol_muted)
                    result.systemVolumeRestored -> getString(R.string.toast_sys_vol_restored)
                    else -> ""
                }
                val msg = if (result.cameraRestarted) {
                    CameraMuteManager.navigateToHome(this)
                    "$stateMsg$volMsg${getString(R.string.toast_camera_navigated_home)}"
                } else {
                    "$stateMsg$volMsg${getString(R.string.toast_mute_success_suffix)}"
                }
                CameraMuteManager.showToast(this, msg)
                CameraMuteManager.showStatusNotification(this, result)
            }
            is MuteResult.EulaRequired -> {
                CameraMuteManager.showToast(this, getString(R.string.toast_eula_required))
                CameraMuteManager.showStatusNotification(this, result)
            }
            is MuteResult.PermissionRequired -> {
                CameraMuteManager.showToast(this, getString(R.string.toast_permission_required))
                CameraMuteManager.showStatusNotification(this, result)
            }
            is MuteResult.Error -> {
                CameraMuteManager.showToast(this, String.format(getString(R.string.toast_error_prefix), result.message))
                CameraMuteManager.showStatusNotification(this, result)
            }
        }
        updateTileState()
        CameraMuteManager.updateTiles(this)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isMutedInCsc = CameraMuteManager.isMuted(this)
        val effectivelyMuted = CameraMuteManager.isEffectivelyMuted(this)
        val soundInfo = CameraMuteManager.getSoundModeInfo(this)

        tile.state = if (effectivelyMuted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_toggle_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                effectivelyMuted -> getString(R.string.tile_sub_muted)
                isMutedInCsc && !soundInfo.isSilentOrVibrate && soundInfo.systemVolume > 0 -> String.format(getString(R.string.tile_sub_volume_active), soundInfo.systemVolume)
                else -> getString(R.string.tile_sub_unmuted)
            }
        }
        // 消音切替も消音からカメラ音有効アイコンへ変更
        val iconRes = if (effectivelyMuted) R.drawable.ic_tile_shh else R.drawable.ic_tile_sound_on
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.updateTile()
    }
}
