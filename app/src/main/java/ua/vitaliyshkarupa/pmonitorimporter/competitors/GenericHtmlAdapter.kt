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
import java.io.IOException
import java.net.URI
import java.net.UnknownHostException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

abstract class GenericHtmlAdapter(
    private val primaryBaseUrl: String,
    private val fallbackBaseUrls: List<String> = emptyList()
) : CompetitorAdapter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    protected open val searchBaseUrls: List<String>
        get() = (listOf(primaryBaseUrl) + fallbackBaseUrls).distinct()

    protected abstract fun buildSearchUrl(encodedQuery: String, baseUrl: String): String

    protected open val cookieUrls: List<String>
        get() = (searchBaseUrls + CompetitorWebConfigs.cookieUrlsFor(canonicalName)).distinct()

    override suspend fun search(product: ExcelProduct): List<WebProductCandidate> = withContext(Dispatchers.IO) {
        val query = ProductTextParser.searchableQuery(product.name, product.brand, product.barcode)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val cookieHeader = webViewCookieHeader()
        var lastError: Throwable? = null
        var hadReadableResponse = false

        // Невелика пауза зменшує шанс блокування при великому Excel.
        delay(120)

        for (baseUrl in searchBaseUrls) {
            val searchUrl = buildSearchUrl(encoded, baseUrl)
            val requestBuilder = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36 PMonitorImporter/1.2")
                .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "uk-UA,uk;q=0.9,ru;q=0.7,en;q=0.6")
                .header("Cache-Control", "no-cache")
                .header("Referer", baseUrl)

            if (!cookieHeader.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookieHeader)
            }

            try {
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful || body.isBlank()) {
                        lastError = IOException("${response.code} ${response.message} для ${hostOf(searchUrl)}")
                        return@use
                    }

                    hadReadableResponse = true
                    val document = Jsoup.parse(body, baseUrl)
                    val candidates = HtmlProductExtractor.extract(document, baseUrl).take(20)
                    if (candidates.isNotEmpty()) {
                        return@withContext candidates
                    }
                }
            } catch (e: UnknownHostException) {
                lastError = IOException("Не вдалося відкрити ${hostOf(searchUrl)}. Перевір DNS/Private DNS/VPN або спробуй іншу мережу", e)
            } catch (e: IOException) {
                lastError = IOException("Мережна помилка ${hostOf(searchUrl)}: ${e.message ?: e::class.java.simpleName}", e)
            } catch (t: Throwable) {
                lastError = t
            }
        }

        if (!hadReadableResponse && lastError is IOException && searchBaseUrls.size > 1) {
            throw IOException("Не вдалося отримати сторінку пошуку ${canonicalName}. Перевір вибір магазину у WebView і підключення до інтернету. Остання помилка: ${lastError?.message}", lastError)
        }

        emptyList()
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

    private fun hostOf(url: String): String = runCatching { URI(url).host ?: url }.getOrDefault(url)
}
