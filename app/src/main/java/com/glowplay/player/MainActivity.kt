package com.glowplay.player

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.glowplay.player.ui.navigation.GlowPlayNav
import com.glowplay.player.ui.theme.GlowPlayTheme
import com.glowplay.player.util.MediaPermissions

class MainActivity : AppCompatActivity() {
    private var granted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.any { it } || MediaPermissions.granted(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        granted = MediaPermissions.granted(this)
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
        granted = MediaPermissions.granted(this)
    }
}
