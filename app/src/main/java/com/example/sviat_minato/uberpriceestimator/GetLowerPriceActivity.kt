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
import api.uber.syncGetPriceEstimation
import android.content.Intent
import android.app.PendingIntent




class GetLowerPriceActivity : AppCompatActivity() {
    private var fromLatitude: Double? = null
    private var fromLongitude: Double? = null
    private var toLatitude: Double? = null
    private var toLongitude: Double? = null
    private var maxRebate: Int? = null
    private var oldMeanEta: Int? = null
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
        oldMeanEta = extras.getInt("meanEta")
        rebateRange = 5..maxRebate!!
        editDuration = findViewById(R.id.edit_duration)
        editMinRebate = findViewById(R.id.edit_min_rebate)
        editMinRebate.hint = "Мін. зниження ціни (від 5 до $maxRebate)"
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
                    getEstimatesAndSendNotification(duration, minRebate)
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
            channel.description = "Information about lower price of uber ride"
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
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

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("notificationTitle", title)
        intent.putExtra("notificationText", message)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.setContentIntent(pendingIntent)

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

    private fun getEstimatesAndSendNotification(duration: Int, minRebate: Int) {
        val handler = Handler()

        val thread = Thread(Runnable {
            try {
                watchForLowerPrice(duration, minRebate, handler)
            } catch (error: Exception) {
                println(error.localizedMessage)
            }
        })

        thread.start()
    }

    private fun watchForLowerPrice(duration: Int, minRebate: Int, handler: Handler) {
        val params = listOf("start_latitude" to fromLatitude, "start_longitude" to fromLongitude,
                "end_latitude" to toLatitude, "end_longitude" to toLongitude)
        var count = 0
        val meanPricesList = mutableListOf(oldMeanEta!!)
        val delay: Long = 60000

        handler.postDelayed(object: Runnable {
            override fun run() {
                count++
                val (isSuccess, meanEta) = syncGetPriceEstimation(params)
                var lowPriceFound = false
                if (isSuccess) {
                    meanPricesList.add(meanEta!!)
                    lowPriceFound = oldMeanEta!! - meanEta >= minRebate
                }

                if (lowPriceFound || count >= duration) {
                    val title: String
                    var message: String

                    if (lowPriceFound) {
                        title = "Знайдено необхідну нижчу ціну"
                        message = "Нова вартість: $meanEta грн.\nПочаткова вартість була $oldMeanEta."
                    } else {
                        val lastMeanEta = meanPricesList.last()
                        meanPricesList.sort()
                        title = "Не вийшло знайти необхідну нижчу ціну"
                        message = "Найменша вартість поїздки, яку вдалось знайти, була ${meanPricesList[0]} грн."
                        message += "\nОстання вартість поїздки була $lastMeanEta грн."
                        message += "\nПочаткова вартість була $oldMeanEta грн."
                    }

                    sendNotification(title, message)
                    handler.removeCallbacks(this)
                } else {
                    handler.postDelayed(this, delay)
                }
            }
        }, delay)
    }
}
