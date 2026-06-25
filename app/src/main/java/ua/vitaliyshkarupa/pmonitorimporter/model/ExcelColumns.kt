package ua.vitaliyshkarupa.pmonitorimporter.model

data class ExcelColumns(
    val productName: Int,
    val barcode: Int?,
    val article: Int?,
    val brand: Int?,
    val unit: Int?,
    val competitor: Int,
    val competitorPrice: Int,
    val promo: Int
)
