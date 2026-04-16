package com.parentalcontrol.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.cloud.CloudSignalingClient
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.SessionEntity
import com.parentalcontrol.app.service.ParentSocketClient
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val client = ParentSocketClient()
    private val sessionDao = AppDatabase.getInstance(appContext).sessionDao()
    private val prefs = PreferenceManager(appContext)
    private val cloudSignaling = CloudSignalingClient()

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected
    private val _connectionEvent = MutableLiveData(0L)
    val connectionEvent: LiveData<Long> = _connectionEvent

    fun validateCode(code: String): Boolean = Regex("^\\d{6}$").matches(code)

    /**
     * Connect to the child device identified by [code].
     *
     * Discovery order:
     * 1. Firebase cloud lookup (works across any network / internet).
     * 2. Last known host from preferences (same-LAN fast-path).
     * 3. Local-network scan (same-LAN fallback).
     *
     * An optional [host] can be supplied to skip cloud/scan and connect
     * directly to a known IP (useful for advanced / debug scenarios).
     */
    fun connect(code: String, host: String? = null) {
        viewModelScope.launch {
            _connected.value = false
            if (!validateCode(code)) {
                _status.value = "Код должен состоять из 6 цифр"
                return@launch
            }
            if (!hasInternetCapability()) {
                _status.value = "Включите интернет и попробуйте снова"
                return@launch
            }

            // --- 1. Direct host override (debug / advanced) ------------------
            if (!host.isNullOrBlank()) {
                _status.value = "Подключаемся напрямую…"
                val result = ParentSocketClient.connectOnce(host, Constants.DEFAULT_SOCKET_PORT, code)
                if (result.isSuccess) {
                    onConnected(host, result.getOrNull().orEmpty(), code)
                } else {
                    _status.value = "Не удалось подключиться по указанному адресу"
                    _connected.value = false
                }
                return@launch
            }

            // --- 2. Cloud lookup via Firebase ---------------------------------
            _status.value = "Ищем устройство ребёнка в облаке…"
            val cloudInfo = cloudSignaling.lookupDevice(code)
            if (cloudInfo != null) {
                // Device found in the cloud — connection is established at the
                // signaling level.  The actual media stream is negotiated via
                // WebRTC (signaling data is already in Firebase under /signaling/{code}).
                _status.value = "Подключено к: ${cloudInfo.deviceName}"
                _connected.value = true
                _connectionEvent.value = System.currentTimeMillis()
                prefs.saveLastConnectionCode(code)
                // Do NOT save an IP-based host for cloud connections; leave the
                // host blank so that CameraStreamActivity switches to cloud mode.
                prefs.saveLastChildHost("")
                sessionDao.insert(
                    SessionEntity(
                        childDeviceId = cloudInfo.deviceName,
                        parentDeviceId = Settings.Secure.getString(
                            appContext.contentResolver,
                            Settings.Secure.ANDROID_ID
                        ).orEmpty(),
                        connectionCode = code
                    )
                )
                return@launch
            }

            // --- 3. Same-LAN TCP scan (fallback when Firebase is not set up) --
            _status.value = "Ищем устройство в локальной сети…"
            val lanResult = connectToChildLan(code)
            if (lanResult != null) {
                val (connectedHost, childName) = lanResult
                onConnected(connectedHost, childName, code)
            } else {
                _status.value = "Не удалось найти устройство по коду. Убедитесь, что ребёнок открыл режим мониторинга"
                _connected.value = false
            }
        }
    }

    private fun onConnected(host: String, childName: String, code: String) {
        viewModelScope.launch {
            _status.value = "Подключено к: $childName"
            _connected.value = true
            _connectionEvent.value = System.currentTimeMillis()
            prefs.saveLastChildHost(host)
            prefs.saveLastConnectionCode(code)
            sessionDao.insert(
                SessionEntity(
                    childDeviceId = childName,
                    parentDeviceId = Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ).orEmpty(),
                    connectionCode = code
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // LAN scan helpers (kept as a fallback when Firebase is not configured)
    // -------------------------------------------------------------------------

    private suspend fun connectToChildLan(code: String): Pair<String, String>? {
        val candidates = buildLanCandidates()
        if (candidates.isEmpty()) return null

        val chunks = candidates.chunked(Constants.SOCKET_SCAN_CHUNK_SIZE)
        for (chunk in chunks) {
            val success = coroutineScope {
                val attempts = chunk.map { candidateHost ->
                    async(Dispatchers.IO) {
                        candidateHost to ParentSocketClient.connectOnce(
                            candidateHost,
                            Constants.DEFAULT_SOCKET_PORT,
                            code
                        )
                    }
                }
                var found: Pair<String, String>? = null
                for (deferred in attempts) {
                    val (candidateHost, result) = deferred.await()
                    if (result.isSuccess && found == null) {
                        found = candidateHost to result.getOrNull().orEmpty()
                    }
                }
                found
            }
            if (success != null) return success
        }
        return null
    }

    private fun buildLanCandidates(): List<String> {
        val localIpv4 = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress.orEmpty() }
                .filter { it.isNotBlank() && it != "127.0.0.1" }
        }.getOrDefault(emptyList())

        val ownAddresses = localIpv4.toSet()

        // Also include common emulator host addresses.
        val extra = listOf("10.0.2.2", "127.0.0.1")

        return (localIpv4
            .mapNotNull { ip ->
                val parts = ip.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else null
            }
            .distinct()
            .flatMap { prefix ->
                (Constants.SOCKET_SCAN_HOST_MIN..Constants.SOCKET_SCAN_HOST_MAX)
                    .map { lastOctet -> "$prefix.$lastOctet" }
            }
            .filterNot { ownAddresses.contains(it) } + extra)
            .distinct()
    }

    private fun hasInternetCapability(): Boolean {
        val manager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
