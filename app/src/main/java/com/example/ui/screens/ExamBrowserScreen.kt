package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.SessionCompletionStatus
import com.example.ui.ActiveExamSession
import com.example.ui.theme.Amber100
import com.example.ui.theme.Crimson100
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy900
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamBrowserScreen(
  session: ActiveExamSession,
  onFocusLoss: () -> Unit,
  onDismissFocusWarning: () -> Unit,
  onBlockedNavigation: (String) -> Unit,
  onTitleUpdated: (String) -> Unit,
  onExitExam: (String) -> Unit,
) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val lifecycleOwner = LocalLifecycleOwner.current

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var pageProgress by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var loadErrorMessage by remember { mutableStateOf<String?>(null) }
  var showExitDialog by remember { mutableStateOf(false) }
  var isPickingFile by remember { mutableStateOf(false) }
  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

  val filePickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
      isPickingFile = false
      pendingFileCallback?.onReceiveValue(uris.toTypedArray())
      pendingFileCallback = null
    }

  // Enforce FLAG_SECURE, Immersive Mode, and optional LockTask Screen Pinning
  DisposableEffect(activity, session.blockScreenshots, session.enableKioskPinning) {
    if (activity != null) {
      if (session.blockScreenshots) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
      val windowInsetsController =
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

      if (session.enableKioskPinning) {
        try {
          activity.startLockTask()
        } catch (_: Exception) {
          // Graceful fallback if device policy restricts lock task
        }
      }
    }

    onDispose {
      if (activity != null) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val windowInsetsController =
          WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        try {
          activity.stopLockTask()
        } catch (_: Exception) {
          // Ignore if not currently in lock task
        }
      }
    }
  }

  // Detect leaving the exam app (focus loss / app switching)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP && !isPickingFile && !showExitDialog) {
        onFocusLoss()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  BackHandler {
    val wv = webViewRef
    if (wv != null && wv.canGoBack()) {
      wv.goBack()
    } else {
      showExitDialog = true
    }
  }

  val customHeaders =
    remember(session.examToken, session.studentId) {
      buildMap {
        put("X-Exam-Browser", "ExamGate/1.0")
        if (session.examToken.isNotBlank()) put("X-Exam-Token", session.examToken)
        if (session.studentId.isNotBlank()) put("X-Student-Id", session.studentId)
      }
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Exam Security Status Strip
    Surface(color = Navy900, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            IconButton(
              onClick = {
                val wv = webViewRef
                if (wv != null && wv.canGoBack()) wv.goBack()
              },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali Halaman",
                tint = Color.White,
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = session.examTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = formatElapsedTime(session.elapsedSeconds),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF93C5FD),
                  fontFamily = JetBrainsMono,
                )
                IntegrityIndicatorChip(
                  focusLossCount = session.focusLossCount,
                  blockedNavCount = session.blockedNavCount,
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                loadErrorMessage = null
                webViewRef?.reload()
              },
              modifier = Modifier.size(48.dp).testTag("exam_reload_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Muat Ulang Soal",
                tint = Color.White,
              )
            }
            IconButton(
              onClick = { showExitDialog = true },
              modifier = Modifier.size(48.dp).testTag("exam_exit_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Keluar Ujian",
                tint = Color(0xFFFCA5A5),
              )
            }
          }
        }

        if (isLoading && pageProgress in 1..99) {
          LinearProgressIndicator(
            progress = { pageProgress / 100f },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Color(0xFF38BDF8),
            trackColor = Navy900,
          )
        }
      }
    }

    // Security & Integrity Warnings
    AnimatedVisibility(visible = session.lastFocusWarningVisible) {
      Surface(color = Crimson100, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = null,
              tint = Crimson600,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                "Peringatan Pengawas: Aktivitas keluar layar tercatat (${session.focusLossCount}x).",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF7F1D1D),
            )
          }
          TextButton(onClick = onDismissFocusWarning) {
            Text("Paham", color = Crimson600)
          }
        }
      }
    }

    AnimatedVisibility(visible = session.lastBlockedHost != null) {
      Surface(color = Amber100, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            tint = Color(0xFFB45309),
            modifier = Modifier.size(16.dp),
          )
          Text(
            text =
              "Akses ke '${session.lastBlockedHost}' diblokir karena di luar whitelist Server Soal.",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF78350F),
          )
        }
      }
    }

    // Main WebView Container
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      AndroidView(
        modifier = Modifier.fillMaxSize().testTag("exam_webview"),
        factory = { viewContext ->
          WebView(viewContext).apply {
            layoutParams =
              ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
              )
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              useWideViewPort = true
              loadWithOverviewMode = true
              setSupportZoom(session.allowZoom)
              builtInZoomControls = session.allowZoom
              displayZoomControls = false
              mediaPlaybackRequiresUserGesture = false
              mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
              userAgentString = session.userAgent
            }

            if (session.disableCopyPaste) {
              isLongClickable = false
              setOnLongClickListener { true }
              isHapticFeedbackEnabled = false
            }

            webViewClient =
              object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                  view: WebView?,
                  request: WebResourceRequest?,
                ): Boolean {
                  val targetUri = request?.url ?: return false
                  val scheme = targetUri.scheme?.lowercase().orEmpty()
                  val host = targetUri.host?.lowercase().orEmpty()

                  if (scheme != "http" && scheme != "https") {
                    onBlockedNavigation(scheme.ifBlank { "tautan eksternal" })
                    return true
                  }

                  if (!isHostAllowed(host, session.allowedHosts)) {
                    onBlockedNavigation(host.ifBlank { targetUri.toString() })
                    return true
                  }
                  return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                  isLoading = true
                  loadErrorMessage = null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                  isLoading = false
                  if (session.disableCopyPaste) {
                    view?.evaluateJavascript(
                      "(function(){var s=document.createElement('style');s.innerHTML='*{-webkit-user-select:none!important;user-select:none!important;-webkit-touch-callout:none!important;}input,textarea{-webkit-user-select:text!important;user-select:text!important;}';document.head.appendChild(s);})();",
                      null,
                    )
                  }
                }

                override fun onReceivedError(
                  view: WebView?,
                  request: WebResourceRequest?,
                  error: WebResourceError?,
                ) {
                  if (request?.isForMainFrame == true) {
                    isLoading = false
                    loadErrorMessage =
                      error?.description?.toString()
                        ?: "Tidak dapat memuat halaman Server Soal."
                  }
                }
              }

            webChromeClient =
              object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                  pageProgress = newProgress
                  if (newProgress >= 100) isLoading = false
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                  if (!title.isNullOrBlank()) {
                    onTitleUpdated(title)
                  }
                }

                override fun onShowFileChooser(
                  webView: WebView?,
                  filePathCallback: ValueCallback<Array<Uri>>?,
                  fileChooserParams: FileChooserParams?,
                ): Boolean {
                  pendingFileCallback?.onReceiveValue(null)
                  pendingFileCallback = filePathCallback
                  isPickingFile = true
                  val mimeTypes =
                    fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
                      ?: arrayOf("image/*", "application/pdf")
                  filePickerLauncher.launch(
                    if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes
                  )
                  return true
                }
              }

            webViewRef = this
            loadUrl(session.targetUrl, customHeaders)
          }
        },
      )

      if (loadErrorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxSize(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Default.WifiOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Koneksi ke Server Soal Terputus",
              style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = loadErrorMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(
                onClick = { onExitExam(SessionCompletionStatus.ERROR_DISCONNECTED) }
              ) {
                Text("Keluar Sesi")
              }
              Button(
                onClick = {
                  loadErrorMessage = null
                  webViewRef?.loadUrl(session.targetUrl, customHeaders)
                }
              ) {
                Text("Coba Muat Ulang")
              }
            }
          }
        }
      }
    }
  }

  if (showExitDialog) {
    ProctorExitPinDialog(
      expectedPin = session.exitPin,
      focusLossCount = session.focusLossCount,
      onDismiss = { showExitDialog = false },
      onConfirmExit = {
        showExitDialog = false
        try {
          activity?.stopLockTask()
        } catch (_: Exception) {
        }
        onExitExam(SessionCompletionStatus.COMPLETED)
      },
    )
  }
}

