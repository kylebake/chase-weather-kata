package com.chaseweatherkata.api

import com.chaseweatherkata.BuildConfig
import com.chaseweatherkata.data.model.WeatherResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Response as OkHttpResponse

object OpenWeatherApiClient {
    private const val BASE_WEATHER_URL = "https://api.openweathermap.org/"

    private val client = OkHttpClient.Builder().addInterceptor(RequestInterceptor).build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_WEATHER_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}

object RequestInterceptor : Interceptor {
    // interceptor to add the app id query param to every request rather than making the consumer responsible for it
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val request = chain.request()
        val requestUrl = request.url()
        val urlWithAppId =
            requestUrl.newBuilder()
                .addQueryParameter("appid", BuildConfig.OPEN_WEATHER_API_KEY)
                .build()
        val newRequest = request.newBuilder().url(urlWithAppId)

        return chain.proceed(newRequest.build())
    }
}

class ApiClient {
    // generic api call function to unwrap the response body and handle errors
    private fun <T> apiCall(
        call: Call<T>,
        onSuccess: (param: T) -> Unit,
        onError: (error: Error) -> Unit
    ) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    onError(Error("Request was unsuccessful"))
                    return
                }

                onSuccess(body)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                onError(Error(t))
            }
        })
    }

    private val weatherApiService: WeatherApiService by lazy {
        OpenWeatherApiClient.retrofit.create(WeatherApiService::class.java)
    }

    fun getWeatherDataForCity(
        city: String,
        onSuccess: (weather: WeatherResponse) -> Unit,
        onError: (error: Error) -> Unit
    ) {
        val call = weatherApiService.getWeather(city)

        apiCall(call, onSuccess, onError)
    }

    fun getWeatherIcon(
        icon: String,
        onSuccess: (weather: ResponseBody) -> Unit,
        onError: (error: Error) -> Unit
    ) {
        val call = weatherApiService.getWeatherIcon(WeatherApiService.iconUrl(icon))

        apiCall(call, onSuccess, onError)
    }
}