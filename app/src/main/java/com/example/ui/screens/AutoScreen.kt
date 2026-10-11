package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.model.RobotMode
import com.example.model.SoilTestState
import com.example.model.WeedClass
import com.example.ui.components.CameraFeedView
import com.example.ui.components.TankLevelBar
import com.example.ui.theme.*
import com.example.viewmodel.RobotUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutoScreen(
    state: RobotUiState,
    onCaptureFrame: () -> Unit,
    onRequestManualMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isAutoActive = state.currentMode == RobotMode.AUTO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Auto Mode Header Bar
        Surface(
            color = if (isAutoActive) AutoModeBlue.copy(alpha = 0.2f) else CharcoalSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoActive) AutoModeBlue else CharcoalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAutoActive) Color(0xFF60A5FA) else StatusOfflineGray)
                    )
                    Column {
                        Text(
                            text = if (isAutoActive) "AUTO MODE ENGAGED" else "AUTO MODE STANDBY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isAutoActive) Color(0xFF93C5FD) else TextWhitePrimary
                        )
                        Text(
                            text = if (isAutoActive) "Autonomous search, weed classification & disease monitoring active" else "Switch to AUTO MODE on Dashboard to start automatic routines",
                            fontSize = 11.sp,
                            color = TextMutedSecondary
                        )
                    }
                }

                if (isAutoActive) {
                    OutlinedButton(
                        onClick = onRequestManualMode,
                        modifier = Modifier.testTag("auto_abort_to_manual_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Halt to Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // BRANCH 1: AI WEED DETECTION
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = ForestGreenLight)
                        Text(text = "BRANCH 1 — AI WEED DETECTION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                    }

                    Surface(
                        color = if (state.isArmOperating) WeedAlertRed.copy(alpha = 0.2f) else ForestGreenContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.isArmOperating) "ARM ACTIVE" else if (isAutoActive) "SEARCHING" else "IDLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isArmOperating) WeedAlertRed else ForestGreenLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Camera Feed with detection bounding box
                CameraFeedView(
                    frame = state.latestCameraFrame,
                    connectionState = state.camInfo.state,
                    errorMessage = state.camInfo.lastError,
                    detection = state.latestWeedDetection,
                    onCaptureClick = onCaptureFrame
                )

                // Current AI Inference Result Box
                val detection = state.latestWeedDetection
                Surface(
                    color = CharcoalSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LATEST CLASSIFICATION:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedSecondary
                            )
                            if (detection != null) {
                                val badgeColor = when (detection.classification) {
                                    WeedClass.WEED -> WeedAlertRed
                                    WeedClass.ONION -> OnionSafeGreen
                                    WeedClass.UNKNOWN -> UnknownGray
                                }
                                Text(
                                    text = "${detection.classification.name} (${(detection.confidence * 100).toInt()}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = badgeColor
                                )
                            } else {
                                Text(text = "Awaiting frames", fontSize = 12.sp, color = TextMutedSecondary)
                            }
                        }

                        if (detection != null) {
                            Text(
                                text = detection.featureSummary,
                                fontSize = 12.sp,
                                color = TextWhitePrimary
                            )

                            if (detection.classification == WeedClass.ONION) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = OnionSafeGreen, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "PROTECT ONION: Cutting sequence permanently locked for onion plants",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnionSafeGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Cutter Countdown (Max 7s rule)
                if (state.cutterCountdownSec > 0) {
                    Surface(
                        color = WeedAlertRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WeedAlertRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, tint = WeedAlertRed)
                                Text(text = "Cutter Running (Max 7s safety limit)", fontWeight = FontWeight.Bold, color = TextWhitePrimary, fontSize = 12.sp)
                            }
                            Text(
                                text = "${state.cutterCountdownSec}s",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WeedAlertRed
                            )
                        }
                    }
                }

                // Recent Weed Actions Log
                if (state.weedHistory.isNotEmpty()) {
                    Text(text = "Recent Weed Actions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.weedHistory.take(3).forEach { item ->
                            Surface(
                                color = CharcoalSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.actionSummary, fontSize = 11.sp, color = TextWhitePrimary)
                                        Text(text = formatTime(item.timestamp), fontSize = 10.sp, color = TextMutedSecondary)
                                    }
                                    Text(
                                        text = "${(item.confidence * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.classification == WeedClass.WEED) WeedAlertRed else OnionSafeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BRANCH 2: ONION DISEASE MONITORING
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = AutoModeBlue)
                        Text(text = "BRANCH 2 — DISEASE MONITORING", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                    }
                    Surface(
                        color = if (state.sprayerCountdownSec > 0) StatusWarningAmber.copy(alpha = 0.2f) else CharcoalSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.sprayerCountdownSec > 0) "SPRAYING" else if (isAutoActive) "MONITORING" else "IDLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.sprayerCountdownSec > 0) StatusWarningAmber else TextMutedSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // TCS3200 Colour Reading & Diagnostics
                val tcs = state.esp2Status.tcs3200
                Surface(
                    color = CharcoalSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "TCS3200 COLOUR SENSOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSecondary)
                            if (tcs != null) {
                                Text(text = "R: ${tcs.r} | G: ${tcs.g} | B: ${tcs.b} | Clear: ${tcs.clear}", fontSize = 12.sp, color = TextWhitePrimary)
                                Text(
                                    text = if (tcs.healthy) "Calibrated foliage: HEALTHY GREEN" else "SUSPECTED DISEASE: ${tcs.suspectedDisease ?: "Colour Anomaly"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tcs.healthy) ForestGreenLight else StatusWarningAmber
                                )
                            } else {
                                Text(text = "No TCS3200 reading received (ESP32 #2)", fontSize = 12.sp, color = TextMutedSecondary)
                            }
                        }

                        if (tcs != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(tcs.r.coerceIn(0, 255), tcs.g.coerceIn(0, 255), tcs.b.coerceIn(0, 255)))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }

                // Chemical Tank Level Bar
                TankLevelBar(
                    title = "Chemical Treatment Tank",
                    levelPct = state.esp2Status.chemicalTankLevelPct,
                    minThresholdPct = state.settings.chemicalTankMinPct,
                    barColor = AutoModeBlue,
                    icon = Icons.Default.Science
                )

                // Mandatory Disease Disclaimer Note
                Text(
                    text = "NOTE: Results are labelled as suspected conditions requiring confirmation; colour readings alone cannot reliably diagnose every onion disease.",
                    fontSize = 11.sp,
                    color = TextMutedTertiary
                )

                // Sprayer Countdown (Max 10s rule)
                if (state.sprayerCountdownSec > 0) {
                    Surface(
                        color = AutoModeBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AutoModeBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Shower, contentDescription = null, tint = AutoModeBlue)
                                Text(text = "Targeted Spraying Active (Max 10s window)", fontWeight = FontWeight.Bold, color = TextWhitePrimary, fontSize = 12.sp)
                            }
                            Text(text = "${state.sprayerCountdownSec}s", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF60A5FA))
                        }
                    }
                }
            }
        }

        // =========================================================================
        // BRANCH 3: SOIL TESTING (MANDATORY STRICT 5-MINUTE TIMING & VISIBILITY RULE)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenDark)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ForestGreenLight)
                        Text(text = "BRANCH 3 — SOIL TESTING (5-MIN INTERVAL)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                    }

                    // Active test state badge
                    Surface(
                        color = when (state.soilTestState) {
                            SoilTestState.SERVING_TO_90, SoilTestState.READING_SENSORS, SoilTestState.PUMPING -> ForestGreenPrimary
                            SoilTestState.RETURNING_HOME -> StatusWarningAmber.copy(alpha = 0.2f)
                            else -> CharcoalSurfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (state.soilTestState) {
                                SoilTestState.COUNTDOWN -> "SCHEDULED"
                                SoilTestState.SERVING_TO_90 -> "SERVO -> 90°"
                                SoilTestState.READING_SENSORS -> "SAMPLING"
                                SoilTestState.PUMPING -> "PUMPING (5s)"
                                SoilTestState.RETURNING_HOME -> "SERVO -> HOME"
                                else -> "IDLE"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // 5-Minute Countdown Timer Widget
                Surface(
                    color = CharcoalSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "NEXT SCHEDULED SOIL TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSecondary)
                            Text(
                                text = if (isAutoActive) {
                                    val mins = state.soilTestCountdownSecondsRemaining / 60
                                    val secs = state.soilTestCountdownSecondsRemaining % 60
                                    String.format(Locale.US, "%02d:%02d remaining", mins, secs)
                                } else {
                                    "Paused (Requires AUTO MODE)"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoActive) ForestGreenLight else TextMutedSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isAutoActive) ForestGreenLight else TextMutedSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Servo State Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Soil Testing Servo Mechanical Position:", fontSize = 12.sp, color = TextMutedSecondary)
                    Text(
                        text = if (state.soilTestState == SoilTestState.SERVING_TO_90 || state.soilTestState == SoilTestState.READING_SENSORS || state.soilTestState == SoilTestState.PUMPING) "90° (DEPLOYED)" else "0° (HOME)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.soilTestState == SoilTestState.SERVING_TO_90 || state.soilTestState == SoilTestState.READING_SENSORS) ForestGreenLight else TextWhitePrimary
                    )
                }

                // MANDATORY TIMING RULE ENFORCEMENT:
                // Value cards (Moisture, Temp, Humidity) are enabled ONLY during active soil test!
                if (state.isSoilTestCardsVisible) {
                    Text(
                        text = "ACTIVE SOIL TEST TELEMETRY (LIVE READINGS):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Soil Moisture Card
                        Surface(
                            color = SoilMoistureBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoilMoistureBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "MOISTURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoilMoistureBlue)
                                Text(
                                    text = if (state.activeSoilTestMoisturePct != null) "${state.activeSoilTestMoisturePct.toInt()}%" else "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = "Threshold: ${state.settings.soilDryThresholdPct.toInt()}%",
                                    fontSize = 9.sp,
                                    color = TextMutedSecondary
                                )
                            }
                        }

                        // Temperature Card
                        Surface(
                            color = TemperatureOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TemperatureOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "TEMP (DHT11)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TemperatureOrange)
                                Text(
                                    text = if (state.activeSoilTestTempC != null) "${"%.1f".format(state.activeSoilTestTempC)}°C" else "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(text = "Ambient", fontSize = 9.sp, color = TextMutedSecondary)
                            }
                        }

                        // Humidity Card
                        Surface(
                            color = HumidityCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HumidityCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "HUMIDITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HumidityCyan)
                                Text(
                                    text = if (state.activeSoilTestHumidityPct != null) "${state.activeSoilTestHumidityPct.toInt()}%" else "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(text = "Relative", fontSize = 9.sp, color = TextMutedSecondary)
                            }
                        }
                    }
                } else {
                    // Explanatory standby box when test is not active
                    Surface(
                        color = CharcoalSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMutedSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Soil moisture, temperature and humidity sensors are deployed only during the scheduled 5-minute test. Live sensor panels remain hidden outside active testing.",
                                fontSize = 11.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }
                }

                // Water Tank Level Bar
                TankLevelBar(
                    title = "Water Tank Level",
                    levelPct = state.esp2Status.waterTankLevelPct,
                    minThresholdPct = state.settings.waterTankMinPct,
                    barColor = SoilMoistureBlue,
                    icon = Icons.Default.Water
                )

                // Water Pump Active Window (Max 5s rule)
                if (state.waterPumpCountdownSec > 0) {
                    Surface(
                        color = SoilMoistureBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoilMoistureBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = SoilMoistureBlue)
                                Text(text = "Water Pump Running (Max 5s safety limit)", fontWeight = FontWeight.Bold, color = TextWhitePrimary, fontSize = 12.sp)
                            }
                            Text(text = "${state.waterPumpCountdownSec}s", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SoilMoistureBlue)
                        }
                    }
                }

                // Recent Soil Test History
                if (state.soilTestHistory.isNotEmpty()) {
                    Text(text = "Recent Scheduled Soil Tests:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.soilTestHistory.take(2).forEach { record ->
                            Surface(
                                color = CharcoalSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Moisture: ${record.soilMoisturePct?.toInt() ?: 0}% | Temp: ${record.temperatureC?.toInt() ?: 0}°C | Humidity: ${record.humidityPct?.toInt() ?: 0}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextWhitePrimary
                                        )
                                        Text(text = formatTime(record.timestamp), fontSize = 10.sp, color = TextMutedSecondary)
                                    }
                                    Text(text = record.summary, fontSize = 11.sp, color = TextMutedSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
