# Rapid Quiz – Android (Kotlin + Jetpack Compose) Proje Dokümanı

> Bu doküman, web'de tamamlanan **Rapid Quiz** uygulamasının Android'e birebir, sade bir şekilde aktarılması için hazırlanmıştır. Android Studio'da proje köküne (`/docs/PROJECT.md` veya kök dizine) koyup hem referans hem de AI asistanına (Gemini in Android Studio vb.) bağlam olarak verilebilir.

---

## 1. Kapsam

### Yapılacaklar
1. **Kategoriler ekranı** – Uygulama açılınca Supabase'den kategoriler listelenir.
2. **Quiz ekranı** – Kategoriye tıklanınca o kategorinin soruları gösterilir, kullanıcı cevaplar.
3. **Sonuç / Submit** – Quiz bitince kullanıcı adını girer ve skorunu scoreboard'a gönderir.
4. **Scoreboard ekranı** – Webdeki gibi: **Genel** scoreboard + **kategori bazlı** scoreboard'lar.

### Yapılmayacaklar (bilinçli olarak kapsam dışı)
- Giriş / kayıt (auth), profil, ayarlar, bildirim, offline mod, admin paneli.
- Web'de olmayan hiçbir yeni özellik.

**Kural:** Oyun kuralları (soru sayısı, süre, puanlama) web ile **birebir aynı** olmalı. Aşağıdaki varsayılan değerler web'deki değerlerle kontrol edilip `QuizConfig` içinde güncellenmelidir.

---

## 2. Teknoloji Yığını

| Katman | Seçim |
|---|---|
| Dil | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Navigasyon | Navigation Compose (type-safe route, `@Serializable`) |
| Mimari | MVVM + tek yönlü veri akışı (UiState + StateFlow) |
| Backend | Supabase (web ile aynı proje) |
| Supabase istemcisi | `supabase-kt` (Postgrest modülü) + Ktor client |
| Serileştirme | kotlinx.serialization |
| Asenkron | Coroutines + Flow |
| DI | Manuel (basit `AppContainer`) – Hilt'e gerek yok |
| Min SDK / Target SDK | 26 / en güncel stabil |

> Sürüm numaraları için Android Studio'nun önerdiği en güncel stabil sürümler kullanılmalı (Compose BOM, supabase-kt BOM, Ktor). Aşağıdaki sürümler örnektir.

---

## 3. Gradle Yapılandırması

### `gradle/libs.versions.toml`
```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
composeBom = "2025.01.00"
activityCompose = "1.9.3"
lifecycle = "2.8.7"
navigation = "2.8.5"
supabase = "3.1.1"
ktor = "3.0.3"
serialization = "1.7.3"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-ui = { module = "androidx.compose.ui:ui" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-material3 = { module = "androidx.compose.material3:material3" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### `app/build.gradle.kts`
```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.rapidquiz.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rapidquiz.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"${localProps["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps["SUPABASE_ANON_KEY"]}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
```

### `local.properties` (git'e EKLENMEZ)
```properties
SUPABASE_URL=https://<proje-ref>.supabase.co
SUPABASE_ANON_KEY=<web'de kullanılan anon / publishable key>
```

### `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 4. Supabase Veri Modeli

> ⚠️ **Tablo ve kolon adları varsayımdır.** Web projesindeki gerçek şemayla (Supabase Dashboard → Table Editor) karşılaştırıp `@SerialName` değerlerini ve tablo adlarını güncelleyin.

| Tablo | Kolonlar (varsayılan) |
|---|---|
| `categories` | `id` (int/uuid), `name` (text), `created_at` |
| `questions` | `id`, `category_id` (FK → categories.id), `question` (text), `options` (jsonb / text[]), `correct_index` (int) |
| `scores` | `id`, `player_name` (text), `category_id` (FK), `score` (int), `created_at` |

### RLS (Row Level Security) beklentisi
Web ile aynı anon key kullanılacağı için mevcut politikalar yeterli olmalı:
- `categories`, `questions`, `scores` → **SELECT** herkese açık.
- `scores` → **INSERT** herkese açık; UPDATE/DELETE kapalı.

---

## 5. Paket Yapısı

