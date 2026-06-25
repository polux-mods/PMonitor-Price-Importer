package ua.vitaliyshkarupa.pmonitorimporter.ui

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportLogItem
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportStats

@Composable
fun PMonitorImporterApp(
    state: AppUiState,
    onOpenFile: () -> Unit,
    onOpenLast: () -> Unit,
    onToggleCompetitor: (String) -> Unit,
    onOpenStoreSetup: (String) -> Unit,
    onOpenDiagnostics: (String) -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    onHome: () -> Unit,
    onFinishStoreSetup: (Boolean) -> Unit,
    onFinishDiagnostics: () -> Unit,
    onDismissError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showResumeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.hasLastImport, state.screen) {
        if (state.hasLastImport && state.screen == AppScreen.HOME) showResumeDialog = true
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Є незбережений імпорт") },
            text = { Text("Відкрити останній імпортований файл і зберегти його?") },
            confirmButton = {
                TextButton(onClick = {
                    showResumeDialog = false
                    onOpenLast()
                }) { Text("Відкрити") }
            },
            dismissButton = {
                TextButton(onClick = { showResumeDialog = false }) { Text("Не зараз") }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (state.screen) {
                AppScreen.HOME -> HomeScreen(state, onOpenFile, onOpenLast)
                AppScreen.COMPETITORS -> CompetitorsScreen(
                    state = state,
                    onToggleCompetitor = onToggleCompetitor,
                    onOpenStoreSetup = onOpenStoreSetup,
                    onOpenDiagnostics = onOpenDiagnostics,
                    onImport = onImport,
                    onHome = onHome
                )
                AppScreen.STORE_WEBVIEW -> StoreWebViewScreen(
                    state = state,
                    onDone = { onFinishStoreSetup(true) },
                    onCancel = { onFinishStoreSetup(false) }
                )
                AppScreen.DIAGNOSTICS_WEBVIEW -> DiagnosticsWebViewScreen(
                    state = state,
                    onClose = onFinishDiagnostics
                )
                AppScreen.IMPORTING -> ImportingScreen(state)
                AppScreen.RESULT -> ResultScreen(state, onSave, onHome)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppUiState, onOpenFile: () -> Unit, onOpenLast: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderBlock()
        Spacer(Modifier.height(28.dp))
        ElevatedButton(onClick = onOpenFile, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
            Text("Відкрити Excel файл")
        }
        AnimatedVisibility(state.hasLastImport) {
            Column {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenLast, modifier = Modifier.fillMaxWidth()) {
                    Text("Відкрити останній незбережений імпорт")
                }
            }
        }
        if (state.isBusy) {
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun HeaderBlock() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("PMonitor Importer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Імпорт цін конкурентів у таблицю Excel: АТБ, Сільпо, Єва, Аврора. Підтримуються .xlsx і .xls.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun CompetitorsScreen(
    state: AppUiState,
    onToggleCompetitor: (String) -> Unit,
    onOpenStoreSetup: (String) -> Unit,
    onOpenDiagnostics: (String) -> Unit,
    onImport: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Файл", style = MaterialTheme.typography.labelLarge)
        Text(state.fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("Вибери конкурентів для імпорту", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ) {
            Text(
                "Перед імпортом можна відкрити сайт конкурента у WebView, вибрати місто/магазин і натиснути “Готово”. Cookies цього вибору будуть використані під час пошуку цін. Також є “Аналіз запитів”, щоб побачити, через які API сайт передає товари й ціни.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        state.competitors.forEach { competitor ->
            val configured = competitor in state.configuredStores
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = competitor in state.selectedCompetitors,
                            onCheckedChange = { onToggleCompetitor(competitor) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(competitor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (configured) "Магазин/місто вибрано у WebView" else "Магазин/місто ще не вибрано",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onOpenStoreSetup(competitor) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (configured) "Змінити магазин" else "Вибрати магазин")
                        }
                        OutlinedButton(
                            onClick = { onOpenDiagnostics(competitor) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Аналіз запитів")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onImport, enabled = state.selectedCompetitors.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Імпортувати ціни")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Назад")
        }
    }
}

@Composable
private fun StoreWebViewScreen(
    state: AppUiState,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val competitor = state.webSetupCompetitor ?: "Магазин"
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember(state.webSetupUrl) { mutableStateOf(state.webSetupUrl) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Вибір магазину: $competitor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "На сайті вибери потрібне місто, адресу або магазин. Після цього натисни “Готово”.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(currentUrl, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val view = webView
                            if (view != null && view.canGoBack()) view.goBack() else onCancel()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Назад") }
                    Button(
                        onClick = {
                            CookieManager.getInstance().flush()
                            onDone()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Готово") }
                }
            }
        }
        AnimatedVisibility(isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            currentUrl = url.orEmpty()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            currentUrl = url.orEmpty()
                            CookieManager.getInstance().flush()
                        }
                    }
                    loadUrl(state.webSetupUrl)
                }
            },
            update = { view ->
                if (view.url.isNullOrBlank() && state.webSetupUrl.isNotBlank()) {
                    view.loadUrl(state.webSetupUrl)
                }
            }
        )
    }
}


