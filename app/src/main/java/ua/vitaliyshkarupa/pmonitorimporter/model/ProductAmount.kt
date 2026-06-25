package ua.vitaliyshkarupa.pmonitorimporter.model

enum class AmountKind { WEIGHT, VOLUME, COUNT, UNKNOWN }

data class ProductAmount(
    val valueBase: Double,
    val kind: AmountKind,
    val source: String
) {
    fun almostSame(other: ProductAmount, tolerance: Double = 0.10): Boolean {
        if (kind != other.kind) return false
        if (valueBase <= 0.0 || other.valueBase <= 0.0) return false
        val max = kotlin.math.max(valueBase, other.valueBase)
        val diff = kotlin.math.abs(valueBase - other.valueBase)
        return diff / max <= tolerance
    }
}
