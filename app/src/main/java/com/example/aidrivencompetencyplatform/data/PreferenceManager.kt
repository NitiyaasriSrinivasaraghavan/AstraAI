package com.example.aidrivencompetencyplatform.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {

    companion object {
        private val HAS_COMPLETED_APP_TOUR = booleanPreferencesKey("has_completed_app_tour")
    }

    val hasCompletedAppTour: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_APP_TOUR] ?: false
        }

    suspend fun setHasCompletedAppTour(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_APP_TOUR] = completed
        }
    }
}