@Composable
private fun DiagnosticsWebViewScreen(
    state: AppUiState,
    onClose: () -> Unit
) {
    val competitor = state.webSetupCompetitor ?: "Сайт"
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember(state.webSetupUrl) { mutableStateOf(state.webSetupUrl) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val requestLog = remember { mutableStateListOf<String>() }
    val clipboard = LocalClipboardManager.current

    fun addLog(raw: String) {
        val value = raw.trim()
        if (value.isBlank()) return
        val cleaned = value.take(900)
        if (requestLog.firstOrNull() == cleaned) return
        if (requestLog.contains(cleaned)) return
        requestLog.add(0, cleaned)
        while (requestLog.size > 220) requestLog.removeAt(requestLog.lastIndex)
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Діагностика сайту: $competitor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Вибери магазин, пошукай 1–2 товари на сайті. Нижче зʼявляться fetch/XHR/API-запити, за якими потім можна зробити реальний адаптер.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(currentUrl, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val view = webView
                            if (view != null && view.canGoBack()) view.goBack() else onClose()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Назад") }
                    OutlinedButton(
                        onClick = { requestLog.clear() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Очистити") }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(requestLog.joinToString("\n")))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = requestLog.isNotEmpty()
                    ) { Text("Копіювати") }
                }
            }
        }
        AnimatedVisibility(isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .background(MaterialTheme.colorScheme.surface),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun log(value: String) {
                                mainHandler.post { addLog("JS $value") }
                            }
                        },
                        "PMonitorCapture"
                    )
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            currentUrl = url.orEmpty()
                            url?.let { mainHandler.post { addLog("PAGE $it") } }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            currentUrl = url.orEmpty()
                            CookieManager.getInstance().flush()
                            view?.evaluateJavascript(pmonitorCaptureScript(), null)
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url?.toString().orEmpty()
                            if (url.isNotBlank() && shouldKeepDiagnosticUrl(url)) {
                                val method = request?.method ?: "GET"
                                mainHandler.post { addLog("$method $url") }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl(state.webSetupUrl)
                }
            },
            update = { view ->
                if (view.url.isNullOrBlank() && state.webSetupUrl.isNotBlank()) {
                    view.loadUrl(state.webSetupUrl)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Лог запитів: ${requestLog.size}. Шукай рядки з api, search, product, price, graphql, catalog, stores, branches.",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
        ) {
            items(requestLog) { line -> DiagnosticLogRow(line) }
        }
    }
}

