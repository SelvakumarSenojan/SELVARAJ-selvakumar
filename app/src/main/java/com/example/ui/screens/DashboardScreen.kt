package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.RobotMode
import com.example.ui.components.ConnectionCard
import com.example.ui.theme.*
import com.example.viewmodel.RobotUiState

@Composable
fun DashboardScreen(
    state: RobotUiState,
    onRequestModeChange: (RobotMode) -> Unit,
    onConfirmEnterAuto: () -> Unit,
    onDismissAutoDialog: () -> Unit,
    onEmergencyStop: () -> Unit,
    onResetEmergencyStop: () -> Unit,
    onTestDevice: (String) -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToAuto: () -> Unit,
    onNavigateToSensors: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI COMPANION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Smart Onion Garden Maintainer Robot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )
                        Text(
                            text = "“See the Weed. Protect the Onion. Work Smart.”",
                            fontSize = 12.sp,
                            color = TextMutedSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Mode Badge
                    val isAuto = state.currentMode == RobotMode.AUTO
                    Surface(
                        color = if (isAuto) AutoModeBlue.copy(alpha = 0.2f) else CharcoalSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAuto) AutoModeBlue else CharcoalBorder)
                    ) {
                        Text(
                            text = state.currentMode.name,
                            color = if (isAuto) Color(0xFF60A5FA) else TextWhitePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CharcoalBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Quick Strip (Battery & Safety)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery Voltage
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = ForestGreenLight, modifier = Modifier.size(18.dp))
                        Column {
                            Text(text = "BATTERY", fontSize = 10.sp, color = TextMutedSecondary)
                            Text(
                                text = if (state.esp1Status.batteryVoltage != null) "${"%.1f".format(state.esp1Status.batteryVoltage)}V" else "— Unavailable",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                        }
                    }

                    // Ultrasonic Obstacle Clearance
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = if (state.esp1Status.obstacleDetected) StatusErrorRed else ForestGreenLight, modifier = Modifier.size(18.dp))
                        Column {
                            Text(text = "OBSTACLE CLEARANCE", fontSize = 10.sp, color = TextMutedSecondary)
                            Text(
                                text = if (state.esp1Info.state == ConnectionState.ONLINE) "${state.esp1Status.frontDistanceCm.toInt()} cm" else "—",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.esp1Status.obstacleDetected) StatusErrorRed else TextWhitePrimary
                            )
                        }
                    }

                    // Cutter State
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = if (state.esp1Status.cutterActive) StatusErrorRed else TextMutedSecondary, modifier = Modifier.size(18.dp))
                        Column {
                            Text(text = "CUTTER", fontSize = 10.sp, color = TextMutedSecondary)
                            Text(
                                text = if (state.esp1Status.cutterActive) "ACTIVE" else "OFF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.esp1Status.cutterActive) StatusErrorRed else TextWhitePrimary
                            )
                        }
                    }
                }
            }
        }

        // Emergency Stop & Fault Alerts
        if (state.emergencyStopActive) {
            Surface(
                color = StatusErrorRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, StatusErrorRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusErrorRed)
                        Column {
                            Text(text = "EMERGENCY STOP ACTIVE", fontWeight = FontWeight.Bold, color = StatusErrorRed, fontSize = 14.sp)
                            Text(text = "All drive motors, arms, pumps and cutters are locked", color = TextWhitePrimary, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = onResetEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
                        modifier = Modifier.testTag("reset_estop_button")
                    ) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        // Active Obstacle Alert
        if (state.esp1Status.obstacleDetected) {
            Surface(
                color = StatusWarningAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarningAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = StatusWarningAmber)
                    Text(
                        text = "SAFETY WARNING: Front obstacle detected (${state.esp1Status.frontDistanceCm.toInt()} cm). Forward drive locked.",
                        color = TextWhitePrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // MODE CONTROLS: Large Manual Mode & Auto Mode Buttons
        Text(
            text = "ROBOT OPERATING MODE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MANUAL MODE BUTTON
            Button(
                onClick = { onRequestModeChange(RobotMode.MANUAL) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("mode_manual_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.currentMode == RobotMode.MANUAL) ForestGreenPrimary else CharcoalSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.currentMode == RobotMode.MANUAL) ForestGreenLight else CharcoalBorder
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Gamepad, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("MANUAL MODE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = if (state.currentMode == RobotMode.MANUAL) "Currently Active" else "Direct operator controls",
                        fontSize = 11.sp,
                        color = if (state.currentMode == RobotMode.MANUAL) Color(0xFFDCFCE7) else TextMutedSecondary
                    )
                }
            }

            // AUTO MODE BUTTON
            Button(
                onClick = { onRequestModeChange(RobotMode.AUTO) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("mode_auto_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.currentMode == RobotMode.AUTO) AutoModeBlue else CharcoalSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.currentMode == RobotMode.AUTO) Color(0xFF60A5FA) else CharcoalBorder
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("AUTO MODE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = if (state.currentMode == RobotMode.AUTO) "3 Branches Running" else "AI & Scheduled test",
                        fontSize = 11.sp,
                        color = if (state.currentMode == RobotMode.AUTO) Color(0xFFDBEAFE) else TextMutedSecondary
                    )
                }
            }
        }

        // Hardware Wi-Fi Devices Connection Cards
        Text(
            text = "HARDWARE CONNECTIONS (LOCAL WI-FI)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        ConnectionCard(
            title = "ESP32 #1 — Main Controller",
            ip = state.esp1Info.ip,
            port = state.esp1Info.port,
            state = state.esp1Info.state,
            pingMs = state.esp1Info.pingMs,
            lastError = state.esp1Info.lastError,
            icon = Icons.Default.DirectionsCar,
            onTestClick = { onTestDevice("ESP1") }
        )

        ConnectionCard(
            title = "ESP32 #2 — Sensor & Pumps",
            ip = state.esp2Info.ip,
            port = state.esp2Info.port,
            state = state.esp2Info.state,
            pingMs = state.esp2Info.pingMs,
            lastError = state.esp2Info.lastError,
            icon = Icons.Default.WaterDrop,
            onTestClick = { onTestDevice("ESP2") }
        )

        ConnectionCard(
            title = "ESP32-CAM — Vision & AI Stream",
            ip = state.camInfo.ip,
            port = state.camInfo.port,
            state = state.camInfo.state,
            pingMs = state.camInfo.pingMs,
            lastError = state.camInfo.lastError,
            icon = Icons.Default.CameraAlt,
            onTestClick = { onTestDevice("CAM") }
        )

        // E-STOP Universal Trigger Button
        Button(
            onClick = onEmergencyStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("estop_main_button"),
            colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color.White)
                Text("EMERGENCY STOP (ALL ACTUATORS)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Deliberate Confirmation Dialog for AUTO MODE
    if (state.showAutoModeConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissAutoDialog,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarningAmber)
                    Text("Confirm AUTO MODE Activation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Activating AUTO MODE initiates autonomous search, three-joint weed arm operations, disease spraying, and scheduled 5-minute soil testing.",
                        fontSize = 13.sp,
                        color = TextWhitePrimary
                    )
                    Text(
                        text = "• Obstacle safety override remains active.\n• Cutter timeout: 7 seconds max.\n• Sprayer timeout: 10 seconds max.\n• Water pump timeout: 5 seconds max.\n• Manual drive controls will be locked.",
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmEnterAuto,
                    colors = ButtonDefaults.buttonColors(containerColor = AutoModeBlue),
                    modifier = Modifier.testTag("confirm_auto_mode_btn")
                ) {
                    Text("Activate AUTO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissAutoDialog) {
                    Text("Cancel")
                }
            },
            containerColor = CharcoalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
