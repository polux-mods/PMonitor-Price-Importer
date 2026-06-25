package ua.vitaliyshkarupa.pmonitorimporter.competitors

class SilpoAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://www.silpo.ua",
    fallbackBaseUrls = listOf("https://silpo.ua", "https://shop.silpo.ua")
) {
    override val canonicalName: String = CompetitorRegistry.SILPO
    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/search?find=$encodedQuery"
}
