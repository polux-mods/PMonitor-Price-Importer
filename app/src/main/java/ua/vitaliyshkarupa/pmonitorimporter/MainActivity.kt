package ua.vitaliyshkarupa.pmonitorimporter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.vitaliyshkarupa.pmonitorimporter.ui.PMonitorImporterApp
import ua.vitaliyshkarupa.pmonitorimporter.ui.PMonitorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val openExcelLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    viewModel.openFile(uri, displayName(uri))
                }
            }
            val saveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri: Uri? ->
                if (uri != null) viewModel.saveToUri(uri)
            }

            PMonitorTheme {
                PMonitorImporterApp(
                    state = state,
                    onOpenFile = {
                        openExcelLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "application/octet-stream"
                            )
                        )
                    },
                    onOpenLast = viewModel::openLastImport,
                    onToggleCompetitor = viewModel::toggleCompetitor,
                    onOpenStoreSetup = viewModel::openStoreSetup,
                    onImport = viewModel::importSelected,
                    onSave = { saveLauncher.launch(viewModel.suggestedFileName()) },
                    onHome = viewModel::goHome,
                    onFinishStoreSetup = viewModel::finishStoreSetup,
                    onDismissError = viewModel::dismissError
                )
            }
        }
    }

    private fun Context.displayName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "monitoring.xlsx"
    }
}
