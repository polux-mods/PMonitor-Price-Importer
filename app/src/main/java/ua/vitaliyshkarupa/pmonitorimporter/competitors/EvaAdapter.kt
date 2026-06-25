package ua.vitaliyshkarupa.pmonitorimporter.competitors

class EvaAdapter : GenericHtmlAdapter("https://eva.ua") {
    override val canonicalName: String = CompetitorRegistry.EVA
    override fun buildSearchUrl(encodedQuery: String): String =
        "https://eva.ua/ua/search/?q=$encodedQuery"
}
