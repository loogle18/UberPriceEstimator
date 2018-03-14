package com.example.sviat_minato.uberpriceestimator

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Button
import android.widget.EditText
import android.view.inputmethod.InputMethodManager


class GetLowerPriceActivity : AppCompatActivity() {
    private var fromLatitude: Double? = null
    private var fromLongitude: Double? = null
    private var toLatitude: Double? = null
    private var toLongitude: Double? = null
    private lateinit var editDuration: EditText
    private lateinit var editMinRebate: EditText
    private lateinit var buttonStartChecking: Button
    private val DURATION_RANGE = 5..20
    private val REBATE_RANGE = 5..70

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_lower_price)
        val extras = intent.extras
        fromLatitude = extras.getDouble("fromLatitude")
        fromLongitude = extras.getDouble("fromLongitude")
        toLatitude = extras.getDouble("toLatitude")
        toLongitude = extras.getDouble("toLongitude")
        editDuration = findViewById(R.id.edit_duration)
        editMinRebate = findViewById(R.id.edit_min_rebate)
        buttonStartChecking = findViewById(R.id.button_start_checking)

        buttonStartCheckingClicked()
    }

    private fun buttonStartCheckingClicked() {
        buttonStartChecking.setOnClickListener {
            val durationText = editDuration.text.toString()
            val minRebateText = editMinRebate.text.toString()
            if (durationText.isNotBlank() && minRebateText.isNotBlank()) {
                val duration = Integer.parseInt(durationText)
                val minRebate = Integer.parseInt(minRebateText)

                if (duration in DURATION_RANGE && minRebate in REBATE_RANGE) {
                    hideKeyboard()
                    showAlert("Запит на перевірку успішно відправлено. Після закінчення Ви отримаєте повідомлення.")
                }
            }
        }
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
        val view = this.currentFocus
        view?.let {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}
