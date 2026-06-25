package ua.vitaliyshkarupa.pmonitorimporter.ui

import ua.vitaliyshkarupa.pmonitorimporter.model.ImportLogItem
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportStats

enum class AppScreen { HOME, COMPETITORS, STORE_WEBVIEW, IMPORTING, RESULT }

data class AppUiState(
    val screen: AppScreen = AppScreen.HOME,
    val isBusy: Boolean = false,
    val fileName: String = "",
    val competitors: List<String> = emptyList(),
    val selectedCompetitors: Set<String> = emptySet(),
    val configuredStores: Set<String> = emptySet(),
    val webSetupCompetitor: String? = null,
    val webSetupUrl: String = "",
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val progressText: String = "",
    val stats: ImportStats? = null,
    val logs: List<ImportLogItem> = emptyList(),
    val error: String? = null,
    val hasLastImport: Boolean = false,
    val saved: Boolean = false
)
