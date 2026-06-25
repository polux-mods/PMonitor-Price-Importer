package ua.vitaliyshkarupa.pmonitorimporter.model

data class ExcelProduct(
    val rowIndex: Int,
    val name: String,
    val competitorRaw: String,
    val competitorKey: String,
    val barcode: String?,
    val article: String?,
    val brand: String?,
    val unit: String?,
    val amount: ProductAmount?
)
