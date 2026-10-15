package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.CameraMuteManager
import com.example.data.MuteResult

class ExternalAutomationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == CameraMuteManager.ACTION_MUTE_SYSTEM_ONLY) {
            CameraMuteManager.triggerHaptic(context)
            val muted = CameraMuteManager.muteSystemVolumeIfNeeded(context)
            if (muted) {
                CameraMuteManager.showToast(context, context.getString(R.string.toast_mute_applied_title) + " (" + context.getString(R.string.toast_sys_vol_muted) + ")")
            }
            CameraMuteManager.updateStatusNotification(context)
            CameraMuteManager.updateTiles(context)
            com.example.service.CameraMuteStatusMonitorService.start(context)
            return
        }

        if (action !in listOf(
                CameraMuteManager.ACTION_TOGGLE,
                CameraMuteManager.ACTION_MUTE_ON,
                CameraMuteManager.ACTION_MUTE_OFF
            )
        ) {
            return
        }

        val result = CameraMuteManager.applyAction(context, action)
        CameraMuteManager.updateStatusNotification(context)
        CameraMuteManager.updateTiles(context)
        com.example.service.CameraMuteStatusMonitorService.start(context)

        when (result) {
            is MuteResult.Success -> {
                val stateMsg = if (result.isMuted) {
                    context.getString(R.string.toast_auto_mute_applied)
                } else {
                    context.getString(R.string.toast_auto_sound_restored)
                }
                val msg = if (result.cameraRestarted) {
                    CameraMuteManager.navigateToHome(context)
                    "$stateMsg${context.getString(R.string.toast_camera_navigated_home)}"
                } else {
                    stateMsg
                }
                CameraMuteManager.showToast(context, msg)
            }
            is MuteResult.EulaRequired -> {
                CameraMuteManager.showToast(context, context.getString(R.string.toast_eula_required))
            }
            is MuteResult.PermissionRequired -> {
                CameraMuteManager.showToast(context, context.getString(R.string.toast_auto_permission_error))
            }
            is MuteResult.Error -> {
                CameraMuteManager.showToast(context, String.format(context.getString(R.string.toast_auto_error_prefix), result.message))
            }
        }
    }
}
