package com.ucbanilad.waitwise

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * SplashActivity
 * First screen that launches. Checks if a valid session token
 * exists and routes the user accordingly:
 *   - User logged in → go straight to DashboardActivity
 *   - No user logged in → go to MainActivity (Login)
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Small delay so splash screen is visible (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if user is logged in via Firebase Auth
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // User is logged in — go to Dashboard
                navigateTo(DashboardActivity::class.java)
            } else {
                // No user — go to Login
                navigateTo(MainActivity::class.java)
            }

        }, 1500)
    }

    private fun navigateTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish() // remove splash from back stack
    }
}