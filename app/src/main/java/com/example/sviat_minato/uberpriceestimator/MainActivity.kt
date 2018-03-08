package com.example.sviat_minato.uberpriceestimator

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    lateinit var editFrom: EditText
    lateinit var editTo: EditText
    lateinit var buttonGetPrice: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editFrom = findViewById<EditText>(R.id.edit_from)
        editTo = findViewById<EditText>(R.id.edit_to)
        buttonGetPrice = findViewById<Button>(R.id.button_get_price)

        val autocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                .build(this)

        editFromAndToClicked(autocompleteIntent)
        buttonGetPriceClicked()
    }


    private fun editFromAndToClicked(intent: Intent) {
        editFrom.setOnClickListener {
            startActivityForResult(intent, 1)
        }

        editTo.setOnClickListener {
            startActivityForResult(intent, 2)
        }
    }

    private fun buttonGetPriceClicked() {
        buttonGetPrice.setOnClickListener() {
            if (editFrom.text.isNotBlank() && editTo.text.isNotBlank()) {
                Toast.makeText(this, "Getting price for ride from ${editFrom.text} to ${editFrom.text}",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (listOf(1, 2).contains(requestCode)) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlaceAutocomplete.getPlace(this, data)
                val lat = place.latLng.latitude
                val lng = place.latLng.longitude
                val address = place.address
                var editText = if (requestCode == 1) this.editFrom else this.editTo

                editText.setText(address)

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
