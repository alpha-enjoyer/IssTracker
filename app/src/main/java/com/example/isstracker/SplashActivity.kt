package com.example.isstracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        val splashScreenDuration = 3000L

        Thread(Runnable {
            Thread.sleep(splashScreenDuration)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }).start()
    }
}