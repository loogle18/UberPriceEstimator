package api.uber

import com.example.sviat_minato.uberpriceestimator.BuildConfig
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result


private val UBER_API_ESTIMATES_PRICE_URL = "https://api.uber.com/v1.2/estimates/price"
private val AUTH_HEADER = "Authorization" to "Token ${BuildConfig.UBER_API_SERVER_TOKEN}"

fun getPriceEstimation(params: List<Pair<String, Double?>>,
                       callback: (isSuccess: Boolean, message: String, lowEta: Int?, meanEta: Int?) -> Unit) {
    Fuel.get(UBER_API_ESTIMATES_PRICE_URL, params).header(AUTH_HEADER).responseJson { _, _, result ->
        var message = "Щось пішло не так. Неможливо знайти ціну по заданим координатам. Перевірте правильність написання."
        var isSuccess = false
        var lowEta: Int? = null
        var meanEta: Int? = null
        when (result) {
            is Result.Success -> {
                val pricesArray = result.get().obj().getJSONArray("prices")
                if (pricesArray != null && pricesArray[0] != null) {
                    val price = pricesArray.getJSONObject(0)
                    val highEta = (price.get("high_estimate") as Double).toInt()
                    lowEta = (price.get("low_estimate") as Double).toInt()
                    meanEta = (highEta + lowEta) / 2

                    isSuccess = true
                    message = "Приблизна вартість від $lowEta до $highEta грн.\nСередня: $meanEta грн."
                }
            }
        }
        callback(isSuccess, message, lowEta, meanEta)
    }
}

fun syncGetPriceEstimation(params: List<Pair<String, Double?>>): Pair<Boolean, Int?> {
    val (_, _, result) = Fuel.get(UBER_API_ESTIMATES_PRICE_URL, params).header(AUTH_HEADER).responseJson()
    var isSuccess = false
    var meanEta: Int? = null
    when (result) {
        is Result.Success -> {
            val pricesArray = result.get().obj().getJSONArray("prices")
            if (pricesArray != null && pricesArray[0] != null) {
                val price = pricesArray.getJSONObject(0)
                val highEta = (price.get("high_estimate") as Double).toInt()
                val lowEta = (price.get("low_estimate") as Double).toInt()
                meanEta = (highEta + lowEta) / 2
                isSuccess = true
            }
        }
    }
    return Pair(isSuccess, meanEta)
}