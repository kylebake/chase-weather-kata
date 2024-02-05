package com.chaseweatherkata.api

import com.chaseweatherkata.data.model.WeatherResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApiService {
    @GET("data/2.5/weather?units=imperial")
    fun getWeather(@Query("q") city: String): Call<WeatherResponse>

    @GET
    fun getWeatherIcon(@Url iconUrl: String): Call<ResponseBody>

    companion object {
        fun iconUrl(icon: String): String {
            return "https://openweathermap.org/img/wn/${icon}@2x.png"
        }
    }
}

