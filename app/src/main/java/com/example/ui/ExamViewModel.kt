package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BrowserPreferences
import com.example.data.local.BrowserPreferencesRepository
import com.example.data.local.ExamDatabase
import com.example.data.local.ExamServerEntity
import com.example.data.local.ExamSessionLogEntity
import com.example.data.local.IntegrationType
import com.example.data.local.ServerStatus
import com.example.data.local.SessionCompletionStatus
import com.example.data.network.ExamServerClient
import com.example.data.network.ServerSyncResult
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SyncBannerState {
  data object Idle : SyncBannerState

  data class Checking(val targetUrl: String) : SyncBannerState

  data class Connected(val summary: ServerSyncResult.Success) : SyncBannerState

  data class Error(val message: String) : SyncBannerState
}

data class ActiveExamSession(
  val logId: Long,
  val serverName: String,
  val examTitle: String,
  val targetUrl: String,
  val examToken: String,
  val studentId: String,
  val exitPin: String,
  val allowedHosts: List<String>,
  val blockScreenshots: Boolean,
  val enableKioskPinning: Boolean,
  val allowZoom: Boolean,
  val disableCopyPaste: Boolean,
  val userAgent: String,
  val startedAt: Long,
  val elapsedSeconds: Long = 0L,
  val focusLossCount: Int = 0,
  val blockedNavCount: Int = 0,
  val lastBlockedHost: String? = null,
  val lastFocusWarningVisible: Boolean = false,
)

data class ExamUiState(
  val servers: List<ExamServerEntity> = emptyList(),
  val sessionLogs: List<ExamSessionLogEntity> = emptyList(),
  val preferences: BrowserPreferences = BrowserPreferences(),
  val quickUrl: String = "",
  val quickToken: String = "",
  val quickStudentId: String = "",
  val syncBanner: SyncBannerState = SyncBannerState.Idle,
  val activeSession: ActiveExamSession? = null,
  val snackbarMessage: String? = null,
)

class ExamViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: ExamRepository =
    ExamRepository(
      examDao = ExamDatabase.getInstance(application).examDao(),
      preferencesRepository = BrowserPreferencesRepository(application),
      serverClient = ExamServerClient(),
    )

  private val _quickUrl = MutableStateFlow("")
  private val _quickToken = MutableStateFlow("")
  private val _quickStudentId = MutableStateFlow("")
  private val _syncBanner = MutableStateFlow<SyncBannerState>(SyncBannerState.Idle)
  private val _activeSession = MutableStateFlow<ActiveExamSession?>(null)
  private val _snackbarMessage = MutableStateFlow<String?>(null)

  private var quickInputsInitialized = false
  private var timerJob: Job? = null

  private val localInputsFlow =
    combine(_quickUrl, _quickToken, _quickStudentId, _syncBanner, _activeSession) {
      url,
      token,
      studentId,
      syncBanner,
      activeSession ->
      LocalInputs(url, token, studentId, syncBanner, activeSession)
    }

  val uiState: StateFlow<ExamUiState> =
    combine(
        repository.serversFlow,
        repository.sessionLogsFlow,
        repository.preferencesFlow,
        localInputsFlow,
        _snackbarMessage,
      ) { servers, logs, prefs, inputs, snackbar ->
        if (!quickInputsInitialized && (prefs.lastQuickUrl.isNotEmpty() || prefs.lastStudentId.isNotEmpty())) {
          quickInputsInitialized = true
          if (_quickUrl.value.isEmpty()) _quickUrl.value = prefs.lastQuickUrl
          if (_quickStudentId.value.isEmpty()) _quickStudentId.value = prefs.lastStudentId
        }
        ExamUiState(
          servers = servers,
          sessionLogs = logs,
          preferences = prefs,
          quickUrl = inputs.url,
          quickToken = inputs.token,
          quickStudentId = inputs.studentId,
          syncBanner = inputs.syncBanner,
          activeSession = inputs.activeSession,
          snackbarMessage = snackbar,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExamUiState(),
      )

  fun onQuickUrlChange(value: String) {
    _quickUrl.value = value
  }

  fun onQuickTokenChange(value: String) {
    _quickToken.value = value
  }

  fun onQuickStudentIdChange(value: String) {
    _quickStudentId.value = value
  }

  fun dismissSnackbar() {
    _snackbarMessage.value = null
  }

  fun showMessage(message: String) {
    _snackbarMessage.value = message
  }

  fun checkQuickServerAndSave(saveAsProfile: Boolean = false) {
    val rawUrl = _quickUrl.value.trim()
    val token = _quickToken.value.trim()
    val studentId = _quickStudentId.value.trim()
    val prefs = uiState.value.preferences

    viewModelScope.launch {
      _syncBanner.value = SyncBannerState.Checking(rawUrl)
      repository.saveLastQuickInputs(rawUrl, studentId)
      val result =
        repository.verifyAndSyncServer(
          rawUrl = rawUrl,
          examToken = token,
          studentId = studentId,
          userAgent = prefs.defaultUserAgent,
        )
      when (result) {
        is ServerSyncResult.Success -> {
          _syncBanner.value = SyncBannerState.Connected(result)
          if (saveAsProfile) {
            repository.saveOrUpdateServerFromSync(
              existingServerId = null,
              customName = null,
              syncResult = result,
              examToken = token,
              defaults = prefs,
            )
            _snackbarMessage.value = "Server Soal '${result.examTitle}' tersimpan."
          }
        }
        is ServerSyncResult.Error -> {
          _syncBanner.value = SyncBannerState.Error(result.message)
        }
      }
    }
  }

  fun syncSavedServer(server: ExamServerEntity) {
    val prefs = uiState.value.preferences
    val token = _quickToken.value.ifBlank { server.examToken }
    val studentId = _quickStudentId.value
    val ua = server.customUserAgent.ifBlank { prefs.defaultUserAgent }

    viewModelScope.launch {
      _syncBanner.value = SyncBannerState.Checking(server.baseUrl)
      val result =
        repository.verifyAndSyncServer(
          rawUrl = server.baseUrl,
          examToken = token,
          studentId = studentId,
          userAgent = ua,
        )
      when (result) {
        is ServerSyncResult.Success -> {
          repository.saveOrUpdateServerFromSync(
            existingServerId = server.id,
            customName = server.name,
            syncResult = result,
            examToken = token,
            defaults = prefs,
          )
          _syncBanner.value = SyncBannerState.Connected(result)
          _snackbarMessage.value = "Sinkronisasi '${server.name}' berhasil (${result.latencyMs} ms)."
        }
        is ServerSyncResult.Error -> {
          repository.markServerSyncFailed(server.id, result.message)
          _syncBanner.value = SyncBannerState.Error(result.message)
        }
      }
    }
  }

  fun saveNewServerProfile(
    name: String,
    url: String,
    token: String,
    exitPin: String,
    allowedHostsInput: String,
    blockScreenshots: Boolean,
    enableKioskPinning: Boolean,
    allowZoom: Boolean,
    disableCopyPaste: Boolean,
    editingServerId: Long? = null,
  ) {
    val normalized = repository.normalizeUrl(url)
    val primaryHost = repository.extractHost(normalized)
    if (normalized.isBlank() || primaryHost == null) {
      _snackbarMessage.value = "URL Server Soal tidak valid."
      return
    }
    val hostsList =
      allowedHostsInput
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf(primaryHost) }

    val prefs = uiState.value.preferences
    viewModelScope.launch {
      val server =
        ExamServerEntity(
          id = editingServerId ?: 0L,
          name = name.trim().ifBlank { "Server $primaryHost" },
          baseUrl = normalized,
          integrationType = IntegrationType.DIRECT_WEB,
          examToken = token.trim(),
          exitPin = exitPin.trim().ifBlank { prefs.defaultExitPin },
          allowedHostsCsv = hostsList.joinToString(","),
          blockScreenshots = blockScreenshots,
          enableKioskPinning = enableKioskPinning,
          allowZoom = allowZoom,
          disableCopyPaste = disableCopyPaste,
          customUserAgent = prefs.defaultUserAgent,
          lastStatus = ServerStatus.UNTESTED,
        )
      repository.upsertServer(server)
      _snackbarMessage.value =
        if (editingServerId == null) "Server '${server.name}' ditambahkan."
        else "Server '${server.name}' diperbarui."
    }
  }

  fun importJsonServerConfig(jsonText: String) {
    val prefs = uiState.value.preferences
    when (val result = repository.parseManualJsonConfig(jsonText)) {
      is ServerSyncResult.Success -> {
        viewModelScope.launch {
          val saved =
            repository.saveOrUpdateServerFromSync(
              existingServerId = null,
              customName = result.serverName ?: result.examTitle,
              syncResult = result,
              examToken = result.token.orEmpty(),
              defaults = prefs,
            )
          _quickUrl.value = saved.baseUrl
          if (saved.examToken.isNotBlank()) _quickToken.value = saved.examToken
          _syncBanner.value = SyncBannerState.Connected(result)
          _snackbarMessage.value = "Konfigurasi '${saved.name}' berhasil diimpor."
        }
      }
      is ServerSyncResult.Error -> {
        _snackbarMessage.value = result.message
      }
    }
  }

  fun deleteServer(serverId: Long) {
    viewModelScope.launch {
      repository.deleteServer(serverId)
      _snackbarMessage.value = "Server Soal dihapus."
    }
  }

  fun launchQuickExam() {
    val rawUrl = _quickUrl.value.trim()
    val prefs = uiState.value.preferences
    val syncSuccess = (_syncBanner.value as? SyncBannerState.Connected)?.summary

    val targetUrl =
      if (syncSuccess != null &&
        (syncSuccess.normalizedRequestUrl == repository.normalizeUrl(rawUrl) || rawUrl.isBlank())
      ) {
        syncSuccess.resolvedExamUrl
      } else {
        repository.normalizeUrl(rawUrl)
      }

    val primaryHost = repository.extractHost(targetUrl)
    if (targetUrl.isBlank() || primaryHost == null) {
      _snackbarMessage.value = "Masukkan URL Server Soal yang valid terlebih dahulu."
      return
    }

    val allowedHosts = syncSuccess?.allowedHosts ?: listOf(primaryHost)
    val examTitle = syncSuccess?.examTitle ?: "Ujian Online ($primaryHost)"
    val serverName = syncSuccess?.serverName ?: primaryHost
    val token = _quickToken.value.trim().ifBlank { syncSuccess?.token.orEmpty() }
    val studentId = _quickStudentId.value.trim()

    viewModelScope.launch {
      repository.saveLastQuickInputs(rawUrl.ifBlank { targetUrl }, studentId)
      startExamSession(
        serverName = serverName,
        examTitle = examTitle,
        targetUrl = targetUrl,
        examToken = token,
        studentId = studentId,
        exitPin = syncSuccess?.exitPin ?: prefs.defaultExitPin,
        allowedHosts = allowedHosts,
        blockScreenshots = syncSuccess?.blockScreenshots ?: prefs.defaultBlockScreenshots,
        enableKioskPinning = syncSuccess?.enableKioskPinning ?: prefs.defaultEnableKioskPinning,
        allowZoom = syncSuccess?.allowZoom ?: prefs.defaultAllowZoom,
        disableCopyPaste = syncSuccess?.disableCopyPaste ?: prefs.defaultDisableCopyPaste,
        userAgent = syncSuccess?.customUserAgent ?: prefs.defaultUserAgent,
      )
    }
  }

  fun launchServerExam(server: ExamServerEntity) {
    val prefs = uiState.value.preferences
    val targetUrl = server.resolvedExamUrl?.takeIf { it.isNotBlank() } ?: server.baseUrl
    val fallbackHost = repository.extractHost(targetUrl)
    val allowedHosts =
      server.allowedHostsCsv
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOfNotNull(fallbackHost) }

    val token = _quickToken.value.trim().ifBlank { server.examToken }
    val studentId = _quickStudentId.value.trim()

    viewModelScope.launch {
      repository.saveLastQuickInputs(server.baseUrl, studentId)
      startExamSession(
        serverName = server.name,
        examTitle = server.resolvedExamTitle ?: server.name,
        targetUrl = targetUrl,
        examToken = token,
        studentId = studentId,
        exitPin = server.exitPin.ifBlank { prefs.defaultExitPin },
        allowedHosts = allowedHosts,
        blockScreenshots = server.blockScreenshots,
        enableKioskPinning = server.enableKioskPinning,
        allowZoom = server.allowZoom,
        disableCopyPaste = server.disableCopyPaste,
        userAgent = server.customUserAgent.ifBlank { prefs.defaultUserAgent },
      )
    }
  }

  private suspend fun startExamSession(
    serverName: String,
    examTitle: String,
    targetUrl: String,
    examToken: String,
    studentId: String,
    exitPin: String,
    allowedHosts: List<String>,
    blockScreenshots: Boolean,
    enableKioskPinning: Boolean,
    allowZoom: Boolean,
    disableCopyPaste: Boolean,
    userAgent: String,
  ) {
    val now = System.currentTimeMillis()
    val logId =
      repository.insertSessionLog(
        ExamSessionLogEntity(
          serverName = serverName,
          examTitle = examTitle,
          examUrl = targetUrl,
          studentId = studentId,
          startedAt = now,
        )
      )

    _activeSession.value =
      ActiveExamSession(
        logId = logId,
        serverName = serverName,
        examTitle = examTitle,
        targetUrl = targetUrl,
        examToken = examToken,
        studentId = studentId,
        exitPin = exitPin,
        allowedHosts = allowedHosts,
        blockScreenshots = blockScreenshots,
        enableKioskPinning = enableKioskPinning,
        allowZoom = allowZoom,
        disableCopyPaste = disableCopyPaste,
        userAgent = userAgent,
        startedAt = now,
      )

    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        while (true) {
          delay(1_000L)
          _activeSession.update { current ->
            current?.copy(
              elapsedSeconds = ((System.currentTimeMillis() - current.startedAt) / 1000L).coerceAtLeast(0L)
            )
          }
        }
      }
  }

  fun recordFocusLoss() {
    _activeSession.update { session ->
      session?.copy(
        focusLossCount = session.focusLossCount + 1,
        lastFocusWarningVisible = true,
      )
    }
  }

  fun dismissFocusWarning() {
    _activeSession.update { session -> session?.copy(lastFocusWarningVisible = false) }
  }

  fun recordBlockedNavigation(hostOrScheme: String) {
    _activeSession.update { session ->
      session?.copy(
        blockedNavCount = session.blockedNavCount + 1,
        lastBlockedHost = hostOrScheme,
      )
    }
  }

  fun updateActiveExamTitle(title: String) {
    if (title.isBlank()) return
    _activeSession.update { session ->
      session?.copy(examTitle = title.take(64))
    }
  }

  fun endExamSession(status: String = SessionCompletionStatus.COMPLETED) {
    timerJob?.cancel()
    timerJob = null
    val session = _activeSession.value ?: return
    _activeSession.value = null
    val endedAt = System.currentTimeMillis()
    val duration = ((endedAt - session.startedAt) / 1000L).coerceAtLeast(0L)

    viewModelScope.launch {
      repository.updateSessionLog(
        ExamSessionLogEntity(
          id = session.logId,
          serverName = session.serverName,
          examTitle = session.examTitle,
          examUrl = session.targetUrl,
          studentId = session.studentId,
          startedAt = session.startedAt,
          endedAt = endedAt,
          durationSeconds = duration,
          focusLossCount = session.focusLossCount,
          blockedNavCount = session.blockedNavCount,
          completionStatus = status,
        )
      )
    }
  }

  fun updateSecurityDefaults(
    exitPin: String,
    userAgent: String,
    blockScreenshots: Boolean,
    enableKioskPinning: Boolean,
    disableCopyPaste: Boolean,
    allowZoom: Boolean,
  ) {
    viewModelScope.launch {
      repository.updateSecurityDefaults(
        exitPin = exitPin,
        userAgent = userAgent,
        blockScreenshots = blockScreenshots,
        enableKioskPinning = enableKioskPinning,
        disableCopyPaste = disableCopyPaste,
        allowZoom = allowZoom,
      )
      _snackbarMessage.value = "Pengaturan keamanan default disimpan."
    }
  }

  fun clearSessionLogs() {
    viewModelScope.launch {
      repository.clearSessionLogs()
      _snackbarMessage.value = "Riwayat sesi ujian dibersihkan."
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }

  private data class LocalInputs(
    val url: String,
    val token: String,
    val studentId: String,
    val syncBanner: SyncBannerState,
    val activeSession: ActiveExamSession?,
  )
}
