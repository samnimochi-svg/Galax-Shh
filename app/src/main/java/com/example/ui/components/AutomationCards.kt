package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CameraMuteManager

@Composable
fun IntegrationCard(context: Context) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_quick_settings),
        stringResource(R.string.tab_shortcuts),
        stringResource(R.string.tab_automation)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.integration_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> QuickSettingsTabContent()
                1 -> ShortcutsTabContent()
                2 -> AutomationTabContent(context = context)
            }
        }
    }
}

@Composable
fun QuickSettingsTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.qs_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TileDocRow(
            title = stringResource(R.string.qs_tile1_title),
            desc = stringResource(R.string.qs_tile1_desc),
            iconRes = R.drawable.ic_tile_shh
        )
        TileDocRow(
            title = stringResource(R.string.qs_tile2_title),
            desc = stringResource(R.string.qs_tile2_desc),
            iconRes = R.drawable.ic_tile_shh
        )
        TileDocRow(
            title = stringResource(R.string.qs_tile3_title),
            desc = stringResource(R.string.qs_tile3_desc),
            iconRes = R.drawable.ic_tile_sound_on
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.qs_a11y_notice_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.qs_a11y_notice_desc),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.qs_edit_hint),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TileDocRow(
    title: String,
    desc: String,
    iconRes: Int = R.drawable.app_icon_shh
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes == R.drawable.app_icon_shh ||
            iconRes == R.drawable.ic_shortcut_toggle ||
            iconRes == R.drawable.ic_shortcut_mute_on ||
            iconRes == R.drawable.ic_shortcut_mute_off
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(if (iconRes == R.drawable.app_icon_shh) RoundedCornerShape(4.dp) else CircleShape)
            )
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ShortcutsTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.shortcuts_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TileDocRow(
            title = stringResource(R.string.shortcut_toggle_short),
            desc = stringResource(R.string.shortcuts_item1_desc),
            iconRes = R.drawable.ic_shortcut_toggle
        )
        TileDocRow(
            title = stringResource(R.string.shortcut_mute_on_short),
            desc = stringResource(R.string.shortcuts_item2_desc),
            iconRes = R.drawable.ic_shortcut_mute_on
        )
        TileDocRow(
            title = stringResource(R.string.shortcut_mute_off_short),
            desc = stringResource(R.string.shortcuts_item3_desc),
            iconRes = R.drawable.ic_shortcut_mute_off
        )

        Text(
            text = stringResource(R.string.shortcuts_drag_hint),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AutomationTabContent(context: Context) {
    fun copyToClipboard(actionString: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Broadcast Action", actionString)
        clipboard.setPrimaryClip(clip)
        CameraMuteManager.triggerHaptic(context)
        CameraMuteManager.showToast(context, context.getString(R.string.toast_copied_to_clipboard, actionString))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.automation_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tasker Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp)
        ) {
            Text(
                text = stringResource(R.string.automation_tasker_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.automation_tasker_desc),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Broadcast Section (MacroDroid / Modes & Routines)
        Text(
            text = stringResource(R.string.automation_broadcast_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        BroadcastCopyRow(
            name = stringResource(R.string.tasker_action_toggle_title),
            action = CameraMuteManager.ACTION_TOGGLE,
            onCopy = { copyToClipboard(CameraMuteManager.ACTION_TOGGLE) }
        )
        BroadcastCopyRow(
            name = stringResource(R.string.tasker_action_mute_on_title),
            action = CameraMuteManager.ACTION_MUTE_ON,
            onCopy = { copyToClipboard(CameraMuteManager.ACTION_MUTE_ON) }
        )
        BroadcastCopyRow(
            name = stringResource(R.string.tasker_action_mute_off_title),
            action = CameraMuteManager.ACTION_MUTE_OFF,
            onCopy = { copyToClipboard(CameraMuteManager.ACTION_MUTE_OFF) }
        )
        BroadcastCopyRow(
            name = stringResource(R.string.notification_action_mute_vol),
            action = CameraMuteManager.ACTION_MUTE_SYSTEM_ONLY,
            onCopy = { copyToClipboard(CameraMuteManager.ACTION_MUTE_SYSTEM_ONLY) }
        )
    }
}

@Composable
fun BroadcastCopyRow(
    name: String,
    action: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = action,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_copy), modifier = Modifier.size(16.dp))
        }
    }
}
