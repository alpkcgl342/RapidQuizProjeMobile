package com.alpkcgl.rapidquizmobile

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.alpkcgl.rapidquizmobile.ui.navigation.AppNavHost
import com.alpkcgl.rapidquizmobile.ui.theme.RapidQuizTheme
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Yalnızca açık tema: sistem çubuğu ikonları her zaman koyu.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                RqColors.Background.toArgb(),
                RqColors.Background.toArgb(),
            ),
        )
        requestLocalNetworkForDebug()
        setContent {
            RapidQuizTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RqColors.Background,
                    contentColor = RqColors.Text,
                ) {
                    AppNavHost()
                }
            }
        }
    }

    /**
     * Debug'da API bilgisayardaki Django'dur (yerel ağ). Android 17+ bunun için
     * ACCESS_LOCAL_NETWORK iznini ister; izin yoksa istekler sessizce zaman aşımına düşer.
     */
    private fun requestLocalNetworkForDebug() {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return
        if (checkSelfPermission(LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(LOCAL_NETWORK), 0)
        }
    }

    private companion object {
        const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
