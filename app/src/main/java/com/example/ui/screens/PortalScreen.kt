package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ExamServerEntity
import com.example.data.local.IntegrationType
import com.example.data.local.ServerStatus
import com.example.ui.ExamUiState
import com.example.ui.SyncBannerState
import com.example.ui.components.AddOrEditServerDialog
import com.example.ui.components.ImportJsonConfigDialog
import com.example.ui.theme.Crimson100
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy900

@Composable
fun PortalScreen(
  uiState: ExamUiState,
  onQuickUrlChange: (String) -> Unit,
  onQuickTokenChange: (String) -> Unit,
  onQuickStudentIdChange: (String) -> Unit,
  onCheckServer: (Boolean) -> Unit,
  onLaunchQuickExam: () -> Unit,
  onSyncSavedServer: (ExamServerEntity) -> Unit,
  onLaunchSavedServer: (ExamServerEntity) -> Unit,
  onSaveServerProfile:
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
      editingId: Long?,
    ) -> Unit,
  onDeleteServer: (Long) -> Unit,
  onImportJsonConfig: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddEditDialog by remember { mutableStateOf(false) }
  var editingServer by remember { mutableStateOf<ExamServerEntity?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        HeroPortalCard(
          blockScreenshots = uiState.preferences.defaultBlockScreenshots,
          kioskEnabled = uiState.preferences.defaultEnableKioskPinning,
          serverCount = uiState.servers.size,
        )
      }

      item {
        QuickConnectCard(
          url = uiState.quickUrl,
          token = uiState.quickToken,
          studentId = uiState.quickStudentId,
          syncBanner = uiState.syncBanner,
          onUrlChange = onQuickUrlChange,
          onTokenChange = onQuickTokenChange,
          onStudentIdChange = onQuickStudentIdChange,
          onCheckOnly = { onCheckServer(false) },
          onCheckAndSave = { onCheckServer(true) },
          onLaunchExam = onLaunchQuickExam,
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Server Soal Tersimpan",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Pilih profil CBT atau endpoint API sekolah",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { showImportDialog = true },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.testTag("open_import_json_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Impor JSON",
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("JSON")
            }
            FilledTonalButton(
              onClick = {
                editingServer = null
                showAddEditDialog = true
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.testTag("add_server_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Server",
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Tambah")
            }
          }
        }
      }

      if (uiState.servers.isEmpty()) {
        item {
          EmptyServerCard(
            onAddClick = {
              editingServer = null
              showAddEditDialog = true
            },
            onImportClick = { showImportDialog = true },
          )
        }
      } else {
        items(uiState.servers, key = { it.id }) { server ->
          SavedServerCard(
            server = server,
            onSync = { onSyncSavedServer(server) },
            onLaunch = { onLaunchSavedServer(server) },
            onEdit = {
              editingServer = server
              showAddEditDialog = true
            },
            onDelete = { onDeleteServer(server.id) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (showAddEditDialog) {
    AddOrEditServerDialog(
      initialServer = editingServer,
      defaults = uiState.preferences,
      onDismiss = { showAddEditDialog = false },
      onSave = { name, url, token, exitPin, hosts, blockSs, kiosk, zoom, copyPaste ->
        onSaveServerProfile(
          name,
          url,
          token,
          exitPin,
          hosts,
          blockSs,
          kiosk,
          zoom,
          copyPaste,
          editingServer?.id,
        )
      },
    )
  }

  if (showImportDialog) {
    ImportJsonConfigDialog(
      onDismiss = { showImportDialog = false },
      onImport = onImportJsonConfig,
    )
  }
}

@Composable
private fun HeroPortalCard(
  blockScreenshots: Boolean,
  kioskEnabled: Boolean,
  serverCount: Int,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = Navy900),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(172.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_exam_hero),
        contentDescription = "Ilustrasi portal ujian ExamGate",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors = listOf(Navy900.copy(alpha = 0.94f), Navy900.copy(alpha = 0.52f))
              )
            )
      )
      Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
          Text(
            text = "ExamGate CBT Browser",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Browser ujian terkunci dengan sinkronisasi API & portal Server Soal.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.90f),
            maxLines = 2,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadinessBadge(
              label = if (blockScreenshots) "Anti-Screenshot" else "Screenshot Bebas",
              active = blockScreenshots,
            )
            ReadinessBadge(
              label = if (kioskEnabled) "Kiosk Pinning" else "Mode Standar",
              active = kioskEnabled,
            )
            ReadinessBadge(label = "$serverCount Server", active = serverCount > 0)
          }
        }
      }
    }
  }
}

@Composable
private fun ReadinessBadge(label: String, active: Boolean) {
  Surface(
    color = if (active) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = if (active) Icons.Outlined.VerifiedUser else Icons.Default.Lock,
        contentDescription = null,
        tint = if (active) Color(0xFF6EE7B7) else Color(0xFFFCD34D),
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
      )
    }
  }
}

