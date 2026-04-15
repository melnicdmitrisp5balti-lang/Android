package com.parentalcontrol.app.service

import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class ParentSocketClient {

    companion object {
        suspend fun connectOnce(host: String, port: Int, code: String): Result<String> =
            withContext(Dispatchers.IO) {
                var socket: Socket? = null
                try {
                    socket = Socket().apply {
                        connect(
                            InetSocketAddress(host, port),
                            Constants.SOCKET_CONNECT_TIMEOUT_MS
                        )
                        soTimeout = Constants.SOCKET_HANDSHAKE_TIMEOUT_MS
                    }
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                    writer.write(SocketManager.createClientConnect(code))
                    writer.newLine()
                    writer.flush()

                    val response = reader.readLine().orEmpty()
                    if (response.isBlank()) {
                        Result.failure(IllegalStateException("Empty server response"))
                    } else {
                        val json = SocketManager.parse(response)
                        val type = json.optString("type")
                        if (type == Constants.MSG_SERVER_OK) {
                            Result.success(json.optString("child_name", "Child"))
                        } else {
                            Result.failure(
                                IllegalStateException(
                                    json.optString("message", "Connection failed")
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    runCatching { socket?.close() }
                }
            }
    }

    suspend fun connect(host: String, port: Int, code: String): Result<String> =
        connectOnce(host, port, code)

    suspend fun requestChildCameraStatus(host: String, port: Int, code: String): Result<String> =
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket().apply {
                    connect(
                        InetSocketAddress(host, port),
                        Constants.SOCKET_CONNECT_TIMEOUT_MS
                    )
                    soTimeout = Constants.SOCKET_HANDSHAKE_TIMEOUT_MS
                }
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                writer.write(SocketManager.createVideoStreamRequest(code))
                writer.newLine()
                writer.flush()

                val response = reader.readLine().orEmpty()
                if (response.isBlank()) {
                    Result.failure(IllegalStateException("Empty server response"))
                } else {
                    val json = SocketManager.parse(response)
                    val type = json.optString("type")
                    if (type == Constants.MSG_SERVER_OK && json.optString("stream_source") == "child_camera") {
                        Result.success(json.optString("child_name", "Child"))
                    } else {
                        Result.failure(
                            IllegalStateException(
                                json.optString("message", "Child camera is unavailable")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                runCatching { socket?.close() }
            }
        }

    @Deprecated("No persistent socket is kept; use connectOnce/connect for each request.")
    fun disconnect() = Unit
}
