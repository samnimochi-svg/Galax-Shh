package com.example

import android.app.Activity
import android.os.Bundle
import com.example.data.CameraMuteManager
import com.example.data.MuteResult

class ShortcutExecutionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        CameraMuteManager.triggerHaptic(this)
        val result = CameraMuteManager.applyAction(this, action)

        when (result) {
            is MuteResult.Success -> {
                val stateMsg = if (result.isMuted) getString(R.string.toast_mute_applied_title) else getString(R.string.toast_sound_restored_title)
                val volMsg = when {
                    result.systemVolumeMuted -> getString(R.string.toast_sys_vol_muted)
                    result.systemVolumeRestored -> getString(R.string.toast_sys_vol_restored)
                    else -> ""
                }
                val message = if (result.cameraRestarted) {
                    CameraMuteManager.navigateToHome(this)
                    "$stateMsg$volMsg${getString(R.string.toast_camera_navigated_home)}"
                } else {
                    "$stateMsg$volMsg${getString(R.string.toast_mute_success_suffix)}"
                }
                CameraMuteManager.showToast(this, message)
            }
            is MuteResult.EulaRequired -> {
                CameraMuteManager.showToast(this, getString(R.string.toast_eula_required))
                val mainIntent = android.content.Intent(this, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(mainIntent)
            }
            is MuteResult.PermissionRequired -> {
                CameraMuteManager.showToast(this, getString(R.string.toast_permission_required))
            }
            is MuteResult.Error -> {
                CameraMuteManager.showToast(this, String.format(getString(R.string.toast_error_prefix), result.message))
            }
        }

        // Post notification so user sees persistent status
        CameraMuteManager.updateStatusNotification(this)
        CameraMuteManager.updateTiles(this)
        com.example.service.CameraMuteStatusMonitorService.start(this)

        finish()
    }
}
