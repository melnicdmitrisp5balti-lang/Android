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

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected
    private val _connectionEvent = MutableLiveData(0L)
    val connectionEvent: LiveData<Long> = _connectionEvent

    fun validateCode(code: String): Boolean = Regex("^\\d{6}$").matches(code)

    fun connect(code: String, host: String? = null, port: Int = Constants.DEFAULT_SOCKET_PORT) {
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

            _status.value = if (host.isNullOrBlank()) {
                "Ищем устройство ребёнка..."
            } else {
                "Подключаемся к устройству ребёнка через интернет..."
            }
            val result = connectToChild(code, host, port)
            if (result != null) {
                val (connectedHost, childName) = result
                _status.value = "Подключено к: $childName"
                _connected.value = true
                _connectionEvent.value = System.currentTimeMillis()
                prefs.saveLastChildHost(connectedHost)
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
            } else {
                _status.value = "Не удалось найти устройство по коду. Убедитесь, что ребёнок открыл режим мониторинга"
                _connected.value = false
            }
        }
    }

    private suspend fun connectToChild(code: String, host: String?, port: Int): Pair<String, String>? {
        val directHosts = buildDirectCandidates(host)
        for (candidateHost in directHosts) {
            val result = ParentSocketClient.connectOnce(candidateHost, port, code)
            if (result.isSuccess) {
                return candidateHost to result.getOrNull().orEmpty()
            }
        }

        if (!host.isNullOrBlank()) return null

        _status.postValue("Проверяем устройства в локальной сети...")
        val localCandidates = buildLocalNetworkCandidates()
            .filterNot { directHosts.contains(it) }
            .distinct()

        if (localCandidates.isEmpty()) return null

        val chunks = localCandidates.chunked(Constants.SOCKET_SCAN_CHUNK_SIZE)
        for (chunk in chunks) {
            val success = coroutineScope {
                val attempts = chunk.map { host ->
                    async(Dispatchers.IO) {
                        host to ParentSocketClient.connectOnce(host, port, code)
                    }
                }
                var found: Pair<String, String>? = null
                for (deferred in attempts) {
                    val (host, result) = deferred.await()
                    if (result.isSuccess && found == null) {
                        found = host to result.getOrNull().orEmpty()
                    }
                }
                found
            }
            if (success != null) {
                return success
            }
        }
        return null
    }

    private fun buildDirectCandidates(host: String?): List<String> {
        val candidates = mutableListOf<String>()
        host?.takeIf { it.isNotBlank() }?.let(candidates::add)
        prefs.getLastChildHost()?.takeIf { it.isNotBlank() }?.let(candidates::add)
        candidates.add("10.0.2.2")
        candidates.add("127.0.0.1")
        return candidates.distinct()
    }

    private fun buildLocalNetworkCandidates(): List<String> {
        val localIpv4 = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress.orEmpty() }
                .filter { it.isNotBlank() && it != "127.0.0.1" }
        }.getOrDefault(emptyList())

        val ownAddresses = localIpv4.toSet()
        return localIpv4
            .mapNotNull { ip ->
                val parts = ip.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else null
            }
            .distinct()
            .flatMap { prefix ->
                (Constants.SOCKET_SCAN_HOST_MIN..Constants.SOCKET_SCAN_HOST_MAX)
                    .map { lastOctet -> "$prefix.$lastOctet" }
            }
            .filterNot { ownAddresses.contains(it) }
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
