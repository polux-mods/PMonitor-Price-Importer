package ua.vitaliyshkarupa.pmonitorimporter.store

import android.content.Context
import androidx.core.content.edit

/**
 * Легкий маркер, що користувач уже відкривав сайт конкурента у WebView і натиснув "Готово".
 * Самі cookies зберігає Android WebView/CookieManager, а тут лише статус для інтерфейсу.
 */
class StoreSessionService(context: Context) {
    private val prefs = context.getSharedPreferences("pmonitor_store_sessions", Context.MODE_PRIVATE)

    fun markConfigured(competitor: String) {
        prefs.edit {
            putBoolean(key(competitor), true)
            putLong(timeKey(competitor), System.currentTimeMillis())
        }
    }

    fun isConfigured(competitor: String): Boolean = prefs.getBoolean(key(competitor), false)

    fun configuredSet(competitors: List<String>): Set<String> = competitors.filter { isConfigured(it) }.toSet()

    private fun key(competitor: String) = "configured_$competitor"
    private fun timeKey(competitor: String) = "configured_at_$competitor"
}
