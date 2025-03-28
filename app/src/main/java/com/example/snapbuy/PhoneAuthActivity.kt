package com.example.snapbuy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class PhoneAuthActivity : AppCompatActivity() {
    private lateinit var phoneNumberEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var verifyOtpButton: Button

    private var generatedOtp: String = ""
    private var enteredPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        otpEditText = findViewById(R.id.otpEditText)
        sendOtpButton = findViewById(R.id.sendOtpButton)
        verifyOtpButton = findViewById(R.id.verifyOtpButton)

        sendOtpButton.setOnClickListener {
            enteredPhoneNumber = phoneNumberEditText.text.toString().trim()
            if (enteredPhoneNumber.isNotEmpty()) {
                generatedOtp = (100000..999999).random().toString() // Generate a 6-digit OTP
                sendOtp(enteredPhoneNumber, generatedOtp)
            } else {
                Toast.makeText(this, "Enter a valid phone number!", Toast.LENGTH_SHORT).show()
            }
        }

        verifyOtpButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()
            if (otp == generatedOtp) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtp(phoneNumber: String, otp: String) {
        val client = OkHttpClient()
        val url = "https://www.fast2sms.com/dev/bulkV2"

        val jsonBody = JSONObject().apply {
            put("authorization", "4jUqRgHnKwzDLL9A0OLz9QBGgiygT4yj0ZI9wdaotF05PCUQnKWog2P9dtZy") // Store API key securely!
            put("route", "q")
            put("message", "Your OTP for SnapBuy is: $otp")
            put("numbers", phoneNumber)
            put("flash", "0")
        }

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        otpEditText.visibility = View.VISIBLE
                        verifyOtpButton.visibility = View.VISIBLE
                        Toast.makeText(this@PhoneAuthActivity, "OTP Sent!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PhoneAuthActivity, "Error sending OTP!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                Log.e("Fast2SMS", "Failed to send OTP: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PhoneAuthActivity, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
