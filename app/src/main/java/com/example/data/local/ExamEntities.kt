package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object IntegrationType {
  const val DIRECT_WEB = "DIRECT_WEB"
  const val JSON_CONFIG_API = "JSON_CONFIG_API"
}

object ServerStatus {
  const val CONNECTED = "TERHUBUNG"
  const val FAILED = "GAGAL"
  const val UNTESTED = "BELUM_DIUJI"
}

object SessionCompletionStatus {
  const val COMPLETED = "SELESAI"
  const val EXITED_EARLY = "KELUAR_SESI"
  const val ERROR_DISCONNECTED = "TERPUTUS"
}

@Entity(tableName = "exam_servers")
data class ExamServerEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val baseUrl: String,
  val integrationType: String = IntegrationType.DIRECT_WEB,
  val examToken: String = "",
  val exitPin: String = "1234",
  val allowedHostsCsv: String = "",
  val blockScreenshots: Boolean = true,
  val enableKioskPinning: Boolean = true,
  val allowZoom: Boolean = false,
  val disableCopyPaste: Boolean = true,
  val customUserAgent: String = "",
  val lastStatus: String = ServerStatus.UNTESTED,
  val lastLatencyMs: Long? = null,
  val lastCheckedAt: Long? = null,
  val resolvedExamTitle: String? = null,
  val resolvedExamUrl: String? = null,
  val lastErrorMessage: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "exam_session_logs")
data class ExamSessionLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val serverName: String,
  val examTitle: String,
  val examUrl: String,
  val studentId: String,
  val startedAt: Long,
  val endedAt: Long? = null,
  val durationSeconds: Long = 0,
  val focusLossCount: Int = 0,
  val blockedNavCount: Int = 0,
  val completionStatus: String = SessionCompletionStatus.COMPLETED,
)
