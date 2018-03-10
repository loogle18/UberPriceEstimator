package com.example.sviat_minato.uberpriceestimator

import android.Manifest
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import com.example.sviat_minato.uberpriceestimator.BuildConfig.API_USER_LOGIN
import com.example.sviat_minato.uberpriceestimator.BuildConfig.API_USER_PASSWORD
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.google.android.gms.maps.model.LatLng
import java.nio.charset.StandardCharsets



class MainActivity : AppCompatActivity() {
    lateinit var autocompleteIntent: Intent
    lateinit var editFrom: EditText
    lateinit var editTo: EditText
    lateinit var buttonGetPrice: Button
    var fromCoordinates: LatLng? = null
    var toCoordinates: LatLng? = null
    private var locationManager: LocationManager? = null
    var latLngBounds: LatLngBounds? = null
    var isLocationAvailable = false
    val MY_REQUEST_ACCESS_COARSE_LOCATION = 0
    val UBER_PRICES_ESTIMATOR_BASE_API_URL = "https://uber-prices-estimator.herokuapp.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editFrom = findViewById<EditText>(R.id.edit_from)
        editTo = findViewById<EditText>(R.id.edit_to)
        buttonGetPrice = findViewById<Button>(R.id.button_get_price)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        val playServicesConnectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        handlePlayServicesConnectionResult(playServicesConnectionResult)

        if (playServicesConnectionResult == ConnectionResult.SUCCESS) {
            requestNeededPermissions()
            requestLocationUpdates()
            initAutocompleteIntent()
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
                val bytesOfToken = "$API_USER_LOGIN:$API_USER_PASSWORD".toByteArray(StandardCharsets.UTF_8)
                val base64Token = Base64.encodeToString(bytesOfToken, Base64.NO_WRAP)
                val data = "{\"token\": \"$base64Token\",  \"slat\": \"${fromCoordinates?.latitude}\", " +
                        "\"slng\": \"${fromCoordinates?.longitude}\", \"elat\": \"${toCoordinates?.latitude}\", " +
                        "\"elng\": \"${toCoordinates?.longitude}\"}"

                Fuel.post("$UBER_PRICES_ESTIMATOR_BASE_API_URL/api/price_eta").body(data).responseJson { _, _, result ->
                    when (result) {
                        is Result.Success -> {
                            val isSuccess = result.get().obj().get("success")
                            if (isSuccess as Boolean) {
                                val resultText = result.get().obj().get("eta_text")
                                showAlert(resultText as String)
                            } else {
                                val error = result.get().obj().get("error")
                                showAlert(error as String)
                            }
                        }
                        is Result.Failure -> {
                            showAlert(result.error.localizedMessage)
                        }
                    }
                }
            }
        }
    }

    private fun showAlert(message: String) {
        val alert = AlertDialog.Builder(this)

        with (alert) {
            setMessage(message)

            setPositiveButton("Close") { dialog, _ ->
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

    private fun initAutocompleteIntent() {
        if (isLocationAvailable) {
            latLngBounds?.let {
                autocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                        .setBoundsBias(it)
                        .build(this)
            }
        } else {
            autocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(this)
        }
    }

    private fun toBounds(center: LatLng): LatLngBounds {
        val distanceFromCenterToCorner = 1000 * Math.sqrt(2.0)
        val southwestCorner = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0)
        val northeastCorner = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0)
        return LatLngBounds(southwestCorner, northeastCorner)
    }

    private fun requestNeededPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_REQUEST_ACCESS_COARSE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_REQUEST_ACCESS_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestLocationUpdates()
                    initAutocompleteIntent()
                    editFromAndToClicked(autocompleteIntent)
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
            locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                latLngBounds = toBounds(LatLng(it.latitude, it.longitude))
            }
            isLocationAvailable = true
        } catch(exception: SecurityException) {
            println(exception)
        }
    }

    private val locationListener: LocationListener = object: LocationListener {
        override fun onLocationChanged(location: Location) {
            latLngBounds = toBounds(LatLng(location.latitude, location.longitude))
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            when (status) {
                LocationProvider.OUT_OF_SERVICE -> isLocationAvailable = false
                LocationProvider.TEMPORARILY_UNAVAILABLE -> isLocationAvailable = false
                LocationProvider.AVAILABLE -> isLocationAvailable = true
            }
        }

        override fun onProviderEnabled(provider: String) {
            isLocationAvailable = true
        }

        override fun onProviderDisabled(provider: String) {
            isLocationAvailable = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (listOf(1, 2).contains(requestCode)) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlaceAutocomplete.getPlace(this, data)
                val address = place.address
                if (requestCode == 1) {
                    editFrom.setText(address)
                    fromCoordinates = place.latLng
                } else {
                    editTo.setText(address)
                    toCoordinates = place.latLng
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                val status = PlaceAutocomplete.getStatus(this, data)
                println("An error occurred: ${status.statusMessage}")

            } else if (resultCode == Activity.RESULT_CANCELED) {
                println("Canceled")
            }
        }
    }
}
