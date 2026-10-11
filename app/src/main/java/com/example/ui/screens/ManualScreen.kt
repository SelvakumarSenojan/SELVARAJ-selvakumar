package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.MotorCommand
import com.example.model.RobotMode
import com.example.model.SpeedLevel
import com.example.ui.components.CameraFeedView
import com.example.ui.theme.*
import com.example.viewmodel.RobotUiState

@Composable
fun ManualScreen(
    state: RobotUiState,
    onMoveStart: (MotorCommand) -> Unit,
    onMoveRelease: () -> Unit,
    onSetSpeed: (SpeedLevel) -> Unit,
    onPanTilt: (String) -> Unit,
    onToggleRelay: () -> Unit,
    onCaptureFrame: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isAutoLocked = state.currentMode == RobotMode.AUTO
    val isEsp1Offline = state.esp1Info.state != ConnectionState.ONLINE
    val isObstacleBlocked = state.esp1Status.obstacleDetected || state.esp1Status.frontDistanceCm <= state.settings.obstacleSafetyThresholdCm

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Auto Mode Lock Banner
        if (isAutoLocked) {
            Surface(
                color = AutoModeBlue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AutoModeBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF60A5FA))
                    Column {
                        Text(
                            text = "MANUAL CONTROLS LOCKED (AUTO MODE ACTIVE)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Switch to MANUAL MODE on the Dashboard to take direct control of motors and servos.",
                            fontSize = 11.sp,
                            color = TextWhitePrimary
                        )
                    }
                }
            }
        }

        // Real ESP32-CAM stream at the top
        CameraFeedView(
            frame = state.latestCameraFrame,
            connectionState = state.camInfo.state,
            errorMessage = state.camInfo.lastError,
            detection = null, // Manual mode does not run auto weed overlays
            onCaptureClick = onCaptureFrame
        )

        // Front Ultrasonic Distance & Safety Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = if (isObstacleBlocked) StatusErrorRed else ForestGreenLight)
                        Text(text = "Front Obstacle Safety", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextWhitePrimary)
                    }
                    Text(
                        text = if (state.esp1Info.state == ConnectionState.ONLINE) "${state.esp1Status.frontDistanceCm.toInt()} cm" else "Offline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isObstacleBlocked) StatusErrorRed else ForestGreenLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val clearancePct = (state.esp1Status.frontDistanceCm / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { clearancePct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isObstacleBlocked) StatusErrorRed else ForestGreenLight,
                    trackColor = CharcoalSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Safety halt limit: ${state.settings.obstacleSafetyThresholdCm.toInt()} cm", fontSize = 10.sp, color = TextMutedSecondary)
                    if (isObstacleBlocked) {
                        Text(text = "FORWARD OVERRIDE BLOCKED", fontSize = 10.sp, color = StatusErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Speed Controls (SLOW, MEDIUM, FAST)
        Text(
            text = "MOTOR SPEED",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedLevel.values().forEach { speed ->
                val isSelected = state.currentSpeed == speed
                Button(
                    onClick = { if (!isAutoLocked) onSetSpeed(speed) },
                    enabled = !isAutoLocked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("speed_${speed.name.lowercase()}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) ForestGreenPrimary else CharcoalSurface,
                        disabledContainerColor = CharcoalSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ForestGreenLight else CharcoalBorder
                    )
                ) {
                    Text(
                        text = "${speed.displayName} (${speed.speedPercent}%)",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextMutedSecondary
                    )
                }
            }
        }

        // 4 Movement Controls (Hold-to-Move approach)
        Text(
            text = "DRIVE CONTROLS (HOLD TO MOVE)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FORWARD
                HoldToMoveButton(
                    command = MotorCommand.FORWARD,
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "FORWARD",
                    isActive = state.activeMoveCommand == MotorCommand.FORWARD,
                    isDisabled = isAutoLocked || isEsp1Offline || isObstacleBlocked,
                    onPress = { onMoveStart(MotorCommand.FORWARD) },
                    onRelease = onMoveRelease,
                    modifier = Modifier.testTag("move_forward_btn")
                )

                // LEFT, STOP, RIGHT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldToMoveButton(
                        command = MotorCommand.LEFT,
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        label = "LEFT",
                        isActive = state.activeMoveCommand == MotorCommand.LEFT,
                        isDisabled = isAutoLocked || isEsp1Offline,
                        onPress = { onMoveStart(MotorCommand.LEFT) },
                        onRelease = onMoveRelease,
                        modifier = Modifier.testTag("move_left_btn")
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center STOP button
                    IconButton(
                        onClick = onMoveRelease,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (state.activeMoveCommand == null || state.activeMoveCommand == MotorCommand.STOP) CharcoalSurfaceVariant else StatusErrorRed.copy(alpha = 0.3f))
                            .testTag("move_stop_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop motors",
                            tint = if (state.activeMoveCommand != null && state.activeMoveCommand != MotorCommand.STOP) StatusErrorRed else TextMutedSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    HoldToMoveButton(
                        command = MotorCommand.RIGHT,
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        label = "RIGHT",
                        isActive = state.activeMoveCommand == MotorCommand.RIGHT,
                        isDisabled = isAutoLocked || isEsp1Offline,
                        onPress = { onMoveStart(MotorCommand.RIGHT) },
                        onRelease = onMoveRelease,
                        modifier = Modifier.testTag("move_right_btn")
                    )
                }

                // REVERSE
                HoldToMoveButton(
                    command = MotorCommand.REVERSE,
                    icon = Icons.Default.KeyboardArrowDown,
                    label = "REVERSE",
                    isActive = state.activeMoveCommand == MotorCommand.REVERSE,
                    isDisabled = isAutoLocked || isEsp1Offline,
                    onPress = { onMoveStart(MotorCommand.REVERSE) },
                    onRelease = onMoveRelease,
                    modifier = Modifier.testTag("move_reverse_btn")
                )
            }
        }

        // Camera Pan / Tilt Controls
        Text(
            text = "CAMERA PAN / TILT SERVOS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pan: ${state.esp1Status.panAngle}°", fontSize = 12.sp, color = TextMutedSecondary)
                    Text(text = "Tilt: ${state.esp1Status.tiltAngle}°", fontSize = 12.sp, color = TextMutedSecondary)
                }

                // Tilt Up
                OutlinedButton(
                    onClick = { onPanTilt("TILT_UP") },
                    enabled = !isAutoLocked,
                    modifier = Modifier.testTag("tilt_up_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TILT UP", fontSize = 11.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pan Left
                    OutlinedButton(
                        onClick = { onPanTilt("PAN_LEFT") },
                        enabled = !isAutoLocked,
                        modifier = Modifier.testTag("pan_left_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAN LEFT", fontSize = 11.sp)
                    }

                    // Center
                    Button(
                        onClick = { onPanTilt("CENTER") },
                        enabled = !isAutoLocked,
                        modifier = Modifier.testTag("pan_tilt_center_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CENTER", fontSize = 11.sp, color = Color.White)
                    }

                    // Pan Right
                    OutlinedButton(
                        onClick = { onPanTilt("PAN_RIGHT") },
                        enabled = !isAutoLocked,
                        modifier = Modifier.testTag("pan_right_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("PAN RIGHT", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // Tilt Down
                OutlinedButton(
                    onClick = { onPanTilt("TILT_DOWN") },
                    enabled = !isAutoLocked,
                    modifier = Modifier.testTag("tilt_down_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TILT DOWN", fontSize = 11.sp)
                }
            }
        }

        // Manual Auxiliary Relay Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Power, contentDescription = null, tint = ForestGreenLight)
                    Column {
                        Text(text = "Manual Auxiliary Relay", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextWhitePrimary)
                        Text(text = "Auxiliary worklight / power circuit", fontSize = 11.sp, color = TextMutedSecondary)
                    }
                }

                Switch(
                    checked = state.manualRelayState,
                    onCheckedChange = { if (!isAutoLocked) onToggleRelay() },
                    enabled = !isAutoLocked,
                    modifier = Modifier.testTag("manual_relay_switch")
                )
            }
        }

        // Safety notes
        Text(
            text = "SAFETY ENFORCEMENT: Automatic weed cutting, disease spraying, and soil testing are locked in Manual Mode. Front ultrasonic collision prevention is strictly active.",
            fontSize = 11.sp,
            color = TextMutedTertiary
        )
    }
}

@Composable
fun HoldToMoveButton(
    command: MotorCommand,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isDisabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(isDisabled) {
                if (!isDisabled) {
                    detectTapGestures(
                        onPress = {
                            onPress()
                            tryAwaitRelease()
                            onRelease()
                        }
                    )
                }
            },
        color = when {
            isDisabled -> CharcoalSurfaceVariant.copy(alpha = 0.5f)
            isActive -> ForestGreenPrimary
            else -> CharcoalSurfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isDisabled -> CharcoalBorder.copy(alpha = 0.5f)
                isActive -> ForestGreenLight
                else -> CharcoalBorder
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    isDisabled -> TextMutedTertiary
                    isActive -> Color.White
                    else -> TextWhitePrimary
                },
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isDisabled -> TextMutedTertiary
                    isActive -> Color.White
                    else -> TextMutedSecondary
                }
            )
        }
    }
}
