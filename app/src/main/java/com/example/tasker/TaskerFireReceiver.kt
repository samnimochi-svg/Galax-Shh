package com.example.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.CameraMuteManager
import com.example.data.MuteResult

class TaskerFireReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != TaskerConstants.ACTION_FIRE_SETTING) return

        val bundle = intent.getBundleExtra(TaskerConstants.EXTRA_BUNDLE)
        val selectedAction = bundle?.getString(TaskerConstants.KEY_ACTION)
            ?: TaskerConstants.VAL_TOGGLE

        val result = when (selectedAction) {
            TaskerConstants.VAL_MUTE_ON -> CameraMuteManager.setMute(context, true)
            TaskerConstants.VAL_MUTE_OFF -> CameraMuteManager.setMute(context, false)
            else -> CameraMuteManager.toggleMute(context)
        }

        CameraMuteManager.updateStatusNotification(context)
        CameraMuteManager.updateTiles(context)
        com.example.service.CameraMuteStatusMonitorService.start(context)

        when (result) {
            is MuteResult.Success -> {
                val stateMsg = if (result.isMuted) {
                    context.getString(R.string.toast_tasker_mute_applied)
                } else {
                    context.getString(R.string.toast_tasker_sound_restored)
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
                CameraMuteManager.showToast(context, context.getString(R.string.toast_tasker_permission_error))
            }
            is MuteResult.Error -> {
                CameraMuteManager.showToast(context, String.format(context.getString(R.string.toast_tasker_error_prefix), result.message))
            }
        }
    }
}
