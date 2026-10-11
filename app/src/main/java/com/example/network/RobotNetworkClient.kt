package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class RobotNetworkClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val esp1StatusAdapter = moshi.adapter(Esp1Status::class.java)
    private val esp2StatusAdapter = moshi.adapter(Esp2Status::class.java)
    private val modeRequestAdapter = moshi.adapter(ModeRequest::class.java)
    private val moveRequestAdapter = moshi.adapter(MoveRequest::class.java)
    private val speedRequestAdapter = moshi.adapter(SpeedRequest::class.java)
    private val servoRequestAdapter = moshi.adapter(ServoRequest::class.java)
    private val cutterRequestAdapter = moshi.adapter(CutterRequest::class.java)
    private val waterPumpRequestAdapter = moshi.adapter(WaterPumpRequest::class.java)
    private val sprayerRequestAdapter = moshi.adapter(SprayerRequest::class.java)
    private val relayRequestAdapter = moshi.adapter(RelayRequest::class.java)
    private val genericResponseAdapter = moshi.adapter(GenericResponse::class.java)

    private fun buildOkHttpClient(timeoutMs: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    private fun addAuthHeader(builder: Request.Builder, token: String) {
        if (token.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
            builder.addHeader("X-Robot-Token", token)
        }
    }

    private fun formatError(e: Throwable): String {
        return when (e) {
            is UnknownHostException -> "Host unreachable. Verify Wi-Fi and IP."
            is ConnectException -> "Connection refused. ESP32 server may be offline."
            is SocketTimeoutException -> "Request timed out. Device not responding."
            else -> e.message ?: "Unknown communication failure"
        }
    }

    // --- ESP32 #1 Calls ---

    suspend fun getEsp1Status(settings: AppSettings): Result<Pair<Esp1Status, Long>> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/status"
        val requestBuilder = Request.Builder().url(url).get()
        addAuthHeader(requestBuilder, settings.authToken)

        val startTime = System.currentTimeMillis()
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val ping = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/status)"))
                    }
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: ""
                val parsed = esp1StatusAdapter.fromJson(bodyString)
                    ?: return@withContext Result.failure(IOException("Invalid JSON payload from ESP32 #1"))
                Result.success(Pair(parsed, ping))
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setMode(settings: AppSettings, mode: RobotMode): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/mode"
        val json = modeRequestAdapter.toJson(ModeRequest(mode.name))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/mode)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Mode updated to ${mode.name}")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun sendMove(settings: AppSettings, command: MotorCommand): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/move"
        val json = moveRequestAdapter.toJson(MoveRequest(command.name))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/move)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Movement $command executed")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setSpeed(settings: AppSettings, speed: SpeedLevel): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/speed"
        val json = speedRequestAdapter.toJson(SpeedRequest(speed.name))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/speed)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Speed updated to ${speed.name}")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun emergencyStop(settings: AppSettings): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/stop"
        val body = "{}".toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/stop)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Emergency stop issued")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setServo(settings: AppSettings, target: String, angle: Int): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/servo"
        val json = servoRequestAdapter.toJson(ServoRequest(target, angle))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/servo)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Servo $target angle set to $angle")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setCutter(settings: AppSettings, state: String, maxSec: Int = 7): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp1Ip}:${settings.esp1Port}/api/cutter"
        val json = cutterRequestAdapter.toJson(CutterRequest(state = state, maxSec = maxSec))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #1 (/api/cutter)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Cutter $state command received")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    // --- ESP32 #2 Calls ---

    suspend fun getEsp2Status(settings: AppSettings): Result<Pair<Esp2Status, Long>> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/status"
        val requestBuilder = Request.Builder().url(url).get()
        addAuthHeader(requestBuilder, settings.authToken)

        val startTime = System.currentTimeMillis()
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val ping = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/status)"))
                    }
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: ""
                val parsed = esp2StatusAdapter.fromJson(bodyString)
                    ?: return@withContext Result.failure(IOException("Invalid JSON payload from ESP32 #2"))
                Result.success(Pair(parsed, ping))
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun startSoilTest(settings: AppSettings): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/soil-test/start"
        val body = "{}".toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/soil-test/start)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Soil test initiated")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun cancelSoilTest(settings: AppSettings): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/soil-test/cancel"
        val body = "{}".toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/soil-test/cancel)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Soil test cancelled")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setWaterPump(settings: AppSettings, state: String, durationSec: Int = 5): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/water-pump"
        val json = waterPumpRequestAdapter.toJson(WaterPumpRequest(state, durationSec))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/water-pump)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Water pump $state")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setSprayer(settings: AppSettings, state: String, durationSec: Int = 10): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/sprayer"
        val json = sprayerRequestAdapter.toJson(SprayerRequest(state, durationSec))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/sprayer)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Sprayer $state")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun setRelay(settings: AppSettings, state: String): Result<GenericResponse> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        val url = "http://${settings.esp2Ip}:${settings.esp2Port}/api/relay"
        val json = relayRequestAdapter.toJson(RelayRequest(state))
        val body = json.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder().url(url).post(body)
        addAuthHeader(requestBuilder, settings.authToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) return@withContext Result.failure(IOException("Endpoint not implemented on ESP32 #2 (/api/relay)"))
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val bodyString = response.body?.string() ?: "{}"
                val parsed = genericResponseAdapter.fromJson(bodyString) ?: GenericResponse(true, "Relay $state")
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    // --- ESP32-CAM Calls ---

    suspend fun testCamConnection(settings: AppSettings): Result<Long> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs)
        // Check capture or stream path
        val url = "http://${settings.camIp}:${settings.camPort}${settings.camCapturePath}"
        val request = Request.Builder().url(url).head().build()
        val startTime = System.currentTimeMillis()
        try {
            client.newCall(request).execute().use { response ->
                val ping = System.currentTimeMillis() - startTime
                if (response.isSuccessful || response.code == 405) { // HEAD might return 405 Method Not Allowed if only GET is supported, which still proves connection!
                    Result.success(ping)
                } else if (response.code == 404) {
                    Result.failure(IOException("ESP32-CAM reached, but path ${settings.camCapturePath} returned 404"))
                } else {
                    Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }

    suspend fun captureCamFrame(settings: AppSettings): Result<Bitmap> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(settings.httpTimeoutMs * 2)
        val url = "http://${settings.camIp}:${settings.camPort}${settings.camCapturePath}"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code} from ESP32-CAM capture"))
                }
                val bytes = response.body?.bytes()
                    ?: return@withContext Result.failure(IOException("Empty frame response from ESP32-CAM"))
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext Result.failure(IOException("Failed to decode JPEG frame from camera"))
                Result.success(bitmap)
            }
        } catch (e: Exception) {
            Result.failure(IOException(formatError(e), e))
        }
    }
}
