package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.CameraMuteManager

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            if (CameraMuteManager.isEulaAccepted(context)) {
                if (CameraMuteManager.isAutoApplyOnBoot(context)) {
                    val result = CameraMuteManager.setMute(context, true)
                    CameraMuteManager.showToast(context, context.getString(R.string.toast_boot_mute_applied))
                }
                CameraMuteManager.updateStatusNotification(context)
                CameraMuteManager.updateTiles(context)
                if (CameraMuteManager.isPersistentNotificationEnabled(context)) {
                    com.example.service.CameraMuteStatusMonitorService.start(context)
                }
            }
        }
    }
}
