package com.chaseweatherkata.ui

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chaseweatherkata.api.ApiClient
import com.chaseweatherkata.data.BitmapHelper
import com.chaseweatherkata.data.model.Clouds
import com.chaseweatherkata.data.model.Coord
import com.chaseweatherkata.data.model.Main
import com.chaseweatherkata.data.model.Weather
import com.chaseweatherkata.data.model.WeatherResponse
import com.chaseweatherkata.data.model.Wind
import com.chaseweatherkata.store.DataStoreSearch
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.verify
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class SearchViewModelTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @MockK
    var apiClient = mockkClass(ApiClient::class, relaxed = true)

    @MockK
    var dataStore = mockkClass(DataStoreSearch::class, relaxed = true)

    @MockK
    var bitmapHelper = mockkClass(BitmapHelper::class, relaxed = true)

    private lateinit var searchViewModel: SearchViewModel

    private val weather = WeatherResponse(
        base = "base",
        coord = Coord(1.23, 3.45),
        weather = listOf(Weather(123, "main", "description", "icon")),
        main = Main(1.23, 3.45, 1.22, 3.22, 10, 90, 5, 2),
        visibility = 1,
        wind = Wind(1.22, 5, 9.99),
        clouds = Clouds(1)
    )

    @Before
    fun setup() {
        searchViewModel = SearchViewModel(apiClient, dataStore, bitmapHelper)
        every {
            apiClient.getWeatherDataForCity(
                any(),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(WeatherResponse) -> Unit>().captured.invoke(
                weather
            )
        }
        every { apiClient.getWeatherIcon(any(), captureLambda(), any()) } answers {
            lambda<(ResponseBody) -> Unit>().captured.invoke(
                ResponseBody.create(MediaType.get("image/png"), "test string")
            )
        }
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun testRetrieveWeatherForCityCallsApi() {
        searchViewModel.retrieveWeatherForCity("Some City")

        verify {
            apiClient.getWeatherDataForCity("Some City", any(), any())
            Assert.assertEquals(searchViewModel.cityWeatherInfo.value?.data, weather)
            Assert.assertTrue(searchViewModel.cityWeatherInfo.value?.success ?: false)
        }
    }

    @Test
    fun testRetrieveWeatherUpdatesDataStore() {
        searchViewModel.retrieveWeatherForCity("Some City")

        coVerify(exactly = 1) {
            dataStore.saveCity("Some City")
        }
    }

    @Test
    fun testRetrieveWeatherCallsRetrieveIcon() {
        searchViewModel.retrieveWeatherForCity("Columbus")

        verify {
            apiClient.getWeatherIcon(weather.weather.first().icon, any(), any())
            Assert.assertNotNull(searchViewModel.weatherIcon.value?.data)
            Assert.assertTrue(searchViewModel.weatherIcon.value?.success ?: false)
        }
    }

    @Test
    fun testRetrieveWeatherFailsAndStoresSuccessValue() {
        every {
            apiClient.getWeatherDataForCity(
                any(),
                any(),
                captureLambda(),
            )
        } answers {
            lambda<(Error) -> Unit>().captured.invoke(
                Error("uh oh")
            )
        }

        searchViewModel.retrieveWeatherForCity("Columbus")

        verify {
            apiClient.getWeatherDataForCity("Columbus", any(), any())
            Assert.assertNull(searchViewModel.cityWeatherInfo.value?.data)
            Assert.assertFalse(searchViewModel.cityWeatherInfo.value?.success ?: true)
        }
    }

    @Test
    fun testRetrieveWeatherFailsForWeatherIconAndStoresSuccessValue() {
        every { apiClient.getWeatherIcon(any(), any(), captureLambda()) } answers {
            lambda<(Error) -> Unit>().captured.invoke(
                Error("uh oh")
            )
        }

        searchViewModel.retrieveWeatherForCity("Cincinnati")

        verify {
            apiClient.getWeatherIcon(weather.weather.first().icon, any(), any())
            Assert.assertNull(searchViewModel.weatherIcon.value?.data)
            Assert.assertFalse(searchViewModel.weatherIcon.value?.success ?: true)
        }
    }
}