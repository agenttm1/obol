package com.tmstudio.obol

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tmstudio.obol.ui.navigation.ObolApp
import com.tmstudio.obol.ui.theme.ObolTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val preferences = (application as ObolApplication).container.preferences
        setContent {
            // null dok se postavka ne pročita — tada se ne crta ništa, da svijetla tema ne bljesne tamnom.
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val mode = themeMode ?: return@setContent
            val dark = mode.isDark()

            // Ikone statusne i navigacijske trake moraju biti vidljive na podlozi teme.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            ObolTheme(darkTheme = dark) {
                ObolApp()
            }
        }
    }
}
