package com.example.automutefinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SelectTimerActivity : AppCompatActivity() {

    /**
     * Called when the activity is first created.
     * Responsibilities:
     * 1. Sets the layout for selecting timer type.
     * 2. Provides two options for the user:
     *    - Quick Timer: navigates to QuickTimerActivity.
     *    - Daily Timer: navigates to DailyTimerListActivity.
     * 3. Sets up button click listeners for both timer options.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_timer)

        val quickButton = findViewById<Button>(R.id.quickTimerButton)

        // Navigate to QuickTimerActivity when quick timer button is clicked
        quickButton.setOnClickListener {
            val intent = Intent(this, QuickTimerActivity::class.java)
            startActivity(intent)
        }

        val dailyButton = findViewById<Button>(R.id.dailyTimerButton)

        // Navigate to DailyTimerListActivity when daily timer button is clicked
        dailyButton.setOnClickListener {
            val intent = Intent(this, DailyTimerListActivity::class.java)
            startActivity(intent)
        }
    }
}
