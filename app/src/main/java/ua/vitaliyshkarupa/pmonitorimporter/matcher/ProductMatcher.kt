package ua.vitaliyshkarupa.pmonitorimporter.matcher

import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.ProductAmount
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser
import ua.vitaliyshkarupa.pmonitorimporter.util.StringSimilarity

data class MatchResult(
    val candidate: WebProductCandidate,
    val score: Double
)

object ProductMatcher {
    private const val MIN_SCORE = 0.68
    private const val MIN_GAP = 0.03

    fun best(product: ExcelProduct, candidates: List<WebProductCandidate>): MatchResult? {
        val scored = ranked(product, candidates)
        val first = scored.firstOrNull() ?: return null
        val secondScore = scored.getOrNull(1)?.score ?: 0.0
        val confidentByGap = first.score - secondScore >= MIN_GAP || first.score >= 0.86
        return if (first.score >= MIN_SCORE && confidentByGap) first else null
    }

    fun ranked(product: ExcelProduct, candidates: List<WebProductCandidate>): List<MatchResult> {
        if (candidates.isEmpty()) return emptyList()
        return candidates.mapNotNull { candidate ->
            val score = score(product, candidate)
            if (score <= 0.0) null else MatchResult(candidate, score)
        }.sortedByDescending { it.score }
    }

    fun score(product: ExcelProduct, candidate: WebProductCandidate): Double {
        val productAmount = product.amount ?: ProductTextParser.extractAmount(product.name)
        val candidateAmount = candidate.amount ?: ProductTextParser.extractAmount(candidate.title)

        if (amountConflicts(productAmount, candidateAmount)) return 0.0
        if (ProductTextParser.sameBarcode(product.barcode, candidate.barcode)) return 0.99

        val nameA = listOfNotNull(product.brand, product.name).joinToString(" ")
        val nameB = candidate.title
        val normA = ProductTextParser.normalize(nameA)
        val normB = ProductTextParser.normalize(nameB)

        val token = StringSimilarity.tokenJaccard(nameA, nameB)
        val coverage = StringSimilarity.oneWayTokenCoverage(nameA, nameB)
        val lev = StringSimilarity.levenshteinRatio(nameA, nameB)
        val contains = StringSimilarity.containsImportantTokens(nameA, nameB)
        val amountScore = when {
            productAmount == null || candidateAmount == null -> 0.06
            productAmount.almostSame(candidateAmount, tolerance = 0.14) -> 0.27
            else -> 0.0
        }
        val brandScore = product.brand?.let { brand ->
            val b = ProductTextParser.normalize(brand)
            if (b.isNotBlank() && normB.contains(b)) 0.12 else 0.0
        } ?: 0.0

        val containmentBoost = if ((normA.contains(normB) || normB.contains(normA)) && amountScore >= 0.27) 0.12 else 0.0
        val exactImportantBoost = if (contains >= 0.72 && amountScore >= 0.27) 0.08 else 0.0

        val score = token * 0.20 + coverage * 0.22 + lev * 0.15 + contains * 0.20 + amountScore + brandScore + containmentBoost + exactImportantBoost
        return score.coerceIn(0.0, 0.98)
    }

    private fun amountConflicts(productAmount: ProductAmount?, candidateAmount: ProductAmount?): Boolean {
        if (productAmount == null || candidateAmount == null) return false
        if (productAmount.kind != candidateAmount.kind) return false
        val max = maxOf(productAmount.valueBase, candidateAmount.valueBase)
        if (max <= 0.0) return false
        val diff = kotlin.math.abs(productAmount.valueBase - candidateAmount.valueBase)
        return diff / max > 0.20
    }
}
