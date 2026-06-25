package ua.vitaliyshkarupa.pmonitorimporter.competitors

/**
 * Стартові сторінки для вибору магазину/міста у WebView.
 * Cookies, які сайт збереже після вибору магазину, потім додаються до HTTP-запитів адаптерів.
 */
data class CompetitorWebConfig(
    val competitor: String,
    val startUrl: String,
    val cookieUrls: List<String>
)

object CompetitorWebConfigs {
    private val all = mapOf(
        CompetitorRegistry.ATB to CompetitorWebConfig(
            competitor = CompetitorRegistry.ATB,
            startUrl = "https://www.atbmarket.com",
            cookieUrls = listOf(
                "https://www.atbmarket.com",
                "https://atbmarket.com"
            )
        ),
        CompetitorRegistry.SILPO to CompetitorWebConfig(
            competitor = CompetitorRegistry.SILPO,
            startUrl = "https://silpo.ua",
            cookieUrls = listOf(
                "https://silpo.ua",
                "https://www.silpo.ua",
                "https://shop.silpo.ua"
            )
        ),
        CompetitorRegistry.EVA to CompetitorWebConfig(
            competitor = CompetitorRegistry.EVA,
            startUrl = "https://eva.ua/ua/",
            cookieUrls = listOf(
                "https://eva.ua",
                "https://www.eva.ua"
            )
        ),
        CompetitorRegistry.AVRORA to CompetitorWebConfig(
            competitor = CompetitorRegistry.AVRORA,
            startUrl = "https://avrora.ua/ua/",
            cookieUrls = listOf(
                "https://avrora.ua",
                "https://www.avrora.ua"
            )
        )
    )

    fun forCompetitor(name: String): CompetitorWebConfig? = all[name]

    fun cookieUrlsFor(name: String): List<String> = all[name]?.cookieUrls.orEmpty()
}
