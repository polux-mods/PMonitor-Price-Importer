package ua.vitaliyshkarupa.pmonitorimporter.competitors

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class AtbAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://www.atbmarket.com",
    fallbackBaseUrls = listOf("https://atbmarket.com")
) {
    override val canonicalName: String = CompetitorRegistry.ATB

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/sch?lang=uk&location=${atbLocation()}&query=$encodedQuery"

    override suspend fun search(product: ExcelProduct): List<WebProductCandidate> = withContext(Dispatchers.IO) {
        val apiCandidates = runCatching { searchMultisearch(product) }.getOrDefault(emptyList())
        if (apiCandidates.isNotEmpty()) return@withContext apiCandidates
        super.search(product)
    }

    private suspend fun searchMultisearch(product: ExcelProduct): List<WebProductCandidate> {
        delay(90)
        val query = ProductTextParser.searchableQuery(product.name, product.brand, product.barcode)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val location = atbLocation()
        val uid = UUID.randomUUID().toString()
        val marker = System.currentTimeMillis().toString()
        val url = "https://api.multisearch.io/?id=11280" +
            "&key=63a6d0a760fd2d0562c4061b78e64754" +
            "&lang=uk&location=$location&m=$marker&q=${marker.takeLast(6)}" +
            "&query=$encoded&s=mini&uid=$uid"

        val body = getText(url, referer = "https://www.atbmarket.com/sch?lang=uk&location=$location&query=$encoded")
        val candidates = JsonProductExtractor.extract(body, "https://www.atbmarket.com")
        if (candidates.isNotEmpty()) return candidates

        // Якщо API повернув неочікувану схему, пробуємо звичайну сторінку пошуку АТБ.
        val searchHtml = getText("https://www.atbmarket.com/sch?lang=uk&location=$location&query=$encoded", "https://www.atbmarket.com/")
        val doc = Jsoup.parse(searchHtml, "https://www.atbmarket.com")
        return HtmlProductExtractor.extract(doc, "https://www.atbmarket.com")
    }

    private fun getText(url: String, referer: String): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json,text/html,application/xhtml+xml,*/*;q=0.8")
            .header("Accept-Language", "uk-UA,uk;q=0.9,ru;q=0.7,en;q=0.5")
            .header("Referer", referer)
            .header("Origin", "https://www.atbmarket.com")
            .header("Cache-Control", "no-cache")

        webCookieHeader()?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Cookie", it) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("АТБ API: ${response.code} ${response.message}")
            return body
        }
    }

    private fun atbLocation(): String {
        val cookies = webCookieHeader().orEmpty()
        val byName = Regex("""(?:user_store|nstore_id|location|store_id)=([0-9]{2,})""")
            .find(cookies)
            ?.groupValues
            ?.getOrNull(1)
        // 1075 — магазин/локація з діагностичного логу. Якщо користувач вибрав магазин у WebView,
        // cookie має замінити це значення.
        return byName ?: "1075"
    }

    private fun webCookieHeader(): String? = runCatching {
        val manager = CookieManager.getInstance()
        manager.flush()
        listOf("https://www.atbmarket.com", "https://atbmarket.com")
            .flatMap { url -> manager.getCookie(url).orEmpty().split(';') }
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains('=') }
            .distinctBy { it.substringBefore('=') }
            .joinToString("; ")
            .ifBlank { null }
    }.getOrNull()

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36 PMonitorImporter/1.4"
    }
}
