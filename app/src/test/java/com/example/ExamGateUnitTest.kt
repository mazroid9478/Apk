package com.example

import com.example.data.local.IntegrationType
import com.example.data.network.ExamServerClient
import com.example.data.network.ServerSyncResult
import com.example.ui.screens.formatElapsedTime
import com.example.ui.screens.isHostAllowed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamGateUnitTest {
  private val client = ExamServerClient()

  @Test
  fun normalizeUrl_addsHttpsWhenMissing() {
    assertEquals("https://cbt.sekolah.sch.id", client.normalizeUrl("cbt.sekolah.sch.id"))
    assertEquals("http://192.168.1.10/ujian", client.normalizeUrl("http://192.168.1.10/ujian"))
  }

  @Test
  fun parseManualJsonConfig_parsesAllServerSoalFields() {
    val json =
      """
      {
        "server_name": "CBT SMAN 1",
        "exam_title": "UAS Fisika",
        "exam_url": "https://ujian.sman1.sch.id/mulai",
        "allowed_hosts": ["ujian.sman1.sch.id", "cdn.sman1.sch.id"],
        "exit_pin": "9090",
        "block_screenshot": true,
        "require_kiosk": true,
        "disable_copy_paste": true,
        "allow_zoom": false
      }
      """
        .trimIndent()

    val result = client.parseManualJsonConfig(json)
    assertTrue(result is ServerSyncResult.Success)
    val success = result as ServerSyncResult.Success
    assertEquals("CBT SMAN 1", success.serverName)
    assertEquals("UAS Fisika", success.examTitle)
    assertEquals("https://ujian.sman1.sch.id/mulai", success.resolvedExamUrl)
    assertEquals(IntegrationType.JSON_CONFIG_API, success.integrationType)
    assertEquals("9090", success.exitPin)
    assertEquals(listOf("ujian.sman1.sch.id", "cdn.sman1.sch.id"), success.allowedHosts)
    assertEquals(true, success.blockScreenshots)
    assertEquals(true, success.enableKioskPinning)
  }

  @Test
  fun isHostAllowed_enforcesWhitelistAndSubdomains() {
    val allowed = listOf("cbt.sekolah.sch.id", "sso.kemdikbud.go.id")
    assertTrue(isHostAllowed("cbt.sekolah.sch.id", allowed))
    assertTrue(isHostAllowed("sub.cbt.sekolah.sch.id", allowed))
    assertFalse(isHostAllowed("google.com", allowed))
    assertFalse(isHostAllowed("evilcbt.sekolah.sch.id.attacker.com", allowed))
  }

  @Test
  fun formatElapsedTime_formatsMinutesAndHours() {
    assertEquals("01:05", formatElapsedTime(65))
    assertEquals("01:01:05", formatElapsedTime(3665))
  }
}
