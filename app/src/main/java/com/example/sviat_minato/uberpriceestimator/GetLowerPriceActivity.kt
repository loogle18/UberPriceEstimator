package com.example.sviat_minato.uberpriceestimator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.widget.Button
import android.widget.EditText
import android.view.inputmethod.InputMethodManager


class GetLowerPriceActivity : AppCompatActivity() {
    private var fromLatitude: Double? = null
    private var fromLongitude: Double? = null
    private var toLatitude: Double? = null
    private var toLongitude: Double? = null
    private var maxRebate: Int? = null
    private lateinit var editDuration: EditText
    private lateinit var editMinRebate: EditText
    private lateinit var buttonStartChecking: Button
    private val DURATION_RANGE = 5..20
    private lateinit var rebateRange: IntRange
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_lower_price)
        val extras = intent.extras
        fromLatitude = extras.getDouble("fromLatitude")
        fromLongitude = extras.getDouble("fromLongitude")
        toLatitude = extras.getDouble("toLatitude")
        toLongitude = extras.getDouble("toLongitude")
        maxRebate = extras.getInt("lowEta", 70)
        rebateRange = 5..maxRebate!!
        editDuration = findViewById(R.id.edit_duration)
        editMinRebate = findViewById(R.id.edit_min_rebate)
        editMinRebate.setHint("Мін. зниження ціни (від 5 до $maxRebate)")
        buttonStartChecking = findViewById(R.id.button_start_checking)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = createNotificationBuilder()
        buttonStartCheckingClicked()
    }

    private fun buttonStartCheckingClicked() {
        buttonStartChecking.setOnClickListener {
            val durationText = editDuration.text.toString()
            val minRebateText = editMinRebate.text.toString()
            if (durationText.isNotBlank() && minRebateText.isNotBlank()) {
                val duration = Integer.parseInt(durationText)
                val minRebate = Integer.parseInt(minRebateText)

                if (duration in DURATION_RANGE && minRebate in rebateRange) {
                    hideKeyboard()
                    getEstimatesAndSendNotification()
                    showAlert("Запит на перевірку успішно відправлено. Після закінчення Ви отримаєте повідомлення.")
                }
            }
        }
    }

    private fun createNotificationBuilder(channelId: String = "com.sviat_minato.uberpriceestimator.estimate"):
            NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Uber Estimator Price Estimate", NotificationManager.IMPORTANCE_DEFAULT)
            channel.setDescription("Information about lower price of uber ride")
            channel.enableLights(true)
            channel.setLightColor(Color.BLUE)
            notificationManager.createNotificationChannel(channel)
        }

        return notificationBuilder
    }

    private fun sendNotification(title: String, message: String) {
        notificationBuilder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setContentInfo("INFO")

        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun showAlert(message: String) {
        val alert = AlertDialog.Builder(this)

        with (alert) {
            setMessage(message)

            setNegativeButton("Закрити") { dialog, _ ->
                dialog.dismiss()
            }
        }

        alert.show()
    }

    private fun hideKeyboard() {
        this.currentFocus?.let {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun getEstimatesAndSendNotification() {
        val handler = Handler()
        val delay: Long = 10000 //milliseconds

        handler.postDelayed(object : Runnable {
            override fun run() {
                val message = "Найменша вартість поїздки, яку вдалось знайти, була 70 грн."
                sendNotification("Не вийшло знайти необхідну нижчу ціну", message)
                handler.postDelayed(this, delay)
            }
        }, delay)
    }
}
