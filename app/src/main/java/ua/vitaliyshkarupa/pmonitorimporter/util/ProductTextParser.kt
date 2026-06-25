package ua.vitaliyshkarupa.pmonitorimporter.util

import ua.vitaliyshkarupa.pmonitorimporter.model.AmountKind
import ua.vitaliyshkarupa.pmonitorimporter.model.ProductAmount

object ProductTextParser {
    private val amountRegex = Regex(
        pattern = """(?i)(\d+(?:[,.]\d+)?)\s*(кг|kg|кілограм|г|гр|g|л|l|литр|літр|мл|ml|шт|штук|табл|капс|pcs)"""
    )

    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.lowercase()
            .replace('ё', 'е')
            .replace('і', 'и')
            .replace('ї', 'и')
            .replace('є', 'е')
            .replace("’", "")
            .replace("'", "")
            .replace(Regex("[^a-zа-я0-9,.%]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun searchableQuery(name: String, brand: String?, barcode: String?): String {
        val b = brand?.takeIf { it.isNotBlank() }.orEmpty()
        val short = normalize("$b $name")
            .split(" ")
            .filter { it.length >= 2 && it !in stopWords }
            .take(8)
            .joinToString(" ")
        return barcode?.takeIf { it.length >= 8 } ?: short.ifBlank { name.take(80) }
    }

    fun tokens(text: String?): Set<String> = normalize(text)
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 2 && it !in stopWords }
        .toSet()

    fun extractAmount(text: String?): ProductAmount? {
        if (text.isNullOrBlank()) return null
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null
        val best = matches.maxByOrNull { it.groupValues[0].length } ?: return null
        val rawNumber = best.groupValues[1].replace(',', '.')
        val number = rawNumber.toDoubleOrNull() ?: return null
        val unit = best.groupValues[2].lowercase()
        val kind: AmountKind
        val base: Double
        when (unit) {
            "кг", "kg", "кілограм" -> {
                kind = AmountKind.WEIGHT
                base = number * 1000.0
            }
            "г", "гр", "g" -> {
                kind = AmountKind.WEIGHT
                base = number
            }
            "л", "l", "литр", "літр" -> {
                kind = AmountKind.VOLUME
                base = number * 1000.0
            }
            "мл", "ml" -> {
                kind = AmountKind.VOLUME
                base = number
            }
            else -> {
                kind = AmountKind.COUNT
                base = number
            }
        }
        return ProductAmount(base, kind, best.groupValues[0])
    }

    fun sameBarcode(a: String?, b: String?): Boolean {
        val aa = cleanBarcode(a)
        val bb = cleanBarcode(b)
        return aa != null && bb != null && aa == bb
    }

    fun cleanBarcode(value: String?): String? {
        val digits = value?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length >= 8 }
    }

    private val stopWords = setOf(
        "та", "і", "и", "для", "від", "без", "з", "із", "со", "на", "у", "в", "до",
        "напій", "напиток", "вода", "товар", "пак", "уп", "пет", "жб", "скло", "скл",
        "смаком", "ароматом", "газований", "негазована", "сильногазований", "слабогазована"
    )
}
