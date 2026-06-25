package ua.vitaliyshkarupa.pmonitorimporter.matcher

import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser
import ua.vitaliyshkarupa.pmonitorimporter.util.StringSimilarity

data class MatchResult(
    val candidate: WebProductCandidate,
    val score: Double
)

object ProductMatcher {
    private const val MIN_SCORE = 0.76
    private const val MIN_GAP = 0.06

    fun best(product: ExcelProduct, candidates: List<WebProductCandidate>): MatchResult? {
        if (candidates.isEmpty()) return null
        val scored = candidates.mapNotNull { candidate ->
            val score = score(product, candidate)
            if (score <= 0.0) null else MatchResult(candidate, score)
        }.sortedByDescending { it.score }

        val first = scored.firstOrNull() ?: return null
        val secondScore = scored.getOrNull(1)?.score ?: 0.0
        val confidentByGap = first.score - secondScore >= MIN_GAP || first.score >= 0.90
        return if (first.score >= MIN_SCORE && confidentByGap) first else null
    }

    fun score(product: ExcelProduct, candidate: WebProductCandidate): Double {
        val productAmount = product.amount ?: ProductTextParser.extractAmount(product.name)
        val candidateAmount = candidate.amount ?: ProductTextParser.extractAmount(candidate.title)

        if (productAmount != null && candidateAmount != null && productAmount.kind == candidateAmount.kind) {
            val max = maxOf(productAmount.valueBase, candidateAmount.valueBase)
            val diff = kotlin.math.abs(productAmount.valueBase - candidateAmount.valueBase)
            if (diff / max > 0.18) return 0.0
        }

        if (ProductTextParser.sameBarcode(product.barcode, candidate.barcode)) return 0.98

        val nameA = listOfNotNull(product.brand, product.name).joinToString(" ")
        val nameB = candidate.title

        val token = StringSimilarity.tokenJaccard(nameA, nameB)
        val lev = StringSimilarity.levenshteinRatio(nameA, nameB)
        val contains = StringSimilarity.containsImportantTokens(nameA, nameB)
        val amountScore = when {
            productAmount == null || candidateAmount == null -> 0.08
            productAmount.almostSame(candidateAmount) -> 0.25
            else -> 0.0
        }
        val brandScore = product.brand?.let { brand ->
            val b = ProductTextParser.normalize(brand)
            if (b.isNotBlank() && ProductTextParser.normalize(candidate.title).contains(b)) 0.10 else 0.0
        } ?: 0.0

        val score = token * 0.34 + lev * 0.23 + contains * 0.20 + amountScore + brandScore
        return score.coerceIn(0.0, 0.97)
    }
}
