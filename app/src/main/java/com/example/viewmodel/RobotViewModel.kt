package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.model.*
import com.example.network.RobotNetworkClient
import com.example.vision.DetectionResult
import com.example.vision.WeedClassifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RobotUiState(
    val currentMode: RobotMode = RobotMode.MANUAL,
    val isModeSwitchPending: Boolean = false,
    val showAutoModeConfirmDialog: Boolean = false,
    val emergencyStopActive: Boolean = false,

    // Devices Connection State
    val esp1Info: DeviceConnectionInfo = DeviceConnectionInfo("ESP32 #1 (Drive/Arm)", "192.168.4.101", 80),
    val esp2Info: DeviceConnectionInfo = DeviceConnectionInfo("ESP32 #2 (Sensors/Pumps)", "192.168.4.102", 80),
    val camInfo: DeviceConnectionInfo = DeviceConnectionInfo("ESP32-CAM", "192.168.4.103", 80),

    // Real Hardware Status
    val esp1Status: Esp1Status = Esp1Status(),
    val esp2Status: Esp2Status = Esp2Status(),
    val latestCameraFrame: Bitmap? = null,
    val isCameraStreaming: Boolean = false,
    val cameraError: String? = null,

    // Manual Mode State
    val currentSpeed: SpeedLevel = SpeedLevel.SLOW,
    val activeMoveCommand: MotorCommand? = null,
    val manualRelayState: Boolean = false,

    // Auto Mode - Branch 1: AI Weed Detection
    val branch1Active: Boolean = false,
    val latestWeedDetection: DetectionResult? = null,
    val isArmOperating: Boolean = false,
    val cutterCountdownSec: Int = 0,
    val weedHistory: List<WeedDetectionRecord> = emptyList(),

    // Auto Mode - Branch 2: Onion Disease Monitoring
    val branch2Active: Boolean = false,
    val suspectedDiseaseAlert: String? = null,
    val sprayerCountdownSec: Int = 0,
    val diseaseHistory: List<DiseaseIncidentRecord> = emptyList(),

    // Auto Mode - Branch 3: Soil Testing (Strict 5-minute rule)
    val branch3Active: Boolean = false,
    val soilTestState: SoilTestState = SoilTestState.IDLE,
    val soilTestCountdownSecondsRemaining: Int = 300, // 5 minutes = 300 seconds
    val isSoilTestCardsVisible: Boolean = false, // Soil/Temp/Humidity ONLY visible during active test
    val activeSoilTestMoisturePct: Float? = null,
    val activeSoilTestTempC: Float? = null,
    val activeSoilTestHumidityPct: Float? = null,
    val waterPumpCountdownSec: Int = 0,
    val soilTestHistory: List<SoilTestRecord> = emptyList(),

    // Settings
    val settings: AppSettings = AppSettings(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class RobotViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val networkClient = RobotNetworkClient()
    private val weedClassifier = WeedClassifier()

    private val _uiState = MutableStateFlow(RobotUiState())
    val uiState: StateFlow<RobotUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var soilTestTimerJob: Job? = null
    private var autoModeWorkflowJob: Job? = null
    private var cameraStreamJob: Job? = null

    init {
        // Observe settings changes
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { newSettings ->
                _uiState.update { state ->
                    state.copy(
                        settings = newSettings,
                        esp1Info = state.esp1Info.copy(ip = newSettings.esp1Ip, port = newSettings.esp1Port),
                        esp2Info = state.esp2Info.copy(ip = newSettings.esp2Ip, port = newSettings.esp2Port),
                        camInfo = state.camInfo.copy(ip = newSettings.camIp, port = newSettings.camPort)
                    )
                }
            }
        }

        // Start hardware polling loop
        startHardwarePolling()
    }

    // --- Hardware Status Polling ---

    fun startHardwarePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentSettings = _uiState.value.settings

                // Poll ESP32 #1
                val esp1Result = networkClient.getEsp1Status(currentSettings)
                withContext(Dispatchers.Main) {
                    esp1Result.fold(
                        onSuccess = { (status, ping) ->
                            _uiState.update { it.copy(
                                esp1Status = status,
                                esp1Info = it.esp1Info.copy(
                                    state = ConnectionState.ONLINE,
                                    pingMs = ping,
                                    lastError = null,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                            ) }
                            // If obstacle detected or front distance safety triggered, halt movement
                            if (status.obstacleDetected || status.frontDistanceCm <= currentSettings.obstacleSafetyThresholdCm) {
                                if (_uiState.value.activeMoveCommand != null && _uiState.value.activeMoveCommand != MotorCommand.STOP) {
                                    sendMoveCommand(MotorCommand.STOP)
                                }
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(
                                esp1Info = it.esp1Info.copy(
                                    state = ConnectionState.ERROR,
                                    lastError = error.message
                                )
                            ) }
                        }
                    )
                }

                // Poll ESP32 #2
                val esp2Result = networkClient.getEsp2Status(currentSettings)
                withContext(Dispatchers.Main) {
                    esp2Result.fold(
                        onSuccess = { (status, ping) ->
                            _uiState.update { it.copy(
                                esp2Status = status,
                                esp2Info = it.esp2Info.copy(
                                    state = ConnectionState.ONLINE,
                                    pingMs = ping,
                                    lastError = null,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                            ) }
                            // Check TCS3200 disease monitoring when in AUTO mode
                            if (_uiState.value.currentMode == RobotMode.AUTO) {
                                evaluateTcs3200Disease(status.tcs3200)
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(
                                esp2Info = it.esp2Info.copy(
                                    state = ConnectionState.ERROR,
                                    lastError = error.message
                                )
                            ) }
                        }
                    )
                }

                // Poll ESP32-CAM Status
                val camResult = networkClient.testCamConnection(currentSettings)
                withContext(Dispatchers.Main) {
                    camResult.fold(
                        onSuccess = { ping ->
                            _uiState.update { it.copy(
                                camInfo = it.camInfo.copy(
                                    state = ConnectionState.ONLINE,
                                    pingMs = ping,
                                    lastError = null,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                ),
                                cameraError = null
                            ) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(
                                camInfo = it.camInfo.copy(
                                    state = ConnectionState.OFFLINE,
                                    lastError = error.message
                                ),
                                cameraError = error.message
                            ) }
                        }
                    )
                }

                delay(currentSettings.refreshIntervalMs.coerceAtLeast(1000L))
            }
        }
    }

    // --- Mode Transitions ---

    fun requestModeChange(targetMode: RobotMode) {
        if (targetMode == RobotMode.AUTO) {
            // Require deliberate confirmation before entering AUTO mode
            _uiState.update { it.copy(showAutoModeConfirmDialog = true) }
        } else {
            switchToManualMode()
        }
    }

    fun dismissAutoModeDialog() {
        _uiState.update { it.copy(showAutoModeConfirmDialog = false) }
    }

    fun confirmEnterAutoMode() {
        _uiState.update { it.copy(showAutoModeConfirmDialog = false, isModeSwitchPending = true) }
        viewModelScope.launch {
            val settings = _uiState.value.settings
            val result = networkClient.setMode(settings, RobotMode.AUTO)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        currentMode = RobotMode.AUTO,
                        isModeSwitchPending = false,
                        activeMoveCommand = null,
                        statusMessage = "AUTO MODE ACTIVATED"
                    ) }
                    startAutoModeWorkflows()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isModeSwitchPending = false,
                        errorMessage = "Failed to switch to AUTO mode: ${error.message}"
                    ) }
                }
            )
        }
    }

    fun switchToManualMode() {
        // Immediately abort all automatic tasks, turn pumps and cutter off, hide cards, return servo HOME
        cancelAutoWorkflows()

        _uiState.update { it.copy(
            isModeSwitchPending = true,
            currentMode = RobotMode.MANUAL,
            branch1Active = false,
            branch2Active = false,
            branch3Active = false,
            soilTestState = SoilTestState.IDLE,
            isSoilTestCardsVisible = false, // Mandatory rule: hide soil test cards outside active auto test
            cutterCountdownSec = 0,
            sprayerCountdownSec = 0,
            waterPumpCountdownSec = 0,
            isArmOperating = false
        ) }

        viewModelScope.launch {
            val settings = _uiState.value.settings
            // Tell ESP32 #1 to enter MANUAL mode
            networkClient.setMode(settings, RobotMode.MANUAL)
            // Stop motors
            networkClient.sendMove(settings, MotorCommand.STOP)
            // Stop cutter
            networkClient.setCutter(settings, "OFF")
            // Return arm HOME
            networkClient.setServo(settings, "arm", 0)
            // Stop pumps & relay on ESP32 #2
            networkClient.setWaterPump(settings, "OFF")
            networkClient.setSprayer(settings, "OFF")
            networkClient.cancelSoilTest(settings)

            _uiState.update { it.copy(
                isModeSwitchPending = false,
                statusMessage = "MANUAL MODE ACTIVE - Automatic branches halted"
            ) }
        }
    }

    // --- AUTO MODE 3-BRANCH COORDINATOR ---

    private fun startAutoModeWorkflows() {
        cancelAutoWorkflows()

        _uiState.update { it.copy(
            branch1Active = true,
            branch2Active = true,
            branch3Active = true,
            soilTestCountdownSecondsRemaining = 300 // 5 minutes initial countdown
        ) }

        // Start Branch 3: Soil Testing 5-minute countdown timer
        startSoilTestingScheduler()

        // Start Branch 1: AI Weed Detection search loop
        startWeedDetectionLoop()
    }

    private fun cancelAutoWorkflows() {
        soilTestTimerJob?.cancel()
        autoModeWorkflowJob?.cancel()
        soilTestTimerJob = null
        autoModeWorkflowJob = null
    }

    /**
     * BRANCH 3 — SOIL TESTING
     * Mandatory rule:
     * - Soil testing occurs ONLY in AUTO MODE.
     * - Run one soil test every 5 minutes while AUTO MODE remains active.
     * - Do not continuously rotate the soil-testing servo.
     * - Do not continuously display soil, temperature or humidity values.
     * - When 5-minute timer expires: stop robot, move servo to 90°, enable cards, read sensors,
     *   evaluate dry-soil threshold, pump max 5s if dry & water available, pump OFF,
     *   return servo HOME, hide cards, resume slow searching.
     */
    private fun startSoilTestingScheduler() {
        soilTestTimerJob?.cancel()
        soilTestTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _uiState.value.currentMode == RobotMode.AUTO) {
                var secondsLeft = 300 // 5 minutes
                _uiState.update { it.copy(
                    soilTestState = SoilTestState.COUNTDOWN,
                    soilTestCountdownSecondsRemaining = secondsLeft,
                    isSoilTestCardsVisible = false // Ensure cards remain OFF during countdown
                ) }

                while (secondsLeft > 0 && isActive && _uiState.value.currentMode == RobotMode.AUTO) {
                    delay(1000L)
                    secondsLeft--
                    _uiState.update { it.copy(soilTestCountdownSecondsRemaining = secondsLeft) }
                }

                if (!isActive || _uiState.value.currentMode != RobotMode.AUTO) break

                // 5-minute timer expired: execute soil test routine
                executeSoilTestRoutine()
            }
        }
    }

    private suspend fun executeSoilTestRoutine() {
        val settings = _uiState.value.settings

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                soilTestState = SoilTestState.SERVING_TO_90,
                statusMessage = "SOIL TEST: Halting robot and moving servo to 90°"
            ) }
        }

        // 1. Stop robot movement
        networkClient.sendMove(settings, MotorCommand.STOP)
        delay(600)

        // 2. Move soil servo to 90 degrees
        networkClient.startSoilTest(settings)
        delay(1200)

        // 3. Enable the soil, temperature and humidity value cards
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                soilTestState = SoilTestState.READING_SENSORS,
                isSoilTestCardsVisible = true // ENABLE CARDS
            ) }
        }

        // 4. Read actual sensors from ESP32 #2
        val esp2Result = networkClient.getEsp2Status(settings)
        var moisturePct: Float? = null
        var tempC: Float? = null
        var humPct: Float? = null
        var waterTankPct: Float? = null

        esp2Result.onSuccess { (status, _) ->
            moisturePct = status.soilMoisturePct
            tempC = status.temperatureC
            humPct = status.humidityPct
            waterTankPct = status.waterTankLevelPct
        }

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                activeSoilTestMoisturePct = moisturePct,
                activeSoilTestTempC = tempC,
                activeSoilTestHumidityPct = humPct
            ) }
        }

        delay(1500)

        // 5. Evaluate dry soil threshold and pump logic
        val isDry = (moisturePct ?: 100f) < settings.soilDryThresholdPct
        var pumpActivated = false
        var testSummary = "Soil moisture adequate (${moisturePct?.toInt() ?: 0}% >= ${settings.soilDryThresholdPct.toInt()}%). No watering needed."

        if (isDry) {
            val isWaterAdequate = (waterTankPct ?: 0f) >= settings.waterTankMinPct
            if (isWaterAdequate) {
                testSummary = "Dry soil detected (${moisturePct?.toInt()} < ${settings.soilDryThresholdPct.toInt()}%). Water tank adequate (${waterTankPct?.toInt()}%). Running pump."
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(
                        soilTestState = SoilTestState.PUMPING,
                        waterPumpCountdownSec = 5
                    ) }
                }

                // Activate water pump for max 5 seconds
                networkClient.setWaterPump(settings, "ON", 5)
                pumpActivated = true

                // Countdown 5s
                for (sec in 5 downTo 1) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(waterPumpCountdownSec = sec - 1) }
                    }
                }
                // Turn water pump OFF
                networkClient.setWaterPump(settings, "OFF")
            } else {
                testSummary = "Dry soil detected (${moisturePct?.toInt()}%), but water tank is LOW (${waterTankPct?.toInt() ?: 0}%). Pump aborted for safety."
            }
        }

        // 6. Return soil testing servo to HOME
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                soilTestState = SoilTestState.RETURNING_HOME,
                statusMessage = "SOIL TEST: Returning soil servo to HOME"
            ) }
        }
        networkClient.cancelSoilTest(settings)
        delay(1200)

        // 7. Hide current-test value cards and record history
        val record = SoilTestRecord(
            soilMoisturePct = moisturePct,
            temperatureC = tempC,
            humidityPct = humPct,
            waterTankLevelPct = waterTankPct,
            dryDetected = isDry,
            pumpActivated = pumpActivated,
            summary = testSummary
        )

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                soilTestState = SoilTestState.IDLE,
                isSoilTestCardsVisible = false, // HIDE CARDS
                soilTestHistory = listOf(record) + it.soilTestHistory.take(49),
                statusMessage = "Soil test complete. Resuming auto search."
            ) }
        }

        // 8. Resume slow searching only after safe completion
        if (_uiState.value.currentMode == RobotMode.AUTO) {
            networkClient.sendMove(settings, MotorCommand.FORWARD)
        }
    }

    /**
     * BRANCH 1 — AI WEED DETECTION
     */
    private fun startWeedDetectionLoop() {
        autoModeWorkflowJob?.cancel()
        autoModeWorkflowJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _uiState.value.currentMode == RobotMode.AUTO) {
                // If soil test is actively testing, wait
                if (_uiState.value.soilTestState != SoilTestState.COUNTDOWN &&
                    _uiState.value.soilTestState != SoilTestState.IDLE) {
                    delay(1500L)
                    continue
                }

                val settings = _uiState.value.settings

                // Capture real camera frame from ESP32-CAM
                val captureResult = networkClient.captureCamFrame(settings)
                captureResult.onSuccess { bitmap ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(latestCameraFrame = bitmap) }
                    }

                    // Run real computer vision analysis
                    val detection = weedClassifier.analyzeFrame(bitmap, settings)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(latestWeedDetection = detection) }
                    }

                    when (detection.classification) {
                        WeedClass.ONION -> {
                            // PROTECT ONION — Never trigger cutter!
                            val record = WeedDetectionRecord(
                                classification = WeedClass.ONION,
                                confidence = detection.confidence,
                                normX = detection.centerNormX,
                                normY = detection.centerNormY,
                                armPosition = "PROTECTED (Arm locked HOME)",
                                cutterActivated = false,
                                actionSummary = "Onion crop identified. Protected from cutter."
                            )
                            withContext(Dispatchers.Main) {
                                _uiState.update { it.copy(weedHistory = listOf(record) + it.weedHistory.take(49)) }
                            }
                        }
                        WeedClass.UNKNOWN -> {
                            // Ignore unknown
                        }
                        WeedClass.WEED -> {
                            if (detection.confidence >= settings.weedConfidenceThreshold && detection.armTargetAngle != null) {
                                // CONFIDENT WEED -> Execute cutting sequence
                                executeWeedCuttingSequence(detection, settings)
                            }
                        }
                    }
                }

                delay(3000L) // Scan cycle interval
            }
        }
    }

    private suspend fun executeWeedCuttingSequence(detection: DetectionResult, settings: AppSettings) {
        val target = detection.armTargetAngle ?: return

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                isArmOperating = true,
                statusMessage = "WEED DETECTED (${(detection.confidence * 100).toInt()}%). Stopping robot."
            ) }
        }

        // 1. Stop robot before arm operation
        networkClient.sendMove(settings, MotorCommand.STOP)
        delay(600)

        // 2. Move three-joint arm to target angles
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(statusMessage = "Positioning 3-joint arm to weed coordinates") }
        }
        networkClient.setServo(settings, "arm_shoulder", target.shoulderAngle)
        delay(300)
        networkClient.setServo(settings, "arm_elbow", target.elbowAngle)
        delay(300)
        networkClient.setServo(settings, "arm_wrist", target.wristAngle)
        delay(800)

        // 3. Activate cutter for maximum of 7 seconds
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                statusMessage = "Cutter ACTIVE (Max 7s safety window)",
                cutterCountdownSec = 7
            ) }
        }
        networkClient.setCutter(settings, "ON", 7)

        for (sec in 7 downTo 1) {
            delay(1000)
            if (_uiState.value.currentMode != RobotMode.AUTO) break
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(cutterCountdownSec = sec - 1) }
            }
        }

        // 4. Turn cutter OFF
        networkClient.setCutter(settings, "OFF")
        delay(400)

        // 5. Return arm HOME
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                statusMessage = "Returning arm to HOME position",
                cutterCountdownSec = 0
            ) }
        }
        networkClient.setServo(settings, "arm", 0) // HOME
        delay(1200)

        val record = WeedDetectionRecord(
            classification = WeedClass.WEED,
            confidence = detection.confidence,
            normX = detection.centerNormX,
            normY = detection.centerNormY,
            armPosition = "Shoulder: ${target.shoulderAngle}°, Elbow: ${target.elbowAngle}°, Wrist: ${target.wristAngle}°",
            cutterActivated = true,
            actionSummary = "Weed cut (7s cycle). Arm returned safely HOME."
        )

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                isArmOperating = false,
                weedHistory = listOf(record) + it.weedHistory.take(49),
                statusMessage = "Weed removal complete. Resuming slow search."
            ) }
        }

        // 6. Resume slow searching only after safe completion
        if (_uiState.value.currentMode == RobotMode.AUTO) {
            networkClient.sendMove(settings, MotorCommand.FORWARD)
        }
    }

    /**
     * BRANCH 2 — ONION DISEASE MONITORING
     */
    private fun evaluateTcs3200Disease(tcsData: Tcs3200Data?) {
        if (tcsData == null) return
        val settings = _uiState.value.settings

        // Disease diagnosis from TCS3200 colour reading
        // Healthy onion leaves are predominantly green (g > r and g > b)
        // Suspected fungal diseases (purple blotch, downy mildew, smut) show abnormal high red/blue or dark blister ratio
        val hasSuspectedDisease = !tcsData.healthy || tcsData.suspectedDisease != null

        if (hasSuspectedDisease && _uiState.value.sprayerCountdownSec == 0 && !_uiState.value.isArmOperating) {
            val diseaseName = tcsData.suspectedDisease ?: "Suspected Foliage Blotch (Abnormal G/R Ratio)"
            viewModelScope.launch {
                executeDiseaseSprayingRoutine(diseaseName, tcsData, settings)
            }
        }
    }

    private suspend fun executeDiseaseSprayingRoutine(diseaseName: String, tcsData: Tcs3200Data, settings: AppSettings) {
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                suspectedDiseaseAlert = diseaseName,
                statusMessage = "DISEASE ALERT: $diseaseName. Stopping robot."
            ) }
        }

        // 1. Stop robot
        networkClient.sendMove(settings, MotorCommand.STOP)
        delay(600)

        // 2. Check chemical tank level using actual sensor
        val chemLevel = _uiState.value.esp2Status.chemicalTankLevelPct
        val isChemAdequate = (chemLevel ?: 0f) >= settings.chemicalTankMinPct
        var sprayerActivated = false
        var logNote = ""

        if (isChemAdequate) {
            // Activate sprayer for max 10 seconds
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(
                    sprayerCountdownSec = 10,
                    statusMessage = "Running chemical sprayer (Max 10s window)"
                ) }
            }
            networkClient.setSprayer(settings, "ON", 10)
            sprayerActivated = true

            for (sec in 10 downTo 1) {
                delay(1000)
                if (_uiState.value.currentMode != RobotMode.AUTO) break
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(sprayerCountdownSec = sec - 1) }
                }
            }

            // Turn sprayer OFF
            networkClient.setSprayer(settings, "OFF")
            logNote = "Targeted treatment sprayed (10s). Tank at ${chemLevel?.toInt()}%."
        } else {
            logNote = "Treatment skipped: Chemical tank level LOW (${chemLevel?.toInt() ?: 0}% < ${settings.chemicalTankMinPct.toInt()}%)."
        }

        val record = DiseaseIncidentRecord(
            suspectedDisease = diseaseName,
            rgbDisplay = "R:${tcsData.r} G:${tcsData.g} B:${tcsData.b}",
            chemicalTankLevelPct = chemLevel,
            sprayerActivated = sprayerActivated,
            notes = logNote
        )

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(
                diseaseHistory = listOf(record) + it.diseaseHistory.take(49),
                sprayerCountdownSec = 0,
                statusMessage = "Disease check concluded. Resuming auto search."
            ) }
        }

        // Resume slow search
        if (_uiState.value.currentMode == RobotMode.AUTO) {
            networkClient.sendMove(settings, MotorCommand.FORWARD)
        }
    }

    // --- MANUAL CONTROLS ---

    fun sendMoveCommand(command: MotorCommand) {
        // Safety lock: Disable manual movement if in AUTO MODE
        if (_uiState.value.currentMode == RobotMode.AUTO) {
            _uiState.update { it.copy(errorMessage = "Manual drive is locked while AUTO MODE is active") }
            return
        }

        // Ultrasonic obstacle safety lock
        if (command != MotorCommand.STOP && command != MotorCommand.REVERSE) {
            val dist = _uiState.value.esp1Status.frontDistanceCm
            val obs = _uiState.value.esp1Status.obstacleDetected
            if (obs || dist <= _uiState.value.settings.obstacleSafetyThresholdCm) {
                _uiState.update { it.copy(errorMessage = "Obstacle safety lock! Front clearance only ${dist.toInt()}cm") }
                return
            }
        }

        _uiState.update { it.copy(activeMoveCommand = command) }
        viewModelScope.launch {
            networkClient.sendMove(_uiState.value.settings, command)
        }
    }

    fun releaseMoveCommand() {
        // Safe hold-to-move / command-release approach: stop when released
        if (_uiState.value.activeMoveCommand != null && _uiState.value.activeMoveCommand != MotorCommand.STOP) {
            sendMoveCommand(MotorCommand.STOP)
        }
    }

    fun setSpeed(speed: SpeedLevel) {
        _uiState.update { it.copy(currentSpeed = speed) }
        viewModelScope.launch {
            networkClient.setSpeed(_uiState.value.settings, speed)
        }
    }

    fun sendPanTilt(direction: String) {
        if (_uiState.value.currentMode == RobotMode.AUTO) return
        val currentPan = _uiState.value.esp1Status.panAngle
        val currentTilt = _uiState.value.esp1Status.tiltAngle
        viewModelScope.launch {
            val settings = _uiState.value.settings
            when (direction) {
                "PAN_LEFT" -> networkClient.setServo(settings, "pan", (currentPan - 15).coerceIn(0, 180))
                "PAN_RIGHT" -> networkClient.setServo(settings, "pan", (currentPan + 15).coerceIn(0, 180))
                "TILT_UP" -> networkClient.setServo(settings, "tilt", (currentTilt - 15).coerceIn(0, 180))
                "TILT_DOWN" -> networkClient.setServo(settings, "tilt", (currentTilt + 15).coerceIn(0, 180))
                "CENTER" -> {
                    networkClient.setServo(settings, "pan", 90)
                    networkClient.setServo(settings, "tilt", 90)
                }
            }
        }
    }

    fun toggleManualRelay() {
        val newState = !_uiState.value.manualRelayState
        _uiState.update { it.copy(manualRelayState = newState) }
        viewModelScope.launch {
            networkClient.setRelay(_uiState.value.settings, if (newState) "ON" else "OFF")
        }
    }

    // --- EMERGENCY STOP ---

    fun triggerEmergencyStop() {
        cancelAutoWorkflows()
        _uiState.update { it.copy(
            emergencyStopActive = true,
            currentMode = RobotMode.MANUAL,
            activeMoveCommand = MotorCommand.STOP,
            branch1Active = false,
            branch2Active = false,
            branch3Active = false,
            isSoilTestCardsVisible = false,
            cutterCountdownSec = 0,
            sprayerCountdownSec = 0,
            waterPumpCountdownSec = 0,
            isArmOperating = false,
            statusMessage = "EMERGENCY STOP TRIGGERED! All actuators halted."
        ) }
        viewModelScope.launch {
            val settings = _uiState.value.settings
            networkClient.emergencyStop(settings)
            networkClient.setCutter(settings, "OFF")
            networkClient.setWaterPump(settings, "OFF")
            networkClient.setSprayer(settings, "OFF")
            networkClient.setRelay(settings, "OFF")
            networkClient.cancelSoilTest(settings)
        }
    }

    fun resetEmergencyStop() {
        _uiState.update { it.copy(emergencyStopActive = false, statusMessage = "Emergency stop cleared") }
    }

    // --- SETTINGS ACTIONS ---

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(newSettings)
            _uiState.update { it.copy(
                settings = newSettings,
                statusMessage = "Configuration saved successfully"
            ) }
        }
    }

    fun testDeviceConnection(target: String) {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            when (target) {
                "ESP1" -> {
                    val result = networkClient.getEsp1Status(settings)
                    result.fold(
                        onSuccess = { (_, ping) ->
                            _uiState.update { it.copy(statusMessage = "ESP32 #1 ONLINE (${ping}ms)") }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = "ESP32 #1 Error: ${error.message}") }
                        }
                    )
                }
                "ESP2" -> {
                    val result = networkClient.getEsp2Status(settings)
                    result.fold(
                        onSuccess = { (_, ping) ->
                            _uiState.update { it.copy(statusMessage = "ESP32 #2 ONLINE (${ping}ms)") }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = "ESP32 #2 Error: ${error.message}") }
                        }
                    )
                }
                "CAM" -> {
                    val result = networkClient.testCamConnection(settings)
                    result.fold(
                        onSuccess = { ping ->
                            _uiState.update { it.copy(statusMessage = "ESP32-CAM ONLINE (${ping}ms)") }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = "ESP32-CAM Error: ${error.message}") }
                        }
                    )
                }
            }
        }
    }

    fun captureStillPhoto() {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            val result = networkClient.captureCamFrame(settings)
            result.fold(
                onSuccess = { bitmap ->
                    _uiState.update { it.copy(
                        latestCameraFrame = bitmap,
                        statusMessage = "Still image captured successfully"
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Camera capture failed: ${error.message}") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
