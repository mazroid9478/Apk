package com.example.ui.screens

import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BrowserPreferences

@Composable
fun SettingsScreen(
  preferences: BrowserPreferences,
  onSaveDefaults:
    (
      exitPin: String,
      userAgent: String,
      blockScreenshots: Boolean,
      enableKioskPinning: Boolean,
      disableCopyPaste: Boolean,
      allowZoom: Boolean,
    ) -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var exitPin by remember(preferences.defaultExitPin) { mutableStateOf(preferences.defaultExitPin) }
  var userAgent by
    remember(preferences.defaultUserAgent) { mutableStateOf(preferences.defaultUserAgent) }
  var blockScreenshots by
    remember(preferences.defaultBlockScreenshots) {
      mutableStateOf(preferences.defaultBlockScreenshots)
    }
  var enableKioskPinning by
    remember(preferences.defaultEnableKioskPinning) {
      mutableStateOf(preferences.defaultEnableKioskPinning)
    }
  var disableCopyPaste by
    remember(preferences.defaultDisableCopyPaste) {
      mutableStateOf(preferences.defaultDisableCopyPaste)
    }
  var allowZoom by
    remember(preferences.defaultAllowZoom) { mutableStateOf(preferences.defaultAllowZoom) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Pengaturan Keamanan Default",
                style = MaterialTheme.typography.titleLarge,
              )
            }
            Text(
              text =
                "Digunakan untuk Koneksi Cepat serta saat Server Soal tidak mengirimkan kebijakan via JSON API.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
              value = exitPin,
              onValueChange = { if (it.length <= 8) exitPin = it },
              label = { Text("PIN Pengawas Default untuk Keluar Ujian") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              modifier = Modifier.fillMaxWidth().testTag("settings_exit_pin_input"),
            )

            OutlinedTextField(
              value = userAgent,
              onValueChange = { userAgent = it },
              label = { Text("Custom User-Agent Header") },
              supportingText = {
                Text("Server Soal dapat memeriksa header ini untuk validasi Exam Browser.")
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("settings_user_agent_input"),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingSwitchItem(
              title = "Anti-Screenshot & Rekam Layar (FLAG_SECURE)",
              subtitle = "Mencegah tangkapan layar selama ujian berlangsung.",
              checked = blockScreenshots,
              onCheckedChange = { blockScreenshots = it },
            )
            SettingSwitchItem(
              title = "Kunci Layar / Screen Pinning",
              subtitle = "Mengaktifkan mode kunci tugas sistem Android saat ujian.",
              checked = enableKioskPinning,
              onCheckedChange = { enableKioskPinning = it },
            )
            SettingSwitchItem(
              title = "Nonaktifkan Copy / Paste",
              subtitle = "Memblokir seleksi teks soal dan menu salin tempel.",
              checked = disableCopyPaste,
              onCheckedChange = { disableCopyPaste = it },
            )
            SettingSwitchItem(
              title = "Izinkan Zoom Halaman",
              subtitle = "Aktifkan jika tampilan soal membutuhkan cubit-zoom.",
              checked = allowZoom,
              onCheckedChange = { allowZoom = it },
            )

            Button(
              onClick = {
                onSaveDefaults(
                  exitPin,
                  userAgent,
                  blockScreenshots,
                  enableKioskPinning,
                  disableCopyPaste,
                  allowZoom,
                )
              },
              modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Simpan Pengaturan")
            }
          }
        }
      }

      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Pemeliharaan Data Browser",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text =
                "Bersihkan cookie login peserta sebelumnya, cache halaman, dan penyimpanan lokal WebView sebelum pergantian sesi ujian.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                onShowMessage("Cache dan cookie WebView berhasil dibersihkan.")
              },
              modifier = Modifier.fillMaxWidth().testTag("clear_webview_cache_button"),
            ) {
              Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Hapus Cache & Cookie Browser")
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun SettingSwitchItem(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
