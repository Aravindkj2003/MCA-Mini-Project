package com.example.automutefinal

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    /**
     * Called when the activity is first created.
     * Responsibilities:
     * 1. Sets the layout for the Options screen
     * 2. Checks and requests Do Not Disturb permission if not granted
     * 3. Sets up buttons to navigate to SelectTimerActivity and LocationListActivity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        checkAndRequestDndPermission()

        val btnSetTimer = findViewById<Button>(R.id.button)
        val btnSetLocation = findViewById<Button>(R.id.button3)

        // Navigate to SelectTimerActivity when "Set Timer" button is clicked
        btnSetTimer.setOnClickListener {
            val intent = Intent(this, SelectTimerActivity::class.java)
            startActivity(intent)
        }

        // Navigate to LocationListActivity when "Set Location" button is clicked
        btnSetLocation.setOnClickListener {
            val intent = Intent(this, LocationListActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Checks if Do Not Disturb (DND) permission is granted.
     * If not, directs the user to the system settings page to grant it.
     * DND permission is required to control the phone's interruption filter for AutoMute functionality.
     */
    private fun checkAndRequestDndPermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            Toast.makeText(this, "Please grant Do Not Disturb permission to AutoMute", Toast.LENGTH_LONG).show()
            startActivity(intent)
        }
    }
}