@Composable
private fun QuickConnectCard(
  url: String,
  token: String,
  studentId: String,
  syncBanner: SyncBannerState,
  onUrlChange: (String) -> Unit,
  onTokenChange: (String) -> Unit,
  onStudentIdChange: (String) -> Unit,
  onCheckOnly: () -> Unit,
  onCheckAndSave: () -> Unit,
  onLaunchExam: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
          imageVector = Icons.Default.Dns,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Column {
          Text(
            text = "Koneksi Cepat Server Soal",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Mendukung URL Web CBT (Moodle/CandyCBT/LAN) atau Endpoint JSON",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        label = { Text("URL Server Soal / Endpoint API") },
        placeholder = { Text("https://cbt.sekolah.sch.id atau http://192.168.1.10/ujian") },
        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
        singleLine = true,
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().testTag("quick_url_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = token,
          onValueChange = onTokenChange,
          label = { Text("Token (Opsional)") },
          leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("quick_token_input"),
        )
        OutlinedTextField(
          value = studentId,
          onValueChange = onStudentIdChange,
          label = { Text("ID Peserta") },
          placeholder = { Text("NIS / NIM") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("quick_student_id_input"),
        )
      }

      AnimatedVisibility(visible = syncBanner !is SyncBannerState.Idle) {
        SyncResultStatusBox(syncBanner = syncBanner)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onCheckOnly,
          enabled = url.isNotBlank() && syncBanner !is SyncBannerState.Checking,
          modifier = Modifier.weight(1f).testTag("check_server_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Cek Server")
        }

        FilledTonalIconButton(
          onClick = onCheckAndSave,
          enabled = url.isNotBlank() && syncBanner !is SyncBannerState.Checking,
          modifier = Modifier.size(48.dp).testTag("sync_and_save_server_button"),
        ) {
          Icon(
            imageVector = Icons.Default.BookmarkAdd,
            contentDescription = "Sinkron dan Simpan ke Daftar Server",
          )
        }

        Button(
          onClick = onLaunchExam,
          enabled = url.isNotBlank(),
          modifier = Modifier.weight(1.1f).testTag("start_quick_exam_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Login,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Mulai Ujian")
        }
      }
    }
  }
}

@Composable
private fun SyncResultStatusBox(syncBanner: SyncBannerState) {
  when (syncBanner) {
    is SyncBannerState.Checking -> {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Text(
            text = "Menghubungi Server Soal...",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
    is SyncBannerState.Connected -> {
      val info = syncBanner.summary
      Surface(
        color = Emerald100.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().testTag("sync_success_banner"),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = info.examTitle,
              style = MaterialTheme.typography.labelLarge,
              color = Color(0xFF064E3B),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "${info.latencyMs} ms",
              style = MaterialTheme.typography.labelSmall,
              color = Emerald600,
            )
          }
          Text(
            text =
              if (info.integrationType == IntegrationType.JSON_CONFIG_API) {
                "Mode JSON API • Domain: ${info.allowedHosts.joinToString(", ")}"
              } else {
                "Mode Portal Web CBT • HTTP ${info.httpCode}"
              },
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF065F46),
          )
        }
      }
    }
    is SyncBannerState.Error -> {
      Surface(
        color = Crimson100.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().testTag("sync_error_banner"),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Crimson600,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = syncBanner.message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF7F1D1D),
          )
        }
      }
    }
    SyncBannerState.Idle -> Unit
  }
}

@Composable
private fun SavedServerCard(
  server: ExamServerEntity,
  onSync: () -> Unit,
  onLaunch: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    modifier = Modifier.fillMaxWidth().testTag("saved_server_card_${server.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = server.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = server.resolvedExamUrl ?: server.baseUrl,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ServerStatusPill(status = server.lastStatus, latencyMs = server.lastLatencyMs)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PolicyChip(
          text =
            if (server.integrationType == IntegrationType.JSON_CONFIG_API) "JSON API"
            else "Web CBT"
        )
        if (server.blockScreenshots) PolicyChip(text = "Anti-Screenshot")
        if (server.enableKioskPinning) PolicyChip(text = "Kiosk Lock")
        PolicyChip(text = "PIN: ${server.exitPin}")
      }

      if (!server.lastErrorMessage.isNullOrBlank() && server.lastStatus == ServerStatus.FAILED) {
        Text(
          text = server.lastErrorMessage,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(onClick = onSync, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sinkronisasi Server",
            )
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Server",
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Hapus Server",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }

        Button(
          onClick = onLaunch,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("launch_server_button_${server.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Masuk Ujian")
        }
      }
    }
  }
}

@Composable
private fun ServerStatusPill(status: String, latencyMs: Long?) {
  val (bg, fg, label) =
    when (status) {
      ServerStatus.CONNECTED ->
        Triple(
          Emerald100,
          Emerald600,
          if (latencyMs != null) "TERHUBUNG • ${latencyMs}ms" else "TERHUBUNG",
        )
      ServerStatus.FAILED -> Triple(Crimson100, Crimson600, "GAGAL")
      else ->
        Triple(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          "BELUM DIUJI",
        )
    }

  Surface(color = bg, shape = RoundedCornerShape(50)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = fg,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun PolicyChip(text: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun EmptyServerCard(
  onAddClick: () -> Unit,
  onImportClick: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      ),
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Dns,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp),
      )
      Text(
        text = "Belum Ada Profil Server Soal",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text =
          "Tambahkan alamat server ujian sekolah/kampus (Moodle, CandyCBT, Google Forms, atau endpoint JSON) agar siswa bisa langsung masuk ujian.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onImportClick) { Text("Impor JSON") }
        Button(onClick = onAddClick) { Text("Tambah Server Soal") }
      }
    }
  }
}
