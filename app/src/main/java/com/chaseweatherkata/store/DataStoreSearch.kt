package com.chaseweatherkata.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSearch(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "search_data_store")

    companion object {
        private val SAVED_CITY = stringPreferencesKey("saved_searched_city")
    }

    suspend fun saveCity(city: String) {
        context.dataStore.edit { search ->
            search[SAVED_CITY] = city
        }
    }

    val readCity: Flow<String>
        get() = context.dataStore.data.map { search ->
            search[SAVED_CITY] ?: ""
        }
}