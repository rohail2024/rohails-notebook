
package com.penandpaper.app.saf
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun rememberCreateDocumentLauncher(mime: String, onResult: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime), onResult)

@Composable
fun rememberOpenDocumentLauncher(mimes: Array<String>, onResult: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onResult)
