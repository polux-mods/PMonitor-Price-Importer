package ua.vitaliyshkarupa.pmonitorimporter.competitors

class EvaAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://www.eva.ua",
    fallbackBaseUrls = listOf("https://eva.ua")
) {
    override val canonicalName: String = CompetitorRegistry.EVA
    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/ua/search/?q=$encodedQuery"
}
