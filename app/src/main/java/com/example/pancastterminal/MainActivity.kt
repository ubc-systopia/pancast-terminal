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
                val list : ArrayList<Encounter> = ArrayList()
                val test: Encounter = Encounter()
                test.beaconId = IntegerContainer.make(12345678)
                test.beaconTime = IntegerContainer.make(1000)
                test.dongleTime = IntegerContainer.make(2000)
                test.locationId = IntegerContainer.make(123456789)
                test.ephId = "NTHU0YVCHgY2Amp8FAA="
                list.add(test)
                val test2: Encounter = Encounter()
                test2.beaconId = IntegerContainer.make("4294967295")
                test2.beaconTime = IntegerContainer.make("4294966095")
                test2.dongleTime = IntegerContainer.make("4294807295")
                test2.locationId = IntegerContainer("18446744073709551615")
                test2.ephId = "NTHU0YVCHgY2Amp8FAA="
                list.add(test2)
                val response: String? = MainActivity.Companion.makeRequest(list);
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
    public fun makeRequest(encounters: List<Encounter>): String? {
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
        val body: String = Util.makeUploadRequest(encounters)
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