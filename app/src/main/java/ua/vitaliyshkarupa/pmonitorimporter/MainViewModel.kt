package ua.vitaliyshkarupa.pmonitorimporter

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.vitaliyshkarupa.pmonitorimporter.cache.CacheService
import ua.vitaliyshkarupa.pmonitorimporter.competitors.CompetitorWebConfigs
import ua.vitaliyshkarupa.pmonitorimporter.competitors.PriceImportRepository
import ua.vitaliyshkarupa.pmonitorimporter.store.StoreSessionService
import ua.vitaliyshkarupa.pmonitorimporter.excel.ExcelService
import ua.vitaliyshkarupa.pmonitorimporter.excel.LoadedWorkbook
import ua.vitaliyshkarupa.pmonitorimporter.ui.AppScreen
import ua.vitaliyshkarupa.pmonitorimporter.ui.AppUiState

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val excelService = ExcelService()
    private val repository = PriceImportRepository(excelService)
    private val cacheService = CacheService(application)
    private val storeSessionService = StoreSessionService(application)

    private var loadedWorkbook: LoadedWorkbook? = null
    private var outputBytes: ByteArray? = null
    private var outputExtension: String = "xlsx"

    private val _uiState = MutableStateFlow(AppUiState(hasLastImport = cacheService.hasLastImport()))
    val uiState: StateFlow<AppUiState> = _uiState

    fun openFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, saved = false) }
            runCatching {
                val loaded = excelService.loadFromUri(getApplication(), uri, fileName)
                loadedWorkbook = loaded
                outputBytes = null
                outputExtension = loaded.session.extension
                _uiState.update {
                    it.copy(
                        screen = AppScreen.COMPETITORS,
                        isBusy = false,
                        fileName = loaded.session.fileName,
                        competitors = loaded.session.competitors,
                        selectedCompetitors = loaded.session.competitors.toSet(),
                        configuredStores = storeSessionService.configuredSet(loaded.session.competitors),
                        stats = null,
                        logs = emptyList(),
                        error = null,
                        hasLastImport = cacheService.hasLastImport(),
                        saved = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.message ?: "Не вдалося відкрити файл") }
            }
        }
    }

    fun toggleCompetitor(name: String) {
        _uiState.update { state ->
            val selected = state.selectedCompetitors.toMutableSet()
            if (name in selected) selected.remove(name) else selected.add(name)
            state.copy(selectedCompetitors = selected)
        }
    }

    fun openStoreSetup(competitor: String) {
        val config = CompetitorWebConfigs.forCompetitor(competitor)
        if (config == null) {
            _uiState.update { it.copy(error = "Для цього конкурента WebView ще не підтримується") }
            return
        }
        _uiState.update {
            it.copy(
                screen = AppScreen.STORE_WEBVIEW,
                webSetupCompetitor = config.competitor,
                webSetupUrl = config.startUrl,
                error = null
            )
        }
    }

    fun finishStoreSetup(saveChoice: Boolean) {
        val competitor = _uiState.value.webSetupCompetitor
        if (saveChoice && competitor != null) storeSessionService.markConfigured(competitor)
        _uiState.update { state ->
            state.copy(
                screen = AppScreen.COMPETITORS,
                webSetupCompetitor = null,
                webSetupUrl = "",
                configuredStores = storeSessionService.configuredSet(state.competitors)
            )
        }
    }

    fun importSelected() {
        val loaded = loadedWorkbook ?: return
        val selected = _uiState.value.selectedCompetitors
        if (selected.isEmpty()) {
            _uiState.update { it.copy(error = "Вибери хоча б одного конкурента") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screen = AppScreen.IMPORTING,
                    isBusy = true,
                    progressDone = 0,
                    progressTotal = loaded.session.products.count { p -> p.competitorKey in selected },
                    progressText = "Починаю імпорт...",
                    error = null,
                    logs = emptyList(),
                    saved = false
                )
            }
            runCatching {
                val result = repository.importPrices(loaded, selected) { done, total, item ->
                    _uiState.update { state ->
                        state.copy(
                            progressDone = done,
                            progressTotal = total,
                            progressText = item?.let { "${it.competitor}: ${it.productName.take(55)}" }.orEmpty(),
                            logs = if (item != null) (listOf(item) + state.logs).take(80) else state.logs
                        )
                    }
                }
                val bytes = excelService.toBytes(loaded.workbook)
                outputBytes = bytes
                outputExtension = loaded.session.extension
                cacheService.saveLastImport(loaded.session.fileName, loaded.session.extension, bytes, result.stats)
                _uiState.update {
                    it.copy(
                        screen = AppScreen.RESULT,
                        isBusy = false,
                        stats = result.stats,
                        logs = result.logs.takeLast(80).reversed(),
                        hasLastImport = true,
                        error = null,
                        saved = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        screen = AppScreen.COMPETITORS,
                        isBusy = false,
                        error = throwable.message ?: "Помилка імпорту"
                    )
                }
            }
        }
    }

    fun openLastImport() {
        val bytes = cacheService.loadLastBytes() ?: return
        outputBytes = bytes
        outputExtension = cacheService.lastExtension()
        loadedWorkbook = null
        _uiState.update {
            it.copy(
                screen = AppScreen.RESULT,
                isBusy = false,
                fileName = cacheService.lastFileName(),
                stats = cacheService.lastStats(),
                logs = emptyList(),
                error = null,
                hasLastImport = true,
                saved = false
            )
        }
    }

    fun saveToUri(uri: Uri) {
        val bytes = outputBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: error("Не вдалося відкрити місце збереження")
                cacheService.clear()
                _uiState.update { it.copy(isBusy = false, hasLastImport = false, saved = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.message ?: "Не вдалося зберегти файл") }
            }
        }
    }

    fun suggestedFileName(): String {
        val base = _uiState.value.fileName.ifBlank { "pmonitor_import" }
            .replace(Regex("\\.(xlsx|xls)$", RegexOption.IGNORE_CASE), "")
        val ext = if (outputExtension.lowercase() == "xls") "xls" else "xlsx"
        return "${base}_imported.$ext"
    }

    fun goHome() {
        loadedWorkbook = null
        outputBytes = null
        _uiState.update {
            AppUiState(hasLastImport = cacheService.hasLastImport())
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
