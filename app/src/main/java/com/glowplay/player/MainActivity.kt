package com.glowplay.player

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.glowplay.player.ui.navigation.GlowPlayNav
import com.glowplay.player.ui.theme.GlowPlayTheme
import com.glowplay.player.util.MediaPermissions

class MainActivity : AppCompatActivity() {
    private var granted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.any { it } || hasMediaPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        granted = hasMediaPermission()
        setContent {
            GlowPlayTheme {
                GlowPlayNav(
                    permissionGranted = granted,
                    onRequestPermission = {
                        permissionLauncher.launch(MediaPermissions.required())
                    },
                )
            }
        }
        if (!granted) {
            permissionLauncher.launch(MediaPermissions.required())
        }
    }

    override fun onResume() {
        super.onResume()
        granted = hasMediaPermission()
    }

    private fun hasMediaPermission(): Boolean {
        return MediaPermissions.required().any { permission ->
            ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
