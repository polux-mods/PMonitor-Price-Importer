package ua.vitaliyshkarupa.pmonitorimporter.model

data class ImportStats(
    val imported: Int = 0,
    val skipped: Int = 0,
    val total: Int = 0,
    val lowConfidence: Int = 0,
    val errors: Int = 0
)
