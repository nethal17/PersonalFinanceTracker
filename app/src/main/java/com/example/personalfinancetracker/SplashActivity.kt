package com.example.personalfinancetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen") // Standard Activity, not using SplashScreen API for simplicity
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 2000 // 2 seconds delay
    private lateinit var repository: FinanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database)

        // Use a Handler to delay the transition to RegisterActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Start RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)

            // Finish SplashActivity so user can't navigate back to it
            finish()
        }, SPLASH_DELAY)
    }
} 