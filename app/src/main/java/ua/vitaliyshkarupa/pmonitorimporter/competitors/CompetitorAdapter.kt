package ua.vitaliyshkarupa.pmonitorimporter.competitors

import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate

interface CompetitorAdapter {
    val canonicalName: String
    suspend fun search(product: ExcelProduct): List<WebProductCandidate>
}