@Composable
private fun DiagnosticLogRow(line: String) {
    val useful = isProbablyUsefulDiagnosticUrl(line)
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useful) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(if (useful) "Можливо корисний запит" else "Запит", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text(line, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun shouldKeepDiagnosticUrl(url: String): Boolean {
    val lower = url.lowercase()
    if (lower.startsWith("data:") || lower.contains(".css") || lower.contains(".png") || lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".webp") || lower.contains(".svg") || lower.contains(".woff") || lower.contains(".ttf")) return false
    return lower.contains("eva.ua") || lower.contains("silpo.ua") || lower.contains("atbmarket") || lower.contains("avrora") || isProbablyUsefulDiagnosticUrl(lower)
}

private fun isProbablyUsefulDiagnosticUrl(text: String): Boolean {
    val lower = text.lowercase()
    return listOf(
        "/api", "api/", "graphql", "search", "suggest", "product", "products", "catalog", "price", "prices", "sku", "store", "stores", "branch", "branches", "delivery", "availability", "stock"
    ).any { lower.contains(it) }
}

private fun pmonitorCaptureScript(): String = """
    (function() {
      if (window.__PMONITOR_CAPTURE_INSTALLED__) return;
      window.__PMONITOR_CAPTURE_INSTALLED__ = true;
      function cut(v) {
        try { return String(v || '').slice(0, 500); } catch(e) { return ''; }
      }
      function send(kind, method, url, body) {
        try {
          if (window.PMonitorCapture && url) window.PMonitorCapture.log(kind + ' ' + method + ' ' + url + (body ? ' BODY ' + cut(body) : ''));
        } catch(e) {}
      }
      var originalFetch = window.fetch;
      if (originalFetch) {
        window.fetch = function(input, init) {
          var url = (typeof input === 'string') ? input : ((input && input.url) ? input.url : '');
          var method = (init && init.method) ? init.method : 'GET';
          var body = (init && init.body) ? init.body : '';
          send('FETCH', method, url, body);
          return originalFetch.apply(this, arguments);
        };
      }
      var originalOpen = XMLHttpRequest.prototype.open;
      var originalSend = XMLHttpRequest.prototype.send;
      XMLHttpRequest.prototype.open = function(method, url) {
        this.__pmonitorMethod = method || 'GET';
        this.__pmonitorUrl = url || '';
        return originalOpen.apply(this, arguments);
      };
      XMLHttpRequest.prototype.send = function(body) {
        send('XHR', this.__pmonitorMethod || 'GET', this.__pmonitorUrl || '', body || '');
        return originalSend.apply(this, arguments);
      };
    })();
""".trimIndent()

@Composable
private fun ImportingScreen(state: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(18.dp))
        Text("Імпортую ціни...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${state.progressDone} / ${state.progressTotal}")
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (state.progressTotal == 0) 0f else state.progressDone.toFloat() / state.progressTotal.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text(state.progressText, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ResultScreen(state: AppUiState, onSave: () -> Unit, onHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Результат імпорту", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(state.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(14.dp))
        StatsCard(state.stats)
        AnimatedVisibility(state.saved) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text("Файл збережено. Кеш очищено.", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSave, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isBusy) "Зберігаю..." else "Зберегти файл")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("На головну")
        }
        Spacer(Modifier.height(12.dp))
        Text("Останні дії", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.logs) { item -> LogRow(item) }
        }
    }
}

@Composable
private fun StatsCard(stats: ImportStats?) {
    val s = stats ?: ImportStats()
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Імпортовано", s.imported.toString())
                StatItem("Всього", s.total.toString())
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Не впевнено", s.lowConfidence.toString())
                StatItem("Помилки", s.errors.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LogRow(item: ImportLogItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("#${item.rowNumber} · ${item.competitor} · ${item.status}", fontWeight = FontWeight.Bold)
            Text(item.productName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            item.price?.let { Text("Ціна: $it грн · score: ${"%.2f".format(item.score ?: 0.0)}") }
            item.matchedTitle?.let { Text("Збіг: $it", maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}
