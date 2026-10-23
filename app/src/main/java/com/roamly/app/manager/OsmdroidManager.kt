package com.roamly.app.manager

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

// Единая точка инициализации osmdroid: вызывается один раз за процесс.
object OsmdroidManager {

    private const val PREFS_FILE = "osmdroid_prefs"
    private const val PREF_USER_AGENT = "useragent"

    // 256 МБ дискового кэша тайлов
    private const val CACHE_MAX_BYTES = 256L * 1024L * 1024L

    @Volatile
    private var configured = false

    fun ensure(context: Context) {
        if (configured) return
        synchronized(this) {
            if (configured) return

            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val config = Configuration.getInstance()

            // 1. Задаем корректный User-Agent ДО загрузки.
            // ВАЖНО: applicationId/namespace приложения не должен содержать "com.example".
            val validUserAgent = "RoamlyApp/1.0 (Android; contact: support@roamly.app)"
            config.userAgentValue = validUserAgent

            // 2. Загружаем конфиг
            config.load(context, prefs)

            // 3. Жестко переустанавливаем User-Agent ПОСЛЕ load(),
            // чтобы метод load() не перезаписал его дефолтным значением "osmdroid".
            config.userAgentValue = validUserAgent
            prefs.edit().putString(PREF_USER_AGENT, validUserAgent).commit()

            // 4. Настройка постоянного дискового кэша тайлов
            val basePath = File(context.filesDir, "osmdroid")
            val cachePath = File(basePath, "tiles")
            if (!basePath.exists()) basePath.mkdirs()
            if (!cachePath.exists()) cachePath.mkdirs()

            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = cachePath
            config.tileFileSystemCacheMaxBytes = CACHE_MAX_BYTES
            config.tileFileSystemCacheTrimBytes = CACHE_MAX_BYTES

            configured = true
        }
    }
}