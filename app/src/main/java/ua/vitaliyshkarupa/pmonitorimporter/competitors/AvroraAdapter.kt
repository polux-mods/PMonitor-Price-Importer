package ua.vitaliyshkarupa.pmonitorimporter.competitors

class AvroraAdapter : GenericHtmlAdapter("https://avrora.ua") {
    override val canonicalName: String = CompetitorRegistry.AVRORA
    override fun buildSearchUrl(encodedQuery: String): String =
        "https://avrora.ua/ua/search/?q=$encodedQuery"
}
