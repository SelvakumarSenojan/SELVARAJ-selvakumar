package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onion_robot_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ESP1_IP = stringPreferencesKey("esp1_ip")
        val ESP1_PORT = intPreferencesKey("esp1_port")
        val ESP2_IP = stringPreferencesKey("esp2_ip")
        val ESP2_PORT = intPreferencesKey("esp2_port")
        val CAM_IP = stringPreferencesKey("cam_ip")
        val CAM_PORT = intPreferencesKey("cam_port")
        val CAM_STREAM_PATH = stringPreferencesKey("cam_stream_path")
        val CAM_CAPTURE_PATH = stringPreferencesKey("cam_capture_path")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val SOIL_DRY_THRESHOLD = floatPreferencesKey("soil_dry_threshold")
        val WATER_TANK_MIN = floatPreferencesKey("water_tank_min")
        val CHEMICAL_TANK_MIN = floatPreferencesKey("chemical_tank_min")
        val WEED_CONFIDENCE = floatPreferencesKey("weed_confidence")
        val OBSTACLE_SAFETY_CM = floatPreferencesKey("obstacle_safety_cm")
        val REFRESH_INTERVAL_MS = longPreferencesKey("refresh_interval_ms")
        val HTTP_TIMEOUT_MS = longPreferencesKey("http_timeout_ms")
        val SERVO_HOME = intPreferencesKey("servo_home")
        val SERVO_TEST = intPreferencesKey("servo_test")
        val ARM_X_SCALE = floatPreferencesKey("arm_x_scale")
        val ARM_Y_SCALE = floatPreferencesKey("arm_y_scale")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppSettings(
                esp1Ip = prefs[Keys.ESP1_IP] ?: "192.168.4.101",
                esp1Port = prefs[Keys.ESP1_PORT] ?: 80,
                esp2Ip = prefs[Keys.ESP2_IP] ?: "192.168.4.102",
                esp2Port = prefs[Keys.ESP2_PORT] ?: 80,
                camIp = prefs[Keys.CAM_IP] ?: "192.168.4.103",
                camPort = prefs[Keys.CAM_PORT] ?: 80,
                camStreamPath = prefs[Keys.CAM_STREAM_PATH] ?: "/stream",
                camCapturePath = prefs[Keys.CAM_CAPTURE_PATH] ?: "/capture",
                authToken = prefs[Keys.AUTH_TOKEN] ?: "",
                soilDryThresholdPct = prefs[Keys.SOIL_DRY_THRESHOLD] ?: 30.0f,
                waterTankMinPct = prefs[Keys.WATER_TANK_MIN] ?: 15.0f,
                chemicalTankMinPct = prefs[Keys.CHEMICAL_TANK_MIN] ?: 15.0f,
                weedConfidenceThreshold = prefs[Keys.WEED_CONFIDENCE] ?: 0.70f,
                obstacleSafetyThresholdCm = prefs[Keys.OBSTACLE_SAFETY_CM] ?: 25.0f,
                refreshIntervalMs = prefs[Keys.REFRESH_INTERVAL_MS] ?: 1500L,
                httpTimeoutMs = prefs[Keys.HTTP_TIMEOUT_MS] ?: 3000L,
                soilServoHomeAngle = prefs[Keys.SERVO_HOME] ?: 0,
                soilServoTestAngle = prefs[Keys.SERVO_TEST] ?: 90,
                armXCalibrationScale = prefs[Keys.ARM_X_SCALE] ?: 1.0f,
                armYCalibrationScale = prefs[Keys.ARM_Y_SCALE] ?: 1.0f
            )
        }

    suspend fun saveSettings(newSettings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ESP1_IP] = newSettings.esp1Ip.trim()
            prefs[Keys.ESP1_PORT] = newSettings.esp1Port
            prefs[Keys.ESP2_IP] = newSettings.esp2Ip.trim()
            prefs[Keys.ESP2_PORT] = newSettings.esp2Port
            prefs[Keys.CAM_IP] = newSettings.camIp.trim()
            prefs[Keys.CAM_PORT] = newSettings.camPort
            prefs[Keys.CAM_STREAM_PATH] = newSettings.camStreamPath.trim()
            prefs[Keys.CAM_CAPTURE_PATH] = newSettings.camCapturePath.trim()
            prefs[Keys.AUTH_TOKEN] = newSettings.authToken.trim()
            prefs[Keys.SOIL_DRY_THRESHOLD] = newSettings.soilDryThresholdPct
            prefs[Keys.WATER_TANK_MIN] = newSettings.waterTankMinPct
            prefs[Keys.CHEMICAL_TANK_MIN] = newSettings.chemicalTankMinPct
            prefs[Keys.WEED_CONFIDENCE] = newSettings.weedConfidenceThreshold
            prefs[Keys.OBSTACLE_SAFETY_CM] = newSettings.obstacleSafetyThresholdCm
            prefs[Keys.REFRESH_INTERVAL_MS] = newSettings.refreshIntervalMs
            prefs[Keys.HTTP_TIMEOUT_MS] = newSettings.httpTimeoutMs
            prefs[Keys.SERVO_HOME] = newSettings.soilServoHomeAngle
            prefs[Keys.SERVO_TEST] = newSettings.soilServoTestAngle
            prefs[Keys.ARM_X_SCALE] = newSettings.armXCalibrationScale
            prefs[Keys.ARM_Y_SCALE] = newSettings.armYCalibrationScale
        }
    }
}
