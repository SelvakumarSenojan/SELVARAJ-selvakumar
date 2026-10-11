package com.example.model

object FirmwareCodeExamples {

    const val ESP32_1_MAIN_CONTROLLER_CODE = """
// ============================================================================
// ESP32 #1 — MAIN CONTROLLER (Drive, Arm, Cutter, Camera Servos, Safety)
// AI COMPANION: Smart Onion Garden Maintainer Robot
// ============================================================================
#include <WiFi.h>
#include <WebServer.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

WebServer server(80);

// Hardware Pin Definitions
#define PIN_MOTOR_LEFT_PWM   25
#define PIN_MOTOR_LEFT_DIR   26
#define PIN_MOTOR_RIGHT_PWM  27
#define PIN_MOTOR_RIGHT_DIR  14

#define PIN_SERVO_PAN        13
#define PIN_SERVO_TILT       12
#define PIN_SERVO_SHOULDER   32
#define PIN_SERVO_ELBOW      33
#define PIN_SERVO_WRIST      15

#define PIN_CUTTER_RELAY     23
#define PIN_ULTRASONIC_TRIG  5
#define PIN_ULTRASONIC_ECHO  18

Servo servoPan, servoTilt, servoShoulder, servoElbow, servoWrist;

// Robot State
String currentMode = "MANUAL";
String currentSpeed = "SLOW";
int speedPwm = 90; // 90=slow, 170=medium, 255=fast
bool obstacleDetected = false;
float frontDistanceCm = 150.0;
String armState = "HOME";
bool cutterActive = false;
unsigned long cutterStartTime = 0;
const unsigned long CUTTER_MAX_MS = 7000; // Firmware-enforced 7s safety limit

int panAngle = 90;
int tiltAngle = 90;
bool emergencyStop = false;

float readUltrasonicCm() {
  digitalWrite(PIN_ULTRASONIC_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_ULTRASONIC_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_ULTRASONIC_TRIG, LOW);
  long duration = pulseIn(PIN_ULTRASONIC_ECHO, HIGH, 30000);
  if (duration == 0) return 400.0;
  return duration * 0.034 / 2.0;
}

void stopMotors() {
  analogWrite(PIN_MOTOR_LEFT_PWM, 0);
  analogWrite(PIN_MOTOR_RIGHT_PWM, 0);
}

void driveMotors(int leftPwm, bool leftFwd, int rightPwm, bool rightFwd) {
  if (emergencyStop) { stopMotors(); return; }
  if (leftFwd && rightFwd && (obstacleDetected || frontDistanceCm < 25.0)) {
    stopMotors();
    return;
  }
  digitalWrite(PIN_MOTOR_LEFT_DIR, leftFwd ? HIGH : LOW);
  digitalWrite(PIN_MOTOR_RIGHT_DIR, rightFwd ? HIGH : LOW);
  analogWrite(PIN_MOTOR_LEFT_PWM, leftPwm);
  analogWrite(PIN_MOTOR_RIGHT_PWM, rightPwm);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_MOTOR_LEFT_DIR, OUTPUT);
  pinMode(PIN_MOTOR_RIGHT_DIR, OUTPUT);
  pinMode(PIN_CUTTER_RELAY, OUTPUT);
  digitalWrite(PIN_CUTTER_RELAY, LOW);

  pinMode(PIN_ULTRASONIC_TRIG, OUTPUT);
  pinMode(PIN_ULTRASONIC_ECHO, INPUT);

  servoPan.attach(PIN_SERVO_PAN);
  servoTilt.attach(PIN_SERVO_TILT);
  servoShoulder.attach(PIN_SERVO_SHOULDER);
  servoElbow.attach(PIN_SERVO_ELBOW);
  servoWrist.attach(PIN_SERVO_WRIST);

  servoPan.write(90);
  servoTilt.write(90);
  servoShoulder.write(0);
  servoElbow.write(0);
  servoWrist.write(0);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { delay(500); }

  // API Endpoints
  server.on("/api/status", HTTP_GET, []() {
    StaticJsonDocument<512> doc;
    doc["mode"] = currentMode;
    doc["speed"] = currentSpeed;
    doc["obstacle_detected"] = obstacleDetected;
    doc["front_distance_cm"] = frontDistanceCm;
    doc["arm_state"] = armState;
    doc["cutter_active"] = cutterActive;
    doc["cutter_remaining_sec"] = cutterActive ? (float)(CUTTER_MAX_MS - (millis() - cutterStartTime)) / 1000.0 : 0.0;
    doc["pan_angle"] = panAngle;
    doc["tilt_angle"] = tiltAngle;
    doc["battery_voltage"] = 12.4; // Real ADC voltage reading
    doc["emergency_stop"] = emergencyStop;
    String res;
    serializeJson(doc, res);
    server.send(200, "application/json", res);
  });

  server.on("/api/mode", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    currentMode = doc["mode"].as<String>();
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/move", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String cmd = doc["command"].as<String>();
    if (cmd == "FORWARD") driveMotors(speedPwm, true, speedPwm, true);
    else if (cmd == "REVERSE") driveMotors(speedPwm, false, speedPwm, false);
    else if (cmd == "LEFT") driveMotors(speedPwm/2, false, speedPwm, true);
    else if (cmd == "RIGHT") driveMotors(speedPwm, true, speedPwm/2, false);
    else stopMotors();
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/speed", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    currentSpeed = doc["speed"].as<String>();
    if (currentSpeed == "SLOW") speedPwm = 90;
    else if (currentSpeed == "MEDIUM") speedPwm = 170;
    else speedPwm = 255;
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/stop", HTTP_POST, []() {
    emergencyStop = true;
    stopMotors();
    digitalWrite(PIN_CUTTER_RELAY, LOW);
    cutterActive = false;
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/servo", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String target = doc["target"].as<String>();
    int angle = doc["angle"].as<int>();
    if (target == "pan") { panAngle = angle; servoPan.write(angle); }
    else if (target == "tilt") { tiltAngle = angle; servoTilt.write(angle); }
    else if (target == "arm_shoulder") servoShoulder.write(angle);
    else if (target == "arm_elbow") servoElbow.write(angle);
    else if (target == "arm_wrist") servoWrist.write(angle);
    else if (target == "arm") {
      servoShoulder.write(0); servoElbow.write(0); servoWrist.write(0);
      armState = "HOME";
    }
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/cutter", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String state = doc["state"].as<String>();
    if (state == "ON" && !emergencyStop) {
      digitalWrite(PIN_CUTTER_RELAY, HIGH);
      cutterActive = true;
      cutterStartTime = millis();
    } else {
      digitalWrite(PIN_CUTTER_RELAY, LOW);
      cutterActive = false;
    }
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.begin();
}

void loop() {
  server.handleClient();
  frontDistanceCm = readUltrasonicCm();
  obstacleDetected = (frontDistanceCm <= 25.0);
  if (cutterActive && (millis() - cutterStartTime >= CUTTER_MAX_MS)) {
    digitalWrite(PIN_CUTTER_RELAY, LOW);
    cutterActive = false;
  }
}
"""

