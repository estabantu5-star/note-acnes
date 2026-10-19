package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SkinCareDatabase
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.skinCareDataStore
import com.example.data.repository.SkinCareRepository
import com.example.notification.NotificationHelper
import com.example.notification.ReminderScheduler
import com.example.ui.MainNavTab
import com.example.ui.SkinCareUiState
import com.example.ui.SkinCareViewModel
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.FirstRunNotificationSetupDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.NonBlockingStepTimerBar
import com.example.ui.components.NonIntrusiveUpdateAvailableBanner
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReminderSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.LiquidGlassTokens
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var pendingTargetPeriodFromNotification by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(applicationContext)
        pendingTargetPeriodFromNotification =
            intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_PERIOD)

        val database = SkinCareDatabase.getInstance(applicationContext)
        val preferencesRepository = UserPreferencesRepository(applicationContext.skinCareDataStore)
        val repository = SkinCareRepository(database.skinCareDao(), preferencesRepository)
        val reminderScheduler = ReminderScheduler(applicationContext)

        setContent {
            val viewModel: SkinCareViewModel = viewModel(
                factory = SkinCareViewModel.provideFactory(
                    context = applicationContext,
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    reminderScheduler = reminderScheduler
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(pendingTargetPeriodFromNotification) {
                pendingTargetPeriodFromNotification?.let { target ->
                    viewModel.handleIncomingNotificationIntent(target)
                    pendingTargetPeriodFromNotification = null
                }
            }

            val darkTheme = when (uiState.settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                SkinCareDailyAppRoot(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTargetPeriodFromNotification =
            intent.getStringExtra(NotificationHelper.EXTRA_TARGET_PERIOD)
    }
}

@Composable
fun SkinCareDailyAppRoot(
    uiState: SkinCareUiState,
    viewModel: SkinCareViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.markPermissionPrompted()
        viewModel.refreshPermissionsAndDateOnResume()
    }

    val requestNotificationPermissionIfNeeded = {
        viewModel.markPermissionPrompted()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshPermissionsAndDateOnResume()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndDateOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }

    // BackHandler on secondary tabs returns to Today tab
    BackHandler(enabled = uiState.selectedTab != MainNavTab.TODAY) {
        viewModel.selectTab(MainNavTab.TODAY)
    }

    // First-run notification onboarding dialog
    if (!uiState.isLoading && !uiState.settings.firstRunOnboardingDone) {
        FirstRunNotificationSetupDialog(
            onConfirmSetup = { morningEnabled, eveningEnabled, shouldAskPermission ->
                viewModel.completeOnboarding(morningEnabled, eveningEnabled)
                if (shouldAskPermission && !uiState.hasNotificationPermission && !uiState.settings.notificationPermissionAskedOnce) {
                    requestNotificationPermissionIfNeeded()
                }
            },
            onDismissLater = {
                viewModel.completeOnboarding(
                    morningEnabled = uiState.settings.morningReminderEnabled,
                    eveningEnabled = uiState.settings.eveningReminderEnabled
                )
            }
        )
    }

    // GitHub Releases Update Available Modal Dialog
    AppUpdateAvailableDialog(
        appUpdateState = uiState.appUpdateState,
        onStartDownload = viewModel::startUpdateDownload,
        onCancelDownload = viewModel::cancelUpdateDownload,
        onInstallDownloadedApk = viewModel::installDownloadedUpdate,
        onOpenUnknownSourcesSettings = viewModel::openUnknownAppSourcesSettings,
        onPostponeUpdate = viewModel::postponeAvailableUpdate,
        onDismiss = viewModel::dismissUpdateDetailsDialog
    )

    LiquidGlassBackground(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 640.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isExpandedScreen) {
                        SkinCareBottomNavigationBar(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        SkinCareNavigationRail(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .fillMaxHeight()
                        ) {
                            // Non-blocking 3-minute step interval timer bar (visible across all tabs when running)
                            NonBlockingStepTimerBar(
                                timerState = uiState.stepTimer,
                                onTogglePauseResume = viewModel::togglePauseResumeStepTimer,
                                onResetAndDismiss = viewModel::resetAndDismissStepTimer,
                                onRestartThreeMinutes = { viewModel.startStepTimer(uiState.stepTimer.contextLabel) }
                            )

                            // Non-intrusive banner when automatic 24h background check detects a new GitHub Release
                            NonIntrusiveUpdateAvailableBanner(
                                appUpdateState = uiState.appUpdateState,
                                onOpenUpdateDialog = viewModel::openUpdateDetailsDialog,
                                onPostponeUpdate = viewModel::postponeAvailableUpdate
                            )

                            when (uiState.selectedTab) {
                                MainNavTab.TODAY -> {
                                    TodayScreen(
                                        uiState = uiState,
                                        onToggleTask = viewModel::toggleTaskCompletion,
                                        onStartStepTimer = viewModel::startStepTimer,
                                        onResetTodayChecklist = viewModel::resetTodayChecklist,
                                        onOpenReminderSettings = { viewModel.selectTab(MainNavTab.REMINDERS) },
                                        onPauseOrResumeSunscreen = { pause ->
                                            val s = uiState.settings
                                            viewModel.configureSunscreenReminder(
                                                enabled = true,
                                                startHour = s.sunscreenStartHour,
                                                startMinute = s.sunscreenStartMinute,
                                                endHour = s.sunscreenEndHour,
                                                intervalHours = s.sunscreenIntervalHours,
                                                paused = pause,
                                                preOutdoorEnabled = s.sunscreenPreOutdoorEnabled
                                            )
                                        },
                                        onStopSunscreen = {
                                            val s = uiState.settings
                                            viewModel.configureSunscreenReminder(
                                                enabled = false,
                                                startHour = s.sunscreenStartHour,
                                                startMinute = s.sunscreenStartMinute,
                                                endHour = s.sunscreenEndHour,
                                                intervalHours = s.sunscreenIntervalHours,
                                                paused = false,
                                                preOutdoorEnabled = s.sunscreenPreOutdoorEnabled
                                            )
                                        },
                                        onLogMedicationDose = viewModel::recordMedicationDose,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                MainNavTab.REMINDERS -> {
                                    ReminderSettingsScreen(
                                        uiState = uiState,
                                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded,
                                        onUpdateMorningReminder = viewModel::updateMorningReminder,
                                        onUpdateEveningReminder = viewModel::updateEveningReminder,
                                        onUpdateFollowUpAndSnooze = viewModel::updateFollowUpAndSnooze,
                                        onToggleStepReminder = viewModel::toggleStepReminder,
                                        onConfigureSunscreenReminder = { enabled, startH, startM, endH, intervalH, paused ->
                                            viewModel.configureSunscreenReminder(
                                                enabled = enabled,
                                                startHour = startH,
                                                startMinute = startM,
                                                endHour = endH,
                                                intervalHours = intervalH,
                                                paused = paused,
                                                preOutdoorEnabled = uiState.settings.sunscreenPreOutdoorEnabled
                                            )
                                        },
                                        onConfirmAndSaveMedication = viewModel::confirmAndSaveMedicationSchedule,
                                        onUpdateDoctorControlReminder = viewModel::updateDoctorControlReminder,
                                        onTriggerTestNotification = viewModel::triggerTestNotification,
                                        onRescheduleAllReminders = viewModel::rescheduleAllRemindersManually,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                MainNavTab.HISTORY -> {
                                    HistoryScreen(
                                        uiState = uiState,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                MainNavTab.SETTINGS -> {
                                    SettingsScreen(
                                        uiState = uiState,
                                        onSelectTheme = viewModel::setThemeMode,
                                        onToggleHideSensitiveNotifications = viewModel::setHideSensitiveNotifications,
                                        onOpenReminderSettings = { viewModel.selectTab(MainNavTab.REMINDERS) },
                                        onAddCustomTask = { period, title, product, instruction, mins, isOptional, category, daysCsv, usage, area ->
                                            viewModel.addCustomTask(
                                                period = period,
                                                title = title,
                                                productName = product,
                                                instruction = instruction,
                                                estimatedMinutes = mins,
                                                isOptional = isOptional,
                                                reminderEnabled = false,
                                                reminderOffsetMinutes = 3,
                                                category = category,
                                                usageAmount = usage,
                                                applicationArea = area,
                                                scheduledDaysCsv = daysCsv
                                            )
                                        },
                                        onUpdateTask = { task, title, product, instruction, mins, isOptional, isActive, category, daysCsv, usage, area ->
                                            viewModel.updateTaskDetails(
                                                taskId = task.id,
                                                title = title,
                                                productName = product,
                                                instruction = instruction,
                                                estimatedMinutes = mins,
                                                isOptional = isOptional,
                                                isActive = isActive,
                                                reminderEnabled = task.reminderEnabled,
                                                reminderOffsetMinutes = task.reminderOffsetMinutes,
                                                category = category,
                                                usageAmount = usage,
                                                applicationArea = area,
                                                scheduledDaysCsv = daysCsv
                                            )
                                        },
                                        onMoveTaskOrder = viewModel::moveTaskOrder,
                                        onRetireTask = { task -> viewModel.retireTask(task.id, task.title) },
                                        onRestoreTaskActive = { task -> viewModel.restoreTaskActive(task.id, task.title) },
                                        onRestoreDermatologistDefaultRoutine = viewModel::restoreDermatologistDefaultRoutine,
                                        onExportJson = viewModel::exportBackupJson,
                                        onImportJson = viewModel::importBackupJson,
                                        onCheckForUpdate = { viewModel.checkForAppUpdates(isManual = true) },
                                        onStartUpdateDownload = viewModel::startUpdateDownload,
                                        onCancelUpdateDownload = viewModel::cancelUpdateDownload,
                                        onInstallDownloadedUpdate = viewModel::installDownloadedUpdate,
                                        onOpenUnknownSourcesSettings = viewModel::openUnknownAppSourcesSettings,
                                        onOpenUpdateDetailsDialog = viewModel::openUpdateDetailsDialog,
                                        onPostponeUpdate = viewModel::postponeAvailableUpdate,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating Frosted Glass Pill Bottom Navigation Bar with glowing active pill capsule.
 */
@Composable
private fun SkinCareBottomNavigationBar(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val pillShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bottom_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = pillShape,
                    ambientColor = glass.accentDeepBlue.copy(alpha = 0.18f),
                    spotColor = glass.accentPrimaryBlue.copy(alpha = 0.24f)
                )
                .clip(pillShape)
                .background(glass.glassSurfaceElevated)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glass.glassHighlightTop,
                            glass.glassBorderStrong,
                            glass.accentElectricCyan.copy(alpha = 0.45f)
                        )
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainNavTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val icon = when (tab) {
                    MainNavTab.TODAY -> if (selected) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday
                    MainNavTab.REMINDERS -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                    MainNavTab.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
                    MainNavTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            brush = if (selected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        glass.accentPrimaryBlue,
                                        glass.accentElectricCyan
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("nav_tab_${tab.route}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (selected) Color.White else glass.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else glass.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinCareNavigationRail(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    val glass = LiquidGlassTokens.colors
    NavigationRail(
        containerColor = glass.glassSurfaceElevated,
        modifier = Modifier.testTag("side_navigation_rail")
    ) {
        MainNavTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    val icon = when (tab) {
                        MainNavTab.TODAY -> if (selected) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday
                        MainNavTab.REMINDERS -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                        MainNavTab.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
                        MainNavTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                    }
                    Icon(imageVector = icon, contentDescription = tab.label)
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_tab_${tab.route}")
            )
        }
    }
}
