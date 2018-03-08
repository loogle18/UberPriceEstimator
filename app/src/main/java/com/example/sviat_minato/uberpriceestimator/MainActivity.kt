package com.example.sviat_minato.uberpriceestimator

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addAddressSuggestionListeners()
    }


    private fun addAddressSuggestionListeners() {
        val autocompleteFragmentFrom = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment_from) as PlaceAutocompleteFragment
        val autocompleteFragmentTo = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment_to) as PlaceAutocompleteFragment
        autocompleteFragmentFrom.setHint("Звідки")
        autocompleteFragmentTo.setHint("Куди")

        autocompleteFragmentFrom.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val lat = place.latLng.latitude
                val lng = place.latLng.longitude
                println( "Place: lat($lat) lng($lng)")
            }

            override fun onError(status: Status) {
                println("An error occurred: $status")
            }
        })

        autocompleteFragmentTo.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val lat = place.latLng.latitude
                val lng = place.latLng.longitude
                println( "Place: lat($lat) lng($lng)")
            }

            override fun onError(status: Status) {
                println("An error occurred: $status")
            }
        })
    }
}
