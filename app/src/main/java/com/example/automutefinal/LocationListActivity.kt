package com.example.automutefinal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import android.content.Context
import android.media.AudioManager
import com.google.gson.reflect.TypeToken

class LocationListActivity : AppCompatActivity() {

    private lateinit var locationContainer: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var backButton: Button
    private lateinit var btnAddLocation: Button

    /**
     * Initializes the UI elements, sets up button click listeners
     * for adding new locations and navigating back.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_list)

        locationContainer = findViewById(R.id.locationContainer)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        backButton = findViewById(R.id.btnBack)
        btnAddLocation = findViewById(R.id.btnAddLocation)

        backButton.setOnClickListener {
            finish()
        }

        btnAddLocation.setOnClickListener {
            val intent = Intent(this, SetLocationActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Called when the activity resumes.
     * Loads and displays the list of saved locations and updates the UI state.
     */
    override fun onResume() {
        super.onResume()
        loadAndDisplayLocations()
        updateUI()
    }

    /**
     * Loads saved locations from SharedPreferences and displays each
     * location as a button. Supports long-press deletion.
     */
    private fun loadAndDisplayLocations() {
        locationContainer.removeAllViews()
        val sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("locations", null)
        val type = object : TypeToken<List<SavedLocation>>() {}.type

        val locations: List<SavedLocation> = if (json != null) gson.fromJson(json, type) else emptyList()

        locations.forEach { savedLocation ->
            val locationButton = Button(this).apply {
                text = savedLocation.name

                // Set button styling
                setBackgroundColor(Color.parseColor("#6BBBE7"))
                setTextColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }

                // Long press to delete the location
                setOnLongClickListener {
                    deleteLocation(savedLocation)
                    loadAndDisplayLocations()
                    updateUI()
                    Toast.makeText(this@LocationListActivity, "Location deleted.", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            locationContainer.addView(locationButton)
        }
    }

    /**
     * Deletes a saved location from SharedPreferences and stops the
     * location service if no locations remain.
     */
    /**
     * Deletes a saved location from SharedPreferences and instantly unmutes
     * the phone if it was muted by the app. Stops the location service
     * if no locations remain.
     */
    private fun deleteLocation(locationToRemove: SavedLocation) {
        val sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = sharedPreferences.getString("locations", null)
        val type = object : TypeToken<MutableList<SavedLocation>>() {}.type
        val locationsList: MutableList<SavedLocation> = if (json != null) gson.fromJson(json, type) else mutableListOf()

        // Remove the specific location from the list
        locationsList.removeAll { it.name == locationToRemove.name && it.latitude == locationToRemove.latitude && it.longitude == locationToRemove.longitude }

        // --- NEW LOGIC TO INSTANTLY UNMUTE ---
        // Get access to the phone's audio controls
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Check the "note" we left in SharedPreferences
        val appStatePrefs = getSharedPreferences("AutoMuteState", MODE_PRIVATE)
        val wasMutedByApp = appStatePrefs.getBoolean("isMutedByApp", false)

        // If our app was the one that muted the phone, it's now our responsibility to unmute it.
        if (wasMutedByApp) {
            // Set the ringer back to normal
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

            // Erase the "note" so the service knows the user is back in control
            appStatePrefs.edit().putBoolean("isMutedByApp", false).apply()
        }
        // --- END OF NEW LOGIC ---

        // Stop the location service if there are no locations left
        if (locationsList.isEmpty()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)

            // Also, clear the mute-by-app flag just in case
            appStatePrefs.edit().putBoolean("isMutedByApp", false).apply()
        }

        // Save the updated list back to SharedPreferences
        val updatedJson = gson.toJson(locationsList)
        editor.putString("locations", updatedJson)
        editor.apply()
    }
    /**
     * Updates the UI to show either the empty state or the list of locations.
     */
    private fun updateUI() {
        if (locationContainer.childCount > 0) {
            emptyStateLayout.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.VISIBLE
        }
    }
}
