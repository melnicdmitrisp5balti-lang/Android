package com.parentalcontrol.app.cloud

import android.util.Log
import com.parentalcontrol.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * High-level Firebase management facade that wraps [CloudSignalingClient].
 *
 * Provides a clean API for:
 * – Device registration (child creates / refreshes its record in Firebase)
 * – Device discovery  (parent looks up a 6-digit code)
 * – Real-time presence updates (heartbeat so the parent knows the child is online)
 * – Session lifecycle (create / close sessions, clear stale signaling data)
 *
 * All network calls are dispatched on [Dispatchers.IO].
 * Configure [Constants.FIREBASE_DATABASE_URL] with your project's URL before use.
 */
class FirebaseConnectionManager {

    companion object {
        private const val TAG = "FirebaseConnMgr"
        private val JSON = "application/json".toMediaType()
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }

    private val signaling = CloudSignalingClient()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val base get() = Constants.FIREBASE_DATABASE_URL

    // -------------------------------------------------------------------------
    // Child-side: registration & heartbeat
    // -------------------------------------------------------------------------

    /**
     * Register (or refresh) a child device in Firebase under [code].
     * Returns the [CloudDeviceInfo] written on success, or null on failure.
     */
    suspend fun registerDevice(
        code: String,
        deviceName: String,
        deviceId: String
    ): CloudDeviceInfo? = withContext(Dispatchers.IO) {
        val ok = signaling.registerChildDevice(code, deviceName, deviceId)
        if (!ok) {
            Log.w(TAG, "registerDevice failed for code=$code")
            return@withContext null
        }
        CloudDeviceInfo(code = code, deviceName = deviceName, deviceId = deviceId, timestamp = System.currentTimeMillis())
    }

    /**
     * Post a heartbeat timestamp so the parent can see the child is still online.
     * Returns true on success.
     */
    suspend fun postHeartbeat(code: String): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = System.currentTimeMillis().toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("$base/devices/$code/lastSeen.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "postHeartbeat failed: ${e.message}")
            false
        }
    }

    /**
     * Mark a device as inactive (e.g. when regenerating the code).
     */
    suspend fun deactivateDevice(code: String): Boolean =
        signaling.deactivateDevice(code)

    // -------------------------------------------------------------------------
    // Parent-side: device discovery
    // -------------------------------------------------------------------------

    /**
     * Look up a 6-digit [code] and return the registered child's info,
     * or null if the code is not found / inactive.
     */
    suspend fun findDevice(code: String): CloudDeviceInfo? =
        signaling.lookupDevice(code)

    /**
     * Returns true if the child device registered under [code] has sent a
     * heartbeat within the last [thresholdMs] milliseconds.
     */
    suspend fun isDeviceOnline(code: String, thresholdMs: Long = 60_000L): Boolean =
        withContext(Dispatchers.IO) {
            if (base.isBlank()) return@withContext false
            try {
                val req = Request.Builder()
                    .url("$base/devices/$code/lastSeen.json")
                    .get()
                    .build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") return@withContext false
                    val lastSeen = body.trim('"').toLongOrNull() ?: return@withContext false
                    (System.currentTimeMillis() - lastSeen) < thresholdMs
                }
            } catch (e: Exception) {
                Log.e(TAG, "isDeviceOnline check failed: ${e.message}")
                false
            }
        }

    // -------------------------------------------------------------------------
    // Signaling helpers (delegates to CloudSignalingClient)
    // -------------------------------------------------------------------------

    suspend fun postOffer(code: String, sdp: String) = signaling.postOffer(code, sdp)
    suspend fun getOffer(code: String) = signaling.getOffer(code)
    suspend fun postAnswer(code: String, sdp: String) = signaling.postAnswer(code, sdp)
    suspend fun getAnswer(code: String) = signaling.getAnswer(code)
    suspend fun postIceCandidate(code: String, side: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int) =
        signaling.postIceCandidate(code, side, candidate, sdpMid, sdpMLineIndex)
    suspend fun getIceCandidates(code: String, side: String) =
        signaling.getIceCandidates(code, side)

    /** Clear all signaling data for [code] after a session ends. */
    suspend fun clearSession(code: String): Boolean = signaling.clearSignaling(code)

    // -------------------------------------------------------------------------
    // Session schema helper — writes a session record to Firestore-style path
    // -------------------------------------------------------------------------

    /**
     * Create a session record in Firebase under `/sessions/{code}`.
     * Useful for audit / activity log purposes.
     */
    suspend fun createSessionRecord(
        code: String,
        parentDeviceId: String,
        childDeviceName: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = JSONObject().apply {
                put("code", code)
                put("parentDeviceId", parentDeviceId)
                put("childDeviceName", childDeviceName)
                put("startedAt", System.currentTimeMillis())
                put("active", true)
            }.toString().toRequestBody(JSON)

            val req = Request.Builder()
                .url("$base/sessions/$code.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "createSessionRecord failed: ${e.message}")
            false
        }
    }
}
