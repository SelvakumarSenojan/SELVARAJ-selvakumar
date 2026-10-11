package com.example

import com.example.model.*
import com.example.vision.WeedClassifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class RobotLogicUnitTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun testEsp1StatusJsonParsing() {
        val json = """
            {
                "mode": "MANUAL",
                "speed": "MEDIUM",
                "obstacle_detected": true,
                "front_distance_cm": 18.5,
                "arm_state": "HOME",
                "cutter_active": false,
                "cutter_remaining_sec": 0.0,
                "pan_angle": 90,
                "tilt_angle": 105,
                "battery_voltage": 12.3,
                "emergency_stop": false
            }
        """.trimIndent()

        val adapter = moshi.adapter(Esp1Status::class.java)
        val status = adapter.fromJson(json)

        assertNotNull(status)
        assertEquals("MANUAL", status!!.mode)
        assertEquals("MEDIUM", status.speed)
        assertTrue(status.obstacleDetected)
        assertEquals(18.5f, status.frontDistanceCm, 0.01f)
        assertEquals(12.3f, status.batteryVoltage!!, 0.01f)
    }

    @Test
    fun testEsp2StatusJsonParsingWithTcs3200() {
        val json = """
            {
                "soil_moisture_pct": 24.0,
                "temperature_c": 28.5,
                "humidity_pct": 62.0,
                "water_tank_level_pct": 75.0,
                "chemical_tank_level_pct": 80.0,
                "tcs3200": {
                    "r": 50,
                    "g": 180,
                    "b": 40,
                    "clear": 260,
                    "dominant_color": "GREEN",
                    "healthy": true
                },
                "soil_servo_angle": 0,
                "water_pump_active": false,
                "sprayer_active": false,
                "relay_active": true
            }
        """.trimIndent()

        val adapter = moshi.adapter(Esp2Status::class.java)
        val status = adapter.fromJson(json)

        assertNotNull(status)
        assertEquals(24.0f, status!!.soilMoisturePct!!, 0.01f)
        assertEquals(28.5f, status.temperatureC!!, 0.01f)
        assertNotNull(status.tcs3200)
        assertTrue(status.tcs3200!!.healthy)
        assertEquals(180, status.tcs3200!!.g)
    }

    @Test
    fun testCutterMax7SecondsConstraint() {
        val cutterReq = CutterRequest(state = "ON", maxSec = 7)
        assertTrue(cutterReq.maxSec <= 7)
    }

    @Test
    fun testSprayerMax10SecondsConstraint() {
        val sprayerReq = SprayerRequest(state = "ON", durationSec = 10)
        assertTrue(sprayerReq.durationSec <= 10)
    }

    @Test
    fun testWaterPumpMax5SecondsConstraint() {
        val pumpReq = WaterPumpRequest(state = "ON", durationSec = 5)
        assertTrue(pumpReq.durationSec <= 5)
    }

    @Test
    fun testArmKinematicsCalculations() {
        val classifier = WeedClassifier()
        val settings = AppSettings()

        // Target at center normX = 0.5, normY = 0.5
        val kinematics = classifier.calculateArmKinematics(0.5f, 0.5f, settings)

        assertTrue(kinematics.shoulderAngle in 15..165)
        assertTrue(kinematics.elbowAngle in 20..160)
        assertTrue(kinematics.wristAngle in 10..170)
        assertEquals(0.0f, kinematics.targetMmX, 1.0f) // X should be centered (0 mm)
    }
}
