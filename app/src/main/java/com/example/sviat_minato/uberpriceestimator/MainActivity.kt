package com.example.sviat_minato.uberpriceestimator

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.widget.Button
import android.widget.EditText


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

        val playServicesConnectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        handlePlayServicesConnectionResult(playServicesConnectionResult)

        if (playServicesConnectionResult == ConnectionResult.SUCCESS) {
            val autocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(this)

            editFromAndToClicked(autocompleteIntent)
            buttonGetPriceClicked()
        }
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
                showAlert("Getting price for ride from ${editFrom.text} to ${editFrom.text}")
            }
        }
    }

    private fun showAlert(message: String) {
        val alert = AlertDialog.Builder(this)

        with (alert) {
            setMessage(message)

            setPositiveButton("Close") { dialog, whichButton ->
                dialog.dismiss()
            }
        }

        alert.show()
    }

    private fun handlePlayServicesConnectionResult(result: Int) {
        when (result) {
            ConnectionResult.SERVICE_MISSING ->
                GoogleApiAvailability.getInstance().getErrorDialog(this, ConnectionResult.SERVICE_MISSING, 0).show()
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ->
                GoogleApiAvailability.getInstance().getErrorDialog(this, ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, 0).show()
            ConnectionResult.SERVICE_DISABLED ->
                GoogleApiAvailability.getInstance().getErrorDialog(this, ConnectionResult.SERVICE_DISABLED, 0).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (listOf(1, 2).contains(requestCode)) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlaceAutocomplete.getPlace(this, data)
                val lat = place.latLng.latitude
                val lng = place.latLng.longitude
                val address = place.address
                val editText = if (requestCode == 1) this.editFrom else this.editTo

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
