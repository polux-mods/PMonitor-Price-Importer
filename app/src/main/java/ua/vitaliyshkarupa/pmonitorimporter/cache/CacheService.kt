package ua.vitaliyshkarupa.pmonitorimporter.cache

import android.content.Context
import ua.vitaliyshkarupa.pmonitorimporter.model.ImportStats
import java.io.File

class CacheService(private val context: Context) {
    private val prefs = context.getSharedPreferences("pmonitor_import_cache", Context.MODE_PRIVATE)
    private val cacheFile: File get() = File(context.cacheDir, "last_import_workbook.bin")

    fun hasLastImport(): Boolean = prefs.getBoolean(KEY_HAS_CACHE, false) && cacheFile.exists()

    fun saveLastImport(fileName: String, extension: String, bytes: ByteArray, stats: ImportStats) {
        cacheFile.writeBytes(bytes)
        prefs.edit()
            .putBoolean(KEY_HAS_CACHE, true)
            .putString(KEY_FILE_NAME, fileName)
            .putString(KEY_EXTENSION, extension)
            .putInt(KEY_IMPORTED, stats.imported)
            .putInt(KEY_TOTAL, stats.total)
            .putInt(KEY_SKIPPED, stats.skipped)
            .putInt(KEY_LOW_CONFIDENCE, stats.lowConfidence)
            .putInt(KEY_ERRORS, stats.errors)
            .apply()
    }

    fun loadLastBytes(): ByteArray? = if (hasLastImport()) cacheFile.readBytes() else null

    fun lastFileName(): String = prefs.getString(KEY_FILE_NAME, "last_import.xlsx") ?: "last_import.xlsx"

    fun lastExtension(): String = prefs.getString(KEY_EXTENSION, "xlsx") ?: "xlsx"

    fun lastStats(): ImportStats = ImportStats(
        imported = prefs.getInt(KEY_IMPORTED, 0),
        total = prefs.getInt(KEY_TOTAL, 0),
        skipped = prefs.getInt(KEY_SKIPPED, 0),
        lowConfidence = prefs.getInt(KEY_LOW_CONFIDENCE, 0),
        errors = prefs.getInt(KEY_ERRORS, 0)
    )

    fun clear() {
        cacheFile.delete()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_HAS_CACHE = "has_cache"
        private const val KEY_FILE_NAME = "file_name"
        private const val KEY_EXTENSION = "extension"
        private const val KEY_IMPORTED = "imported"
        private const val KEY_TOTAL = "total"
        private const val KEY_SKIPPED = "skipped"
        private const val KEY_LOW_CONFIDENCE = "low_confidence"
        private const val KEY_ERRORS = "errors"
    }
}
