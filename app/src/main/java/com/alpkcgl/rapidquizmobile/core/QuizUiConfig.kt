package com.alpkcgl.rapidquizmobile.core

/**
 * İstemcideki görsel sabitler. Oyun kuralları (soru sayısı, süre, puan) sunucudan gelir;
 * burada yalnızca web istemcisindeki arayüz zamanlamaları tutulur.
 */
object QuizUiConfig {
    /** Hazırlık ekranında 3, 2, 1 adımlarının her biri. */
    const val COUNTDOWN_BEAT_MS = 700L

    /** "BAŞLA!" en az bu kadar görünür. */
    const val COUNTDOWN_GO_MS = 400L

    /** Sunucu sonraki sorunun zamanını vermezse (son soru) geri bildirim süresi. */
    const val FEEDBACK_FALLBACK_MS = 1200L
    const val FEEDBACK_MIN_MS = 600L
    const val FEEDBACK_MAX_MS = 2500L

    /** Soru süresi sunucudan gelmezse kullanılır. */
    const val DEFAULT_TIME_LIMIT_MS = 5000L
    const val DEFAULT_TOTAL_QUESTIONS = 20

    const val LEADERBOARD_LIMIT = 10

    const val REQUEST_TIMEOUT_MS = 8000L

    /** Yalnızca ağ hatasında, tek sefer (web §9.4). */
    const val RETRY_DELAY_MS = 400L
}
