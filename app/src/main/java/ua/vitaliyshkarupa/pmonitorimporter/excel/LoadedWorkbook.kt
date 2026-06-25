package ua.vitaliyshkarupa.pmonitorimporter.excel

import org.apache.poi.ss.usermodel.Workbook
import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelColumns
import ua.vitaliyshkarupa.pmonitorimporter.model.WorkbookSession

data class LoadedWorkbook(
    val workbook: Workbook,
    val sheetIndex: Int,
    val columns: ExcelColumns,
    val session: WorkbookSession
)
