package ua.vitaliyshkarupa.pmonitorimporter.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportLogItem
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportStats

@Composable
fun PMonitorImporterApp(
    state: AppUiState,
    onOpenFile: () -> Unit,
    onOpenLast: () -> Unit,
    onToggleCompetitor: (String) -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    onHome: () -> Unit,
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
                AppScreen.COMPETITORS -> CompetitorsScreen(state, onToggleCompetitor, onImport, onHome)
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
        Spacer(Modifier.height(10.dp))
        state.competitors.forEach { competitor ->
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = competitor in state.selectedCompetitors,
                        onCheckedChange = { onToggleCompetitor(competitor) }
                    )
                    Text(competitor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
