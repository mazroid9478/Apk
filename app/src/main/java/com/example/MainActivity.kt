package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ExamViewModel
import com.example.ui.screens.ExamBrowserScreen
import com.example.ui.screens.HistoryAndApiScreen
import com.example.ui.screens.PortalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ExamGateTheme

private enum class ExamTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  PORTAL(
    route = "portal",
    labelRes = R.string.tab_portal,
    selectedIcon = Icons.Filled.Dns,
    unselectedIcon = Icons.Outlined.Dns,
    testTag = "nav_tab_portal",
  ),
  HISTORY(
    route = "history",
    labelRes = R.string.tab_history,
    selectedIcon = Icons.Filled.History,
    unselectedIcon = Icons.Outlined.History,
    testTag = "nav_tab_history",
  ),
  SETTINGS(
    route = "settings",
    labelRes = R.string.tab_settings,
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_tab_settings",
  ),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ExamGateTheme {
        ExamGateRoot()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamGateRoot(examViewModel: ExamViewModel = viewModel()) {
  val uiState by examViewModel.uiState.collectAsStateWithLifecycle()
  val activeSession = uiState.activeSession

  if (activeSession != null) {
    ExamBrowserScreen(
      session = activeSession,
      onFocusLoss = examViewModel::recordFocusLoss,
      onDismissFocusWarning = examViewModel::dismissFocusWarning,
      onBlockedNavigation = examViewModel::recordBlockedNavigation,
      onTitleUpdated = examViewModel::updateActiveExamTitle,
      onExitExam = examViewModel::endExamSession,
    )
    return
  }

  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      examViewModel.dismissSnackbar()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Text(
              text = stringResource(id = R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
            )
          },
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.background
            ),
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar {
            ExamTab.entries.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(id = tab.labelRes),
                  )
                },
                label = { Text(stringResource(id = tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            ExamTab.entries.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(id = tab.labelRes),
                  )
                },
                label = { Text(stringResource(id = tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          NavHost(navController = navController, startDestination = ExamTab.PORTAL.route) {
            composable(ExamTab.PORTAL.route) {
              PortalScreen(
                uiState = uiState,
                onQuickUrlChange = examViewModel::onQuickUrlChange,
                onQuickTokenChange = examViewModel::onQuickTokenChange,
                onQuickStudentIdChange = examViewModel::onQuickStudentIdChange,
                onCheckServer = examViewModel::checkQuickServerAndSave,
                onLaunchQuickExam = examViewModel::launchQuickExam,
                onSyncSavedServer = examViewModel::syncSavedServer,
                onLaunchSavedServer = examViewModel::launchServerExam,
                onSaveServerProfile = examViewModel::saveNewServerProfile,
                onDeleteServer = examViewModel::deleteServer,
                onImportJsonConfig = examViewModel::importJsonServerConfig,
              )
            }
            composable(ExamTab.HISTORY.route) {
              HistoryAndApiScreen(
                sessionLogs = uiState.sessionLogs,
                onClearLogs = examViewModel::clearSessionLogs,
                onCopiedNotification = examViewModel::showMessage,
              )
            }
            composable(ExamTab.SETTINGS.route) {
              SettingsScreen(
                preferences = uiState.preferences,
                onSaveDefaults = examViewModel::updateSecurityDefaults,
                onShowMessage = examViewModel::showMessage,
              )
            }
          }
        }
      }
    }
  }
}
