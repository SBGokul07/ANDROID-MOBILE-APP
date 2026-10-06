package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.R
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType

@Composable
fun AboutScreen(padding: PaddingValues) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("title") {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF0B1A2E)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(96.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("AeroMaintenance AI", style = MaterialTheme.typography.headlineMedium, color = Aero.Text)
                Text("Version 1.0 prototype", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                Spacer(Modifier.height(12.dp))
                Text(
                    "An AI-assisted predictive maintenance prototype for aircraft health monitoring and component risk assessment.",
                    style = MaterialTheme.typography.bodyLarge, color = Aero.Text,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        item("disclaimer") {
            Panel(accent = Aero.Amber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = Aero.Amber)
                    Spacer(Modifier.width(10.dp))
                    Text("IMPORTANT", style = AeroType.Tag, color = Aero.Amber)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "This application is an academic research prototype using simulated/synthetic data. It is not certified for real-world aviation safety, maintenance, or flight-critical decision-making.",
                    style = MaterialTheme.typography.bodyLarge, color = Aero.Text,
                )
            }
        }
        item("facts") {
            Panel {
                Fact("Data", "All aircraft, telemetry, alerts and work orders are generated on the phone. No aircraft or airline data source is connected.")
                Fact("AI", "Prototype / Simulated AI Inference: a deterministic statistical pipeline that stands in for a trained time-series neural network.")
                Fact("Fleet", "12 fictional aircraft, AERO-101 to AERO-112. Type names are used only to make flight hours and cycles realistic.")
                Fact("Built with", "Kotlin, Jetpack Compose, Material 3. Runs offline on Android 8.0 and later.")
                Fact("Typeface", "Barlow by Jeremy Tribby, SIL Open Font License.")
            }
        }
    }
}

@Composable
private fun Fact(k: String, v: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(k, style = MaterialTheme.typography.labelMedium, color = Aero.Cyan)
        Text(v, style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted)
    }
}
