package ua.vitaliyshkarupa.pmonitorimporter.competitors

class AtbAdapter : GenericHtmlAdapter("https://www.atbmarket.com") {
    override val canonicalName: String = CompetitorRegistry.ATB
    override fun buildSearchUrl(encodedQuery: String): String =
        "https://www.atbmarket.com/catalog/search?query=$encodedQuery"
}
