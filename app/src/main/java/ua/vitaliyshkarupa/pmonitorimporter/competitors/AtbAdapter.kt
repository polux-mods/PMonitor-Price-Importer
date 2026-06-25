package ua.vitaliyshkarupa.pmonitorimporter.competitors

class AtbAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://www.atbmarket.com",
    fallbackBaseUrls = listOf("https://atbmarket.com")
) {
    override val canonicalName: String = CompetitorRegistry.ATB
    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/catalog/search?query=$encodedQuery"
}
