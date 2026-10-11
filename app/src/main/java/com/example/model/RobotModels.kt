package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class RobotMode {
    MANUAL,
    AUTO
}

enum class ConnectionState {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR
}

enum class SpeedLevel(val displayName: String, val speedPercent: Int) {
    SLOW("SLOW", 35),
    MEDIUM("MEDIUM", 65),
    FAST("FAST", 100)
}

enum class MotorCommand {
    FORWARD,
    REVERSE,
    LEFT,
    RIGHT,
    STOP
}

enum class WeedClass {
    WEED,
    ONION,
    UNKNOWN
}

enum class ArmState {
    HOME,
    TARGET,
    MOVING
}

enum class SoilTestState {
    IDLE,
    COUNTDOWN,
    SERVING_TO_90,
    READING_SENSORS,
    PUMPING,
    RETURNING_HOME,
    CANCELLED
}

@JsonClass(generateAdapter = true)
data class Esp1Status(
    @Json(name = "mode") val mode: String = "MANUAL",
    @Json(name = "speed") val speed: String = "SLOW",
    @Json(name = "obstacle_detected") val obstacleDetected: Boolean = false,
    @Json(name = "front_distance_cm") val frontDistanceCm: Float = 999.0f,
    @Json(name = "arm_state") val armState: String = "HOME",
    @Json(name = "cutter_active") val cutterActive: Boolean = false,
    @Json(name = "cutter_remaining_sec") val cutterRemainingSec: Float = 0.0f,
    @Json(name = "pan_angle") val panAngle: Int = 90,
    @Json(name = "tilt_angle") val tiltAngle: Int = 90,
    @Json(name = "battery_voltage") val batteryVoltage: Float? = null,
    @Json(name = "emergency_stop") val emergencyStop: Boolean = false,
    @Json(name = "safety_fault") val safetyFault: String? = null
)

@JsonClass(generateAdapter = true)
data class Tcs3200Data(
    @Json(name = "r") val r: Int = 0,
    @Json(name = "g") val g: Int = 0,
    @Json(name = "b") val b: Int = 0,
    @Json(name = "clear") val clear: Int = 0,
    @Json(name = "dominant_color") val dominantColor: String = "UNKNOWN",
    @Json(name = "healthy") val healthy: Boolean = true,
    @Json(name = "suspected_disease") val suspectedDisease: String? = null
)

@JsonClass(generateAdapter = true)
data class Esp2Status(
    @Json(name = "soil_moisture_raw") val soilMoistureRaw: Float? = null,
    @Json(name = "soil_moisture_pct") val soilMoisturePct: Float? = null,
    @Json(name = "temperature_c") val temperatureC: Float? = null,
    @Json(name = "humidity_pct") val humidityPct: Float? = null,
    @Json(name = "water_tank_level_pct") val waterTankLevelPct: Float? = null,
    @Json(name = "chemical_tank_level_pct") val chemicalTankLevelPct: Float? = null,
    @Json(name = "tcs3200") val tcs3200: Tcs3200Data? = null,
    @Json(name = "soil_servo_angle") val soilServoAngle: Int = 0,
    @Json(name = "water_pump_active") val waterPumpActive: Boolean = false,
    @Json(name = "sprayer_active") val sprayerActive: Boolean = false,
    @Json(name = "relay_active") val relayActive: Boolean = false,
    @Json(name = "buzzer_active") val buzzerActive: Boolean = false,
    @Json(name = "last_soil_test_timestamp") val lastSoilTestTimestamp: Long? = null,
    @Json(name = "safety_fault") val safetyFault: String? = null
)

// Request bodies
@JsonClass(generateAdapter = true)
data class ModeRequest(
    @Json(name = "mode") val mode: String
)

@JsonClass(generateAdapter = true)
data class MoveRequest(
    @Json(name = "command") val command: String
)

@JsonClass(generateAdapter = true)
data class SpeedRequest(
    @Json(name = "speed") val speed: String
)

@JsonClass(generateAdapter = true)
data class ServoRequest(
    @Json(name = "target") val target: String,
    @Json(name = "angle") val angle: Int
)

@JsonClass(generateAdapter = true)
data class CutterRequest(
    @Json(name = "state") val state: String,
    @Json(name = "max_sec") val maxSec: Int = 7
)

@JsonClass(generateAdapter = true)
data class WaterPumpRequest(
    @Json(name = "state") val state: String,
    @Json(name = "duration_sec") val durationSec: Int = 5
)

@JsonClass(generateAdapter = true)
data class SprayerRequest(
    @Json(name = "state") val state: String,
    @Json(name = "duration_sec") val durationSec: Int = 10
)

@JsonClass(generateAdapter = true)
data class RelayRequest(
    @Json(name = "state") val state: String
)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String = "",
    @Json(name = "fault") val fault: String? = null
)

// UI History Records
data class SoilTestRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val soilMoisturePct: Float?,
    val temperatureC: Float?,
    val humidityPct: Float?,
    val waterTankLevelPct: Float?,
    val dryDetected: Boolean,
    val pumpActivated: Boolean,
    val summary: String
)

data class WeedDetectionRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val classification: WeedClass,
    val confidence: Float,
    val normX: Float,
    val normY: Float,
    val armPosition: String,
    val cutterActivated: Boolean,
    val actionSummary: String
)

data class DiseaseIncidentRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val suspectedDisease: String,
    val rgbDisplay: String,
    val chemicalTankLevelPct: Float?,
    val sprayerActivated: Boolean,
    val notes: String
)

data class DeviceConnectionInfo(
    val name: String,
    val ip: String,
    val port: Int,
    val state: ConnectionState = ConnectionState.OFFLINE,
    val pingMs: Long = 0L,
    val lastError: String? = null,
    val lastSeenTimestamp: Long = 0L
)

data class AppSettings(
    val esp1Ip: String = "192.168.4.101",
    val esp1Port: Int = 80,
    val esp2Ip: String = "192.168.4.102",
    val esp2Port: Int = 80,
    val camIp: String = "192.168.4.103",
    val camPort: Int = 80,
    val camStreamPath: String = "/stream",
    val camCapturePath: String = "/capture",
    val authToken: String = "",
    val soilDryThresholdPct: Float = 30.0f,
    val waterTankMinPct: Float = 15.0f,
    val chemicalTankMinPct: Float = 15.0f,
    val weedConfidenceThreshold: Float = 0.70f,
    val obstacleSafetyThresholdCm: Float = 25.0f,
    val refreshIntervalMs: Long = 1500L,
    val httpTimeoutMs: Long = 3000L,
    val soilServoHomeAngle: Int = 0,
    val soilServoTestAngle: Int = 90,
    val armXCalibrationScale: Float = 1.0f,
    val armYCalibrationScale: Float = 1.0f
)
