package ua.vitaliyshkarupa.pmonitorimporter.competitors

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser

object HtmlProductExtractor {
    private val priceRegex = Regex("""(?i)(\d{1,5}(?:[\s\u00A0]?\d{3})*(?:[,.]\d{1,2})?)\s*(грн|₴|uah)?""")
    private val promoWords = listOf("акц", "зниж", "скид", "sale", "promo", "old-price", "oldprice")

    fun extract(document: Document, baseUrl: String): List<WebProductCandidate> {
        val result = mutableListOf<WebProductCandidate>()
        result += extractJsonLd(document)
        result += extractFromHtmlCards(document, baseUrl)
        return result
            .filter { it.title.length > 3 && it.price > 0.0 }
            .distinctBy { ProductTextParser.normalize(it.title) + "|" + it.price }
            .take(40)
    }

    private fun extractJsonLd(document: Document): List<WebProductCandidate> {
        val out = mutableListOf<WebProductCandidate>()
        document.select("script[type*=ld+json]").forEach { script ->
            val jsonText = script.data().ifBlank { script.html() }.trim()
            runCatching {
                when {
                    jsonText.startsWith("[") -> walkJsonArray(JSONArray(jsonText), out)
                    jsonText.startsWith("{") -> walkJsonObject(JSONObject(jsonText), out)
                }
            }
        }
        return out
    }

    private fun walkJsonArray(array: JSONArray, out: MutableList<WebProductCandidate>) {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is JSONObject -> walkJsonObject(item, out)
                is JSONArray -> walkJsonArray(item, out)
            }
        }
    }

    private fun walkJsonObject(obj: JSONObject, out: MutableList<WebProductCandidate>) {
        val type = obj.opt("@type")?.toString().orEmpty().lowercase()
        if (type.contains("product")) {
            val title = obj.optString("name")
            val barcode = obj.optString("gtin13").takeIf { it.isNotBlank() }
                ?: obj.optString("gtin").takeIf { it.isNotBlank() }
            val url = obj.optString("url").takeIf { it.isNotBlank() }
            val offers = obj.opt("offers")
            val price = when (offers) {
                is JSONObject -> offers.optDoubleOrNull("price") ?: offers.optDoubleOrNull("lowPrice")
                is JSONArray -> (0 until offers.length()).asSequence()
                    .mapNotNull { (offers.opt(it) as? JSONObject)?.optDoubleOrNull("price") }
                    .firstOrNull()
                else -> null
            }
            if (!title.isNullOrBlank() && price != null && price > 0.0) {
                out += WebProductCandidate(
                    title = title,
                    price = price,
                    promo = obj.toString().lowercase().let { text -> promoWords.any { text.contains(it) } },
                    url = url,
                    barcode = barcode,
                    amount = ProductTextParser.extractAmount(title)
                )
            }
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = obj.opt(key)) {
                is JSONObject -> walkJsonObject(value, out)
                is JSONArray -> walkJsonArray(value, out)
            }
        }
    }

    private fun extractFromHtmlCards(document: Document, baseUrl: String): List<WebProductCandidate> {
        val selectors = listOf(
            "[itemtype*=Product]",
            "[data-product]",
            ".product",
            ".product-card",
            ".catalog-card",
            ".goods-tile",
            ".card"
        )
        val cards = selectors.flatMap { document.select(it) }.distinct()
        return cards.mapNotNull { card -> parseCard(card, baseUrl) }
    }

    private fun parseCard(card: Element, baseUrl: String): WebProductCandidate? {
        val title = card.selectFirst("[itemprop=name], .product-title, .card-title, .title, h2, h3, a[title]")
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.trim()
            ?: return null
        val text = card.text()
        val price = card.selectFirst("[itemprop=price], [data-price], .price, .product-price, .card-price")
            ?.let { element ->
                element.attr("content").ifBlank { element.attr("data-price") }.ifBlank { element.text() }
            }
            ?.let { parsePrice(it) }
            ?: parsePrice(text)
            ?: return null
        val oldPrice = card.selectFirst(".old-price, .oldprice, .price-old, .crossed")
            ?.text()
            ?.let { parsePrice(it) }
        val href = card.selectFirst("a[href]")?.absUrl("href")?.ifBlank { null }
        val lower = card.html().lowercase()
        val promo = oldPrice?.let { it > price } == true || promoWords.any { lower.contains(it) }
        return WebProductCandidate(
            title = title,
            price = price,
            oldPrice = oldPrice,
            promo = promo,
            url = href,
            amount = ProductTextParser.extractAmount(title)
        )
    }

    private fun parsePrice(raw: String): Double? {
        val cleaned = raw.replace("\u00A0", " ")
        val candidates = priceRegex.findAll(cleaned)
            .mapNotNull { match ->
                val hasCurrency = match.groupValues.getOrNull(2)?.isNotBlank() == true
                val nearby = cleaned.substring(match.range.first, minOf(cleaned.length, match.range.last + 15)).lowercase()
                val likely = hasCurrency || nearby.contains("грн") || nearby.contains("₴") || nearby.contains("uah")
                val number = match.groupValues[1]
                    .replace(" ", "")
                    .replace("\u00A0", "")
                    .replace(',', '.')
                    .toDoubleOrNull()
                if (number != null && number in 0.01..99999.0) number to likely else null
            }.toList()
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { it.second }?.first ?: candidates.first().first
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name)) return null
        return when (val value = opt(name)) {
            is Number -> value.toDouble()
            is String -> value.replace(',', '.').toDoubleOrNull()
            else -> null
        }
    }
}
