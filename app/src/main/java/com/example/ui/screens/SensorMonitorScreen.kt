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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.RobotMode
import com.example.model.SoilTestState
import com.example.ui.components.TankLevelBar
import com.example.ui.theme.*
import com.example.viewmodel.RobotUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SensorMonitorScreen(
    state: RobotUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val esp2 = state.esp2Status
    val isEsp2Online = state.esp2Info.state == ConnectionState.ONLINE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "SENSOR MONITOR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhitePrimary
                )
                Text(
                    text = "Telemetry direct from ESP32 #2 Controller",
                    fontSize = 12.sp,
                    color = TextMutedSecondary
                )
            }

            Surface(
                color = if (isEsp2Online) StatusOnlineGreen.copy(alpha = 0.2f) else StatusOfflineGray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEsp2Online) StatusOnlineGreen else StatusOfflineGray)
            ) {
                Text(
                    text = if (isEsp2Online) "LIVE (ESP32 #2)" else "OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEsp2Online) StatusOnlineGreen else StatusOfflineGray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // =========================================================================
        // ACTIVE SOIL TEST PANEL (MANDATORY TIMING & VISIBILITY RULE)
        // Only active during the scheduled AUTO test; outside of that, shows standby banner.
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenDark)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = ForestGreenLight)
                        Text(text = "Scheduled Soil Test Panel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                    }

                    Surface(
                        color = if (state.isSoilTestCardsVisible) ForestGreenPrimary else CharcoalSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (state.isSoilTestCardsVisible) "ACTIVE TEST" else "STANDBY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (state.isSoilTestCardsVisible) {
                    // LIVE READINGS ONLY DURING ACTIVE 5-MIN AUTO TEST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SoilMoistureBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoilMoistureBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "SOIL MOISTURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoilMoistureBlue)
                                Text(
                                    text = if (state.activeSoilTestMoisturePct != null) "${state.activeSoilTestMoisturePct.toInt()}%" else "Unavailable",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(text = "Capacitive sensor", fontSize = 9.sp, color = TextMutedSecondary)
                            }
                        }

                        Surface(
                            color = TemperatureOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TemperatureOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "TEMPERATURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TemperatureOrange)
                                Text(
                                    text = if (state.activeSoilTestTempC != null) "${"%.1f".format(state.activeSoilTestTempC)}°C" else "Unavailable",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(text = "DHT11", fontSize = 9.sp, color = TextMutedSecondary)
                            }
                        }

                        Surface(
                            color = HumidityCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HumidityCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "HUMIDITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HumidityCyan)
                                Text(
                                    text = if (state.activeSoilTestHumidityPct != null) "${state.activeSoilTestHumidityPct.toInt()}%" else "Unavailable",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(text = "DHT11", fontSize = 9.sp, color = TextMutedSecondary)
                            }
                        }
                    }
                } else {
                    // STANDBY NOTE
                    Surface(
                        color = CharcoalSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Panel Inactive (Only active during 5-minute Auto Soil Test)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhitePrimary
                            )
                            Text(
                                text = if (state.currentMode == RobotMode.AUTO) {
                                    val mins = state.soilTestCountdownSecondsRemaining / 60
                                    val secs = state.soilTestCountdownSecondsRemaining % 60
                                    "Next sample cycle in ${String.format(Locale.US, "%02d:%02d", mins, secs)} (Servo will deploy to 90°)."
                                } else {
                                    "Scheduled soil tests run exclusively when AUTO MODE is active."
                                },
                                fontSize = 11.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }
                }
            }
        }

        // Tank Levels Section
        Text(
            text = "LIQUID RESERVOIRS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        TankLevelBar(
            title = "Water Tank Level",
            levelPct = esp2.waterTankLevelPct,
            minThresholdPct = state.settings.waterTankMinPct,
            barColor = SoilMoistureBlue,
            icon = Icons.Default.Water
        )

        TankLevelBar(
            title = "Chemical Treatment Tank",
            levelPct = esp2.chemicalTankLevelPct,
            minThresholdPct = state.settings.chemicalTankMinPct,
            barColor = AutoModeBlue,
            icon = Icons.Default.Science
        )

        // TCS3200 Colour Sensor Telemetry
        Text(
            text = "TCS3200 ONION FOLIAGE COLOUR SENSOR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val tcs = esp2.tcs3200
                if (tcs != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "RAW RGB FREQUENCIES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedSecondary
                            )
                            Text(
                                text = "R: ${tcs.r}   G: ${tcs.g}   B: ${tcs.b}   Clear: ${tcs.clear}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(tcs.r.coerceIn(0, 255), tcs.g.coerceIn(0, 255), tcs.b.coerceIn(0, 255)))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        )
                    }

                    HorizontalDivider(color = CharcoalBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Health Assessment:", fontSize = 12.sp, color = TextMutedSecondary)
                        Text(
                            text = if (tcs.healthy) "NORMAL GREEN FOLIAGE" else "SUSPECTED DISEASE: ${tcs.suspectedDisease ?: "Foliage Anomaly"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (tcs.healthy) ForestGreenLight else StatusWarningAmber
                        )
                    }
                } else {
                    Text(
                        text = "TCS3200 colour reading unavailable (Awaiting response from ESP32 #2)",
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )
                }

                Text(
                    text = "Requires confirmation: TCS3200 spectral analysis provides preliminary screening only.",
                    fontSize = 10.sp,
                    color = TextMutedTertiary
                )
            }
        }

        // Historical Soil Tests (Separate Historical Log)
        Text(
            text = "HISTORICAL SOIL TEST RECORDS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMutedSecondary
        )

        if (state.soilTestHistory.isEmpty()) {
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No recorded historical soil tests yet. Tests log automatically every 5 minutes in AUTO MODE.",
                    fontSize = 12.sp,
                    color = TextMutedSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.soilTestHistory.forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sampled: ${formatFullDateTime(record.timestamp)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = if (record.pumpActivated) "WATERED (5s)" else "NO WATER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.pumpActivated) SoilMoistureBlue else TextMutedSecondary
                                )
                            }
                            Text(
                                text = "Moisture: ${record.soilMoisturePct?.toInt() ?: 0}% | Temp: ${record.temperatureC?.toInt() ?: 0}°C | Humidity: ${record.humidityPct?.toInt() ?: 0}%",
                                fontSize = 12.sp,
                                color = TextMutedSecondary
                            )
                            Text(
                                text = record.summary,
                                fontSize = 11.sp,
                                color = TextWhitePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFullDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
