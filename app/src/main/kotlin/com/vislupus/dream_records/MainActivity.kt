package com.vislupus.dream_records

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.config.Strings
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.nav.AppNav
import com.vislupus.dream_records.ui.theme.DreamRecordsTheme
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = AppConfig.Theme.background.toArgb()
        window.navigationBarColor = AppConfig.Theme.background.toArgb()
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            DreamRecordsTheme {
                CompositionLocalProvider(LocalStrings provides Strings.forCode(settings.languageCode)) {
                    AppNav(vm)
                }
            }
        }
    }

    /** Called by the Record screen while recording when AppConfig says to keep the screen on. */
    fun setKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
