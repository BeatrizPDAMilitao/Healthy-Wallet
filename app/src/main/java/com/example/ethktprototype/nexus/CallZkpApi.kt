package com.example.ethktprototype.nexus

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.ZonedDateTime

object CallZkpApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun sendValue(
        value: Int,
        min: Int,
        max: Int,
        timestamp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val timestampSec = ZonedDateTime.parse(timestamp)
            .toEpochSecond()

        val json = JSONObject().apply {
            put("value", value)
            put("min", min)
            put("max", max)
            put("timestamp", timestampSec)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://192.168.1.76:3000/prove")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onError("Server error: ${response.code}")
                        return
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        onSuccess(responseBody)
                    } else {
                        onError("Empty response")
                    }
                }
            }
        })
    }
}