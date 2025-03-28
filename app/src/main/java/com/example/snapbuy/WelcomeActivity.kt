package com.example.snapbuy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already logged in
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // If user is logged in, go directly to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return  // Stop further execution
        }

        setContentView(R.layout.activity_welcome)

        // Initialize UI elements
        val signUpButton: Button = findViewById(R.id.signUpButton)
        val signInText: TextView = findViewById(R.id.signInText)

        // Navigate to SignUpActivity when Sign Up button is clicked
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Navigate to LoginActivity when Sign In text is clicked
        signInText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
