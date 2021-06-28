package com.example.pancastterminal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sendBtn: Button = findViewById(R.id.sendBtn)
        // Listener for the send request button
        sendBtn.setOnClickListener(View.OnClickListener {
            thread(start = true) {
                val response: String? = MainActivity.Companion.makeRequest(
                    "NTHU0YVCHgY2Amp8FAA=",
                    Integer(0), Integer(0),
                    Integer(1000), Integer(2000)
                );
                if (response != null) {
                    Log.d("REQ", response)
                }
            }
        })

        val scanBtn: Button = findViewById(R.id.scanBtn)
        scanBtn.setOnClickListener(View.OnClickListener {
            thread(start = true) {
                Log.d("MAIN", "Click")
                val intent = Intent(this, ScanActivity::class.java)
                startActivity(intent)
            }
        });
    }

    companion object  {
    public fun makeRequest(
        ephId: String,
        dongleTime: Integer,
        beaconTime: Integer,
        beaconId: Integer,
        locationId: Integer
    ): String? {
        /*
            Makes a request to a static URL. Uses a trust manager that accepts literally every
            single certificate (INSECURE, vuln. to MITM attacks).

            Placeholder while I try to figure out how to ONLY accept Pancast server's certificate
            (and optionally more certs, if the need arises)

            Please don't do MITM
         */
        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")
        val myTrustManagerArray: Array<TrustManager> = arrayOf<TrustManager>(NaiveTrustManager())
        sslContext.init(null, myTrustManagerArray, SecureRandom())
        val trustManager: NaiveTrustManager = NaiveTrustManager()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
        val url = "${Constants.WEB_PROTOCOL}://${Constants.BACKEND_ADDR}:${Constants.BACKEND_PORT}/upload"
        val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
        //val body: String = "{\"Entries\":[{\"EphemeralID\":\"deadbeefdeadbee\",\"DongleClock\":0,\"BeaconClock\":0,\"BeaconID\":1,\"LocationID\":\"LOC00001\"}],\"Type\":0}"
        val body: String = String.format("{"            +
                    "\"Entries\": ["                    +
                        "{"                             +
                            "\"EphemeralID\": \"%s\","  +
                            "\"DongleClock\": %d,"      +
                            "\"BeaconClock\": %d,"      +
                            "\"BeaconId\":    %d,"      +
                            "\"LocationID\":  %d"       +
                        "}"                             +
                    "],"                                +
                    "\"Type\": 0"                       +
                "}", ephId, dongleTime, beaconTime, beaconId, locationId);
        Log.d("REQUEST", body);
        val reqBody: RequestBody = body.toRequestBody(contentType)
        val request: Request = Request.Builder()
            .url(url)
            .post(reqBody)
            .build()

        client.newCall(request).execute().use { response -> return response.body?.string() }
    }
        }


}