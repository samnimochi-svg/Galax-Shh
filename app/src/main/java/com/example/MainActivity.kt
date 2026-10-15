package com.example

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CameraMuteManager
import com.example.data.MuteResult
import com.example.service.CameraMuteAccessibilityService
import com.example.service.CameraMuteStatusMonitorService
import com.example.ui.components.AccessibilityHomeServiceCard
import com.example.ui.components.BatteryOptimizationCard
import com.example.ui.components.BootAutoApplyCard
import com.example.ui.components.EthicsAndLegalNoticeCard
import com.example.ui.components.EulaAgreementDialog
import com.example.ui.components.FaqAccordionCard
import com.example.ui.components.IntegrationCard
import com.example.ui.components.MainStatusCard
import com.example.ui.components.PermissionRequiredCard
import com.example.ui.components.SoundModeGuideCard
import com.example.ui.components.StatusNotificationCard
import com.example.ui.components.SystemDiagnosisCard
import com.example.ui.components.UninstallAndResetCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OneUiBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusGreenText

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupShortcuts()

        setContent {
            MyApplicationTheme {
                GalaxShhMainScreen()
            }
        }
    }

    private fun setupShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val shortcutManager = getSystemService(ShortcutManager::class.java)
                if (shortcutManager != null) {
                    val toggleShortcut = ShortcutInfo.Builder(this, "shortcut_toggle")
                        .setShortLabel(getString(R.string.shortcut_toggle_short))
                        .setLongLabel(getString(R.string.shortcut_toggle_long))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_toggle))
                        .setIntent(
                            Intent(CameraMuteManager.ACTION_TOGGLE)
                                .setClass(this, ShortcutExecutionActivity::class.java)
                        )
                        .build()

                    val muteOnShortcut = ShortcutInfo.Builder(this, "shortcut_mute_on")
                        .setShortLabel(getString(R.string.shortcut_mute_on_short))
                        .setLongLabel(getString(R.string.shortcut_mute_on_long))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_mute_on))
                        .setIntent(
                            Intent(CameraMuteManager.ACTION_MUTE_ON)
                                .setClass(this, ShortcutExecutionActivity::class.java)
                        )
                        .build()

                    val muteOffShortcut = ShortcutInfo.Builder(this, "shortcut_mute_off")
                        .setShortLabel(getString(R.string.shortcut_mute_off_short))
                        .setLongLabel(getString(R.string.shortcut_mute_off_long))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_mute_off))
                        .setIntent(
                            Intent(CameraMuteManager.ACTION_MUTE_OFF)
                                .setClass(this, ShortcutExecutionActivity::class.java)
                        )
                        .build()

                    shortcutManager.dynamicShortcuts = listOf(toggleShortcut, muteOnShortcut, muteOffShortcut)
                }
            } catch (_: Throwable) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxShhMainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reactive states
    var isEulaAccepted by remember { mutableStateOf(CameraMuteManager.isEulaAccepted(context)) }
    var isMuted by remember { mutableStateOf(CameraMuteManager.isMuted(context)) }
    var rawCscValue by remember { mutableStateOf(CameraMuteManager.getRawCscValue(context)) }
    var soundModeInfo by remember { mutableStateOf(CameraMuteManager.getSoundModeInfo(context)) }
    var autoApplyOnBoot by remember { mutableStateOf(CameraMuteManager.isAutoApplyOnBoot(context)) }
    var canWriteSettings by remember { mutableStateOf(CameraMuteManager.canModifySystemSettings(context)) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(CameraMuteAccessibilityService.isServiceEnabledInSettings(context))
    }
    var isAccessibilityRunning by remember {
        mutableStateOf(CameraMuteAccessibilityService.isRunning())
    }
    var isIgnoringBattery by remember {
        mutableStateOf(CameraMuteManager.isIgnoringBatteryOptimizations(context))
    }
    var isPersistentNotification by remember {
        mutableStateOf(CameraMuteManager.isPersistentNotificationEnabled(context))
    }
    var showPermissionError by remember { mutableStateOf(false) }

    // Navigation state
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var selectedFoldableRightTab by remember { mutableIntStateOf(0) }

    fun refreshAllState() {
        isEulaAccepted = CameraMuteManager.isEulaAccepted(context)
        isMuted = CameraMuteManager.isMuted(context)
        rawCscValue = CameraMuteManager.getRawCscValue(context)
        soundModeInfo = CameraMuteManager.getSoundModeInfo(context)
        autoApplyOnBoot = CameraMuteManager.isAutoApplyOnBoot(context)
        isPersistentNotification = CameraMuteManager.isPersistentNotificationEnabled(context)
        canWriteSettings = CameraMuteManager.canModifySystemSettings(context)
        isAccessibilityEnabled = CameraMuteAccessibilityService.isServiceEnabledInSettings(context)
        isAccessibilityRunning = CameraMuteAccessibilityService.isRunning()
        isIgnoringBattery = CameraMuteManager.isIgnoringBatteryOptimizations(context)

        if (isIgnoringBattery) {
            CameraMuteManager.dismissBatterySetupNotification(context)
        }
        if (isAccessibilityEnabled) {
            CameraMuteManager.dismissA11ySetupNotification(context)
        }
    }

    // Check notifications & start background monitor service on launch
    LaunchedEffect(Unit) {
        CameraMuteManager.checkAndSendSetupNotifications(context)
        CameraMuteManager.updateStatusNotification(context)
        CameraMuteStatusMonitorService.start(context)
    }

    // Refresh on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllState()
                CameraMuteManager.updateStatusNotification(context)
                CameraMuteManager.updateTiles(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Listen to Ringer Mode, Volume changes, and CSC Setting changes in real time
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            @Suppress("DEPRECATION")
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                soundModeInfo = CameraMuteManager.getSoundModeInfo(context)
                CameraMuteManager.updateTiles(context)
                CameraMuteManager.updateStatusNotification(context)
            }
        }
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val cscObserver = object : android.database.ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshAllState()
                CameraMuteManager.updateTiles(context)
                CameraMuteManager.updateStatusNotification(context)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Throwable) {}

        try {
            val cscUri = Settings.System.getUriFor(CameraMuteManager.CSC_KEY)
            context.contentResolver.registerContentObserver(cscUri, false, cscObserver)
        } catch (_: Throwable) {}

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Throwable) {}
            try {
                context.contentResolver.unregisterContentObserver(cscObserver)
            } catch (_: Throwable) {}
        }
    }

    fun applyMuteChange(mute: Boolean) {
        val result = CameraMuteManager.setMute(context, mute)
        CameraMuteManager.updateStatusNotification(context)
        CameraMuteManager.updateTiles(context)
        CameraMuteStatusMonitorService.start(context)
        refreshAllState()
        when (result) {
            is MuteResult.Success -> {
                showPermissionError = false
                val stateMsg = if (result.isMuted) context.getString(R.string.toast_mute_applied_title) else context.getString(R.string.toast_sound_restored_title)
                val volMsg = when {
                    result.systemVolumeMuted -> context.getString(R.string.toast_sys_vol_muted)
                    result.systemVolumeRestored -> context.getString(R.string.toast_sys_vol_restored)
                    else -> ""
                }
                val msg = if (result.cameraRestarted) {
                    "$stateMsg$volMsg${context.getString(R.string.toast_camera_restart_needed)}"
                } else {
                    "$stateMsg$volMsg${context.getString(R.string.toast_mute_success_suffix)}"
                }
                CameraMuteManager.showToast(context, msg)
            }
            is MuteResult.EulaRequired -> {
                showPermissionError = false
                isEulaAccepted = false
                CameraMuteManager.showToast(context, context.getString(R.string.toast_eula_required))
            }
            is MuteResult.PermissionRequired -> {
                showPermissionError = true
                CameraMuteManager.showToast(context, context.getString(R.string.toast_permission_required))
            }
            is MuteResult.Error -> {
                showPermissionError = true
                CameraMuteManager.showToast(context, String.format(context.getString(R.string.toast_error_prefix), result.message))
            }
        }
    }

    fun applyMuteSystemVolume() {
        val success = CameraMuteManager.muteSystemVolumeIfNeeded(context)
        soundModeInfo = CameraMuteManager.getSoundModeInfo(context)
        CameraMuteManager.updateStatusNotification(context)
        CameraMuteManager.updateTiles(context)
        if (success) {
            CameraMuteManager.showToast(context, context.getString(R.string.toast_sys_vol_muted))
        }
    }

    if (!isEulaAccepted) {
        EulaAgreementDialog(
            onAccept = {
                CameraMuteManager.setEulaAccepted(context, true)
                isEulaAccepted = true
                CameraMuteManager.checkAndSendSetupNotifications(context, force = true)
                refreshAllState()
            },
            onDecline = {
                (context as? Activity)?.finishAffinity()
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isFoldableExpanded = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_icon_shh),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isFoldableExpanded) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(StatusGreenContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ViewSidebar,
                                                    contentDescription = null,
                                                    tint = StatusGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.foldable_dual_pane_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusGreenText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.app_header_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            refreshAllState()
                            CameraMuteManager.updateStatusNotification(context)
                            CameraMuteManager.updateTiles(context)
                            CameraMuteManager.showToast(context, context.getString(R.string.toast_reloaded))
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isFoldableExpanded) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        val items = listOf(
                            NavigationItem(stringResource(R.string.nav_control), Icons.AutoMirrored.Filled.VolumeMute),
                            NavigationItem(stringResource(R.string.nav_integration), Icons.Default.TouchApp),
                            NavigationItem(stringResource(R.string.nav_settings), Icons.Default.Settings),
                            NavigationItem(stringResource(R.string.nav_guide), Icons.AutoMirrored.Filled.HelpOutline)
                        )
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedNavIndex == index,
                                onClick = { selectedNavIndex = index },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontWeight = if (selectedNavIndex == index) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = OneUiBlue,
                                    selectedTextColor = OneUiBlue,
                                    indicatorColor = OneUiBlue.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (isFoldableExpanded) {
                // =========================================================================
                // GALAXY Z FOLD / TABLET EXPANDED DUAL-PANE LAYOUT
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Left Pane: Permanent Live Control Hub (380.dp fixed width)
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EthicsAndLegalNoticeCard()

                        if (showPermissionError || !canWriteSettings) {
                            PermissionRequiredCard(onOpenSettings = {
                                openSettingsScreen(context)
                            })
                        }

                        MainStatusCard(
                            isMuted = isMuted,
                            onToggle = { applyMuteChange(!isMuted) },
                            onApplyMute = { applyMuteChange(true) },
                            onRestoreSound = { applyMuteChange(false) }
                        )

                        SoundModeGuideCard(
                            isMuted = isMuted,
                            soundModeInfo = soundModeInfo,
                            onOpenSoundSettings = { openSoundSettingsScreen(context) },
                            onMuteSystemVolume = { applyMuteSystemVolume() }
                        )

                        StatusNotificationCard(
                            isPersistent = isPersistentNotification,
                            isEffectivelyMuted = isMuted && (soundModeInfo.isSilentOrVibrate || soundModeInfo.systemVolume == 0),
                            isCscMuted = isMuted,
                            soundModeInfo = soundModeInfo,
                            onTogglePersistent = { enabled ->
                                CameraMuteManager.setPersistentNotificationEnabled(context, enabled)
                                isPersistentNotification = enabled
                            },
                            onRefreshNotification = {
                                CameraMuteManager.updateStatusNotification(context)
                                CameraMuteManager.showToast(context, context.getString(R.string.toast_notif_refreshed))
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Right Pane: Detail & Configuration Tabs (Flexible weight)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 12.dp)
                    ) {
                        val rightPaneTabs = listOf(
                            stringResource(R.string.nav_integration),
                            stringResource(R.string.nav_settings),
                            stringResource(R.string.nav_guide)
                        )

                        TabRow(
                            selectedTabIndex = selectedFoldableRightTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            divider = {}
                        ) {
                            rightPaneTabs.forEachIndexed { index, tabTitle ->
                                Tab(
                                    selected = selectedFoldableRightTab == index,
                                    onClick = { selectedFoldableRightTab = index },
                                    text = {
                                        Text(
                                            text = tabTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selectedFoldableRightTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (selectedFoldableRightTab) {
                                0 -> {
                                    IntegrationCard(context = context)
                                    AccessibilityHomeServiceCard(
                                        context = context,
                                        isEnabled = isAccessibilityEnabled,
                                        isRunning = isAccessibilityRunning,
                                        onOpenSettings = {
                                            CameraMuteAccessibilityService.openAccessibilitySettings(context)
                                        },
                                        onTestHome = {
                                            val success = CameraMuteAccessibilityService.pressHome()
                                            val msg = if (success) context.getString(R.string.toast_test_home_success) else context.getString(R.string.toast_test_home_not_running)
                                            CameraMuteManager.showToast(context, msg)
                                        }
                                    )
                                }
                                1 -> {
                                    BootAutoApplyCard(
                                        autoApply = autoApplyOnBoot,
                                        onToggle = { enabled ->
                                            CameraMuteManager.setAutoApplyOnBoot(context, enabled)
                                            autoApplyOnBoot = enabled
                                            val msg = if (enabled) context.getString(R.string.toast_boot_auto_apply_enabled) else context.getString(R.string.toast_boot_auto_apply_disabled)
                                            CameraMuteManager.showToast(context, msg)
                                        }
                                    )
                                    BatteryOptimizationCard(
                                        isIgnoringBattery = isIgnoringBattery,
                                        onRequestIgnore = {
                                            CameraMuteManager.requestIgnoreBatteryOptimization(context)
                                        }
                                    )
                                    SystemDiagnosisCard(
                                        rawCscValue = rawCscValue,
                                        canWrite = canWriteSettings,
                                        onRefresh = { refreshAllState() }
                                    )
                                    UninstallAndResetCard(
                                        onRestoreDefaults = {
                                            val success = CameraMuteManager.restoreAllToDefaults(context)
                                            refreshAllState()
                                            val msg = if (success) context.getString(R.string.toast_defaults_restored) else context.getString(R.string.toast_permission_required)
                                            CameraMuteManager.showToast(context, msg)
                                        },
                                        onOpenUninstall = {
                                            CameraMuteManager.restoreAllToDefaults(context)
                                            refreshAllState()
                                            CameraMuteManager.launchUninstall(context)
                                        }
                                    )
                                }
                                2 -> {
                                    EthicsAndLegalNoticeCard()
                                    FaqAccordionCard()
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            } else {
                // =========================================================================
                // COMPACT / FOLDED SCREEN (COVER SCREEN) TABBED LAYOUT
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedNavIndex) {
                        0 -> {
                            // Tab 0: Master Control & Shutter Mute
                            EthicsAndLegalNoticeCard()

                            if (showPermissionError || !canWriteSettings) {
                                PermissionRequiredCard(onOpenSettings = {
                                    openSettingsScreen(context)
                                })
                            }

                            MainStatusCard(
                                isMuted = isMuted,
                                onToggle = { applyMuteChange(!isMuted) },
                                onApplyMute = { applyMuteChange(true) },
                                onRestoreSound = { applyMuteChange(false) }
                            )

                            SoundModeGuideCard(
                                isMuted = isMuted,
                                soundModeInfo = soundModeInfo,
                                onOpenSoundSettings = { openSoundSettingsScreen(context) },
                                onMuteSystemVolume = { applyMuteSystemVolume() }
                            )

                            StatusNotificationCard(
                                isPersistent = isPersistentNotification,
                                isEffectivelyMuted = isMuted && (soundModeInfo.isSilentOrVibrate || soundModeInfo.systemVolume == 0),
                                isCscMuted = isMuted,
                                soundModeInfo = soundModeInfo,
                                onTogglePersistent = { enabled ->
                                    CameraMuteManager.setPersistentNotificationEnabled(context, enabled)
                                    isPersistentNotification = enabled
                                },
                                onRefreshNotification = {
                                    CameraMuteManager.updateStatusNotification(context)
                                    CameraMuteManager.showToast(context, context.getString(R.string.toast_notif_refreshed))
                                }
                            )
                        }
                        1 -> {
                            // Tab 1: External Automation & Shortcuts
                            IntegrationCard(context = context)

                            AccessibilityHomeServiceCard(
                                context = context,
                                isEnabled = isAccessibilityEnabled,
                                isRunning = isAccessibilityRunning,
                                onOpenSettings = {
                                    CameraMuteAccessibilityService.openAccessibilitySettings(context)
                                },
                                onTestHome = {
                                    val success = CameraMuteAccessibilityService.pressHome()
                                    val msg = if (success) context.getString(R.string.toast_test_home_success) else context.getString(R.string.toast_test_home_not_running)
                                    CameraMuteManager.showToast(context, msg)
                                }
                            )
                        }
                        2 -> {
                            // Tab 2: Settings, Diagnostics & Reset
                            BootAutoApplyCard(
                                autoApply = autoApplyOnBoot,
                                onToggle = { enabled ->
                                    CameraMuteManager.setAutoApplyOnBoot(context, enabled)
                                    autoApplyOnBoot = enabled
                                    val msg = if (enabled) context.getString(R.string.toast_boot_auto_apply_enabled) else context.getString(R.string.toast_boot_auto_apply_disabled)
                                    CameraMuteManager.showToast(context, msg)
                                }
                            )

                            BatteryOptimizationCard(
                                isIgnoringBattery = isIgnoringBattery,
                                onRequestIgnore = {
                                    CameraMuteManager.requestIgnoreBatteryOptimization(context)
                                }
                            )

                            SystemDiagnosisCard(
                                rawCscValue = rawCscValue,
                                canWrite = canWriteSettings,
                                onRefresh = { refreshAllState() }
                            )

                            UninstallAndResetCard(
                                onRestoreDefaults = {
                                    val success = CameraMuteManager.restoreAllToDefaults(context)
                                    refreshAllState()
                                    val msg = if (success) context.getString(R.string.toast_defaults_restored) else context.getString(R.string.toast_permission_required)
                                    CameraMuteManager.showToast(context, msg)
                                },
                                onOpenUninstall = {
                                    CameraMuteManager.restoreAllToDefaults(context)
                                    refreshAllState()
                                    CameraMuteManager.launchUninstall(context)
                                }
                            )
                        }
                        3 -> {
                            // Tab 3: Legal Notice & FAQ
                            EthicsAndLegalNoticeCard()
                            FaqAccordionCard()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun openSettingsScreen(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openSoundSettingsScreen(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        CameraMuteManager.showToast(context, context.getString(R.string.toast_cannot_open_sound_settings))
    }
}
