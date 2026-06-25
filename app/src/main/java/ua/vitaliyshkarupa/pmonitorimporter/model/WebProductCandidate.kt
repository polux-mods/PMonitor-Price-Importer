package ua.vitaliyshkarupa.pmonitorimporter.model

data class WebProductCandidate(
    val title: String,
    val price: Double,
    val oldPrice: Double? = null,
    val promo: Boolean = false,
    val url: String? = null,
    val barcode: String? = null,
    val amount: ProductAmount? = null
)
