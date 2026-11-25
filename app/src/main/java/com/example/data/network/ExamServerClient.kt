package com.example.data.network

import com.example.data.local.IntegrationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class ServerSoalConfigDto(
  @Json(name = "server_name") val serverName: String? = null,
  @Json(name = "exam_title") val examTitle: String? = null,
  @Json(name = "nama_ujian") val namaUjian: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "exam_url") val examUrl: String? = null,
  @Json(name = "url_ujian") val urlUjian: String? = null,
  @Json(name = "url") val url: String? = null,
  @Json(name = "allowed_hosts") val allowedHosts: List<String>? = null,
  @Json(name = "domain_izinkan") val domainIzinkan: List<String>? = null,
  @Json(name = "exit_pin") val exitPin: String? = null,
  @Json(name = "pin_keluar") val pinKeluar: String? = null,
  @Json(name = "block_screenshot") val blockScreenshot: Boolean? = null,
  @Json(name = "anti_screenshot") val antiScreenshot: Boolean? = null,
  @Json(name = "require_kiosk") val requireKiosk: Boolean? = null,
  @Json(name = "kunci_layar") val kunciLayar: Boolean? = null,
  @Json(name = "allow_zoom") val allowZoom: Boolean? = null,
  @Json(name = "disable_copy_paste") val disableCopyPaste: Boolean? = null,
  @Json(name = "user_agent") val userAgent: String? = null,
  @Json(name = "token") val token: String? = null,
  @Json(name = "status") val status: String? = null,
  @Json(name = "message") val message: String? = null,
)

sealed interface ServerSyncResult {
  data class Success(
    val normalizedRequestUrl: String,
    val resolvedExamUrl: String,
    val examTitle: String,
    val serverName: String?,
    val integrationType: String,
    val httpCode: Int,
    val latencyMs: Long,
    val allowedHosts: List<String>,
    val exitPin: String?,
    val blockScreenshots: Boolean?,
    val enableKioskPinning: Boolean?,
    val allowZoom: Boolean?,
    val disableCopyPaste: Boolean?,
    val customUserAgent: String?,
    val token: String?,
    val serverMessage: String?,
  ) : ServerSyncResult

  data class Error(
    val normalizedRequestUrl: String,
    val message: String,
    val httpCode: Int? = null,
  ) : ServerSyncResult
}

class ExamServerClient {
  private val okHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val configAdapter = moshi.adapter(ServerSoalConfigDto::class.java)

  fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("http://", ignoreCase = true) ||
      trimmed.startsWith("https://", ignoreCase = true)
    ) {
      trimmed
    } else {
      "https://$trimmed"
    }
  }

  fun extractHost(url: String): String? {
    return try {
      URI(normalizeUrl(url)).host?.lowercase()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun verifyAndSyncServer(
    rawUrl: String,
    examToken: String,
    studentId: String,
    userAgent: String,
  ): ServerSyncResult =
    withContext(Dispatchers.IO) {
      val normalizedUrl = normalizeUrl(rawUrl)
      if (normalizedUrl.isBlank()) {
        return@withContext ServerSyncResult.Error(
          normalizedRequestUrl = rawUrl,
          message = "URL Server Soal tidak boleh kosong.",
        )
      }

      val host = extractHost(normalizedUrl)
      if (host.isNullOrBlank()) {
        return@withContext ServerSyncResult.Error(
          normalizedRequestUrl = normalizedUrl,
          message = "Format URL tidak valid. Contoh: https://cbt.sekolah.sch.id",
        )
      }

      val requestBuilder =
        Request.Builder()
          .url(normalizedUrl)
          .get()
          .header("User-Agent", userAgent)
          .header("X-Exam-Browser", "ExamGate/1.0")
          .header("Accept", "application/json, text/html;q=0.9, */*;q=0.8")

      if (examToken.isNotBlank()) {
        requestBuilder.header("X-Exam-Token", examToken.trim())
      }
      if (studentId.isNotBlank()) {
        requestBuilder.header("X-Student-Id", studentId.trim())
      }

      val startTime = System.currentTimeMillis()
      try {
        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
          val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
          val code = response.code
          val contentType = response.header("Content-Type").orEmpty().lowercase()
          val finalUrl = response.request.url.toString()
          val bodyString = response.body?.string().orEmpty()

          if (!response.isSuccessful) {
            return@withContext ServerSyncResult.Error(
              normalizedRequestUrl = normalizedUrl,
              httpCode = code,
              message =
                when (code) {
                  401,
                  403 -> "Akses ditolak server (HTTP $code). Periksa Token Ujian atau User-Agent."
                  404 -> "Endpoint Server Soal tidak ditemukan (HTTP 404)."
                  in 500..599 -> "Server Soal sedang mengalami gangguan (HTTP $code)."
                  else -> "Server merespons dengan kode HTTP $code."
                },
            )
          }

          val trimmedBody = bodyString.trimStart()
          val looksLikeJson =
            contentType.contains("application/json") || trimmedBody.startsWith("{")

          if (looksLikeJson) {
            val dto =
              try {
                configAdapter.fromJson(bodyString)
              } catch (_: Exception) {
                null
              }

            if (dto != null) {
              val resolvedUrlRaw = dto.examUrl ?: dto.urlUjian ?: dto.url
              val resolvedUrl =
                if (!resolvedUrlRaw.isNullOrBlank()) {
                  normalizeUrl(resolvedUrlRaw)
                } else {
                  finalUrl
                }
              val resolvedHost = extractHost(resolvedUrl) ?: host
              val hosts =
                (dto.allowedHosts ?: dto.domainIzinkan)
                  ?.map { it.trim().lowercase() }
                  ?.filter { it.isNotBlank() }
                  ?.ifEmpty { listOf(resolvedHost) } ?: listOf(resolvedHost, host).distinct()

              val title =
                dto.examTitle
                  ?: dto.namaUjian
                  ?: dto.title
                  ?: "Ujian Terintegrasi ($resolvedHost)"

              return@withContext ServerSyncResult.Success(
                normalizedRequestUrl = normalizedUrl,
                resolvedExamUrl = resolvedUrl,
                examTitle = title,
                serverName = dto.serverName,
                integrationType = IntegrationType.JSON_CONFIG_API,
                httpCode = code,
                latencyMs = latency,
                allowedHosts = hosts,
                exitPin = dto.exitPin ?: dto.pinKeluar,
                blockScreenshots = dto.blockScreenshot ?: dto.antiScreenshot,
                enableKioskPinning = dto.requireKiosk ?: dto.kunciLayar,
                allowZoom = dto.allowZoom,
                disableCopyPaste = dto.disableCopyPaste,
                customUserAgent = dto.userAgent,
                token = dto.token,
                serverMessage = dto.message,
              )
            }
          }

          val extractedTitle = extractHtmlTitle(bodyString) ?: "Portal Ujian ($host)"
          val finalHost = extractHost(finalUrl) ?: host
          return@withContext ServerSyncResult.Success(
            normalizedRequestUrl = normalizedUrl,
            resolvedExamUrl = finalUrl,
            examTitle = extractedTitle,
            serverName = null,
            integrationType = IntegrationType.DIRECT_WEB,
            httpCode = code,
            latencyMs = latency,
            allowedHosts = listOf(host, finalHost).distinct(),
            exitPin = null,
            blockScreenshots = null,
            enableKioskPinning = null,
            allowZoom = null,
            disableCopyPaste = null,
            customUserAgent = null,
            token = null,
            serverMessage = "Portal web CBT terhubung (${latency} ms).",
          )
        }
      } catch (_: UnknownHostException) {
        return@withContext ServerSyncResult.Error(
          normalizedRequestUrl = normalizedUrl,
          message = "Domain atau alamat IP Server Soal tidak ditemukan. Periksa koneksi Wi-Fi/internet.",
        )
      } catch (_: SSLException) {
        return@withContext ServerSyncResult.Error(
          normalizedRequestUrl = normalizedUrl,
          message = "Sertifikat SSL/HTTPS bermasalah. Jika server lokal sekolah, gunakan http://.",
        )
      } catch (e: IOException) {
        return@withContext ServerSyncResult.Error(
          normalizedRequestUrl = normalizedUrl,
          message = "Gagal menghubungi Server Soal: ${e.localizedMessage ?: "Timeout koneksi"}.",
        )
      }
    }

  fun parseManualJsonConfig(jsonText: String): ServerSyncResult {
    val trimmed = jsonText.trim()
    if (!trimmed.startsWith("{")) {
      return ServerSyncResult.Error(
        normalizedRequestUrl = "",
        message = "Format JSON tidak valid. Harus diawali tanda '{'.",
      )
    }
    return try {
      val dto =
        configAdapter.fromJson(trimmed)
          ?: return ServerSyncResult.Error(
            normalizedRequestUrl = "",
            message = "JSON konfigurasi kosong atau tidak dapat dibaca.",
          )
      val rawTarget = dto.examUrl ?: dto.urlUjian ?: dto.url.orEmpty()
      val resolvedUrl = normalizeUrl(rawTarget)
      val host = extractHost(resolvedUrl)
      if (resolvedUrl.isBlank() || host == null) {
        return ServerSyncResult.Error(
          normalizedRequestUrl = "",
          message = "JSON harus memiliki field 'exam_url' atau 'url' yang valid.",
        )
      }
      val hosts =
        (dto.allowedHosts ?: dto.domainIzinkan)
          ?.map { it.trim().lowercase() }
          ?.filter { it.isNotBlank() }
          ?.ifEmpty { listOf(host) } ?: listOf(host)

      ServerSyncResult.Success(
        normalizedRequestUrl = resolvedUrl,
        resolvedExamUrl = resolvedUrl,
        examTitle = dto.examTitle ?: dto.namaUjian ?: dto.title ?: "Ujian ($host)",
        serverName = dto.serverName,
        integrationType = IntegrationType.JSON_CONFIG_API,
        httpCode = 200,
        latencyMs = 0L,
        allowedHosts = hosts,
        exitPin = dto.exitPin ?: dto.pinKeluar,
        blockScreenshots = dto.blockScreenshot ?: dto.antiScreenshot,
        enableKioskPinning = dto.requireKiosk ?: dto.kunciLayar,
        allowZoom = dto.allowZoom,
        disableCopyPaste = dto.disableCopyPaste,
        customUserAgent = dto.userAgent,
        token = dto.token,
        serverMessage = dto.message ?: "Konfigurasi JSON berhasil diimpor.",
      )
    } catch (e: Exception) {
      ServerSyncResult.Error(
        normalizedRequestUrl = "",
        message = "Gagal parsing JSON: ${e.localizedMessage ?: "Periksa sintaks JSON."}",
      )
    }
  }

  private fun extractHtmlTitle(html: String): String? {
    val regex = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val raw = regex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return null
    val cleaned = raw.replace(Regex("\\s+"), " ").trim()
    return cleaned.takeIf { it.isNotBlank() }?.take(80)
  }
}
