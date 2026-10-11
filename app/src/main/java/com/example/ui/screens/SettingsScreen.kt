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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.FirmwareCodeExamples
import com.example.ui.theme.*
import com.example.viewmodel.RobotUiState

@Composable
fun SettingsScreen(
    state: RobotUiState,
    onSaveSettings: (AppSettings) -> Unit,
    onTestDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var esp1Ip by remember(state.settings.esp1Ip) { mutableStateOf(state.settings.esp1Ip) }
    var esp1Port by remember(state.settings.esp1Port) { mutableStateOf(state.settings.esp1Port.toString()) }

    var esp2Ip by remember(state.settings.esp2Ip) { mutableStateOf(state.settings.esp2Ip) }
    var esp2Port by remember(state.settings.esp2Port) { mutableStateOf(state.settings.esp2Port.toString()) }

    var camIp by remember(state.settings.camIp) { mutableStateOf(state.settings.camIp) }
    var camPort by remember(state.settings.camPort) { mutableStateOf(state.settings.camPort.toString()) }
    var camCapturePath by remember(state.settings.camCapturePath) { mutableStateOf(state.settings.camCapturePath) }
    var camStreamPath by remember(state.settings.camStreamPath) { mutableStateOf(state.settings.camStreamPath) }

    var authToken by remember(state.settings.authToken) { mutableStateOf(state.settings.authToken) }

    var soilDryThreshold by remember(state.settings.soilDryThresholdPct) { mutableStateOf(state.settings.soilDryThresholdPct.toString()) }
    var waterTankMin by remember(state.settings.waterTankMinPct) { mutableStateOf(state.settings.waterTankMinPct.toString()) }
    var chemicalTankMin by remember(state.settings.chemicalTankMinPct) { mutableStateOf(state.settings.chemicalTankMinPct.toString()) }
    var weedConfidence by remember(state.settings.weedConfidenceThreshold) { mutableStateOf((state.settings.weedConfidenceThreshold * 100).toInt().toString()) }
    var obstacleSafetyCm by remember(state.settings.obstacleSafetyThresholdCm) { mutableStateOf(state.settings.obstacleSafetyThresholdCm.toInt().toString()) }

    var refreshInterval by remember(state.settings.refreshIntervalMs) { mutableStateOf(state.settings.refreshIntervalMs.toString()) }
    var httpTimeout by remember(state.settings.httpTimeoutMs) { mutableStateOf(state.settings.httpTimeoutMs.toString()) }

    var showFirmwareDialog by remember { mutableStateOf(false) }

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
                    text = "ROBOT CONFIGURATION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhitePrimary
                )
                Text(
                    text = "Local Wi-Fi endpoints, calibration & safety limits",
                    fontSize = 12.sp,
                    color = TextMutedSecondary
                )
            }

            Button(
                onClick = { showFirmwareDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("open_firmware_docs_btn")
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ESP32 Code", fontSize = 11.sp)
            }
        }

        // ==========================================
        // DEVICE ENDPOINTS (ESP32 #1, #2, CAM)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "DEVICE 1: ESP32 #1 (Drive, Arm, Cutter)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenLight)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = esp1Ip,
                        onValueChange = { esp1Ip = it },
                        label = { Text("IP Address") },
                        modifier = Modifier.weight(2f).testTag("input_esp1_ip"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = esp1Port,
                        onValueChange = { esp1Port = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f).testTag("input_esp1_port"),
                        singleLine = true
                    )
                }
                OutlinedButton(
                    onClick = { onTestDevice("ESP1") },
                    modifier = Modifier.align(Alignment.End).testTag("settings_test_esp1_btn")
                ) {
                    Text("Test ESP32 #1 Connection", fontSize = 12.sp)
                }

                HorizontalDivider(color = CharcoalBorder)

                Text(text = "DEVICE 2: ESP32 #2 (Sensors & Pumps)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AutoModeBlue)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = esp2Ip,
                        onValueChange = { esp2Ip = it },
                        label = { Text("IP Address") },
                        modifier = Modifier.weight(2f).testTag("input_esp2_ip"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = esp2Port,
                        onValueChange = { esp2Port = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f).testTag("input_esp2_port"),
                        singleLine = true
                    )
                }
                OutlinedButton(
                    onClick = { onTestDevice("ESP2") },
                    modifier = Modifier.align(Alignment.End).testTag("settings_test_esp2_btn")
                ) {
                    Text("Test ESP32 #2 Connection", fontSize = 12.sp)
                }

                HorizontalDivider(color = CharcoalBorder)

                Text(text = "DEVICE 3: ESP32-CAM (Vision Stream)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenLight)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = camIp,
                        onValueChange = { camIp = it },
                        label = { Text("IP Address") },
                        modifier = Modifier.weight(2f).testTag("input_cam_ip"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = camPort,
                        onValueChange = { camPort = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f).testTag("input_cam_port"),
                        singleLine = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = camCapturePath,
                        onValueChange = { camCapturePath = it },
                        label = { Text("Capture Path") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = camStreamPath,
                        onValueChange = { camStreamPath = it },
                        label = { Text("Stream Path") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedButton(
                    onClick = { onTestDevice("CAM") },
                    modifier = Modifier.align(Alignment.End).testTag("settings_test_cam_btn")
                ) {
                    Text("Test ESP32-CAM Connection", fontSize = 12.sp)
                }
            }
        }

        // ==========================================
        // LOCAL AUTH TOKEN
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "LOCAL NETWORK AUTH TOKEN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhitePrimary)
                Text(text = "Token included in Authorization header for secure private controller requests.", fontSize = 11.sp, color = TextMutedSecondary)
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    placeholder = { Text("Optional bearer token (e.g. onion-robot-2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // ==========================================
        // SAFETY & ACTUATOR THRESHOLDS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "AGRICULTURAL SAFETY THRESHOLDS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhitePrimary)

                OutlinedTextField(
                    value = soilDryThreshold,
                    onValueChange = { soilDryThreshold = it },
                    label = { Text("Soil Dry Moisture Threshold (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = waterTankMin,
                    onValueChange = { waterTankMin = it },
                    label = { Text("Water Tank Minimum Level (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chemicalTankMin,
                    onValueChange = { chemicalTankMin = it },
                    label = { Text("Chemical Tank Minimum Level (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weedConfidence,
                    onValueChange = { weedConfidence = it },
                    label = { Text("Weed Detection Confidence Threshold (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = obstacleSafetyCm,
                    onValueChange = { obstacleSafetyCm = it },
                    label = { Text("Front Obstacle Emergency Halt Distance (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // ==========================================
        // NETWORK TIMING
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "NETWORK REFRESH & TIMEOUTS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhitePrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = refreshInterval,
                        onValueChange = { refreshInterval = it },
                        label = { Text("Poll Interval (ms)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = httpTimeout,
                        onValueChange = { httpTimeout = it },
                        label = { Text("HTTP Timeout (ms)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // SAVE BUTTON
        Button(
            onClick = {
                val updated = state.settings.copy(
                    esp1Ip = esp1Ip.trim(),
                    esp1Port = esp1Port.toIntOrNull() ?: 80,
                    esp2Ip = esp2Ip.trim(),
                    esp2Port = esp2Port.toIntOrNull() ?: 80,
                    camIp = camIp.trim(),
                    camPort = camPort.toIntOrNull() ?: 80,
                    camCapturePath = camCapturePath.trim(),
                    camStreamPath = camStreamPath.trim(),
                    authToken = authToken.trim(),
                    soilDryThresholdPct = soilDryThreshold.toFloatOrNull() ?: 30f,
                    waterTankMinPct = waterTankMin.toFloatOrNull() ?: 15f,
                    chemicalTankMinPct = chemicalTankMin.toFloatOrNull() ?: 15f,
                    weedConfidenceThreshold = (weedConfidence.toFloatOrNull() ?: 70f) / 100f,
                    obstacleSafetyThresholdCm = obstacleSafetyCm.toFloatOrNull() ?: 25f,
                    refreshIntervalMs = refreshInterval.toLongOrNull() ?: 1500L,
                    httpTimeoutMs = httpTimeout.toLongOrNull() ?: 3000L
                )
                onSaveSettings(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_settings_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Apply Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    // Firmware Code Modal / Dialog
    if (showFirmwareDialog) {
        AlertDialog(
            onDismissRequest = { showFirmwareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = ForestGreenLight)
                    Text("Matching ESP32 Firmware Specification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val docScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(docScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "The app implements real HTTP client calls for the exact endpoints defined below. Flash these onto your respective ESP32 modules:",
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )

                    Text(text = "ESP32 #1 — Main Controller (Drive, Arm, Cutter):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestGreenLight)
                    Surface(
                        color = CharcoalBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = FirmwareCodeExamples.ESP32_1_MAIN_CONTROLLER_CODE.trim(),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextWhitePrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "ESP32 #2 — Sensor & Pump Controller:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AutoModeBlue)
                    Surface(
                        color = CharcoalBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = FirmwareCodeExamples.ESP32_2_SENSOR_CONTROLLER_CODE.trim(),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextWhitePrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFirmwareDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = CharcoalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
