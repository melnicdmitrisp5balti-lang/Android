package com.parentalcontrol.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.SessionEntity
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class ChildSocketServer : Service() {

    companion object {
        private const val TAG = "ChildSocketServer"
        const val EXTRA_CODE = "extra_code"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var inactivityJob: Job? = null
    private var currentSessionId: Long? = null
    private var expectedCode: String = ""

    private val logRepository by lazy { ActivityLogRepository(applicationContext) }
    private val sessionDao by lazy { AppDatabase.getInstance(applicationContext).sessionDao() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        expectedCode = intent?.getStringExtra(EXTRA_CODE).orEmpty()
        if (expectedCode.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (serverSocket == null) {
            serviceScope.launch {
                runServer()
            }
        }
        return START_STICKY
    }

    private suspend fun runServer() {
        try {
            serverSocket = ServerSocket(Constants.DEFAULT_SOCKET_PORT)
            broadcastStatus("Ожидание подключения...")
            while (true) {
                val accepted = serverSocket?.accept() ?: break
                handleClient(accepted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server error: ${e.message}")
            broadcastStatus("Ошибка сервера")
        }
    }

    private suspend fun handleClient(socket: Socket) {
        clientSocket?.close()
        clientSocket = socket

        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        val raw = reader.readLine().orEmpty()
        if (raw.isBlank()) {
            writer.write(SocketManager.createServerError("Empty payload"))
            writer.newLine()
            writer.flush()
            socket.close()
            return
        }

        val request = SocketManager.parse(raw)
        val type = request.optString("type")
        val code = request.optString("code")
        val parentId = socket.inetAddress.hostAddress.orEmpty()

        logRepository.addLog(
            Constants.LOG_CONNECTION_ATTEMPT,
            "Parent connection attempt from $parentId with code $code"
        )

        if (type != Constants.MSG_CLIENT_CONNECT || code != expectedCode) {
            writer.write(SocketManager.createServerError("Invalid code"))
            writer.newLine()
            writer.flush()
            logRepository.addLog(Constants.LOG_CONNECTION_REJECTED, "Rejected parent $parentId")
            socket.close()
            return
        }

        val activeSession = sessionDao.getActiveByCode(code)
        if (activeSession != null) {
            writer.write(SocketManager.createServerError("Code is already in active session"))
            writer.newLine()
            writer.flush()
            logRepository.addLog(Constants.LOG_CONNECTION_REJECTED, "Code already in use")
            socket.close()
            return
        }

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        currentSessionId = sessionDao.insert(
            SessionEntity(
                childDeviceId = deviceId,
                parentDeviceId = parentId,
                connectionCode = code
            )
        )

        writer.write(SocketManager.createServerOk(android.os.Build.MODEL))
        writer.newLine()
        writer.flush()
        logRepository.addLog(Constants.LOG_CONNECTION_SUCCESS, "Parent connected from $parentId")
        broadcastStatus("Родитель подключен ✓")
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = serviceScope.launch {
            delay(Constants.SOCKET_TIMEOUT_MS)
            closeActiveSession()
            broadcastStatus("Ожидание подключения...")
        }
    }

    private suspend fun closeActiveSession() {
        currentSessionId?.let { sessionDao.closeSession(it, System.currentTimeMillis()) }
        currentSessionId = null
        try {
            clientSocket?.close()
        } catch (_: Exception) {
        }
        clientSocket = null
    }

    private fun broadcastStatus(status: String) {
        sendBroadcast(
            Intent(Constants.ACTION_CHILD_CONNECTION_STATUS).apply {
                putExtra(Constants.EXTRA_CONNECTION_STATUS, status)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            closeActiveSession()
        }
        inactivityJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