@Composable
private fun IntegrityIndicatorChip(focusLossCount: Int, blockedNavCount: Int) {
  val total = focusLossCount + blockedNavCount
  val isClean = total == 0
  Surface(
    color =
      if (isClean) Emerald600.copy(alpha = 0.25f) else Crimson600.copy(alpha = 0.35f),
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        tint = if (isClean) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
        modifier = Modifier.size(12.dp),
      )
      Text(
        text = if (isClean) "Aman" else "$total Pelanggaran",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
      )
    }
  }
}

@Composable
private fun ProctorExitPinDialog(
  expectedPin: String,
  focusLossCount: Int,
  onDismiss: () -> Unit,
  onConfirmExit: () -> Unit,
) {
  var enteredPin by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = { Text("Akhiri Sesi Ujian?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Masukkan PIN Pengawas untuk keluar dari browser ujian dan membuka kunci layar.",
          style = MaterialTheme.typography.bodyMedium,
        )
        if (focusLossCount > 0) {
          Text(
            text = "Catatan integritas: terdeteksi $focusLossCount kali berpindah aplikasi.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
        OutlinedTextField(
          value = enteredPin,
          onValueChange = {
            enteredPin = it
            pinError = null
          },
          label = { Text("PIN Pengawas") },
          placeholder = { Text("Masukkan PIN") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = pinError != null,
          supportingText = pinError?.let { msg -> { Text(msg) } },
          modifier = Modifier.fillMaxWidth().testTag("exit_pin_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (enteredPin.trim() == expectedPin.trim()) {
            onConfirmExit()
          } else {
            pinError = "PIN Pengawas salah. Periksa kembali PIN pada pengaturan server."
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_exit_exam_button"),
      ) {
        Text("Keluar Ujian")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Lanjutkan Ujian")
      }
    },
  )
}

internal fun isHostAllowed(host: String, allowedHosts: List<String>): Boolean {
  if (host.isBlank() || allowedHosts.isEmpty()) return false
  val normalizedHost = host.lowercase()
  return allowedHosts.any { allowed ->
    val clean = allowed.trim().lowercase()
    clean.isNotEmpty() &&
      (normalizedHost == clean || normalizedHost.endsWith(".$clean"))
  }
}

internal fun formatElapsedTime(totalSeconds: Long): String {
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
