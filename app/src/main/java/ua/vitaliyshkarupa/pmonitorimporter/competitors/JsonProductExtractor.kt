package ua.vitaliyshkarupa.pmonitorimporter.competitors

import org.json.JSONArray
import org.json.JSONObject
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser

/**
 * Універсальний витягувач товарів із JSON-відповідей магазинів.
 * Він не привʼязаний до однієї точної схеми, бо магазини часто міняють назви полів.
 */
object JsonProductExtractor {
    private val titleKeys = listOf("name", "title", "productName", "fullName", "caption", "label", "displayName", "seoTitle")
    private val priceKeys = listOf("price", "currentPrice", "finalPrice", "salePrice", "actualPrice", "priceValue", "value", "amount")
    private val oldPriceKeys = listOf("oldPrice", "regularPrice", "basePrice", "previousPrice", "crossedPrice", "priceOld")
    private val urlKeys = listOf("url", "link", "href", "productUrl", "path", "slug")
    private val barcodeKeys = listOf("barcode", "barCode", "ean", "gtin", "gtin13")
    private val promoKeys = listOf("promo", "isPromo", "promotion", "hasPromo", "hasDiscount", "discount", "action", "isAction")

    fun extract(jsonText: String, baseUrl: String): List<WebProductCandidate> {
        val out = mutableListOf<WebProductCandidate>()
        runCatching {
            val trimmed = jsonText.trim()
            when {
                trimmed.startsWith("{") -> walk(JSONObject(trimmed), baseUrl, out)
                trimmed.startsWith("[") -> walk(JSONArray(trimmed), baseUrl, out)
            }
        }
        return out
            .filter { it.title.length > 3 && it.price > 0.0 }
            .distinctBy { ProductTextParser.normalize(it.title) + "|" + it.price + "|" + it.url.orEmpty() }
            .take(60)
    }

    private fun walk(array: JSONArray, baseUrl: String, out: MutableList<WebProductCandidate>) {
        for (i in 0 until array.length()) {
            when (val value = array.opt(i)) {
                is JSONObject -> walk(value, baseUrl, out)
                is JSONArray -> walk(value, baseUrl, out)
            }
        }
    }

    private fun walk(obj: JSONObject, baseUrl: String, out: MutableList<WebProductCandidate>) {
        candidateFrom(obj, baseUrl)?.let { out += it }

        val keys = obj.keys()
        while (keys.hasNext()) {
            when (val value = obj.opt(keys.next())) {
                is JSONObject -> walk(value, baseUrl, out)
                is JSONArray -> walk(value, baseUrl, out)
            }
        }
    }

    private fun candidateFrom(obj: JSONObject, baseUrl: String): WebProductCandidate? {
        val title = firstString(obj, titleKeys)?.trim().orEmpty()
        if (title.length < 4) return null

        val price = firstNumber(obj, priceKeys) ?: findNestedPrice(obj) ?: return null
        if (price <= 0.0 || price > 999999.0) return null

        val oldPrice = firstNumber(obj, oldPriceKeys)
        val url = firstString(obj, urlKeys)?.let { absolutize(it, baseUrl) }
        val barcode = firstString(obj, barcodeKeys)
        val lower = obj.toString().lowercase()
        val promo = oldPrice?.let { it > price } == true ||
            promoKeys.any { key -> obj.opt(key).toString().equals("true", ignoreCase = true) } ||
            listOf("акц", "зниж", "discount", "promotion", "promo").any { lower.contains(it) }

        return WebProductCandidate(
            title = title,
            price = price,
            oldPrice = oldPrice,
            promo = promo,
            url = url,
            barcode = barcode,
            amount = ProductTextParser.extractAmount(title)
        )
    }

    private fun firstString(obj: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (!obj.has(key)) continue
            val value = obj.opt(key)
            val text = when (value) {
                is String -> value
                is Number -> value.toString()
                else -> null
            }?.trim()
            if (!text.isNullOrBlank() && text != "null") return text
        }
        return null
    }

    private fun firstNumber(obj: JSONObject, keys: List<String>): Double? {
        for (key in keys) {
            if (!obj.has(key)) continue
            when (val value = obj.opt(key)) {
                is Number -> return value.toDouble()
                is String -> value.toPriceOrNull()?.let { return it }
                is JSONObject -> {
                    firstNumber(value, listOf("value", "amount", "price", "current", "uah"))?.let { return it }
                }
            }
        }
        return null
    }

    private fun findNestedPrice(obj: JSONObject): Double? {
        val offers = obj.opt("offers")
        if (offers is JSONObject) {
            firstNumber(offers, priceKeys)?.let { return it }
        }
        if (offers is JSONArray) {
            for (i in 0 until offers.length()) {
                val item = offers.opt(i) as? JSONObject ?: continue
                firstNumber(item, priceKeys)?.let { return it }
            }
        }
        return null
    }

    private fun absolutize(raw: String, baseUrl: String): String? {
        val value = raw.trim()
        if (value.isBlank() || value == "#") return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val path = if (value.startsWith("/")) value else "/$value"
        return baseUrl.trimEnd('/') + path
    }

    private fun String.toPriceOrNull(): Double? {
        val cleaned = replace("\u00A0", " ")
            .replace("грн", "", ignoreCase = true)
            .replace("uah", "", ignoreCase = true)
            .replace("₴", "")
            .replace(" ", "")
            .replace(',', '.')
            .trim()
        return cleaned.toDoubleOrNull()
    }
}
