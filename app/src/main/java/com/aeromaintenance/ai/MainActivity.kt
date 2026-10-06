package com.aeromaintenance.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aeromaintenance.ai.notify.AlertNotifier
import com.aeromaintenance.ai.ui.AeroApp
import com.aeromaintenance.ai.ui.theme.AeroTheme

class MainActivity : ComponentActivity() {

    private val vm: AeroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        AlertNotifier.createChannel(this)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            AeroTheme {
                AeroApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Optional extras so a presenter (or the CI screenshot script) can open any
     * screen directly, e.g.
     * adb shell am start -n com.aeromaintenance.ai/.MainActivity --es screen analysis --es condition abnormal --ez run_analysis true
     */
    private fun handleIntent(intent: Intent?) {
        val i = intent ?: return
        if (i.extras == null) return
        vm.applyLaunchOptions(
            LaunchOptions(
                screen = i.getStringExtra(EXTRA_SCREEN),
                aircraft = i.getStringExtra("aircraft"),
                condition = i.getStringExtra("condition"),
                runAnalysis = i.getBooleanExtra("run_analysis", false),
                startDemo = i.getBooleanExtra("demo", false),
                tab = if (i.hasExtra("tab")) i.getIntExtra("tab", 0) else null,
                generateReport = i.getBooleanExtra("report", false),
            ),
        )
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
    }
}
