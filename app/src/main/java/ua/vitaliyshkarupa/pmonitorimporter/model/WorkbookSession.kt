package ua.vitaliyshkarupa.pmonitorimporter.model

data class WorkbookSession(
    val fileName: String,
    val extension: String,
    val products: List<ExcelProduct>,
    val competitors: List<String>
)
