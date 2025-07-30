package com.example.ethktprototype.nexus

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.time.ZonedDateTime
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CallZkpApi {
    private lateinit var client: OkHttpClient

    fun init(context: Context) {
        client = getUnsafeClient(context)
    }

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
            .url("https://192.168.1.254:3000/prove")
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
    fun getUnsafeClient(context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream = context.assets.open("server.crt")
        val cert = certificateFactory.generateCertificate(inputStream)
        inputStream.close()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", cert)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Accept all hostnames (not recommended for production)
            .build()
    }
}