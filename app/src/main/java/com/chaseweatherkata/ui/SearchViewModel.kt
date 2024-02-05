package com.chaseweatherkata.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chaseweatherkata.api.ApiClient
import com.chaseweatherkata.data.BitmapHelper
import com.chaseweatherkata.data.model.WeatherResponse
import com.chaseweatherkata.store.DataStoreSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModelFactory(
    private val apiClient: ApiClient,
    private val dataStore: DataStoreSearch,
    private val bitmapHelper: BitmapHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(apiClient, dataStore, bitmapHelper) as T
    }
}

data class LiveDataResult<T>(
    val success: Boolean,
    val data: T?
)

class SearchViewModel(
    private val apiClient: ApiClient,
    private val dataStore: DataStoreSearch,
    private val bitmapHelper: BitmapHelper
) : ViewModel() {
    var cityWeatherInfo = MutableLiveData<LiveDataResult<WeatherResponse>>()
    var weatherIcon = MutableLiveData<LiveDataResult<Bitmap>>()

    fun retrieveWeatherForCity(city: String) {
        apiClient.getWeatherDataForCity(city, { weatherResponse ->
            cityWeatherInfo.value = LiveDataResult(true, weatherResponse)
            retrieveWeatherIcon(weatherResponse.weather.first().icon)
            viewModelScope.launch(Dispatchers.IO) {
                dataStore.saveCity(city)
            }
        }, {
            Log.e("Error", it.message ?: "")
            cityWeatherInfo.value = LiveDataResult(false, null)
        })
    }

    private fun retrieveWeatherIcon(icon: String) {
        apiClient.getWeatherIcon(icon, {
            val bmp = bitmapHelper.decodeStream(it.byteStream())
            weatherIcon.value = LiveDataResult(true, bmp)
        }, {
            Log.e("Error", it.message ?: "")
            weatherIcon.value = LiveDataResult(false, null)
        })
    }
}