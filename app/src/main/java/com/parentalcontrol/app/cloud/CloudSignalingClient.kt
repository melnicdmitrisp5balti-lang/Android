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
 * Firebase Realtime Database REST API client for cloud-based signaling.
 *
 * Child devices register their 6-digit code and device info so that
 * parents can discover them from anywhere over the internet using only
 * the code — no IP address required.
 *
 * The same client is also used to exchange WebRTC offer/answer SDPs
 * and ICE candidates when establishing a P2P media connection.
 *
 * Configure [Constants.FIREBASE_DATABASE_URL] with your Firebase project's
 * Realtime Database URL (e.g. "https://my-project-default-rtdb.firebaseio.com").
 */
class CloudSignalingClient {

    companion object {
        private const val TAG = "CloudSignaling"
        private val JSON = "application/json".toMediaType()
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val base get() = Constants.FIREBASE_DATABASE_URL

    // -------------------------------------------------------------------------
    // Device registration (child side)
    // -------------------------------------------------------------------------

    /**
     * Register (or refresh) this child device under [code] in Firebase.
     * Returns true on success, false if the Firebase URL is not configured
     * or the request fails.
     */
    suspend fun registerChildDevice(
        code: String,
        deviceName: String,
        deviceId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) {
            Log.w(TAG, "FIREBASE_DATABASE_URL not configured – skipping cloud registration")
            return@withContext false
        }
        try {
            val body = JSONObject().apply {
                put("code", code)
                put("deviceName", deviceName)
                put("deviceId", deviceId)
                put("timestamp", System.currentTimeMillis())
                put("active", true)
            }.toString().toRequestBody(JSON)

            val req = Request.Builder()
                .url("$base/devices/$code.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "registerChildDevice failed: ${e.message}")
            false
        }
    }

    /**
     * Mark a previously-registered code as inactive (e.g. when regenerating).
     */
    suspend fun deactivateDevice(code: String): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = "false".toRequestBody(JSON)
            val req = Request.Builder()
                .url("$base/devices/$code/active.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "deactivateDevice failed: ${e.message}")
            false
        }
    }

    // -------------------------------------------------------------------------
    // Device discovery (parent side)
    // -------------------------------------------------------------------------

    /**
     * Look up a child device by its 6-digit [code].
     * Returns [CloudDeviceInfo] when the code is valid and the device is active,
     * or null if not found / Firebase not configured.
     */
    suspend fun lookupDevice(code: String): CloudDeviceInfo? = withContext(Dispatchers.IO) {
        if (base.isBlank()) {
            Log.w(TAG, "FIREBASE_DATABASE_URL not configured – skipping cloud lookup")
            return@withContext null
        }
        try {
            val req = Request.Builder()
                .url("$base/devices/$code.json")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") return@withContext null
                val json = JSONObject(body)
                if (!json.optBoolean("active", false)) return@withContext null
                CloudDeviceInfo(
                    code = json.optString("code", code),
                    deviceName = json.optString("deviceName", "Child"),
                    deviceId = json.optString("deviceId", ""),
                    timestamp = json.optLong("timestamp", 0L)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "lookupDevice failed: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------------------
    // WebRTC signaling — offer / answer / ICE
    // -------------------------------------------------------------------------

    /** Child posts its WebRTC offer SDP so the parent can retrieve it. */
    suspend fun postOffer(code: String, sdpOffer: String): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = JSONObject().apply {
                put("type", "offer")
                put("sdp", sdpOffer)
                put("timestamp", System.currentTimeMillis())
            }.toString().toRequestBody(JSON)

            val req = Request.Builder()
                .url("$base/signaling/$code/offer.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "postOffer failed: ${e.message}")
            false
        }
    }

    /** Parent retrieves the child's WebRTC offer SDP. Returns null on failure. */
    suspend fun getOffer(code: String): String? = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url("$base/signaling/$code/offer.json")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") return@withContext null
                JSONObject(body).optString("sdp", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getOffer failed: ${e.message}")
            null
        }
    }

    /** Parent posts its WebRTC answer SDP so the child can retrieve it. */
    suspend fun postAnswer(code: String, sdpAnswer: String): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = JSONObject().apply {
                put("type", "answer")
                put("sdp", sdpAnswer)
                put("timestamp", System.currentTimeMillis())
            }.toString().toRequestBody(JSON)

            val req = Request.Builder()
                .url("$base/signaling/$code/answer.json")
                .put(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "postAnswer failed: ${e.message}")
            false
        }
    }

    /** Child retrieves the parent's WebRTC answer SDP. Returns null on failure. */
    suspend fun getAnswer(code: String): String? = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url("$base/signaling/$code/answer.json")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") return@withContext null
                JSONObject(body).optString("sdp", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAnswer failed: ${e.message}")
            null
        }
    }

    /**
     * Post an ICE candidate from [side] ("child" or "parent").
     * Candidates are appended under `/signaling/{code}/ice_{side}/`.
     */
    suspend fun postIceCandidate(
        code: String,
        side: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val body = JSONObject().apply {
                put("candidate", candidate)
                put("sdpMid", sdpMid ?: "")
                put("sdpMLineIndex", sdpMLineIndex)
                put("timestamp", System.currentTimeMillis())
            }.toString().toRequestBody(JSON)

            val req = Request.Builder()
                .url("$base/signaling/$code/ice_$side.json")
                .post(body)
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "postIceCandidate failed: ${e.message}")
            false
        }
    }

    /**
     * Fetch all ICE candidates posted by [side] ("child" or "parent").
     * Returns a list of JSON objects, each containing "candidate", "sdpMid",
     * and "sdpMLineIndex".
     */
    suspend fun getIceCandidates(code: String, side: String): List<JSONObject> =
        withContext(Dispatchers.IO) {
            if (base.isBlank()) return@withContext emptyList()
            try {
                val req = Request.Builder()
                    .url("$base/signaling/$code/ice_$side.json")
                    .get()
                    .build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") return@withContext emptyList()
                    val root = JSONObject(body)
                    root.keys().asSequence().mapNotNull { key ->
                        runCatching { root.getJSONObject(key) }.getOrNull()
                    }.toList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getIceCandidates failed: ${e.message}")
                emptyList()
            }
        }

    /** Remove all signaling data for [code] (call when session ends). */
    suspend fun clearSignaling(code: String): Boolean = withContext(Dispatchers.IO) {
        if (base.isBlank()) return@withContext false
        try {
            val req = Request.Builder()
                .url("$base/signaling/$code.json")
                .delete()
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "clearSignaling failed: ${e.message}")
            false
        }
    }
}

/** Lightweight data class representing a registered child device in Firebase. */
data class CloudDeviceInfo(
    val code: String,
    val deviceName: String,
    val deviceId: String,
    val timestamp: Long
)
