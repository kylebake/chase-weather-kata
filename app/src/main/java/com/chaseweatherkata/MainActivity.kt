package com.chaseweatherkata

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.chaseweatherkata.api.ApiClient
import com.chaseweatherkata.data.BitmapHelper
import com.chaseweatherkata.data.model.WeatherResponse
import com.chaseweatherkata.databinding.MainBinding
import com.chaseweatherkata.store.DataStoreSearch
import com.chaseweatherkata.ui.SearchViewModel
import com.chaseweatherkata.ui.SearchViewModelFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainBinding
    private lateinit var searchViewModel: SearchViewModel
    private val dataStore by lazy { DataStoreSearch(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation()
            }
        }

    private fun retrieveWeatherForCity(city: String) {
        searchViewModel.retrieveWeatherForCity(city)
    }

    private fun setupView() {
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchViewModel = ViewModelProvider(
            this,
            SearchViewModelFactory(ApiClient(), dataStore, BitmapHelper())
        )[SearchViewModel::class.java]

        setupOnClicks()
        setupObservers()
    }

    private fun setupObservers() {
        searchViewModel.cityWeatherInfo.observe(this) { weatherResponse ->
            if (!weatherResponse.success || weatherResponse.data == null) {
                showErrorDialog("An error occurred while trying to retrieve the weather")
                return@observe
            }

            populateWeatherFields(weatherResponse.data)
        }
        searchViewModel.weatherIcon.observe(this) { weatherIcon ->
            if (!weatherIcon.success || weatherIcon.data == null) {
                showErrorDialog("An error occurred while trying to retrieve the weather icon")
                return@observe
            }

            binding.weatherIcon.setImageBitmap(weatherIcon.data)
        }
        dataStore.readCity.asLiveData().observe(this) { city ->
            binding.searchCity.setText(city)
            retrieveWeatherForCity(city)
        }
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
    }

    private fun getCurrentLocation(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val fineLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) return false

        // this is slow to resolve; would like to add some kind of loading indicator (a spinner or some text)
        // to show the user that something is happening
        manager.requestSingleUpdate(
            LocationManager.GPS_PROVIDER, { location ->
                val city = Geocoder(this, Locale.getDefault()).getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )?.first()?.locality

                if (city.isNullOrEmpty()) return@requestSingleUpdate

                retrieveWeatherForCity(city)
            }, null
        )

        return true
    }

    private fun setupOnClicks() {
        binding.getLocation.setOnClickListener {
            if (!getCurrentLocation()) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        binding.getWeather.setOnClickListener {
            val city = binding.searchCity.text.toString()

            if (city.isEmpty()) return@setOnClickListener

            retrieveWeatherForCity(city)
        }
    }

    private fun populateWeatherFields(response: WeatherResponse) {
        binding.tempValue.text = getString(R.string.temperature, response.main.temp)
        binding.feelsLikeValue.text = getString(R.string.temperature, response.main.feelsLike)
        binding.humidityValue.text = getString(R.string.humidity, response.main.humidity)

        val weather = response.weather.first()
        binding.weatherConditionsMain.text = weather.main
        binding.weatherConditionsDescription.text = weather.description
    }
}
