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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.components.FirstRunNotificationSetupDialog
import com.example.ui.components.NonBlockingStepTimerBar
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReminderSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var pendingTargetPeriodFromNotification: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        pendingTargetPeriodFromNotification =
            intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_PERIOD)

        val app = application as? SkinCareApplication
        val database = app?.database ?: SkinCareDatabase.getInstance(this)
        val prefsRepo = app?.preferencesRepository ?: UserPreferencesRepository(this.skinCareDataStore)
        val repository = app?.repository ?: SkinCareRepository(database.skinCareDao(), prefsRepo)
        val scheduler = app?.reminderScheduler ?: ReminderScheduler(this)

        setContent {
            val viewModel: SkinCareViewModel = viewModel(
                factory = SkinCareViewModel.provideFactory(
                    context = this,
                    repository = repository,
                    preferencesRepository = prefsRepo,
                    reminderScheduler = scheduler
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                                            paused = pause
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
                                            paused = false
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
                                    onConfigureSunscreenReminder = viewModel::configureSunscreenReminder,
                                    onConfirmAndSaveMedication = viewModel::confirmAndSaveMedicationSchedule,
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
                                    onAddCustomTask = { period, title, product, instruction, mins, isOptional ->
                                        viewModel.addCustomTask(
                                            period = period,
                                            title = title,
                                            productName = product,
                                            instruction = instruction,
                                            estimatedMinutes = mins,
                                            isOptional = isOptional,
                                            reminderEnabled = false,
                                            reminderOffsetMinutes = 3
                                        )
                                    },
                                    onUpdateTask = { task, title, product, instruction, mins, isOptional, isActive ->
                                        viewModel.updateTaskDetails(
                                            taskId = task.id,
                                            title = title,
                                            productName = product,
                                            instruction = instruction,
                                            estimatedMinutes = mins,
                                            isOptional = isOptional,
                                            isActive = isActive,
                                            reminderEnabled = task.reminderEnabled,
                                            reminderOffsetMinutes = task.reminderOffsetMinutes
                                        )
                                    },
                                    onMoveTaskOrder = viewModel::moveTaskOrder,
                                    onRetireTask = { task -> viewModel.retireTask(task.id, task.title) },
                                    onRestoreTaskActive = { task -> viewModel.restoreTaskActive(task.id, task.title) },
                                    onRestoreDermatologistDefaultRoutine = viewModel::restoreDermatologistDefaultRoutine,
                                    onExportJson = viewModel::exportBackupJson,
                                    onImportJson = viewModel::importBackupJson,
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

@Composable
private fun SkinCareBottomNavigationBar(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
        MainNavTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
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
                modifier = Modifier.testTag("nav_tab_${tab.route}")
            )
        }
    }
}

@Composable
private fun SkinCareNavigationRail(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
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
