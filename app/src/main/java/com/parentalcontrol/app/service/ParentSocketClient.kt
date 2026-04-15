package com.parentalcontrol.app.service

import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class ParentSocketClient {

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    suspend fun connect(host: String, port: Int, code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port).apply {
                soTimeout = Constants.SOCKET_TIMEOUT_MS.toInt()
            }
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))

            writer?.write(SocketManager.createClientConnect(code))
            writer?.newLine()
            writer?.flush()

            val response = reader?.readLine().orEmpty()
            if (response.isBlank()) {
                Result.failure(IllegalStateException("Empty server response"))
            } else {
                val json = SocketManager.parse(response)
                val type = json.optString("type")
                if (type == Constants.MSG_SERVER_OK) {
                    Result.success(json.optString("child_name", "Child"))
                } else {
                    Result.failure(IllegalStateException(json.optString("message", "Connection failed")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) {
        } finally {
            reader = null
            writer = null
            socket = null
        }
    }
}
