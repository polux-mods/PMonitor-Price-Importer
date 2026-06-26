package ua.vitaliyshkarupa.pmonitorimporter.competitors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.vitaliyshkarupa.pmonitorimporter.excel.ExcelService
import ua.vitaliyshkarupa.pmonitorimporter.excel.LoadedWorkbook
import ua.vitaliyshkarupa.pmonitorimporter.matcher.ProductMatcher
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportLogItem
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportStats

class PriceImportRepository(
    private val excelService: ExcelService = ExcelService(),
    private val adapters: Map<String, CompetitorAdapter> = CompetitorRegistry.adapters()
) {
    suspend fun importPrices(
        loaded: LoadedWorkbook,
        selectedCompetitors: Set<String>,
        onProgress: suspend (done: Int, total: Int, item: ImportLogItem?) -> Unit
    ): ImportResult = withContext(Dispatchers.IO) {
        val products = loaded.session.products.filter { it.competitorKey in selectedCompetitors }
        var imported = 0
        var skipped = 0
        var lowConfidence = 0
        var errors = 0
        val logs = mutableListOf<ImportLogItem>()

        products.forEachIndexed { index, product ->
            val adapter = adapters[product.competitorKey]
            val log = if (adapter == null) {
                skipped++
                ImportLogItem(product.rowIndex + 1, product.name, product.competitorKey, "Немає адаптера")
            } else {
                try {
                    val candidates = adapter.search(product)
                    val ranked = ProductMatcher.ranked(product, candidates)
                    val match = ProductMatcher.best(product, candidates)
                    if (match != null) {
                        excelService.applyPrice(
                            workbook = loaded.workbook,
                            sheetIndex = loaded.sheetIndex,
                            columns = loaded.columns,
                            rowIndex = product.rowIndex,
                            price = match.candidate.price,
                            promo = match.candidate.promo
                        )
                        imported++
                        ImportLogItem(
                            rowNumber = product.rowIndex + 1,
                            productName = product.name,
                            competitor = product.competitorKey,
                            status = "Імпортовано",
                            price = match.candidate.price,
                            score = match.score,
                            matchedTitle = match.candidate.title
                        )
                    } else {
                        lowConfidence++
                        val top = ranked.firstOrNull()
                        ImportLogItem(
                            rowNumber = product.rowIndex + 1,
                            productName = product.name,
                            competitor = product.competitorKey,
                            status = if (candidates.isEmpty()) "Не знайдено кандидатів" else "Не впевнений збіг",
                            price = top?.candidate?.price,
                            score = top?.score,
                            matchedTitle = top?.candidate?.title
                        )
                    }
                } catch (t: Throwable) {
                    errors++
                    ImportLogItem(
                        rowNumber = product.rowIndex + 1,
                        productName = product.name,
                        competitor = product.competitorKey,
                        status = "Помилка: ${t.message ?: t::class.java.simpleName}"
                    )
                }
            }
            logs += log
            onProgress(index + 1, products.size, log)
        }

        ImportResult(
            stats = ImportStats(
                imported = imported,
                skipped = skipped,
                total = products.size,
                lowConfidence = lowConfidence,
                errors = errors
            ),
            logs = logs
        )
    }
}

data class ImportResult(
    val stats: ImportStats,
    val logs: List<ImportLogItem>
)
