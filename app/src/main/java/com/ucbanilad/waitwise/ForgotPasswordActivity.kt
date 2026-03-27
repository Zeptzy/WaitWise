package com.ucbanilad.waitwise

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnReset: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackToLogin: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        btnReset = findViewById(R.id.btnReset)
        progressBar = findViewById(R.id.progressBar)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnReset.setOnClickListener {
            resetPassword()
        }


        tvBackToLogin.setOnClickListener {
            finish()
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Goes back to login screen
        }

    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        btnReset.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = android.view.View.GONE
                btnReset.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}