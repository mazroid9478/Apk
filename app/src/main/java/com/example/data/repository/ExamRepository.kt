package com.example.data.repository

import com.example.data.local.BrowserPreferences
import com.example.data.local.BrowserPreferencesRepository
import com.example.data.local.ExamDao
import com.example.data.local.ExamServerEntity
import com.example.data.local.ExamSessionLogEntity
import com.example.data.local.ServerStatus
import com.example.data.network.ExamServerClient
import com.example.data.network.ServerSyncResult
import kotlinx.coroutines.flow.Flow

class ExamRepository(
  private val examDao: ExamDao,
  private val preferencesRepository: BrowserPreferencesRepository,
  private val serverClient: ExamServerClient,
) {
  val serversFlow: Flow<List<ExamServerEntity>> = examDao.observeAllServers()
  val sessionLogsFlow: Flow<List<ExamSessionLogEntity>> = examDao.observeSessionLogs()
  val preferencesFlow: Flow<BrowserPreferences> = preferencesRepository.preferencesFlow

  fun normalizeUrl(rawUrl: String): String = serverClient.normalizeUrl(rawUrl)

  fun extractHost(url: String): String? = serverClient.extractHost(url)

  suspend fun verifyAndSyncServer(
    rawUrl: String,
    examToken: String,
    studentId: String,
    userAgent: String,
  ): ServerSyncResult = serverClient.verifyAndSyncServer(rawUrl, examToken, studentId, userAgent)

  fun parseManualJsonConfig(jsonText: String): ServerSyncResult =
    serverClient.parseManualJsonConfig(jsonText)

  suspend fun saveOrUpdateServerFromSync(
    existingServerId: Long?,
    customName: String?,
    syncResult: ServerSyncResult.Success,
    examToken: String,
    defaults: BrowserPreferences,
  ): ExamServerEntity {
    val existing = existingServerId?.let { examDao.getServerById(it) }
    val resolvedName =
      customName?.takeIf { it.isNotBlank() }
        ?: syncResult.serverName?.takeIf { it.isNotBlank() }
        ?: existing?.name
        ?: syncResult.examTitle

    val entity =
      ExamServerEntity(
        id = existing?.id ?: 0,
        name = resolvedName,
        baseUrl = syncResult.normalizedRequestUrl,
        integrationType = syncResult.integrationType,
        examToken = syncResult.token ?: examToken.ifBlank { existing?.examToken.orEmpty() },
        exitPin = syncResult.exitPin ?: existing?.exitPin ?: defaults.defaultExitPin,
        allowedHostsCsv = syncResult.allowedHosts.joinToString(","),
        blockScreenshots =
          syncResult.blockScreenshots
            ?: existing?.blockScreenshots
            ?: defaults.defaultBlockScreenshots,
        enableKioskPinning =
          syncResult.enableKioskPinning
            ?: existing?.enableKioskPinning
            ?: defaults.defaultEnableKioskPinning,
        allowZoom = syncResult.allowZoom ?: existing?.allowZoom ?: defaults.defaultAllowZoom,
        disableCopyPaste =
          syncResult.disableCopyPaste
            ?: existing?.disableCopyPaste
            ?: defaults.defaultDisableCopyPaste,
        customUserAgent =
          syncResult.customUserAgent
            ?: existing?.customUserAgent
            ?: defaults.defaultUserAgent,
        lastStatus = ServerStatus.CONNECTED,
        lastLatencyMs = syncResult.latencyMs,
        lastCheckedAt = System.currentTimeMillis(),
        resolvedExamTitle = syncResult.examTitle,
        resolvedExamUrl = syncResult.resolvedExamUrl,
        lastErrorMessage = null,
        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
      )

    val savedId = examDao.insertServer(entity)
    return entity.copy(id = if (entity.id == 0L) savedId else entity.id)
  }

  suspend fun markServerSyncFailed(serverId: Long, errorMessage: String) {
    val existing = examDao.getServerById(serverId) ?: return
    examDao.updateServer(
      existing.copy(
        lastStatus = ServerStatus.FAILED,
        lastCheckedAt = System.currentTimeMillis(),
        lastErrorMessage = errorMessage,
      )
    )
  }

  suspend fun upsertServer(server: ExamServerEntity): Long = examDao.insertServer(server)

  suspend fun deleteServer(serverId: Long) = examDao.deleteServerById(serverId)

  suspend fun insertSessionLog(log: ExamSessionLogEntity): Long = examDao.insertSessionLog(log)

  suspend fun updateSessionLog(log: ExamSessionLogEntity) = examDao.updateSessionLog(log)

  suspend fun clearSessionLogs() = examDao.clearAllSessionLogs()

  suspend fun updateSecurityDefaults(
    exitPin: String,
    userAgent: String,
    blockScreenshots: Boolean,
    enableKioskPinning: Boolean,
    disableCopyPaste: Boolean,
    allowZoom: Boolean,
  ) =
    preferencesRepository.updateSecurityDefaults(
      exitPin = exitPin,
      userAgent = userAgent,
      blockScreenshots = blockScreenshots,
      enableKioskPinning = enableKioskPinning,
      disableCopyPaste = disableCopyPaste,
      allowZoom = allowZoom,
    )

  suspend fun saveLastQuickInputs(url: String, studentId: String) =
    preferencesRepository.saveLastQuickInputs(url, studentId)
}
