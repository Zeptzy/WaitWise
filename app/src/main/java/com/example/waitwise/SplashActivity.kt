package com.example.waitwise

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity
 * First screen that launches. Checks if a valid session token
 * exists and routes the user accordingly:
 *   - Token exists → go straight to DashboardActivity
 *   - No token     → go to MainActivity (Login)
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)

        // Small delay so splash screen is visible (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            if (session.isLoggedIn() && session.getToken() != null) {
                // Token exists — skip login, go to Dashboard
                navigateTo(DashboardActivity::class.java)
            } else {
                // No token — go to Login
                navigateTo(MainActivity::class.java)
            }
        }, 1500)
    }

    private fun navigateTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish() // remove splash from back stack
    }
}