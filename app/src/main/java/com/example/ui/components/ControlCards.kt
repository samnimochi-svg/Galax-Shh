package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SoundModeInfo
import com.example.ui.theme.OneUiBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer

@Composable
fun PermissionRequiredCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StatusRedContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.permission_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusRed
                )
            }
            Text(
                text = stringResource(R.string.permission_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = StatusRed,
                lineHeight = 16.sp
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.permission_card_btn))
            }
        }
    }
}

@Composable
fun MainStatusCard(
    isMuted: Boolean,
    onToggle: () -> Unit,
    onApplyMute: () -> Unit,
    onRestoreSound: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMuted) StatusGreenContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isMuted) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon_shh),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tile_sound_on),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isMuted) stringResource(R.string.status_muted_active) else stringResource(R.string.status_sound_active),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) StatusGreenText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isMuted) stringResource(R.string.status_csc_muted_desc) else stringResource(R.string.status_csc_sound_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMuted) StatusGreenText.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                Switch(
                    checked = isMuted,
                    onCheckedChange = { onToggle() }
                )
            }

            HorizontalDivider(
                color = if (isMuted) StatusGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Direct Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onApplyMute,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMuted) StatusGreen else OneUiBlue
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tile_shh),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_apply_mute))
                }

                val restoreContentColor = if (isMuted) StatusGreenText else MaterialTheme.colorScheme.onSurface
                val restoreBorderColor = if (isMuted) StatusGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline

                OutlinedButton(
                    onClick = onRestoreSound,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, restoreBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMuted) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                        contentColor = restoreContentColor
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tile_sound_on),
                        contentDescription = null,
                        tint = restoreContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_restore_sound),
                        color = restoreContentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SoundModeGuideCard(
    isMuted: Boolean,
    soundModeInfo: SoundModeInfo,
    onOpenSoundSettings: () -> Unit,
    onMuteSystemVolume: (() -> Unit)? = null
) {
    val isCompletelySilent = isMuted && (soundModeInfo.isSilentOrVibrate || soundModeInfo.systemVolume == 0)

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.sound_guide_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoundStatusBadge(
                    label = stringResource(R.string.sound_badge_device_mode),
                    value = soundModeInfo.ringerModeName,
                    isGood = soundModeInfo.isSilentOrVibrate,
                    modifier = Modifier.weight(1f)
                )
                SoundStatusBadge(
                    label = stringResource(R.string.sound_badge_system_vol),
                    value = "${soundModeInfo.systemVolume} / ${soundModeInfo.maxSystemVolume}",
                    isGood = soundModeInfo.systemVolume == 0,
                    modifier = Modifier.weight(1f)
                )
            }

            // Real-time condition evaluation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCompletelySilent) StatusGreenContainer
                        else if (isMuted && soundModeInfo.systemVolume > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCompletelySilent) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isCompletelySilent) StatusGreen else if (isMuted && soundModeInfo.systemVolume > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompletelySilent) {
                            stringResource(R.string.sound_status_silent_ok)
                        } else if (isMuted) {
                            stringResource(R.string.sound_status_warning_vol)
                        } else {
                            stringResource(R.string.sound_status_sound_on)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCompletelySilent || (isMuted && soundModeInfo.systemVolume > 0)) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCompletelySilent) StatusGreenText else if (isMuted && soundModeInfo.systemVolume > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            if (isMuted && !soundModeInfo.isSilentOrVibrate && soundModeInfo.systemVolume > 0 && onMuteSystemVolume != null) {
                Button(
                    onClick = onMuteSystemVolume,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.btn_mute_system_volume_guarantee),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            FilledTonalButton(
                onClick = onOpenSoundSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_open_sound_settings))
            }
        }
    }
}

@Composable
fun SoundStatusBadge(
    label: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGood) StatusGreen else MaterialTheme.colorScheme.outline)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusNotificationCard(
    isPersistent: Boolean,
    isEffectivelyMuted: Boolean,
    isCscMuted: Boolean,
    soundModeInfo: SoundModeInfo,
    onTogglePersistent: (Boolean) -> Unit,
    onRefreshNotification: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.notif_setting_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusGreenContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.notif_setting_active_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreenText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.notif_setting_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            // Current State preview in Notification
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isEffectivelyMuted) StatusGreenContainer
                        else if (isCscMuted && soundModeInfo.systemVolume > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(
                            if (isEffectivelyMuted) R.drawable.app_icon_shh else R.drawable.ic_tile_sound_on
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isEffectivelyMuted -> stringResource(R.string.notification_status_muted_title)
                                isCscMuted && soundModeInfo.systemVolume > 0 -> String.format(stringResource(R.string.notification_status_warning_vol_title), soundModeInfo.systemVolume)
                                else -> stringResource(R.string.notification_status_sound_on_title)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEffectivelyMuted) StatusGreenText else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                isEffectivelyMuted -> stringResource(R.string.notification_status_muted_detail)
                                isCscMuted && soundModeInfo.systemVolume > 0 -> String.format(stringResource(R.string.notification_status_warning_vol_desc), soundModeInfo.systemVolume)
                                else -> stringResource(R.string.notification_status_sound_on_detail)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (isEffectivelyMuted) StatusGreenText.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notif_setting_persistent_switch),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(if (isPersistent) R.string.notif_setting_persistent_on else R.string.notif_setting_persistent_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPersistent,
                    onCheckedChange = { onTogglePersistent(it) }
                )
            }

            FilledTonalButton(
                onClick = onRefreshNotification,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.notif_setting_refresh_btn))
            }
        }
    }
}
