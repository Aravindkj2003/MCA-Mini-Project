
package com.example.automutefinal

import android.media.AudioManager

data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val ringerMode: Int,
    val radius: Float
)