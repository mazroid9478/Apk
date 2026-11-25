package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExamSessionLogEntity
import com.example.ui.theme.Crimson100
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SAMPLE_SERVER_JSON =
  """{
  "server_name": "CBT Utama Sekolah",
  "exam_title": "Penilaian Akhir Semester",
  "exam_url": "https://cbt.sekolah.sch.id/login",
  "allowed_hosts": ["cbt.sekolah.sch.id", "cdn.sekolah.sch.id"],
  "exit_pin": "2026",
  "block_screenshot": true,
  "require_kiosk": true,
  "disable_copy_paste": true,
  "allow_zoom": false,
  "user_agent": "ExamGate-Browser/1.0"
}"""

@Composable
fun HistoryAndApiScreen(
  sessionLogs: List<ExamSessionLogEntity>,
  onClearLogs: () -> Unit,
  onCopiedNotification: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboardManager = LocalClipboardManager.current

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ServerIntegrationSpecCard(
          onCopyJson = {
            clipboardManager.setText(AnnotatedString(SAMPLE_SERVER_JSON))
            onCopiedNotification("Contoh JSON Server Soal disalin ke clipboard.")
          }
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
              text = "Riwayat Integritas Ujian",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Catatan durasi sesi, pindah aplikasi, dan blokir domain",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (sessionLogs.isNotEmpty()) {
            TextButton(
              onClick = onClearLogs,
              modifier = Modifier.testTag("clear_logs_button"),
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Bersihkan")
            }
          }
        }
      }

      if (sessionLogs.isEmpty()) {
        item {
          Card(
            shape = MaterialTheme.shapes.large,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
              )
              Text(
                text = "Belum Ada Riwayat Sesi Ujian",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text =
                  "Setiap sesi ujian yang dijalankan melalui ExamGate akan tercatat otomatis di sini untuk audit pengawas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(sessionLogs, key = { it.id }) { log ->
          SessionLogCard(log = log)
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun ServerIntegrationSpecCard(onCopyJson: () -> Unit) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Http,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Protokol Integrasi Server Soal",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Text(
        text =
          "Server Soal (PHP, Laravel, Node.js, Moodle, atau CBT lokal) dapat memvalidasi browser ini lewat HTTP Header berikut:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text =
            "User-Agent: ExamGate-Browser/1.0 (Android; CBT-Secure)\n" +
              "X-Exam-Browser: ExamGate/1.0\n" +
              "X-Exam-Token: <token_opsional>\n" +
              "X-Student-Id: <nis_nim_opsional>",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMono,
          modifier = Modifier.padding(12.dp),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Code,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Respons JSON Opsional dari Endpoint Server Soal:",
          style = MaterialTheme.typography.labelLarge,
        )
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = SAMPLE_SERVER_JSON,
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMono,
          modifier = Modifier.padding(12.dp),
        )
      }

      OutlinedButton(
        onClick = onCopyJson,
        modifier = Modifier.align(Alignment.End).testTag("copy_json_schema_button"),
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Salin Contoh JSON")
      }
    }
  }
}

@Composable
private fun SessionLogCard(log: ExamSessionLogEntity) {
  val totalViolations = log.focusLossCount + log.blockedNavCount
  val isClean = totalViolations == 0

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = log.examTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${log.serverName} • ${formatTimestamp(log.startedAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          color = if (isClean) Emerald100 else Crimson100,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector =
                if (isClean) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
              contentDescription = null,
              tint = if (isClean) Emerald600 else Crimson600,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (isClean) "Bersih" else "$totalViolations Peringatan",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isClean) Emerald600 else Crimson600,
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            "Durasi: ${formatElapsedTime(log.durationSeconds)}" +
              if (log.studentId.isNotBlank()) " • ID: ${log.studentId}" else "",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMono,
        )
        Text(
          text = "Keluar Layar: ${log.focusLossCount}x • Blokir: ${log.blockedNavCount}x",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun formatTimestamp(millis: Long): String {
  return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    .format(Date(millis))
}