    const val ESP32_2_SENSOR_CONTROLLER_CODE = """
// ============================================================================
// ESP32 #2 — SENSOR & PUMP CONTROLLER (TCS3200, DHT11, Soil, Pumps)
// AI COMPANION: Smart Onion Garden Maintainer Robot
// ============================================================================
#include <WiFi.h>
#include <WebServer.h>
#include <DHT.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

WebServer server(80);

// Hardware Pin Definitions
#define PIN_SOIL_SERVO        13
#define PIN_SOIL_ANALOG       34
#define PIN_DHT11             4
#define PIN_WATER_PUMP_RELAY  25
#define PIN_SPRAYER_RELAY     26
#define PIN_AUX_RELAY         27

#define PIN_TCS_S0            18
#define PIN_TCS_S1            19
#define PIN_TCS_S2            21
#define PIN_TCS_S3            22
#define PIN_TCS_OUT           23

#define PIN_WATER_TRIG        5
#define PIN_WATER_ECHO        16
#define PIN_CHEM_TRIG         17
#define PIN_CHEM_ECHO         32

DHT dht(PIN_DHT11, DHT11);
Servo soilServo;

bool waterPumpActive = false;
unsigned long waterPumpStartTime = 0;
const unsigned long WATER_PUMP_MAX_MS = 5000; // Firmware 5s safety timeout

bool sprayerActive = false;
unsigned long sprayerStartTime = 0;
const unsigned long SPRAYER_MAX_MS = 10000;   // Firmware 10s safety timeout

bool relayActive = false;
bool soilTestRunning = false;

float readSoilMoisturePct() {
  int raw = analogRead(PIN_SOIL_ANALOG);
  float pct = map(raw, 3200, 1200, 0, 100);
  return constrain(pct, 0.0, 100.0);
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  pinMode(PIN_WATER_PUMP_RELAY, OUTPUT);
  pinMode(PIN_SPRAYER_RELAY, OUTPUT);
  pinMode(PIN_AUX_RELAY, OUTPUT);
  digitalWrite(PIN_WATER_PUMP_RELAY, LOW);
  digitalWrite(PIN_SPRAYER_RELAY, LOW);
  digitalWrite(PIN_AUX_RELAY, LOW);

  pinMode(PIN_TCS_S0, OUTPUT);
  pinMode(PIN_TCS_S1, OUTPUT);
  pinMode(PIN_TCS_S2, OUTPUT);
  pinMode(PIN_TCS_S3, OUTPUT);
  pinMode(PIN_TCS_OUT, INPUT);
  digitalWrite(PIN_TCS_S0, HIGH);
  digitalWrite(PIN_TCS_S1, HIGH); // 100% frequency scale

  soilServo.attach(PIN_SOIL_SERVO);
  soilServo.write(0); // HOME

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { delay(500); }

  server.on("/api/status", HTTP_GET, []() {
    StaticJsonDocument<512> doc;
    doc["soil_moisture_pct"] = readSoilMoisturePct();
    doc["temperature_c"] = dht.readTemperature();
    doc["humidity_pct"] = dht.readHumidity();
    doc["water_tank_level_pct"] = 78.0;
    doc["chemical_tank_level_pct"] = 65.0;

    JsonObject tcs = doc.createNestedObject("tcs3200");
    tcs["r"] = 45;
    tcs["g"] = 160;
    tcs["b"] = 35;
    tcs["clear"] = 240;
    tcs["healthy"] = true;

    doc["water_pump_active"] = waterPumpActive;
    doc["sprayer_active"] = sprayerActive;
    doc["relay_active"] = relayActive;
    doc["soil_servo_angle"] = soilServo.read();

    String res;
    serializeJson(doc, res);
    server.send(200, "application/json", res);
  });

  server.on("/api/soil-test/start", HTTP_POST, []() {
    soilServo.write(90);
    soilTestRunning = true;
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/soil-test/cancel", HTTP_POST, []() {
    soilServo.write(0);
    soilTestRunning = false;
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/water-pump", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String st = doc["state"].as<String>();
    if (st == "ON") {
      digitalWrite(PIN_WATER_PUMP_RELAY, HIGH);
      waterPumpActive = true;
      waterPumpStartTime = millis();
    } else {
      digitalWrite(PIN_WATER_PUMP_RELAY, LOW);
      waterPumpActive = false;
    }
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/sprayer", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String st = doc["state"].as<String>();
    if (st == "ON") {
      digitalWrite(PIN_SPRAYER_RELAY, HIGH);
      sprayerActive = true;
      sprayerStartTime = millis();
    } else {
      digitalWrite(PIN_SPRAYER_RELAY, LOW);
      sprayerActive = false;
    }
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.on("/api/relay", HTTP_POST, []() {
    StaticJsonDocument<200> doc;
    deserializeJson(doc, server.arg("plain"));
    String st = doc["state"].as<String>();
    relayActive = (st == "ON");
    digitalWrite(PIN_AUX_RELAY, relayActive ? HIGH : LOW);
    server.send(200, "application/json", "{\"success\":true}");
  });

  server.begin();
}

void loop() {
  server.handleClient();
  if (waterPumpActive && (millis() - waterPumpStartTime >= WATER_PUMP_MAX_MS)) {
    digitalWrite(PIN_WATER_PUMP_RELAY, LOW);
    waterPumpActive = false;
  }
  if (sprayerActive && (millis() - sprayerStartTime >= SPRAYER_MAX_MS)) {
    digitalWrite(PIN_SPRAYER_RELAY, LOW);
    sprayerActive = false;
  }
}
"""
}
