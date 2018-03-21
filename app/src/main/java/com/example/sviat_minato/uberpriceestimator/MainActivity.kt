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
import android.location.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import api.uber.getPriceEstimation
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var autocompleteIntent: Intent
    private var fromCoordinates: LatLng? = null
    private var toCoordinates: LatLng? = null
    private var locationManager: LocationManager? = null
    private var latLngBounds: LatLngBounds? = null
    private var isLocationAvailable = false
    private val MY_REQUEST_ACCESS_FINE_LOCATION = 0
    private val MIN_DISTANCE_IN_METERS_CHANGE_FOR_LOCATION_UPDATES = 10f
    private val MIN_TIME_BETWEEN_UPDATES: Long = 60000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        val playServicesConnectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        handlePlayServicesConnectionResult(playServicesConnectionResult)

        if (playServicesConnectionResult == ConnectionResult.SUCCESS) {
            requestNeededPermissions()
            requestLocationUpdatesAndReturnLastKnown()
            initAutocompleteIntent()
            editFromAndToClicked(autocompleteIntent)
            clearFromAndToButtonClicked()
            buttonGetPriceClicked()
            buttonGetLocationClicked()
            addChangeEditTextListener(editFrom)
            addChangeEditTextListener(editTo)
        }
    }

    override fun onNewIntent(intent: Intent) {
        editFrom.setText("")
        editTo.setText("")

        intent.extras?.let {
            val notificationTitle = it.getString("notificationTitle", "")
            val notificationText = it.getString("notificationText", "")
            if (notificationTitle.isNotBlank() && notificationText.isNotBlank()) {
                showAlertWithTitle(notificationTitle, notificationText)
            }
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

    private fun addChangeEditTextListener(editText: EditText) {
        val isEditFrom = editText.id == editFrom.id
        val changeEditToTextListener: TextWatcher = object: TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(newText: Editable?) {
                val visibility = if (newText.isNullOrEmpty()) View.GONE else View.VISIBLE
                if (isEditFrom) {
                    buttonClearFrom.visibility = visibility
                    buttonGetLocation.visibility = if (newText.isNullOrEmpty()) View.VISIBLE else View.GONE
                } else {
                    buttonClearTo.visibility = visibility
                }
            }
        }

        editText.addTextChangedListener(changeEditToTextListener)
    }

    private fun clearFromAndToButtonClicked() {
        buttonClearFrom.setOnClickListener {
            editFrom.setText("")
        }

        buttonClearTo.setOnClickListener {
            editTo.setText("")
        }
    }

    private fun buttonGetLocationClicked() {
        buttonGetLocation.setOnClickListener {
            val geoCoder = Geocoder(applicationContext, Locale.getDefault())
            requestLocationUpdatesAndReturnLastKnown()?.let {
                try {
                    val listAddresses = geoCoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (listAddresses != null && listAddresses.isNotEmpty()) {
                        val currentAddress = listAddresses[0]
                        editFrom.setText(currentAddress.getAddressLine(0))
                        fromCoordinates = LatLng(currentAddress.latitude, currentAddress.longitude)
                    }
                } catch (error: IOException) {
                    println(error.localizedMessage)
                }
            }
        }
    }

    private fun buttonGetPriceClicked() {
        buttonGetPrice.setOnClickListener {
            if (editFrom.text.isNotBlank() && editTo.text.isNotBlank()) {
                progressBar.visibility = View.VISIBLE
                editFrom.isEnabled = false
                editTo.isEnabled = false
                buttonGetPrice.isEnabled = false

                val params = listOf("start_latitude" to fromCoordinates?.latitude, "start_longitude" to fromCoordinates?.longitude,
                        "end_latitude" to toCoordinates?.latitude, "end_longitude" to toCoordinates?.longitude)

                getPriceEstimation(params) { isSuccess, message, lowEta, meanEta ->
                    progressBar.visibility = View.GONE
                    editFrom.isEnabled = true
                    editTo.isEnabled = true
                    buttonGetPrice.isEnabled = true
                    showAlert(message, isSuccess, lowEta, meanEta)
                }
            }
        }
    }

    private fun showAlertWithTitle(title: String, message: String) {
        val alert = AlertDialog.Builder(this)

        with (alert) {
            setTitle(title)
            setMessage(message)

            setNegativeButton("Закрити") { dialog, _ ->
                dialog.dismiss()
            }
        }

        alert.show()
    }

    private fun showAlert(message: String, isSuccess: Boolean, lowEta: Int?, meanEta: Int?) {
        val alert = AlertDialog.Builder(this)

        with (alert) {
            setMessage(message)

            setNegativeButton("Закрити") { dialog, _ ->
                dialog.dismiss()
            }

            if (isSuccess) {
                setPositiveButton("Знайти нижчу ціну", { _, _ ->
                    val newIntent = Intent(applicationContext, GetLowerPriceActivity::class.java)
                    newIntent.putExtra("fromLatitude", fromCoordinates?.latitude)
                    newIntent.putExtra("fromLongitude", fromCoordinates?.longitude)
                    newIntent.putExtra("toLatitude", toCoordinates?.latitude)
                    newIntent.putExtra("toLongitude", toCoordinates?.longitude)
                    newIntent.putExtra("lowEta", lowEta)
                    newIntent.putExtra("meanEta", meanEta)
                    this@MainActivity.startActivity(newIntent)
                })
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestLocationUpdatesAndReturnLastKnown()
                    initAutocompleteIntent()
                    editFromAndToClicked(autocompleteIntent)
                }
            }
        }
    }

    private fun requestLocationUpdatesAndReturnLastKnown(): LatLng? {
        var latLng: LatLng? = null

        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_IN_METERS_CHANGE_FOR_LOCATION_UPDATES, locationListener)
            locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                latLng = LatLng(it.latitude, it.longitude)
                isLocationAvailable = true
                latLngBounds = toBounds(latLng!!)
            }
        } catch(exception: SecurityException) {
            isLocationAvailable = false
            println(exception.localizedMessage)
        }

        return latLng
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
                val name = place.name
                if (requestCode == 1) {
                    editFrom.setText(name)
                    fromCoordinates = place.latLng
                } else {
                    editTo.setText(name)
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
