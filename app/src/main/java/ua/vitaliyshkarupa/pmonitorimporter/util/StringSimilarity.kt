package ua.vitaliyshkarupa.pmonitorimporter.util

object StringSimilarity {
    fun levenshteinRatio(aRaw: String, bRaw: String): Double {
        val a = ProductTextParser.normalize(aRaw)
        val b = ProductTextParser.normalize(bRaw)
        if (a.isBlank() && b.isBlank()) return 1.0
        if (a.isBlank() || b.isBlank()) return 0.0
        val distance = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length).coerceAtLeast(1)
        return 1.0 - distance.toDouble() / maxLen.toDouble()
    }

    fun tokenJaccard(aRaw: String, bRaw: String): Double {
        val a = ProductTextParser.tokens(aRaw)
        val b = ProductTextParser.tokens(bRaw)
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble().coerceAtLeast(1.0)
        return intersection / union
    }

    fun containsImportantTokens(aRaw: String, bRaw: String): Double {
        val a = ProductTextParser.tokens(aRaw).filter { it.length >= 4 }.toSet()
        val b = ProductTextParser.tokens(bRaw).filter { it.length >= 4 }.toSet()
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val contained = a.count { token -> b.any { it.contains(token) || token.contains(it) } }
        return contained.toDouble() / a.size.toDouble()
    }

    private fun levenshtein(a: String, b: String): Int {
        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var last = i - 1
            costs[0] = i
            for (j in 1..b.length) {
                val newValue = if (a[i - 1] == b[j - 1]) last else minOf(last, costs[j - 1], costs[j]) + 1
                last = costs[j]
                costs[j] = newValue
            }
        }
        return costs[b.length]
    }
}
