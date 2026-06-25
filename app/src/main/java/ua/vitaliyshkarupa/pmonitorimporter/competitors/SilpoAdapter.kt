package ua.vitaliyshkarupa.pmonitorimporter.competitors

class SilpoAdapter : GenericHtmlAdapter("https://silpo.ua") {
    override val canonicalName: String = CompetitorRegistry.SILPO
    override fun buildSearchUrl(encodedQuery: String): String =
        "https://silpo.ua/search?find=$encodedQuery"
}
