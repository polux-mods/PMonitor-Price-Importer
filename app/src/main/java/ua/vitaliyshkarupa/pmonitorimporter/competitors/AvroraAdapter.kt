package ua.vitaliyshkarupa.pmonitorimporter.competitors

class AvroraAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://avrora.ua",
    fallbackBaseUrls = listOf("https://www.avrora.ua")
) {
    override val canonicalName: String = CompetitorRegistry.AVRORA
    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/ua/search/?q=$encodedQuery"
}
