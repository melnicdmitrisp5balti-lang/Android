package com.parentalcontrol.app.utils

import org.json.JSONObject

object SocketManager {

    fun createClientConnect(code: String): String = JSONObject()
        .put("type", Constants.MSG_CLIENT_CONNECT)
        .put("code", code)
        .toString()

    fun createVideoStreamRequest(code: String): String = JSONObject()
        .put("type", Constants.MSG_VIDEO_STREAM)
        .put("code", code)
        .toString()

    fun createServerOk(childName: String): String = JSONObject()
        .put("type", Constants.MSG_SERVER_OK)
        .put("status", "connected")
        .put("child_name", childName)
        .toString()

    fun createServerError(message: String): String = JSONObject()
        .put("type", Constants.MSG_SERVER_ERROR)
        .put("status", "error")
        .put("message", message)
        .toString()

    fun createVideoStreamStatus(childName: String): String = JSONObject()
        .put("type", Constants.MSG_SERVER_OK)
        .put("status", "stream_ready")
        .put("child_name", childName)
        .put("stream_source", "child_camera")
        .toString()

    fun parse(raw: String): JSONObject = JSONObject(raw)
}
