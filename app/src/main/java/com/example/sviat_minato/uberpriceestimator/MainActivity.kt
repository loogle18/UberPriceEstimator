package com.example.sviat_minato.uberpriceestimator

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import android.content.Intent
import android.widget.EditText


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val autocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                .build(this)

        handleEditFromTo(autocompleteIntent)
    }


    private fun handleEditFromTo(intent: Intent) {
        val editFrom = findViewById<EditText>(R.id.edit_from)
        val editTo = findViewById<EditText>(R.id.edit_to)

        editFrom.setOnClickListener {
            startActivityForResult(intent, 1)
        }

        editTo.setOnClickListener {
            startActivityForResult(intent, 2)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (listOf(1, 2).contains(requestCode)) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlaceAutocomplete.getPlace(this, data)
                val lat = place.latLng.latitude
                val lng = place.latLng.longitude
                println( "Place: lat($lat) lng($lng)")

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                val status = PlaceAutocomplete.getStatus(this, data)
                println("An error occurred: ${status.statusMessage}")

            } else if (resultCode == Activity.RESULT_CANCELED) {
                println("Canceled")
            }
        }
    }
}
