# Rapid Quiz – Android (Kotlin + Jetpack Compose) Proje Dokümanı

> Web'de tamamlanan **Rapid Quiz** uygulamasının Android istemcisidir. Android uygulaması,
> web ile **aynı Django REST API'sini** kullanır; oyun kuralları, puanlama ve süre tamamen
> sunucudadır.
>
> **Referans kaynaklar (tek doğruluk kaynağı bunlardır):**
> - Backend: [alpkcgl342/RapidQuizProjeBackend](https://github.com/alpkcgl342/RapidQuizProjeBackend)
>   → `docs/rapid-quiz-proje-dokumani.md` (özellikle §3 akış, §4 oyun mekaniği, §7 API, §11 mobil hazırlık) ve `openapi.yaml`
> - Web frontend: [alpkcgl342/RapidQuizProjeFrontend](https://github.com/alpkcgl342/RapidQuizProjeFrontend)
>   → `src/stores/quiz.ts` (oturum durum makinesi), `src/views/*`, `src/assets/styles/tokens.css`
>
> Bu doküman ile backend/web arasında çelişki olursa **backend ve web kazanır**; bu doküman güncellenir.

---

## 1. Kapsam

### Yapılacaklar (web ile birebir)
1. **Ana ekran** – Kategoriler API'den listelenir; alt kısımda "Skor Tablosu" butonu.
2. **Hazırlık ekranı** – 3 → 2 → 1 → BAŞLA! geri sayımı, "BAŞLA!" anında oturum açılır.
3. **Soru ekranı** – 20 soru, her biri 5 saniye; geri sayım halkası, 4 şık, anlık puan.
4. **Cevap geri bildirimi** – Doğru/yanlış/süre doldu + kazanılan puan rozeti, otomatik geçiş.
5. **Sonuç ekranı** – Toplam puan, doğru/yanlış/kaçırılan, doğruluk, ortalama süre, tahmini sıra.
6. **Takma ad ekranı** – Skoru skor tablosuna kaydetme (veya "Kaydetmeden geç").
7. **Skor tablosu** – `Genel` + her kategori için sekme, Top 10; kullanıcının kendi satırı vurgulu.

### Yapılmayacaklar (bilinçli olarak kapsam dışı)
- Giriş/kayıt, profil, ayarlar, bildirim, offline mod, admin paneli.
- Supabase'e doğrudan erişim (bkz. §2).
- Web'de olmayan hiçbir yeni özellik (koyu tema dahil; web yalnızca açık tema).

---

## 2. Mimari

```
┌──────────────────┐   HTTPS / JSON    ┌───────────────────────┐        ┌──────────────┐
│ Android (Compose)│ ────────────────▶ │ Django REST API       │ ─────▶ │ PostgreSQL   │
│ Vue web istemcisi│   /api/v1/...     │ (oyun mantığı burada) │        │ (Supabase)   │
└──────────────────┘                   └───────────────────────┘        └──────────────┘
```

- Android uygulaması **yalnızca Django API ile** konuşur. Supabase sadece Django'nun veritabanı
  sunucusudur; uygulamaya Supabase URL'i veya anahtarı **girmez**.
- Neden: süre ve puan **sunucu otoriteli**. Doğru şık istemciye cevap verilmeden gönderilmez,
  sorular tek tek servis edilir, skor istemciden alınmaz. Doğrudan veritabanına bağlanmak bu
  güvenceleri atlar ve web ile farklı sonuç üretir.

---

## 3. Oyun Kuralları (sunucudan gelir — istemcide sabitlenmez)

| Kural | Değer | Kaynak |
|---|---|---|
| Oturum başına soru | 20 | `Category.questions_per_session`, `Session.total_questions` |
| Soru başına süre | 5000 ms | `ServedQuestion.time_limit_ms` |
| Ağ toleransı | 800 ms | Sunucuda; istemci bilmez |
| Doğru cevap puanı | `100 + round(100 × (5000 − min(elapsed, 5000)) / 5000)` → 100–200 | Sunucu hesaplar, `points_earned` döner |
| Yanlış / süre doldu | 0 | |
| Geri bildirim süresi | ~1200 ms | Sonraki sorunun `served_at` değeri belirler |
| Oturum ömrü | 30 dk | `Session.expires_at` |
| Skor tablosu | Top 10 | `limit=10` |
| Takma ad | 2–20 karakter, `^[A-Za-z0-9ÇĞİÖŞÜçğıöşüÂÎÛâîû_\- ]+$`, boşluklar normalize | Backend `common/nickname.py` |

İstemcide yalnızca **görsel** sabitler tutulur (`QuizUiConfig`):

```kotlin
object QuizUiConfig {
    const val COUNTDOWN_BEAT_MS = 700L        // 3, 2, 1 adımları
    const val COUNTDOWN_GO_MS = 400L          // "BAŞLA!"
    const val FEEDBACK_FALLBACK_MS = 1200L    // son soruda (sonraki soru yoksa)
    const val FEEDBACK_MIN_MS = 600L
    const val FEEDBACK_MAX_MS = 2500L
    const val LEADERBOARD_LIMIT = 10
    const val NICKNAME_MIN = 2
    const val NICKNAME_MAX = 20
    const val REQUEST_TIMEOUT_MS = 8000L
    const val RETRY_DELAY_MS = 400L           // yalnız ağ hatasında, tek sefer
}
```

---

## 4. Teknoloji Yığını

| Katman | Seçim |
|---|---|
| Dil | Kotlin (AGP 9'un yerleşik Kotlin desteği; ayrı `kotlin-android` eklentisi yok) |
| UI | Jetpack Compose + Material 3 |
| Navigasyon | Navigation Compose, type-safe route (`@Serializable`) |
| Mimari | MVVM + tek yönlü veri akışı (UiState + StateFlow) |
| Ağ | Ktor Client (OkHttp engine) + ContentNegotiation + kotlinx.serialization |
| Asenkron | Coroutines + Flow |
| DI | Manuel `AppContainer` (Hilt yok) |
| SDK | minSdk 27, compile/target 37 (proje oluşturulurken seçildiği gibi) |
| Font | Space Grotesk (başlık/sayı) + Inter (gövde), `res/font` içinde gömülü |

---

## 5. Gradle Yapılandırması

Mevcut sürümler korunur (AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01). Eklenecekler
(`gradle/libs.versions.toml`), sürümler eklenirken en güncel stabil olarak seçilir:

```toml
[versions]
navigation = "…"
lifecycle = "…"          # lifecycle-viewmodel-compose / runtime-compose
ktor = "3.x"
serialization = "1.x"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`app/build.gradle.kts` içinde API adresi `BuildConfig`'e gelir:

```kotlin
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    defaultConfig {
        // Emülatörden bilgisayardaki Django'ya: 10.0.2.2
        buildConfigField(
            "String", "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8000/api/v1/")}\""
        )
    }
    buildTypes {
        release {
            // Canlı adres belli olunca burada sabitlenir (bkz. §18)
            buildConfigField("String", "API_BASE_URL", "\"https://api.rapidquiz.app/api/v1/\"")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
}
```

### `local.properties` (git'e EKLENMEZ, isteğe bağlı)
```properties
# Gerçek cihazla test ederken bilgisayarın yerel IP'si:
API_BASE_URL=http://192.168.1.20:8000/api/v1/
```

### `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />
<application android:name=".RapidQuizApp" … >
```

---

## 6. Yerel Geliştirme Ortamı

Backend şimdilik yerelde çalışır (backend README'deki "Hızlı başlangıç").

| Gereksinim | Neden / Nasıl |
|---|---|
| Backend `.env` → `ALLOWED_HOSTS=localhost,127.0.0.1,10.0.2.2` | Emülatör `Host: 10.0.2.2` gönderir; eklenmezse Django `400 Bad Request` döner. Gerçek cihazda bilgisayarın IP'si de eklenir ve `runserver 0.0.0.0:8000` ile başlatılır. |
| Cleartext HTTP yalnızca debug'da | `src/debug/res/xml/network_security_config.xml` debug'da düz HTTP'ye genel izin verir (bilgisayarın yerel IP'si DHCP ile değişebildiği için adres listesi tutulmaz); release HTTPS zorunlu. |
| Gerçek telefon | `local.properties` → `API_BASE_URL=http://<bilgisayar-IP>:8000/api/v1/`; backend `.env` → `ALLOWED_HOSTS`'a aynı IP; Windows Güvenlik Duvarı'nda 8000 portu yerel ağa açık olmalı. Telefon ile bilgisayar aynı ağda olmalı. |
| Yerel ağ izni (Android 17+) | targetSdk 37 uygulamalar `ACCESS_LOCAL_NETWORK` izni olmadan `10.0.2.2` / `192.168.x.x` adreslerine ulaşamaz; paketler sessizce düşer ve istekler zaman aşımına uğrar. İzin yalnızca `src/debug/AndroidManifest.xml`'de tanımlıdır ve debug sürüm açılışta ister. Release'te gerekmez (canlı API herkese açık HTTPS). |
| CORS | Mobil istemci CORS'a tabi değildir, ayar gerekmez. |
| Throttle | Oturum açma 30/saat, cevap 600/saat, skor 20/saat (IP bazlı). Yoğun testte `429` görülebilir; backend `.env`'de `THROTTLE_*` gevşetilebilir. |

---

## 7. API Sözleşmesi

Base URL: `BuildConfig.API_BASE_URL` (`…/api/v1/`). Ayrıntılı şema: backend `openapi.yaml`.

### 7.1 Uç noktalar

| Metot | Yol | Auth | Kullanıldığı yer |
|---|---|---|---|
| GET | `categories/` | — | Ana ekran, skor tablosu sekmeleri |
| POST | `quiz-sessions/` `{category: slug}` | — | Hazırlık ekranı "BAŞLA!" anı → `session` + ilk `question` |
| POST | `quiz-sessions/{id}/answers/` `{question_id, selected_option_id \| null}` | Token | Cevap / süre doldu → `result`, `session`, `next_question`, son soruda `summary` |
| GET | `quiz-sessions/{id}/current-question/` | Token | Kopma veya arka plandan dönüşte eşitleme (resync) |
| GET | `quiz-sessions/{id}/summary/` | Token | Sonuç ekranı (cevap yanıtında gelmediyse) |
| POST | `quiz-sessions/{id}/submit-score/` `{nickname}` | Token | Takma ad ekranı → `entry` (rank_overall, rank_in_category) |
| GET | `leaderboard/?category=<slug>&limit=10` | — | Skor tablosu; `category` yoksa genel |

### 7.2 Başlıklar
Her istekte:
- `X-Client-Platform: android`
- `X-Client-Version: <BuildConfig.VERSION_NAME>`
- Oturum isteklerinde `X-Session-Token: <session.token>`

### 7.3 Hata zarfı ve kodlar
Tüm hatalar `{"error": {"code", "message", "details"}}` biçimindedir. `message` Türkçedir ve
doğrudan kullanıcıya gösterilebilir.

| Kod | HTTP | İstemci davranışı |
|---|---|---|
| `NETWORK_ERROR` (istemci üretir) | — | Cevap gönderiminde 400 ms sonra **bir kez** tekrar dene; yine olmazsa "Bağlantı koptu" → "Devam et" = resync |
| `ALREADY_ANSWERED`, `SESSION_ALREADY_FINISHED` | 409 | Sessizce resync |
| `SCORE_ALREADY_SUBMITTED` | 409 | Skor tablosuna geç |
| `INVALID_SESSION_TOKEN`, `SESSION_EXPIRED` | 401 / 410 | Oturumu sıfırla, ana ekrana dön, uyarı göster ("Oturumun süresi doldu…") |
| `INSUFFICIENT_QUESTIONS` | 422 | "Bu kategori şu an hazırlanıyor." (tekrar dene yok) |
| `VALIDATION_ERROR` | 400 | Takma adda `details.nickname[0]` alan hatası olarak gösterilir |
| `RATE_LIMITED` | 429 | Sunucu mesajı gösterilir |

---

## 8. Kotlin Modelleri (`data/model`)

JSON `snake_case`; Kotlin tarafında `@SerialName` kullanılır. `Json { ignoreUnknownKeys = true }`
(sunucu alan eklerse eski sürüm bozulmasın).

```kotlin
@Serializable data class Category(
    val slug: String, val name: String, val description: String = "",
    @SerialName("color_hex") val colorHex: String, val icon: String,
    @SerialName("questions_per_session") val questionsPerSession: Int,
    @SerialName("available_question_count") val availableQuestionCount: Int,
    @SerialName("is_playable") val isPlayable: Boolean,
)
@Serializable data class CategoryList(val results: List<Category>)
@Serializable data class CategoryRef(val slug: String, val name: String, @SerialName("color_hex") val colorHex: String)

@Serializable data class ServedOption(val id: Long, val label: String, val text: String)
@Serializable data class ServedQuestion(
    val id: Long, val index: Int, val text: String, val options: List<ServedOption>,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    @SerialName("served_at") val servedAt: String,
    @SerialName("deadline_at") val deadlineAt: String,
)
@Serializable data class Session(
    val id: String, val token: String, val category: CategoryRef,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("current_index") val currentIndex: Int,
    val score: Int, val status: String,
    @SerialName("expires_at") val expiresAt: String,
)
@Serializable data class CreateSessionResponse(val session: Session, val question: ServedQuestion)

@Serializable data class SessionProgress(
    @SerialName("current_index") val currentIndex: Int, val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("wrong_count") val wrongCount: Int,
    @SerialName("timeout_count") val timeoutCount: Int,
    val status: String,
)
@Serializable enum class Outcome { @SerialName("correct") CORRECT, @SerialName("wrong") WRONG, @SerialName("timeout") TIMEOUT }
@Serializable data class AnswerResult(
    val outcome: Outcome, @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("correct_option_id") val correctOptionId: Long,
    @SerialName("selected_option_id") val selectedOptionId: Long?,
    @SerialName("elapsed_ms") val elapsedMs: Long,
    @SerialName("points_earned") val pointsEarned: Int,
    val explanation: String = "",
)
@Serializable data class AnswerRequest(
    @SerialName("question_id") val questionId: Long,
    @SerialName("selected_option_id") val selectedOptionId: Long?,   // null = süre doldu
)
@Serializable data class AnswerResponse(
    val result: AnswerResult, val session: SessionProgress,
    @SerialName("next_question") val nextQuestion: ServedQuestion?,
    val summary: Summary? = null,
)
@Serializable data class CurrentQuestionResponse(val question: ServedQuestion?, val session: SessionProgress)

@Serializable data class Summary(
    @SerialName("session_id") val sessionId: String, val category: CategoryRef, val status: String,
    val score: Int, @SerialName("max_possible_score") val maxPossibleScore: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("wrong_count") val wrongCount: Int,
    @SerialName("timeout_count") val timeoutCount: Int,
    @SerialName("accuracy_pct") val accuracyPct: Double,
    @SerialName("average_elapsed_ms") val averageElapsedMs: Long?,
    @SerialName("fastest_correct_ms") val fastestCorrectMs: Long?,
    @SerialName("total_elapsed_ms") val totalElapsedMs: Long,
    @SerialName("score_submitted") val scoreSubmitted: Boolean,
    @SerialName("estimated_rank") val estimatedRank: Int,
    @SerialName("finished_at") val finishedAt: String,
)

@Serializable data class SubmitScoreRequest(val nickname: String)
@Serializable data class Entry(
    val id: Long, val nickname: String, val score: Int, val category: CategoryRef,
    @SerialName("rank_in_category") val rankInCategory: Int,
    @SerialName("rank_overall") val rankOverall: Int,
    @SerialName("created_at") val createdAt: String,
)
@Serializable data class SubmitScoreResponse(val entry: Entry /* leaderboard alanı kullanılmıyor */)

@Serializable data class LeaderboardRow(
    val id: Long, val rank: Int, val nickname: String, val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    val category: CategoryRef, @SerialName("created_at") val createdAt: String,
)
@Serializable data class LeaderboardResponse(val results: List<LeaderboardRow>)

@Serializable data class ErrorEnvelope(val error: ErrorBody)
@Serializable data class ErrorBody(val code: String, val message: String, val details: JsonObject = JsonObject(emptyMap()))
```

Zaman damgaları ISO 8601 (UTC); `java.time.Instant.parse(...)` ile çözülür (minSdk 27'de mevcut).

---

## 9. Paket Yapısı

```
com.alpkcgl.rapidquizmobile
├── MainActivity.kt
├── RapidQuizApp.kt                  // Application + AppContainer
├── core/
│   ├── QuizUiConfig.kt
│   ├── UiState.kt
│   └── Nickname.kt                  // normalize + doğrulama (backend ile aynı regex)
├── data/
│   ├── api/
│   │   ├── HttpClientFactory.kt     // Ktor: timeout, başlıklar, JSON, hata zarfı → ApiException
│   │   ├── ApiException.kt          // status, code, message, details, isNetwork
│   │   └── RapidQuizApi.kt          // uç nokta fonksiyonları
│   ├── model/                       // §8
│   └── repository/
│       ├── CatalogRepository.kt     // kategori listesi, bellekte önbellek
│       ├── QuizSessionManager.kt    // oturum durum makinesi (§10), uygulama ömrü boyunca tek örnek
│       └── LeaderboardRepository.kt
└── ui/
    ├── navigation/ (Routes.kt, AppNavHost.kt)
    ├── theme/ (Color.kt, Type.kt, Theme.kt, CategoryColors.kt)
    ├── components/ (LoadingView, ErrorView, CategoryIcon, RankMedal, …)
    ├── home/ (HomeScreen, HomeViewModel)
    ├── countdown/ (CountdownScreen, CountdownViewModel)
    ├── quiz/ (QuizScreen, QuizViewModel, CountdownRing, OptionButton, FeedbackOverlay)
    ├── result/ (ResultScreen, ResultViewModel)
    ├── nickname/ (NicknameScreen, NicknameViewModel)
    └── leaderboard/ (LeaderboardScreen, LeaderboardViewModel)
```

```kotlin
class AppContainer {
    private val http = HttpClientFactory.create(BuildConfig.API_BASE_URL)
    private val api = RapidQuizApi(http)
    val catalogRepository = CatalogRepository(api)
    val leaderboardRepository = LeaderboardRepository(api)
    val quizSession = QuizSessionManager(api)          // ekranlar arası paylaşılan oturum
}
```

ViewModel'ler `viewModelFactory { initializer { … } }` ile oluşturulur;
container'a `(this[APPLICATION_KEY] as RapidQuizApp).container` ile erişilir.

---

## 10. Oturum Durum Makinesi (`QuizSessionManager`)

Web'deki `stores/quiz.ts` Kotlin'e aktarılır. Oturum Hazırlık → Soru → Sonuç → Takma ad →
Skor tablosu boyunca yaşadığı için tek bir ViewModel'de değil, `AppContainer`'daki
`QuizSessionManager` içinde tutulur ve `StateFlow<QuizState>` yayınlar.

```
idle → preparing → question → answering → feedback → question … → finished → submitted
                      ▲            │ ağ hatası
                      └─ resync ── disconnected
```

```kotlin
enum class QuizStatus { IDLE, PREPARING, QUESTION, ANSWERING, FEEDBACK, DISCONNECTED, FINISHED, SUBMITTED, ERROR }

data class QuizState(
    val status: QuizStatus = QuizStatus.IDLE,
    val sessionId: String? = null,
    val category: CategoryRef? = null,
    val question: ServedQuestion? = null,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 20,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeoutCount: Int = 0,
    val lastResult: AnswerResult? = null,
    val summary: Summary? = null,
    val submittedEntry: Entry? = null,
    val deadlineAtLocalMs: Long? = null,     // mevcut sorunun yerel saatle bitişi
    val feedbackUntilLocalMs: Long? = null,
    val error: ApiException? = null,
)
```

**Kurallar:**
- **Saat sapması:** `startSession` yanıtı gelince `clockSkewMs = parse(question.served_at) − now()`.
  Tüm sunucu zamanları `yerel = sunucu − clockSkewMs` ile çevrilir. Böylece sayaç yanıt
  ulaştığında tam 5 sn ile başlar.
- **Zaman kaynağı:** `SystemClock.elapsedRealtime()` değil duvar saati (`System.currentTimeMillis()`)
  kullanılır, çünkü sunucu zamanlarıyla karşılaştırılır; sapma zaten skew ile düzeltilir.
- **answer(optionId?)**: yalnız `QUESTION` durumunda kabul edilir (çift dokunma koruması) →
  `ANSWERING` → istek (ağ hatasında 400 ms sonra bir kez tekrar) → `FEEDBACK`.
  `feedbackUntil = clamp(yerel(next_question.served_at), now+600, now+2500)`; sonraki soru yoksa `now+1200`.
- **advance()**: geri bildirim süresi bitince bekleyen soruyu göster; yoksa `FINISHED`.
- **Süre doldu:** istemci sayacı 0'a inince `answer(null)`; sonucu (`timeout`) sunucu belirler.
- **resync()**: `current-question` → soru varsa göster; yoksa `summary` çek → `FINISHED`/`SUBMITTED`.
- **Arka plan:** Uygulama öne gelince (`Lifecycle.ON_START`) `QuizViewModel` zamanlayıcıları
  duvar saatine göre yeniden kurar; süre dolmuşsa hemen `answer(null)` gönderilir
  (backend §11: arka planda süre işlemeye devam eder). `delay` cihaz uykusunu saymadığı için
  bu gereklidir. Ağ koparsa `resync()` "Devam et" ile tetiklenir.
- **Token saklama:** Token yalnızca bellekte (`QuizSessionManager`) tutulur; diske yazılmaz.
  Süreç ölürse oturum kaybolur ve kullanıcı ana ekrandan yeniden başlar (web'de sekmeyi
  kapatmakla aynı). Ekran döndürme süreci öldürmediği için etkilenmez.
- **reset()**: ana ekrana dönüşte veya oyundan çıkışta.

---

## 11. Navigasyon

```kotlin
@Serializable data object HomeRoute
@Serializable data class CountdownRoute(val categorySlug: String)
@Serializable data object QuizRoute
@Serializable data object ResultRoute
@Serializable data object NicknameRoute
@Serializable data class LeaderboardRoute(val categorySlug: String? = null)   // null = Genel
```

```
Home ──(kategori)──▶ Countdown ──(BAŞLA!)──▶ Quiz ──(20. soru)──▶ Result ──(Skoru Kaydet)──▶ Nickname
  │                    [replace]              [replace]              │                           │
  │                                                                  ├─(Tekrar Oyna)──▶ Countdown │
  └────────────────(Skor Tablosu)──────────▶ Leaderboard ◀──────────┴──(Skor Tablosu)────────────┘
```

- Countdown → Quiz → Result geçişleri `popUpTo(...) { inclusive = true }` ile geri yığından
  çıkarılır; geri tuşu bitmiş bir soruya/geri sayıma döndürmez.
- **Quiz ekranında geri tuşu / X:** `BackHandler` → "Çıkarsan skorun kaydedilmez, emin misin?"
  onayı; onaylanırsa `reset()` + Home.
- Nickname → Leaderboard: kaydedilen kategorinin sekmesiyle açılır; geri tuşu Home'a döner.
- Leaderboard'dan "Tekrar Oyna" (oturumun kategorisi varsa) ve "Ana Sayfa".

---

## 12. Ekran Detayları

### 12.1 Home
- Logo + "Her soruya **5 saniye**. Hızlı düşün, hızlı dokun."
- Başlık "Kategorini seç", `LazyVerticalGrid` **2 sütun** kategori kartları:
  ikon, ad, açıklama, rozet (`20 soru` ya da oynanamıyorsa `Hazırlanıyor` + kart pasif),
  kategori rengiyle gradyan zemin.
- Altta "🏆 Skor Tablosu" ikincil butonu.
- Durumlar: yükleniyor / hata ("Kategoriler yüklenemedi" + Tekrar dene) / liste.
- Oturum düştüyse gelen uyarı: "Oturumun süresi doldu. Yeni bir tur başlatabilirsin."

### 12.2 Countdown
- Kategori rengiyle tam ekran; kategori adı üstte.
- Büyük `3 → 2 → 1 → BAŞLA!` (700 ms adımlar, "BAŞLA!" 400 ms).
- `startSession` **"BAŞLA!" anında** çağrılır (daha erken çağrılırsa geri sayım oyuncunun
  süresinden yer). "BAŞLA!" en az 400 ms görünür, istek paralel yürür.
- Alt ipucu: "Her soru için 5 saniyen var. Hazır ol!"
- Hata: `INSUFFICIENT_QUESTIONS` → "Bu kategori şu an hazırlanıyor." (tekrar dene yok) +
  "Ana sayfa"; diğerleri → mesaj + Tekrar dene.

### 12.3 Quiz
- **Üst bar:** X (çıkış), `Soru 7 / 20` + ilerleme, anlık puan (`tr-TR` binlik ayırıcı).
- **Geri sayım halkası:** `Canvas` ile dairesel progress, ortada kalan saniye (`ceil`).
  Renk: > 3 sn yeşil, 3–1.5 sn amber, < 1.5 sn kırmızı; son 2 sn'de hafif nabız.
  `withFrameMillis` döngüsüyle `deadlineAtLocalMs − now` hesaplanır.
- **Soru metni** + **4 şık** (A/B/C/D harf rozeti, min. 56 dp yükseklik).
- **Şık durumları:** idle → seçilince seçilen `selected`, diğerleri pasif →
  geri bildirimde doğru şık yeşil, yanlış seçilen kırmızı, diğerleri pasif.
- **Geri bildirim:** "Doğru!" / "Yanlış" / "Süre doldu!" rozeti, puan > 0 ise `+135`
  yukarı kayan rozet; doğruda küçük konfeti (14 parça). Süre dolduysa ekran uyarı tonu.
- **Bağlantı koptu:** "Bağlantı koptu — Devam etmek için dokun; kaldığın yerden sürdüreceğiz." → resync.
- Durum `FINISHED` olunca Result'a geçilir.

### 12.4 Result
- Kategori adı, motive edici başlık:
  doğruluk ≥ 90 ve ort. süre < 1.5 sn → "Şimşek gibisin!"; ≥ 80 "Harika iş!";
  ≥ 60 "Çok iyi gidiyorsun!"; ≥ 40 "Fena değil!"; aksi "Isınma turu bitti, şimdi asıl tur!"
- Büyük puan (0'dan sayan animasyon) + `/ 4.000 puan` (`max_possible_score`).
- Üç kutu: Doğru / Yanlış / Kaçırılan. Doğruluk `%85`, Ortalama süre `1,8 sn` (yoksa `—`).
- Kaydedilmediyse "Skorunu kaydedersen **N. sıraya** yerleşeceksin!" (`estimated_rank`),
  kaydedildiyse "Skorun kaydedildi."
- Butonlar: **Skoru Kaydet** (kaydedilmediyse), **Tekrar Oyna**, **Skor Tablosu**.

### 12.5 Nickname
- "**1.840** puanın skor tablosuna yazılacak."
- `OutlinedTextField` "Takma adın ne?", `maxLength = 20`, ipucu:
  "2–20 karakter. Harf, rakam, boşluk, _ ve - kullanabilirsin."
- İstemci doğrulaması (backend ile aynı), sunucu `details.nickname[0]` hatası alan altında.
- **Kaydet** (gönderim sırasında progress, çift gönderim yok) → Leaderboard (kategori sekmesi).
- **Kaydetmeden geç** → Leaderboard.

### 12.6 Leaderboard
- Başlık "Skor Tablosu"; `ScrollableTabRow`: **Genel** + her kategori (API sırası).
- `LazyColumn` satırları: sıra (ilk 3 altın/gümüş/bronz madalya), takma ad
  (kendi satırında "Sen" rozeti), Genel sekmesinde kategori rozeti, tarih (`26 Eyl`), puan.
- Kullanıcı az önce kaydettiyse satırı vurgulu; Top 10'da değilse listenin altında `•••` ve
  ayrı satırda kendi sırası (Genel'de `rank_overall`, kendi kategorisinde `rank_in_category`).
- Sekme değişince yeniden çekilir; eski isteğin yanıtı yeni sekmenin üstüne yazmaz.
- Durumlar: yükleniyor / hata + Tekrar dene / "Henüz skor yok".
- Butonlar: **Tekrar Oyna**, **Ana Sayfa**.

---

## 13. Tasarım

Web `tokens.css` renkleri `ui/theme/Color.kt`'e aktarılır. **Yalnızca açık tema**
(web'de koyu tema yok); dinamik renk kapalı.

| Token | Değer | | Token | Değer |
|---|---|---|---|---|
| primary | `#6C4DFF` | | bg | `#F7F5FF` |
| primary-strong | `#5438E0` | | surface | `#FFFFFF` |
| primary-soft | `#EFEBFF` | | surface-alt | `#F1EEFC` |
| accent | `#FF5C8A` | | border | `#E4DFF7` |
| accent-soft | `#FFE9F0` | | text | `#1B1233` |
| success | `#0FA968` (strong `#0A7D4D`, soft `#E3F8EE`) | | text-muted | `#5A5273` |
| danger | `#E11D48` (soft `#FFE7EC`) | | warning | `#F59E0B` (soft `#FFF4E0`) |

Hero gradyanı: `#6C4DFF → #FF5C8A` (135°).

**Kategori renkleri** (canlı / koyu — koyu ton üstünde beyaz metin):

| slug | ikon | canlı | koyu |
|---|---|---|---|
| `yazilim` | code | `#6C4DFF` | `#5438E0` |
| `yapay-zeka` | brain | `#00C2A8` | `#00806F` |
| `bilgisayar-muhendisligi` | chip | `#2B8CFF` | `#1A6FD4` |
| `ulkeler` | globe | `#FF8A3D` | `#C25510` |
| `fizik` | atom | `#FF5C8A` | `#D62E5E` |
| bilinmeyen | sparkles | `color_hex` | `color_hex` %72 + siyah |

İkonlar Lucide eşdeğeri vektör çizimler olarak `res/drawable`'a eklenir (ikon kütüphanesi bağımlılığı yok).

- Köşe yarıçapı 10 / 16 / 24 / 28 dp, boşluk 4 dp tabanlı (4, 8, 12, 16, 24, 32, 48, 64).
- Font: başlık ve sayılar Space Grotesk, gövde Inter; sayılarda `tnum` (sabit genişlik).
- Metinler `strings.xml` (Türkçe). Sayılar `tr-TR` biçiminde.
- Edge-to-edge (`enableEdgeToEdge()` + `Scaffold` padding'leri). Dokunma alanı ≥ 48 dp.
- Sistemde animasyonlar kapalıysa (`animator duration scale = 0`) animasyonlar atlanır.

---

## 14. Hata Yönetimi

- Tüm ağ çağrıları `ApiException`'a dönüştürülür (§7.3); kullanıcıya sunucunun Türkçe
  `message` alanı, ağ hatasında "Sunucuya ulaşılamadı. Bağlantını kontrol et." gösterilir.
- Her yükleme ekranında "Tekrar dene".
- Oturumu geçersiz kılan kodlarda (`INVALID_SESSION_TOKEN`, `SESSION_EXPIRED`) oturum
  sıfırlanır ve Home'a uyarıyla dönülür.

---

## 15. Güvenlik Notları

- Uygulamada **hiçbir sır yoktur**: Supabase URL/anahtarı veya Django `SECRET_KEY` istemciye girmez.
- Oturum token'ı yalnızca bellekte; loglara yazılmaz (Ktor logging yalnız debug'da ve
  `X-Session-Token` başlığı gizlenir).
- Release'te yalnızca HTTPS; cleartext izni sadece debug kaynak setinde.
- Puan ve süre sunucuda hesaplandığı için istemci manipülasyonu skoru değiştiremez.

---

## 16. Geliştirme Adımları (Sıralı)

1. Gradle: bağımlılıklar, serialization eklentisi, `API_BASE_URL`, INTERNET izni,
   debug network security config, `RapidQuizApp`.
2. Tema: renkler, fontlar, kategori renkleri/ikonları.
3. Ağ katmanı: `HttpClientFactory`, `ApiException`, modeller, `RapidQuizApi`.
4. Home + `CatalogRepository` → kategoriler listeleniyor mu (yerel backend ile).
5. Navigasyon iskeleti.
6. `QuizSessionManager` + birim testleri (skew, feedback süresi, çift cevap, resync).
7. Countdown + Quiz ekranı (halka, şıklar, geri bildirim, kopma).
8. Result + Nickname (skor gönderimi → web skor tablosunda da görünüyor mu).
9. Leaderboard (sekmeler, kendi satırı).
10. Boş/hata durumları, erişilebilirlik, animasyon cilası.
11. Test ve release build.

---

## 17. Test Kontrol Listesi

- [ ] Açılışta kategoriler geliyor; oynanamayan kategori pasif ve "Hazırlanıyor".
- [ ] Geri sayımdan sonra ilk soru tam 5 sn ile başlıyor.
- [ ] Bir soruya iki kez cevap verilemiyor.
- [ ] Süre dolunca `timeout` gönderiliyor ve sonraki soruya geçiliyor.
- [ ] Puanlar web ile aynı (sunucu `points_earned`).
- [ ] Ekran döndürünce quiz durumu ve sayaç korunuyor.
- [ ] Uygulama arka plana alınıp dönünce oturum eşitleniyor.
- [ ] Wi-Fi kapatılınca "Bağlantı koptu", açınca "Devam et" ile kaldığı yerden sürüyor.
- [ ] Quiz'den geri tuşu onay soruyor; onaylanınca Home.
- [ ] Geçersiz takma adda buton hata gösteriyor; sunucu hatası alan altında.
- [ ] Kaydedilen skor web skor tablosunda da görünüyor; Android'de kendi satırı vurgulu.
- [ ] Top 10 dışındaysa kendi sırası listenin altında.
- [ ] Skor tablosu Genel + kategori sekmeleri web ile aynı sırayı gösteriyor.
- [ ] 30 dk sonra oturum düşünce Home'a uyarıyla dönülüyor.

---

## 18. Açık Konular

| # | Konu | Durum |
|---|---|---|
| A1 | Backend canlı adresi | Şimdilik yerel (`10.0.2.2:8000`). Canlıya alınınca release `API_BASE_URL` güncellenir. |
| A2 | Backend `ALLOWED_HOSTS` | Yerel testte `10.0.2.2` eklenmeli (§6). |
| A3 | Mobil grace period | Backend §11: düşük bant genişliğinde 800 ms yetmeyebilir; gerçek cihaz testinden sonra değerlendirilir. |
| A4 | "Uygulamadan çıkınca süre işlemeye devam eder" bilgisi | **Eklenmeyecek** (web ile aynı kalınır). |

---

## 19. Release

- `versionCode` / `versionName` güncellenir (`X-Client-Version` bu değeri gönderir).
- Minify açılırsa kotlinx.serialization ve Ktor için keep kuralları eklenir.
- **Build → Generate Signed App Bundle** ile `.aab`; keystore repoya girmez.
