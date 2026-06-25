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
import java.util.concurrent.TimeUnit

abstract class GenericHtmlAdapter(
    private val baseUrl: String
) : CompetitorAdapter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    protected abstract fun buildSearchUrl(encodedQuery: String): String

    protected open val cookieUrls: List<String>
        get() = listOf(baseUrl) + CompetitorWebConfigs.cookieUrlsFor(canonicalName)

    override suspend fun search(product: ExcelProduct): List<WebProductCandidate> = withContext(Dispatchers.IO) {
        val query = ProductTextParser.searchableQuery(product.name, product.brand, product.barcode)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val requestBuilder = Request.Builder()
            .url(buildSearchUrl(encoded))
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36 PMonitorImporter/1.1")
            .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "uk-UA,uk;q=0.9,ru;q=0.7,en;q=0.6")
            .header("Cache-Control", "no-cache")

        webViewCookieHeader()?.let { requestBuilder.header("Cookie", it) }

        // Невелика пауза зменшує шанс блокування при великому Excel.
        delay(120)

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) return@withContext emptyList()
            val document = Jsoup.parse(body, baseUrl)
            HtmlProductExtractor.extract(document, baseUrl).take(20)
        }
    }

    private fun webViewCookieHeader(): String? {
        return runCatching {
            val manager = CookieManager.getInstance()
            manager.flush()
            cookieUrls
                .distinct()
                .flatMap { url -> manager.getCookie(url).orEmpty().split(';') }
                .map { it.trim() }
                .filter { it.isNotBlank() && it.contains('=') }
                .distinctBy { it.substringBefore('=') }
                .joinToString("; ")
                .ifBlank { null }
        }.getOrNull()
    }
}
