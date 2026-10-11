package com.penandpaper.app.ui
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.penandpaper.app.annotate.AnnotateRoute
import com.penandpaper.app.camera.CameraRoute
import com.penandpaper.app.scan.ScanImportScreen
import com.penandpaper.app.ui.editor.EditorScreen
import com.penandpaper.app.ui.list.NotesListScreen
import com.penandpaper.app.ui.settings.SettingsScreen
import com.penandpaper.app.ui.pdf.PdfAnnotatorScreen
import com.penandpaper.app.ui.unlock.UnlockScreen
import com.penandpaper.app.ui.folders.FolderManagerScreen
import com.penandpaper.app.ui.about.AboutScreen

@Composable
fun App() {
    val nav = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "unlock") {
            composable("unlock") { UnlockScreen(onUnlocked = { nav.navigate("list") { popUpTo("unlock") { inclusive = true } } }) }
            composable("list") {
                NotesListScreen(
                    onOpen = { id -> nav.navigate("edit/$id") },
                    onCreate = { nav.navigate("edit/0") },
                    onManageFolders = { nav.navigate("folders") },
                    onAbout = { nav.navigate("about") },
                    onScanNew = { id -> nav.navigate("scan/$id") },
                    onCameraNew = { id -> nav.navigate("camera/$id") }
                )
            }
            composable("edit/{id}") { backStack ->
                val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EditorScreen(
                    noteId = id,
                    onBack = { nav.popBackStack() },
                    onScan = { nid -> nav.navigate("scan/$nid") },
                    onCamera = { nid -> nav.navigate("camera/$nid") },
                    onAnnotate = { nid, bid -> nav.navigate("annotate/$nid/$bid") }
                )
            }
            composable("scan/{noteId}") { backStack ->
                val nid = backStack.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                ScanImportScreen(noteId = nid, onDone = { nav.popBackStack() })
            }
            composable("camera/{noteId}") { backStack ->
                val nid = backStack.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                CameraRoute(noteId = nid, onDone = { nav.popBackStack() })
            }
            composable("annotate/{noteId}/{blockId}") { backStack ->
                val nid = backStack.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                val bid = backStack.arguments?.getString("blockId")?.toLongOrNull() ?: 0L
                AnnotateRoute(noteId = nid, blockId = bid, onDone = { nav.popBackStack() })
            }
            composable("settings") { SettingsScreen() }
            composable("annotator") { PdfAnnotatorScreen() }
            composable("folders") { FolderManagerScreen() }
            composable("about") { AboutScreen() }
        }
    }
}
