package com.roamly.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.roamly.app.settings.ColorSettings
import com.roamly.app.ui.screens.MainScreen
import com.roamly.app.ui.theme.ColorViewModel
import com.roamly.app.ui.theme.LocalAppColors
import com.roamly.app.ui.theme.getColorsByConfig
import com.roamly.app.websocket.NotificationHelper
import com.roamly.app.websocket.SockJSManager
import com.roamly.app.websocket.WebSocketService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class
MainActivity : ComponentActivity() {
    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var webSocketService: WebSocketService

    lateinit var navController: NavHostController

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SockJSManager.getInstance().setNotificationHelper(notificationHelper)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        webSocketService.initialize()
        val openNotifications = intent?.getBooleanExtra("open_notifications", false) ?: false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SockJSManager.getInstance().connectionState.collect { state ->
                    when (state) {
                        is SockJSManager.ConnectionState.Connecting -> {
                            Log.e("MainActivity", "🔄 SockJS CONNECTING...")
                        }
                        is SockJSManager.ConnectionState.Connected -> {
                            Log.d("MainActivity", "✅ SockJS CONNECTED")
                        }
                        is SockJSManager.ConnectionState.Disconnected -> {
                            Log.e("MainActivity", "🔴 SockJS DISCONNECTED")
                        }
                        is SockJSManager.ConnectionState.Failed -> {
                            Log.e("MainActivity", "❌ SockJS FAILED: ${state.reason}")
                        }
                    }
                }
            }
        }



        setContent {
            val context = LocalContext.current

            // Создание ViewModel через Factory (для передачи UserSettings)
            // viewModel() теперь доступен благодаря импорту
            val viewModel: ColorViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ColorViewModel(ColorSettings(context)) as T
                    }
                }
            )

            val currentThemeConfig by viewModel.currentTheme.collectAsState()

            // Игнорируем системный Dark Mode (передаем false)
            val targetColors = getColorsByConfig(currentThemeConfig, false)

            CompositionLocalProvider(
                LocalAppColors provides targetColors
            ) {
                MaterialTheme {
                    MainScreen()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val permission = Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                REQUEST_CODE_NOTIFICATION_PERMISSION
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
    }
}