package com.alpkcgl.rapidquizmobile.data.repository

import com.alpkcgl.rapidquizmobile.data.api.RapidQuizApi
import com.alpkcgl.rapidquizmobile.data.model.Category
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Kategori listesi; ana ekran, hazırlık ekranı ve skor tablosu sekmeleri paylaşır. */
class CatalogRepository(private val api: RapidQuizApi) {
    private val mutex = Mutex()
    private var cache: List<Category>? = null

    /** Önbellek varsa onu döner; [force] ile sunucudan yeniden çeker. */
    suspend fun getCategories(force: Boolean = false): List<Category> = mutex.withLock {
        if (!force) cache?.let { return it }
        api.getCategories().also { cache = it }
    }
}
