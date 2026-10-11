# AI COMPANION — Smart Onion Garden Maintainer Robot
## Hardware Integration, Firmware Setup, & Device Testing Checklist

> **Tagline:** “See the Weed. Protect the Onion. Work Smart.”

---

### 1. Hardware Architecture Overview

The system consists of three independent Wi-Fi nodes operating over a local area network (LAN / Wi-Fi SoftAP or router):

1. **ESP32 #1 — Main Controller** (Default IP: `192.168.4.101:80`)
   - **Actuators:**
     - Left & Right DC/Stepper drive motors with H-Bridge driver
     - 3-Joint Weed Removal Robotic Arm (Shoulder, Elbow, Wrist servos)
     - Cutter driver with hardware & firmware 7-second safety cutoff
     - Camera Pan & Tilt 2-axis servos
   - **Sensors:**
     - Front HC-SR04 ultrasonic distance sensor (collision prevention)
     - Battery voltage divider (analog ADC)
   - **Mode:**
     - Real-time `MANUAL` and `AUTO` states
     - Auto obstacle stop (< 25 cm clearance)

2. **ESP32 #2 — Sensor and Pump Controller** (Default IP: `192.168.4.102:80`)
   - **Actuators:**
     - Soil testing deployment servo (HOME: 0°, Test: 90°)
     - Water pump relay (firmware-enforced 5-second maximum runtime)
     - Chemical sprayer relay (firmware-enforced 10-second maximum runtime)
     - Auxiliary worklight/relay
   - **Sensors:**
     - Capacitive soil moisture sensor
     - DHT11 ambient temperature and relative humidity sensor
     - TCS3200 spectral/colour sensor (onion foliage health vs disease detection)
     - Water tank ultrasonic/capacitive level sensor
     - Chemical tank ultrasonic/capacitive level sensor

3. **ESP32-CAM — Vision & AI Node** (Default IP: `192.168.4.103:80` / `81`)
   - OV2640 camera module
   - Live stream endpoint (`/stream`)
   - Still frame capture endpoint (`/capture`)
   - Real image feed consumed by Android on-device vision morphology engine for weed vs onion classification

---

### 2. Network API Specifications

All POST requests use `Content-Type: application/json` and return a standard `GenericResponse`:
```json
{
  "success": true,
  "message": "Status description",
  "fault": null
}
```

#### ESP32 #1 Endpoints
| Method | Path | Request Body | Description |
|---|---|---|---|
| `GET` | `/api/status` | None | Returns JSON status of motors, arm, cutter, ultrasonic distance, battery, pan/tilt |
| `POST` | `/api/mode` | `{"mode": "MANUAL" \| "AUTO"}` | Switches operating mode |
| `POST` | `/api/move` | `{"command": "FORWARD" \| "REVERSE" \| "LEFT" \| "RIGHT" \| "STOP"}` | Direct motor drive (hold-to-move) |
| `POST` | `/api/speed` | `{"speed": "SLOW" \| "MEDIUM" \| "FAST"}` | Adjusts motor PWM duty cycle |
| `POST` | `/api/stop` | `{}` | Universal emergency stop |
| `POST` | `/api/servo` | `{"target": "pan" \| "tilt" \| "arm_shoulder" \| "arm_elbow" \| "arm_wrist" \| "arm", "angle": int}` | Servo positioning |
| `POST` | `/api/cutter` | `{"state": "ON" \| "OFF", "max_sec": 7}` | Cutter activation (max 7s cutoff) |

#### ESP32 #2 Endpoints
| Method | Path | Request Body | Description |
|---|---|---|---|
| `GET` | `/api/status` | None | Returns soil, DHT11, TCS3200 RGB, water and chemical tank levels |
| `POST` | `/api/soil-test/start` | `{}` | Deploys servo to 90° for scheduled 5-min test |
| `POST` | `/api/soil-test/cancel` | `{}` | Returns servo to HOME (0°) |
| `POST` | `/api/water-pump` | `{"state": "ON" \| "OFF", "duration_sec": 5}` | Runs water pump (max 5s cutoff) |
| `POST` | `/api/sprayer` | `{"state": "ON" \| "OFF", "duration_sec": 10}` | Runs chemical sprayer (max 10s cutoff) |
| `POST` | `/api/relay` | `{"state": "ON" \| "OFF"}` | Auxiliary relay switch |

#### ESP32-CAM Endpoints
| Method | Path | Description |
|---|---|---|
| `GET` | `/capture` | Returns JPEG still frame |
| `GET` | `/stream` | Real MJPEG video stream |

---

### 3. Core Safety Rules & Timing Constraints

1. **Mandatory 5-Minute Soil Test Rule:**
   - Soil testing runs **ONLY in AUTO MODE**.
   - Runs on a **strict 5-minute interval timer** (300 seconds).
   - Soil moisture, DHT11 temperature, and humidity sensors are **deployed and displayed ONLY during the active test**.
   - Outside the active test, the soil testing servo stays safely HOME (0°) and sensor value cards are hidden in standby.
   - If switched to `MANUAL MODE`, automatic soil testing is immediately cancelled, the pump is stopped, and the servo returns HOME.
2. **actuator Runtime Limits:**
   - Cutter: strictly capped at **7 seconds**.
   - Sprayer: strictly capped at **10 seconds**.
   - Water Pump: strictly capped at **5 seconds**.
3. **Collision Override:**
   - Front clearance `< 25 cm` immediately locks forward movement.
4. **Onion Protection Guarantee:**
   - AI vision classifies `ONION`, `WEED`, and `UNKNOWN`.
   - Onions are protected; weed cutter trigger is hardware- and software-locked when onion foliage is detected.

---

### 4. Device Testing Checklist

- [ ] Connect Android device to the robot's local Wi-Fi network.
- [ ] Open Settings tab in the Android app.
- [ ] Tap **Test ESP32 #1 Connection** → verify latency is displayed and state shows `ONLINE`.
- [ ] Tap **Test ESP32 #2 Connection** → verify latency is displayed and state shows `ONLINE`.
- [ ] Tap **Test ESP32-CAM Connection** → verify live frames are received.
- [ ] Switch to **Manual Tab**:
  - [ ] Test hold-to-move buttons (Forward, Left, Right, Reverse). Verify motors stop on finger release.
  - [ ] Test pan and tilt controls.
  - [ ] Place hand in front of ultrasonic sensor (<25cm) → verify forward movement is blocked.
- [ ] Switch to **Dashboard Tab**:
  - [ ] Tap **AUTO MODE** → confirm safety dialog.
  - [ ] Verify 5-minute countdown clock begins.
  - [ ] Verify soil cards are hidden during countdown and appear only when the 5-minute test fires.
  - [ ] Tap **EMERGENCY STOP** → verify all motors, cutter, and pumps halt immediately.