```
com.rapidquiz.app
├── MainActivity.kt
├── RapidQuizApp.kt                 // Application + AppContainer
├── core/
│   ├── QuizConfig.kt               // Soru sayısı, süre, puanlama (web ile aynı)
│   └── UiState.kt
├── data/
│   ├── SupabaseProvider.kt
│   ├── model/
│   │   ├── Category.kt
│   │   ├── Question.kt
│   │   └── Score.kt
│   └── repository/
│       ├── CategoryRepository.kt
│       ├── QuestionRepository.kt
│       └── ScoreRepository.kt
└── ui/
    ├── navigation/
    │   ├── Routes.kt
    │   └── AppNavHost.kt
    ├── theme/                      // Web'in renkleri ile Material3 tema
    ├── categories/
    │   ├── CategoriesScreen.kt
    │   └── CategoriesViewModel.kt
    ├── quiz/
    │   ├── QuizScreen.kt
    │   ├── QuizViewModel.kt
    │   └── ResultSection.kt        // Skor + isim + submit
    ├── scoreboard/
    │   ├── ScoreboardScreen.kt
    │   └── ScoreboardViewModel.kt
    └── components/                 // Loading, ErrorView, ortak kartlar
```

---

## 6. Temel Kod İskeleti

### 6.1 Supabase istemcisi
```kotlin
object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }
}
```

### 6.2 Modeller
```kotlin
@Serializable
data class Category(
    val id: Long,
    val name: String
)

@Serializable
data class Question(
    val id: Long,
    @SerialName("category_id") val categoryId: Long,
    val question: String,
    val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int
)

@Serializable
data class Score(
    val id: Long? = null,
    @SerialName("player_name") val playerName: String,
    @SerialName("category_id") val categoryId: Long,
    val score: Int,
    @SerialName("created_at") val createdAt: String? = null
)
```

### 6.3 Repository'ler
```kotlin
class CategoryRepository(private val client: SupabaseClient) {
    suspend fun getCategories(): List<Category> =
        client.from("categories").select {
            order("name", Order.ASCENDING)
        }.decodeList()
}

class QuestionRepository(private val client: SupabaseClient) {
    suspend fun getQuestions(categoryId: Long): List<Question> =
        client.from("questions").select {
            filter { eq("category_id", categoryId) }
        }.decodeList<Question>()
            .shuffled()
            .take(QuizConfig.QUESTION_COUNT)
}

class ScoreRepository(private val client: SupabaseClient) {
    suspend fun submit(score: Score) {
        client.from("scores").insert(score)
    }

    suspend fun getOverall(limit: Int = QuizConfig.LEADERBOARD_LIMIT): List<Score> =
        client.from("scores").select {
            order("score", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()

    suspend fun getByCategory(categoryId: Long, limit: Int = QuizConfig.LEADERBOARD_LIMIT): List<Score> =
        client.from("scores").select {
            filter { eq("category_id", categoryId) }
            order("score", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()
}
```

> Web'deki "Genel" scoreboard farklı hesaplanıyorsa (ör. kullanıcı başına toplam puan, bir view veya RPC), `getOverall` aynı view/RPC'yi çağıracak şekilde değiştirilmeli: `client.from("<view_adi>")` veya `client.postgrest.rpc("<fonksiyon>")`.

### 6.4 Konfigürasyon
```kotlin
object QuizConfig {
    const val QUESTION_COUNT = 10          // web ile eşitle
    const val SECONDS_PER_QUESTION = 15    // web'de süre yoksa timer'ı kaldır
    const val POINTS_PER_CORRECT = 10      // web'deki puanlama formülü
    const val LEADERBOARD_LIMIT = 20
    const val MAX_NAME_LENGTH = 20
}
```

### 6.5 UiState
```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

### 6.6 AppContainer (manuel DI)
```kotlin
class AppContainer {
    private val client = SupabaseProvider.client
    val categoryRepository = CategoryRepository(client)
    val questionRepository = QuestionRepository(client)
    val scoreRepository = ScoreRepository(client)
}

class RapidQuizApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
```
`AndroidManifest.xml` → `<application android:name=".RapidQuizApp" ...>`

ViewModel'ler `viewModelFactory { initializer { ... } }` ile oluşturulur; container'a `(this[APPLICATION_KEY] as RapidQuizApp).container` ile erişilir.

---

## 7. Navigasyon

```kotlin
@Serializable data object CategoriesRoute
@Serializable data class QuizRoute(val categoryId: Long, val categoryName: String)
@Serializable data class ScoreboardRoute(val initialCategoryId: Long? = null)
```

```kotlin
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = CategoriesRoute) {
        composable<CategoriesRoute> {
            CategoriesScreen(
                onCategoryClick = { c -> navController.navigate(QuizRoute(c.id, c.name)) },
                onScoreboardClick = { navController.navigate(ScoreboardRoute()) }
            )
        }
        composable<QuizRoute> { entry ->
            val route = entry.toRoute<QuizRoute>()
            QuizScreen(
                categoryId = route.categoryId,
                categoryName = route.categoryName,
                onSubmitted = {
                    navController.navigate(ScoreboardRoute(route.categoryId)) {
                        popUpTo(CategoriesRoute)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ScoreboardRoute> { entry ->
            ScoreboardScreen(
                initialCategoryId = entry.toRoute<ScoreboardRoute>().initialCategoryId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Akış:**
```
Categories ──(kategori)──▶ Quiz ──(submit)──▶ Scoreboard (ilgili kategori sekmesi açık)
    │                                              ▲
    └──────────────(Scoreboard butonu)─────────────┘
```
Scoreboard'dan geri basınca Categories ekranına dönülür (popUpTo sayesinde bitmiş quiz'e dönülmez).

---

## 8. Ekran Detayları

### 8.1 CategoriesScreen
- **TopAppBar:** "Rapid Quiz" başlığı + sağda 🏆 Scoreboard ikonu.
- **İçerik:** Kategori kartları (`LazyVerticalGrid`, 2 sütun) veya liste.
- **Durumlar:** Loading → `CircularProgressIndicator`; Error → mesaj + "Tekrar dene"; Boş → "Henüz kategori yok".

**ViewModel:**
```kotlin
class CategoriesViewModel(private val repo: CategoryRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = runCatching { repo.getCategories() }
            .fold({ UiState.Success(it) }, { UiState.Error(it.message ?: "Bir hata oluştu") })
    }
}
```

### 8.2 QuizScreen
- **Üst kısım:** Kategori adı, "Soru 3 / 10", `LinearProgressIndicator` (ilerleme ve/veya kalan süre), anlık skor.
- **Soru kartı:** Soru metni.
- **Şıklar:** Tam genişlikte butonlar. Seçildikten sonra doğru şık yeşil, yanlış seçilen kırmızı gösterilir (web'deki davranış neyse o), kısa bir gecikmeyle (~800 ms) sonraki soruya geçilir.
- **Süre:** Web'de soru başına süre varsa, süre dolunca soru yanlış sayılıp geçilir.
- **Bitince:** Aynı ekranda `ResultSection` gösterilir.

**QuizUiState:**
```kotlin
data class QuizUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedIndex: Int? = null,     // cevap gösterim aşaması
    val score: Int = 0,
    val correctCount: Int = 0,
    val remainingSeconds: Int = QuizConfig.SECONDS_PER_QUESTION,
    val isFinished: Boolean = false,
    val playerName: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null
)
```

**QuizViewModel sorumlulukları:**
- `init`: `categoryId`'ye göre soruları yükle (`SavedStateHandle.toRoute<QuizRoute>()` ile al).
- `onAnswer(index)`: Cevap zaten seçildiyse yok say (çift tıklama koruması); doğruysa puan ekle; timer'ı durdur; gecikme sonrası `next()`.
- `startTimer()`: `viewModelScope` içinde `Job`; her saniye `remainingSeconds--`; 0 olunca `onAnswer(-1)` gibi davran.
- `next()`: Son soruysa `isFinished = true`.
- `onNameChange(name)`: `MAX_NAME_LENGTH` ile sınırla.
- `submit(onSuccess)`: İsim boşsa buton pasif; `ScoreRepository.submit(...)`; başarılıysa `onSuccess()`; hata varsa `submitError` göster, tekrar denemeye izin ver; `isSubmitting` sırasında buton pasif (çift gönderim olmasın).

> Timer ve quiz durumu ViewModel'de tutulduğu için ekran döndürmede kaybolmaz.

### 8.3 ResultSection (Quiz ekranı içinde)
- Büyük skor gösterimi: "80 puan · 8/10 doğru".
- `OutlinedTextField` → "Adın".
- **"Scoreboard'a Gönder"** butonu (gönderim sırasında progress).
- İkincil buton: **"Kategorilere Dön"** (göndermeden çıkmak için).

### 8.4 ScoreboardScreen
- **TopAppBar:** "Scoreboard" + geri.
- **Sekmeler:** `ScrollableTabRow` → ilk sekme **"Genel"**, ardından her kategori için bir sekme.
- `initialCategoryId` geldiyse ilgili kategori sekmesi seçili açılır, yoksa "Genel".
- **Liste:** `LazyColumn` satırları → sıra (🥇🥈🥉 ilk 3), oyuncu adı, (Genel sekmesinde) kategori adı, skor.
- Pull-to-refresh (`PullToRefreshBox`) ile yenileme.
- Durumlar: Loading / Error / "Henüz skor yok".

**ScoreboardViewModel:**
```kotlin
data class ScoreboardUiState(
    val categories: List<Category> = emptyList(),
    val selectedTab: Long? = null,           // null = Genel
    val scores: UiState<List<Score>> = UiState.Loading,
    val isRefreshing: Boolean = false
)
```
- `init`: Kategorileri yükle, `selectedTab = initialCategoryId`, skorları yükle.
- `selectTab(categoryId: Long?)`: Sekmeyi değiştir, skorları yeniden çek (istenirse sekme bazlı `Map<Long?, List<Score>>` cache).
- Genel sekmesinde kategori adını göstermek için `categories` listesinden `id → name` eşlemesi yapılır.

---

## 9. Tasarım

- Material 3, web uygulamasının ana renkleri `ui/theme/Color.kt` içine aktarılır (primary, secondary, background).
- Açık / koyu tema desteği (`isSystemInDarkTheme()`); dinamik renk kapalı ki marka renkleri korunsun.
- Metinler `strings.xml` içinde (Türkçe varsayılan).
- Minimum dokunma alanı 48dp; şıklar büyük ve rahat tıklanır.
- Edge-to-edge: `enableEdgeToEdge()` + `Scaffold` padding'leri.

---

## 10. Hata Yönetimi

- Tüm ağ çağrıları `runCatching` içinde; hatalar kullanıcıya anlaşılır Türkçe mesajla gösterilir ("İnternet bağlantınızı kontrol edin").
- Her ekranda "Tekrar dene" aksiyonu.
- Kategoride soru yoksa Quiz ekranı "Bu kategoride soru bulunamadı" gösterir ve geri döner.

---

## 11. Güvenlik Notları

- Anon/publishable key istemcide bulunur – bu normaldir; güvenlik **RLS** ile sağlanır. `service_role` key **asla** uygulamaya konmaz.
- Key'ler `local.properties` üzerinden gelir, repoya commit edilmez.
- Web'de olduğu gibi skorlar istemcide hesaplanıp gönderiliyor; manipülasyon riski web ile aynıdır. İleride gerekirse skor doğrulaması bir Supabase Edge Function / RPC'ye taşınabilir (şimdilik kapsam dışı).
- İsim alanı uzunluk sınırı ve `trim()` ile temizlenir.

---

## 12. Geliştirme Adımları (Sıralı)

1. Android Studio → **New Project → Empty Activity (Compose)**, paket: `com.rapidquiz.app`.
2. `libs.versions.toml` ve `app/build.gradle.kts` bölüm 3'e göre güncellenir, `local.properties` doldurulur, INTERNET izni eklenir.
3. Supabase şeması web'den kontrol edilip `data/model` sınıfları yazılır.
4. `SupabaseProvider`, repository'ler ve `AppContainer` yazılır.
5. `CategoriesScreen` + ViewModel → kategoriler listeleniyor mu test edilir.
6. Navigasyon kurulur (`AppNavHost`).
7. `QuizScreen` + ViewModel + timer + `ResultSection`.
8. Skor gönderimi → Supabase'de satırın oluştuğu kontrol edilir.
9. `ScoreboardScreen` (Genel + kategori sekmeleri).
10. Tema/renkler web'e uyarlanır, loading/error/boş durumlar tamamlanır.
11. Test ve release build.

---

## 13. Test Kontrol Listesi

- [ ] Uygulama açılınca kategoriler geliyor.
- [ ] Kategoriye tıklayınca doğru kategorinin soruları geliyor.
- [ ] Soru sayısı, süre ve puanlama web ile aynı.
- [ ] Bir soruya iki kez cevap verilemiyor.
- [ ] Süre dolunca otomatik sonraki soruya geçiliyor.
- [ ] Ekran döndürünce quiz durumu ve süre korunuyor.
- [ ] İsim boşken submit butonu pasif.
- [ ] Submit sonrası skor web scoreboard'unda da görünüyor.
- [ ] Submit sonrası Scoreboard ilgili kategori sekmesiyle açılıyor; geri tuşu Kategoriler'e dönüyor.
- [ ] Genel scoreboard web'dekiyle aynı sıralamayı gösteriyor.
- [ ] İnternet yokken anlaşılır hata ve "Tekrar dene" çıkıyor.
- [ ] Açık/koyu temada okunabilirlik tamam.

---

## 14. Release

- `versionCode` / `versionName` güncellenir.
- `isMinifyEnabled = true` açılırsa kotlinx.serialization ve Ktor için ProGuard kuralları eklenir (`@Serializable` sınıfların korunması).
- **Build → Generate Signed App Bundle** ile `.aab` oluşturulur; keystore güvenli yerde saklanır.
