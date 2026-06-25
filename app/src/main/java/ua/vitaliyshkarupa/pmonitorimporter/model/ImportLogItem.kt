package ua.vitaliyshkarupa.pmonitorimporter.model

data class ImportLogItem(
    val rowNumber: Int,
    val productName: String,
    val competitor: String,
    val status: String,
    val price: Double? = null,
    val score: Double? = null,
    val matchedTitle: String? = null
)
