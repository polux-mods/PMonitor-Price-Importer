package ua.vitaliyshkarupa.pmonitorimporter.excel

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import ua.vitaliyshkarupa.pmonitorimporter.competitors.CompetitorRegistry
import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelColumns
import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.WorkbookSession
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale

class ExcelService {
    private val formatter = DataFormatter(Locale("uk", "UA"))

    suspend fun loadFromUri(context: Context, uri: Uri, fileName: String): LoadedWorkbook = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Не вдалося прочитати файл")
        loadFromBytes(bytes, fileName)
    }

    suspend fun loadFromBytes(bytes: ByteArray, fileName: String): LoadedWorkbook = withContext(Dispatchers.IO) {
        val workbook = WorkbookFactory.create(ByteArrayInputStream(bytes))
        parseWorkbook(workbook, fileName)
    }

    fun applyPrice(workbook: Workbook, sheetIndex: Int, columns: ExcelColumns, rowIndex: Int, price: Double, promo: Boolean) {
        val sheet = workbook.getSheetAt(sheetIndex)
        val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        val priceCell = row.getCell(columns.competitorPrice) ?: row.createCell(columns.competitorPrice)
        priceCell.setCellValue(price)
        val promoCell = row.getCell(columns.promo) ?: row.createCell(columns.promo)
        promoCell.setCellValue(if (promo) "Да" else "Нет")
    }

    suspend fun toBytes(workbook: Workbook): ByteArray = withContext(Dispatchers.IO) {
        ByteArrayOutputStream().use { out ->
            workbook.write(out)
            out.toByteArray()
        }
    }

    private fun parseWorkbook(workbook: Workbook, fileName: String): LoadedWorkbook {
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0) ?: error("У файлі не знайдено рядок заголовків")
        val headers = (0 until headerRow.lastCellNum).map { idx -> cellText(headerRow.getCell(idx)) }
        val columns = findColumns(headers)
        val products = mutableListOf<ExcelProduct>()
        val detectedCompetitors = linkedSetOf<String>()

        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val name = cellText(row.getCell(columns.productName)).trim()
            if (name.isBlank()) continue
            val competitorRaw = cellText(row.getCell(columns.competitor)).trim()
            val key = CompetitorRegistry.normalize(competitorRaw)
            if (key != null) detectedCompetitors += key
            if (key == null) continue
            val barcode = columns.barcode?.let { ProductTextParser.cleanBarcode(cellText(row.getCell(it))) }
            val brand = columns.brand?.let { cellText(row.getCell(it)).trim() }?.takeIf { it.isNotBlank() && it != "29" }
            val article = columns.article?.let { cellText(row.getCell(it)).trim() }?.takeIf { it.isNotBlank() && it != "29" }
            val unit = columns.unit?.let { cellText(row.getCell(it)).trim() }?.takeIf { it.isNotBlank() }
            products += ExcelProduct(
                rowIndex = r,
                name = name,
                competitorRaw = competitorRaw,
                competitorKey = key,
                barcode = barcode,
                article = article,
                brand = brand,
                unit = unit,
                amount = ProductTextParser.extractAmount(name)
            )
        }

        if (products.isEmpty()) error("У таблиці не знайдено рядки з підтримуваними конкурентами: ${CompetitorRegistry.supported.joinToString()}")

        val extension = fileName.substringAfterLast('.', "xlsx").lowercase()
        return LoadedWorkbook(
            workbook = workbook,
            sheetIndex = 0,
            columns = columns,
            session = WorkbookSession(
                fileName = fileName,
                extension = extension,
                products = products,
                competitors = detectedCompetitors.toList()
            )
        )
    }

    private fun findColumns(headers: List<String>): ExcelColumns {
        fun norm(value: String) = value.lowercase()
            .replace("ё", "е")
            .replace("і", "и")
            .replace("ї", "и")
            .replace("є", "е")
            .replace(Regex("[^a-zа-я0-9]+"), "")

        val normalized = headers.map(::norm)
        fun requireOne(vararg names: String): Int {
            val variants = names.map(::norm)
            val index = normalized.indexOfFirst { it in variants }
            if (index < 0) error("Не знайдено потрібну колонку: ${names.joinToString(" / ")}")
            return index
        }
        fun optional(vararg names: String): Int? {
            val variants = names.map(::norm)
            return normalized.indexOfFirst { it in variants }.takeIf { it >= 0 }
        }

        return ExcelColumns(
            productName = requireOne("Товар", "Назва", "Название"),
            barcode = optional("Штрих-код", "Штрихкод", "Barcode", "EAN"),
            article = optional("Артикул", "Article"),
            brand = optional("ТМ", "Бренд", "Brand"),
            unit = optional("Ед.изм.", "Ед изм", "Од.виміру", "Одиниця виміру"),
            competitor = requireOne("Конкурент"),
            competitorPrice = requireOne("Цена конкурента", "Ціна конкурента", "Конкурент цена"),
            promo = requireOne("Акция", "Акція", "Акционная", "Промо")
        )
    }

    private fun cellText(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.orEmpty()
                CellType.NUMERIC -> formatter.formatCellValue(cell)
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> formatter.formatCellValue(cell)
                CellType.BLANK -> ""
                else -> formatter.formatCellValue(cell)
            }
        } catch (_: Throwable) {
            formatter.formatCellValue(cell)
        }
    }
}
