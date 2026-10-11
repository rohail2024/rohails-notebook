
package com.penandpaper.app.ui.about
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("About", style = MaterialTheme.typography.headlineSmall)
        Text("Rohail’s Note Book is a fully offline, privacy‑focused notes application, created exclusively for personal use by Dr. Rohail Walayat from Khuiratta, District Kotli, Azad Jammu & Kashmir.")
        Text("It’s tailored to my workflow: handwriting, typed text, audio, and PDF annotation with on‑device storage only. No internet permission. No analytics. No ads.")
        Text("Contact")
        Text("doctor.rohail@gmail.com", color = MaterialTheme.colorScheme.primary)
        Divider()
        Text("This app is not for commercial distribution. Every feature prioritizes privacy, control, and self‑reliance.")
    }
}
