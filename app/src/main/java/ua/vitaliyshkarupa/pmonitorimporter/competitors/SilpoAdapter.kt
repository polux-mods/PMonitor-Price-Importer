package ua.vitaliyshkarupa.pmonitorimporter.competitors

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ua.vitaliyshkarupa.pmonitorimporter.model.ExcelProduct
import ua.vitaliyshkarupa.pmonitorimporter.model.WebProductCandidate
import ua.vitaliyshkarupa.pmonitorimporter.util.ProductTextParser
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SilpoAdapter : GenericHtmlAdapter(
    primaryBaseUrl = "https://silpo.ua",
    fallbackBaseUrls = listOf("https://www.silpo.ua", "https://shop.silpo.ua")
) {
    override val canonicalName: String = CompetitorRegistry.SILPO

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun buildSearchUrl(encodedQuery: String, baseUrl: String): String =
        "$baseUrl/search?find=$encodedQuery"

    override suspend fun search(product: ExcelProduct): List<WebProductCandidate> = withContext(Dispatchers.IO) {
        val apiCandidates = runCatching { searchSilpoApi(product) }.getOrDefault(emptyList())
        if (apiCandidates.isNotEmpty()) return@withContext apiCandidates
        super.search(product)
    }

    private suspend fun searchSilpoApi(product: ExcelProduct): List<WebProductCandidate> {
        delay(100)
        val branchId = silpoBranchId()
        val out = mutableListOf<WebProductCandidate>()

        for (query in ProductTextParser.searchQueryVariants(product.name, product.brand, product.barcode)) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urls = listOf(
                "https://sf-ecom-api.silpo.ua/v1/uk/branches/$branchId/quick-search" +
                    "?limit=10&search=$encoded&sortBy=productsList&sortDirection=desc&deliveryType=DeliveryHome",
                "https://sf-ecom-api.silpo.ua/v1/uk/branches/$branchId/products" +
                    "?limit=47&offset=0&deliveryType=DeliveryHome&includeChildCategories=true" +
                    "&sortBy=productsList&sortDirection=desc&inStock=true&search=$encoded"
            )

            for (url in urls) {
                val body = getText(url, referer = "https://silpo.ua/search?find=$encoded")
                out += JsonProductExtractor.extract(body, "https://silpo.ua")
                if (out.isNotEmpty()) break
            }
            if (out.isNotEmpty()) break
        }

        return out.distinctBy { ProductTextParser.normalize(it.title) + "|" + it.price }.take(40)
    }

    private fun getText(url: String, referer: String): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json,text/plain,*/*")
            .header("Accept-Language", "uk-UA,uk;q=0.9,ru;q=0.7,en;q=0.5")
            .header("Origin", "https://silpo.ua")
            .header("Referer", referer)
            .header("Cache-Control", "no-cache")

        webCookieHeader()?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Cookie", it) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Сільпо API: ${response.code} ${response.message}")
            return body
        }
    }

    private fun silpoBranchId(): String {
        val cookies = webCookieHeader().orEmpty()
        val uuid = Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")
            .find(cookies)
            ?.value
        // У твоєму логу Сільпо поки віддає нульову гілку. Коли WebView реально збереже branchId,
        // адаптер автоматично підставить його замість цього значення.
        return uuid ?: "00000000-0000-0000-0000-000000000000"
    }

    private fun webCookieHeader(): String? = runCatching {
        val manager = CookieManager.getInstance()
        manager.flush()
        listOf("https://silpo.ua", "https://www.silpo.ua", "https://shop.silpo.ua", "https://sf-ecom-api.silpo.ua")
            .flatMap { url -> manager.getCookie(url).orEmpty().split(';') }
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains('=') }
            .distinctBy { it.substringBefore('=') }
            .joinToString("; ")
            .ifBlank { null }
    }.getOrNull()

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36 PMonitorImporter/1.5"
    }
}
