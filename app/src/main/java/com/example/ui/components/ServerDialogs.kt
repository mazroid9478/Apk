package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.ExamServerEntity

@Composable
fun AddOrEditServerDialog(
  initialServer: ExamServerEntity?,
  defaults: BrowserPreferences,
  onDismiss: () -> Unit,
  onSave:
    (
      name: String,
      url: String,
      token: String,
      exitPin: String,
      allowedHostsCsv: String,
      blockScreenshots: Boolean,
      enableKioskPinning: Boolean,
      allowZoom: Boolean,
      disableCopyPaste: Boolean,
    ) -> Unit,
) {
  var name by remember(initialServer) { mutableStateOf(initialServer?.name.orEmpty()) }
  var url by remember(initialServer) { mutableStateOf(initialServer?.baseUrl.orEmpty()) }
  var token by remember(initialServer) { mutableStateOf(initialServer?.examToken.orEmpty()) }
  var exitPin by
    remember(initialServer) {
      mutableStateOf(initialServer?.exitPin ?: defaults.defaultExitPin)
    }
  var allowedHosts by
    remember(initialServer) { mutableStateOf(initialServer?.allowedHostsCsv.orEmpty()) }
  var blockScreenshots by
    remember(initialServer) {
      mutableStateOf(initialServer?.blockScreenshots ?: defaults.defaultBlockScreenshots)
    }
  var enableKiosk by
    remember(initialServer) {
      mutableStateOf(initialServer?.enableKioskPinning ?: defaults.defaultEnableKioskPinning)
    }
  var disableCopyPaste by
    remember(initialServer) {
      mutableStateOf(initialServer?.disableCopyPaste ?: defaults.defaultDisableCopyPaste)
    }
  var allowZoom by
    remember(initialServer) { mutableStateOf(initialServer?.allowZoom ?: defaults.defaultAllowZoom) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialServer == null) "Tambah Server Soal" else "Edit Server Soal",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nama Server / Ujian") },
          placeholder = { Text("Contoh: CBT Sekolah Utama") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("dialog_server_name_input"),
        )
        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("URL Portal atau Endpoint JSON") },
          placeholder = { Text("https://cbt.sekolah.sch.id") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
          modifier = Modifier.fillMaxWidth().testTag("dialog_server_url_input"),
        )
        OutlinedTextField(
          value = token,
          onValueChange = { token = it },
          label = { Text("Token Server (Opsional)") },
          placeholder = { Text("Contoh: UJIAN2026") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("dialog_server_token_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = exitPin,
            onValueChange = { if (it.length <= 8) exitPin = it },
            label = { Text("PIN Keluar") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.weight(0.42f).testTag("dialog_server_pin_input"),
          )
          OutlinedTextField(
            value = allowedHosts,
            onValueChange = { allowedHosts = it },
            label = { Text("Domain Diizinkan") },
            placeholder = { Text("cbt.sch.id,cdn.sch.id") },
            singleLine = true,
            modifier = Modifier.weight(0.58f).testTag("dialog_server_hosts_input"),
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Kebijakan Keamanan Ujian",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )

        PolicyToggleRow(
          title = "Blokir Screenshot & Rekam Layar",
          checked = blockScreenshots,
          onCheckedChange = { blockScreenshots = it },
        )
        PolicyToggleRow(
          title = "Kunci Layar / Screen Pinning",
          checked = enableKiosk,
          onCheckedChange = { enableKiosk = it },
        )
        PolicyToggleRow(
          title = "Nonaktifkan Salin-Tempel (Copy/Paste)",
          checked = disableCopyPaste,
          onCheckedChange = { disableCopyPaste = it },
        )
        PolicyToggleRow(
          title = "Izinkan Zoom Halaman",
          checked = allowZoom,
          onCheckedChange = { allowZoom = it },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            name,
            url,
            token,
            exitPin,
            allowedHosts,
            blockScreenshots,
            enableKiosk,
            allowZoom,
            disableCopyPaste,
          )
          onDismiss()
        },
        enabled = url.isNotBlank(),
        modifier = Modifier.testTag("dialog_save_server_button"),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    },
  )
}

@Composable
private fun PolicyToggleRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun ImportJsonConfigDialog(
  onDismiss: () -> Unit,
  onImport: (String) -> Unit,
) {
  var jsonInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Impor Konfigurasi JSON Server",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text =
            "Tempel payload JSON dari Server Soal atau QR Config untuk mengatur URL ujian, domain whitelist, dan PIN keluar otomatis.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = jsonInput,
          onValueChange = { jsonInput = it },
          label = { Text("JSON Konfigurasi Server Soal") },
          placeholder = {
            Text(
              "{\n  \"exam_title\": \"UAS Matematika\",\n  \"exam_url\": \"https://cbt.sekolah.sch.id/ujian\",\n  \"exit_pin\": \"4589\"\n}"
            )
          },
          minLines = 5,
          maxLines = 8,
          textStyle = MaterialTheme.typography.labelSmall,
          modifier = Modifier.fillMaxWidth().testTag("import_json_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onImport(jsonInput)
          onDismiss()
        },
        enabled = jsonInput.isNotBlank(),
        modifier = Modifier.testTag("confirm_import_json_button"),
      ) {
        Text("Impor")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Tutup")
      }
    },
  )
}
