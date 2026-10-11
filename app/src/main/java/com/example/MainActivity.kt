package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RobotMode
import com.example.ui.screens.*
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.OnionRobotTheme
import com.example.ui.theme.TextMutedSecondary
import com.example.viewmodel.RobotViewModel

enum class NavigationScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MANUAL("Manual", Icons.Default.Gamepad),
    AUTO("Auto Mode", Icons.Default.AutoMode),
    SENSORS("Sensors", Icons.Default.Sensors),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnionRobotTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: RobotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle messages
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(message = err, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    // Back handler: return to Dashboard if on subscreen
    if (currentScreen != NavigationScreen.DASHBOARD) {
        BackHandler {
            currentScreen = NavigationScreen.DASHBOARD
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        contentColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        NavigationScreen.values().forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ForestGreenLight,
                                    selectedTextColor = ForestGreenLight,
                                    unselectedIconColor = TextMutedSecondary,
                                    unselectedTextColor = TextMutedSecondary,
                                    indicatorColor = CharcoalBorder
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tablet / Landscape Side Navigation Rail
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        contentColor = Color.White,
                        modifier = Modifier.width(88.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NavigationScreen.values().forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ForestGreenLight,
                                    selectedTextColor = ForestGreenLight,
                                    unselectedIconColor = TextMutedSecondary,
                                    unselectedTextColor = TextMutedSecondary,
                                    indicatorColor = CharcoalBorder
                                ),
                                modifier = Modifier.testTag("nav_rail_${screen.name.lowercase()}")
                            )
                        }
                    }
                }

                // Main Screen Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CharcoalBackground)
                ) {
                    when (currentScreen) {
                        NavigationScreen.DASHBOARD -> {
                            DashboardScreen(
                                state = uiState,
                                onRequestModeChange = { mode -> viewModel.requestModeChange(mode) },
                                onConfirmEnterAuto = { viewModel.confirmEnterAutoMode() },
                                onDismissAutoDialog = { viewModel.dismissAutoModeDialog() },
                                onEmergencyStop = { viewModel.triggerEmergencyStop() },
                                onResetEmergencyStop = { viewModel.resetEmergencyStop() },
                                onTestDevice = { target -> viewModel.testDeviceConnection(target) },
                                onNavigateToManual = { currentScreen = NavigationScreen.MANUAL },
                                onNavigateToAuto = { currentScreen = NavigationScreen.AUTO },
                                onNavigateToSensors = { currentScreen = NavigationScreen.SENSORS },
                                onNavigateToSettings = { currentScreen = NavigationScreen.SETTINGS }
                            )
                        }
                        NavigationScreen.MANUAL -> {
                            ManualScreen(
                                state = uiState,
                                onMoveStart = { cmd -> viewModel.sendMoveCommand(cmd) },
                                onMoveRelease = { viewModel.releaseMoveCommand() },
                                onSetSpeed = { speed -> viewModel.setSpeed(speed) },
                                onPanTilt = { dir -> viewModel.sendPanTilt(dir) },
                                onToggleRelay = { viewModel.toggleManualRelay() },
                                onCaptureFrame = { viewModel.captureStillPhoto() },
                                onEmergencyStop = { viewModel.triggerEmergencyStop() }
                            )
                        }
                        NavigationScreen.AUTO -> {
                            AutoScreen(
                                state = uiState,
                                onCaptureFrame = { viewModel.captureStillPhoto() },
                                onRequestManualMode = { viewModel.requestModeChange(RobotMode.MANUAL) }
                            )
                        }
                        NavigationScreen.SENSORS -> {
                            SensorMonitorScreen(
                                state = uiState
                            )
                        }
                        NavigationScreen.SETTINGS -> {
                            SettingsScreen(
                                state = uiState,
                                onSaveSettings = { newSettings -> viewModel.updateSettings(newSettings) },
                                onTestDevice = { target -> viewModel.testDeviceConnection(target) }
                            )
                        }
                    }
                }
            }
        }
    }
}
