
package com.penandpaper.app.ui.unlock
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.penandpaper.app.crypto.PasswordVault
import com.penandpaper.app.crypto.KeyHolder

@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    var password by remember { mutableStateOf("") }
    var isNew by remember { mutableStateOf(!PasswordVault.isInitialized(ctx)) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (isNew) "Set App Password" else "Unlock", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (isNew) "Choose password" else "Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (isNew) {
                    PasswordVault.initialize(ctx, password.toCharArray())
                    isNew = false
                    Toast.makeText(ctx, "Password set. Please unlock.", Toast.LENGTH_SHORT).show()
                    password = ""
                } else {
                    val key = PasswordVault.unlock(ctx, password.toCharArray())
                    if (key != null) {
                        KeyHolder.setKey(key)
                        onUnlocked()
                    } else {
                        Toast.makeText(ctx, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = password.length >= 4
        ) { Text(if (isNew) "Set password" else "Unlock") }
        Text("Note: password protects attachments and backups. You can still browse non-locked text, but media will require unlock.", style = MaterialTheme.typography.bodySmall)
    }
}
