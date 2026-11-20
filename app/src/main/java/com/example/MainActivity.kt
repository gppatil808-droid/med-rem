package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.DoseStatus
import com.example.reminder.NotificationHelper
import com.example.ui.MedCueViewModel
import com.example.ui.components.ReminderAlertDialog
import com.example.ui.screens.AddEditMedicineScreen
import com.example.ui.screens.CabinetScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MedCueTheme

object MedCueRoutes {
    const val ROUTE_TODAY = "today"
    const val ROUTE_CABINET = "cabinet"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_ADD_EDIT = "add_edit"
    const val ROUTE_SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private var pendingDeepLinkIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLinkIntent = intent

        setContent {
            MedCueTheme {
                val app = application as MedCueApplication
                val viewModel: MedCueViewModel = viewModel(
                    factory = MedCueViewModel.factory(app),
                )
                MedCueRoot(
                    viewModel = viewModel,
                    deepLinkIntent = pendingDeepLinkIntent,
                    onConsumeDeepLink = { pendingDeepLinkIntent = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkIntent = intent
    }
}

@Composable
private fun MedCueRoot(
    viewModel: MedCueViewModel,
    deepLinkIntent: Intent?,
    onConsumeDeepLink: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(viewModel.canScheduleExactAlarms())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                val nowExact = viewModel.canScheduleExactAlarms()
                if (nowExact && !hasExactAlarmPermission) {
                    viewModel.rescheduleAllAlarms()
                }
                hasExactAlarmPermission = nowExact
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(deepLinkIntent) {
        val intent = deepLinkIntent ?: return@LaunchedEffect
        val medId = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_MEDICINE_ID, -1L)
        if (medId > 0L) {
            val timeStr = intent.getStringExtra(NotificationHelper.EXTRA_OPEN_TIME_STR) ?: ""
            val autoSpeak = intent.getBooleanExtra(NotificationHelper.EXTRA_AUTO_SPEAK, false)
            viewModel.openReminderFromIntent(medId, timeStr, autoSpeak)
        }
        onConsumeDeepLink()
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    LaunchedEffect(state.ttsState.errorMessage) {
        val err = state.ttsState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.clearTtsError()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MedCueRoutes.ROUTE_TODAY
    val showTopLevelNav = currentRoute in setOf(
        MedCueRoutes.ROUTE_TODAY,
        MedCueRoutes.ROUTE_CABINET,
        MedCueRoutes.ROUTE_HISTORY,
    )

    val navigateTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(MedCueRoutes.ROUTE_TODAY) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val requestNotificationPerm: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val openExactAlarmSettings: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showTopLevelNav) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("${MedCueRoutes.ROUTE_ADD_EDIT}?id=-1") },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.action_add_medicine)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_medicine"),
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(
                        header = {
                            FloatingActionButton(
                                onClick = { navController.navigate("${MedCueRoutes.ROUTE_ADD_EDIT}?id=-1") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .testTag("rail_add_medicine"),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_medicine))
                            }
                        },
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}"),
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = MedCueRoutes.ROUTE_TODAY,
                    ) {
                        composable(MedCueRoutes.ROUTE_TODAY) {
                            TodayScreen(
                                state = state,
                                hasNotificationPermission = hasNotificationPermission,
                                hasExactAlarmPermission = hasExactAlarmPermission,
                                onRequestNotificationPermission = requestNotificationPerm,
                                onOpenExactAlarmSettings = openExactAlarmSettings,
                                onAddMedicineClick = { navController.navigate("${MedCueRoutes.ROUTE_ADD_EDIT}?id=-1") },
                                onOpenSettingsClick = { navController.navigate(MedCueRoutes.ROUTE_SETTINGS) },
                                onReadScheduleClick = { viewModel.speakTodaySummary() },
                                onStopVoiceClick = { viewModel.stopSpeaking() },
                                onTakeDose = { med, time -> viewModel.markDoseStatus(med, time, DoseStatus.TAKEN) },
                                onSkipDose = { med, time -> viewModel.markDoseStatus(med, time, DoseStatus.SKIPPED) },
                                onSnoozeDose = { med, time -> viewModel.markDoseStatus(med, time, DoseStatus.SNOOZED) },
                                onUndoDose = { medId, time -> viewModel.undoDoseStatus(medId, time) },
                                onSpeakDose = { med, time -> viewModel.speakDose(med, time) },
                            )
                        }

                        composable(MedCueRoutes.ROUTE_CABINET) {
                            CabinetScreen(
                                state = state,
                                onAddMedicineClick = { navController.navigate("${MedCueRoutes.ROUTE_ADD_EDIT}?id=-1") },
                                onEditMedicineClick = { id ->
                                    navController.navigate("${MedCueRoutes.ROUTE_ADD_EDIT}?id=$id")
                                },
                                onToggleActive = { med -> viewModel.toggleMedicineActive(med) },
                                onDeleteMedicine = { med -> viewModel.deleteMedicine(med) },
                                onSpeakMedicine = { med -> viewModel.speakDose(med, null) },
                                onStopVoice = { viewModel.stopSpeaking() },
                                onTestNotification = { med -> viewModel.triggerTestNotification(med) },
                            )
                        }

                        composable(MedCueRoutes.ROUTE_HISTORY) {
                            HistoryScreen(state = state)
                        }

                        composable(
                            route = "${MedCueRoutes.ROUTE_ADD_EDIT}?id={id}",
                            arguments = listOf(
                                navArgument("id") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                },
                            ),
                        ) { entry ->
                            val argId = entry.arguments?.getLong("id")?.takeIf { it > 0L }
                            AddEditMedicineScreen(
                                medicineId = argId,
                                loadMedicine = { id -> viewModel.getMedicineById(id) },
                                isSpeakingPreview = state.ttsState.isSpeaking &&
                                    state.ttsState.activeKey == "draft_preview",
                                onPreviewSpeech = { name, dosage, quantity, instructions, firstTime ->
                                    viewModel.previewDraftSpeech(name, dosage, quantity, instructions, firstTime)
                                },
                                onStopSpeech = { viewModel.stopSpeaking() },
                                onSave = { existingId, name, dosage, quantity, form, instructions, colorTag, times, freq, mask, tts ->
                                    viewModel.saveMedicine(
                                        existingId = existingId,
                                        name = name,
                                        dosage = dosage,
                                        quantity = quantity,
                                        form = form,
                                        instructions = instructions,
                                        colorTag = colorTag,
                                        times = times,
                                        frequencyType = freq,
                                        daysMask = mask,
                                        ttsEnabled = tts,
                                        onSaved = { navController.popBackStack() },
                                    )
                                },
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable(MedCueRoutes.ROUTE_SETTINGS) {
                            SettingsScreen(
                                state = state,
                                hasNotificationPermission = hasNotificationPermission,
                                hasExactAlarmPermission = hasExactAlarmPermission,
                                onRequestNotificationPermission = requestNotificationPerm,
                                onOpenExactAlarmSettings = openExactAlarmSettings,
                                onSetAutoSpeak = { viewModel.setAutoSpeakReminders(it) },
                                onSetIncludeInstructions = { viewModel.setIncludeInstructionsInSpeech(it) },
                                onSetSpeechRate = { viewModel.setSpeechRate(it) },
                                onSetSpeechPitch = { viewModel.setSpeechPitch(it) },
                                onSetSnoozeMinutes = { viewModel.setSnoozeMinutes(it) },
                                onTestVoice = { viewModel.testVoiceFromSettings() },
                                onStopVoice = { viewModel.stopSpeaking() },
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    state.activeReminderAlert?.let { alert ->
        val alertKey = "med_${alert.medicine.id}_${alert.scheduleTime.toStorageString()}"
        ReminderAlertDialog(
            alert = alert,
            isSpeaking = state.ttsState.isSpeaking && state.ttsState.activeKey == alertKey,
            onTake = { viewModel.markDoseStatus(alert.medicine, alert.scheduleTime, DoseStatus.TAKEN) },
            onSnooze = { viewModel.markDoseStatus(alert.medicine, alert.scheduleTime, DoseStatus.SNOOZED) },
            onSpeak = { viewModel.speakDose(alert.medicine, alert.scheduleTime) },
            onDismiss = { viewModel.dismissReminderAlert() },
        )
    }
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val TopLevelTabs = listOf(
    TopLevelTab(
        route = MedCueRoutes.ROUTE_TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Rounded.Today,
        unselectedIcon = Icons.Outlined.Today,
    ),
    TopLevelTab(
        route = MedCueRoutes.ROUTE_CABINET,
        labelRes = R.string.nav_cabinet,
        selectedIcon = Icons.Rounded.Medication,
        unselectedIcon = Icons.Outlined.Medication,
    ),
    TopLevelTab(
        route = MedCueRoutes.ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
    ),
)

private fun checkNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
