package ua.vitaliyshkarupa.pmonitorimporter.competitors

import kotlinx.coroutines.Dispatchers
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

    override suspend fun search(product: ExcelProduct): List<WebProductCandidate> = withContext(Dispatchers.IO) {
        val query = ProductTextParser.searchableQuery(product.name, product.brand, product.barcode)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url(buildSearchUrl(encoded))
            .header("User-Agent", "Mozilla/5.0 (Android) PMonitorImporter/1.0")
            .header("Accept", "text/html,application/xhtml+xml,application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) return@withContext emptyList()
            val document = Jsoup.parse(body, baseUrl)
            HtmlProductExtractor.extract(document, baseUrl).take(20)
        }
    }
}
