package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "examgate_preferences")

data class BrowserPreferences(
  val defaultExitPin: String = "1234",
  val defaultUserAgent: String = DEFAULT_USER_AGENT,
  val defaultBlockScreenshots: Boolean = true,
  val defaultEnableKioskPinning: Boolean = true,
  val defaultDisableCopyPaste: Boolean = true,
  val defaultAllowZoom: Boolean = false,
  val lastQuickUrl: String = "",
  val lastStudentId: String = "",
) {
  companion object {
    const val DEFAULT_USER_AGENT = "ExamGate-Browser/1.0 (Android; CBT-Secure)"
  }
}

class BrowserPreferencesRepository(private val context: Context) {
  private object Keys {
    val DEFAULT_EXIT_PIN = stringPreferencesKey("default_exit_pin")
    val DEFAULT_USER_AGENT = stringPreferencesKey("default_user_agent")
    val DEFAULT_BLOCK_SCREENSHOTS = booleanPreferencesKey("default_block_screenshots")
    val DEFAULT_ENABLE_KIOSK = booleanPreferencesKey("default_enable_kiosk")
    val DEFAULT_DISABLE_COPY_PASTE = booleanPreferencesKey("default_disable_copy_paste")
    val DEFAULT_ALLOW_ZOOM = booleanPreferencesKey("default_allow_zoom")
    val LAST_QUICK_URL = stringPreferencesKey("last_quick_url")
    val LAST_STUDENT_ID = stringPreferencesKey("last_student_id")
  }

  val preferencesFlow: Flow<BrowserPreferences> =
    context.dataStore.data.map { prefs ->
      BrowserPreferences(
        defaultExitPin = prefs[Keys.DEFAULT_EXIT_PIN] ?: "1234",
        defaultUserAgent =
          prefs[Keys.DEFAULT_USER_AGENT] ?: BrowserPreferences.DEFAULT_USER_AGENT,
        defaultBlockScreenshots = prefs[Keys.DEFAULT_BLOCK_SCREENSHOTS] ?: true,
        defaultEnableKioskPinning = prefs[Keys.DEFAULT_ENABLE_KIOSK] ?: true,
        defaultDisableCopyPaste = prefs[Keys.DEFAULT_DISABLE_COPY_PASTE] ?: true,
        defaultAllowZoom = prefs[Keys.DEFAULT_ALLOW_ZOOM] ?: false,
        lastQuickUrl = prefs[Keys.LAST_QUICK_URL] ?: "",
        lastStudentId = prefs[Keys.LAST_STUDENT_ID] ?: "",
      )
    }

  suspend fun updateSecurityDefaults(
    exitPin: String,
    userAgent: String,
    blockScreenshots: Boolean,
    enableKioskPinning: Boolean,
    disableCopyPaste: Boolean,
    allowZoom: Boolean,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DEFAULT_EXIT_PIN] = exitPin.ifBlank { "1234" }
      prefs[Keys.DEFAULT_USER_AGENT] =
        userAgent.ifBlank { BrowserPreferences.DEFAULT_USER_AGENT }
      prefs[Keys.DEFAULT_BLOCK_SCREENSHOTS] = blockScreenshots
      prefs[Keys.DEFAULT_ENABLE_KIOSK] = enableKioskPinning
      prefs[Keys.DEFAULT_DISABLE_COPY_PASTE] = disableCopyPaste
      prefs[Keys.DEFAULT_ALLOW_ZOOM] = allowZoom
    }
  }

  suspend fun saveLastQuickInputs(url: String, studentId: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.LAST_QUICK_URL] = url
      prefs[Keys.LAST_STUDENT_ID] = studentId
    }
  }
}
