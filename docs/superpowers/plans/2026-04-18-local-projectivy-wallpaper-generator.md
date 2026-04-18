# Local Projectivy Wallpaper Generator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an installable Projectivy wallpaper provider APK that authenticates with Trakt, fetches anticipated movies and shows, fetches TMDB artwork with built-in developer credentials, generates wallpapers locally on Android, and returns local image URIs to Projectivy without needing a TVBG Suite web server.

**Architecture:** Keep the existing Projectivy AIDL/API module and `WallpaperProviderService`, but replace the web-server `ApiService` path with a local generation pipeline. The service will refresh Trakt/TMDB metadata, render a 3840x2160 JPEG into app-private cache using Android `Canvas`, expose it via `FileProvider`, and return it as a `Wallpaper(type=IMAGE, displayMode=CROP)`. Settings remain TV-friendly Leanback guided actions, adding Trakt device OAuth status/actions and removing server/layout/filter concepts that only applied to the web backend.

**Tech Stack:** Kotlin, Android SDK Canvas/Bitmap/Paint, Retrofit/OkHttp/Gson, Leanback GuidedStep UI, Projectivy wallpaper provider AIDL, JUnit/Robolectric.

---

## Scope Check

This is a self-contained first version of a new local-generation mode for the existing Projectivy plugin. It deliberately does not port the whole TVBG Suite layout editor to Android. It generates one opinionated wallpaper style:

- 3840x2160 output.
- TMDB backdrop image cover-cropped and biased to the upper-right focal area.
- Left-side and bottom gradients like the provided screenshot.
- TMDB title logo fit into a top-left logo box.
- Trakt anticipated movies and anticipated shows as the only initial catalogs.
- Local cached JPEGs returned to Projectivy.

Do not include the previous web-server URL, genre/year/rating filters, Jellyfin client launch support, or TVBG Suite layout selection in this first pass.

## Existing Codebase Map

- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/api/src/main/aidl/tv/projectivy/plugin/wallpaperprovider/api/IWallpaperProviderService.aidl`: Projectivy service contract.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/Wallpaper.kt`: Returned wallpaper parcelable.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/WallpaperProviderService.kt`: Existing service calls the TVBG Suite web API. This becomes the local generation entry point.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/SettingsFragment.kt`: Existing Leanback settings UI. This becomes Trakt auth and generation settings UI.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/PreferencesManager.kt`: Existing shared preferences wrapper. Add Trakt tokens, selected catalogs, and last local wallpaper URI.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/src/main/AndroidManifest.xml`: Add FileProvider and remove obsolete external client package queries later if desired.
- `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/build.gradle.kts`: Add BuildConfig provider keys, OkHttp logging, coroutines, tests, and FileProvider dependency support.
- `/Users/jneerdael/Scripts/tvbgsuite/trakt.apib`: Trakt device auth and anticipated endpoint reference.
- `/Users/jneerdael/Scripts/tvbgsuite/tmdb.json`: TMDB API reference.
- `/Users/jneerdael/Scripts/nexio/app/src/main/java/com/nexio/tv/data/repository/TraktAuthService.kt`: Working Trakt device OAuth behavior. Device code uses `client_id`; token polling uses `code`, `client_id`, and `client_secret`; refresh uses `redirect_uri=urn:ietf:wg:oauth:2.0:oob`.
- `/Users/jneerdael/Scripts/nexio/app/build.gradle.kts`: BuildConfig pattern for `TMDB_API_KEY`, `TMDB_API_URL`, `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, and `TRAKT_REDIRECT_URI`.

## File Structure

Create:

- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperModels.kt`: Pure data models for Trakt, TMDB, and rendered wallpaper candidates.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktLocalApi.kt`: Retrofit API for Trakt OAuth and anticipated catalogs.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbLocalApi.kt`: Retrofit API for TMDB details/images.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepository.kt`: Device OAuth start/poll/refresh token flow backed by `PreferencesManager`.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepository.kt`: Fetches anticipated movies/shows and converts them into TMDB lookup candidates.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepository.kt`: Fetches TMDB details and selects backdrop/logo URLs.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpec.kt`: Stable numeric layout constants for the screenshot style.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderer.kt`: Android `Canvas` renderer that writes 3840x2160 JPEGs.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCache.kt`: File naming, cache writes, and content URI conversion.
- `sample/src/main/res/xml/file_paths.xml`: FileProvider paths for generated wallpapers.
- `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepositoryTest.kt`: Mapping tests for anticipated movie/show payloads.
- `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpecTest.kt`: Layout math tests.
- `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepositoryTest.kt`: Device token status mapping tests.

Modify:

- `sample/build.gradle.kts`: Add BuildConfig fields and dependencies.
- `sample/src/main/AndroidManifest.xml`: Add FileProvider, keep Projectivy service.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/PreferencesManager.kt`: Store Trakt auth state, selected catalogs, and cached wallpaper metadata.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/SettingsFragment.kt`: Replace server/layout/filter settings with Trakt auth and local generation controls.
- `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/WallpaperProviderService.kt`: Use local generator instead of TVBG Suite web API.
- `sample/src/main/res/values/strings.xml`: Update product copy and add strings.
- `README.md`: Document standalone local generation and required build properties.

## Task 1: Build Config And Dependencies

**Files:**
- Modify: `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/build.gradle.kts`
- Modify: `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/README.md`
- Test: `/Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin/sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/BuildConfigContractTest.kt`

- [ ] **Step 1: Add BuildConfig contract test**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/BuildConfigContractTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.butch708.projectivy.tvbgsuite.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildConfigContractTest {
    @Test
    fun providerBuildConfigFieldsExist() {
        assertNotNull(BuildConfig.TMDB_API_KEY)
        assertEquals("https://api.themoviedb.org/3/", BuildConfig.TMDB_API_URL.ifBlank { "https://api.themoviedb.org/3/" })
        assertNotNull(BuildConfig.TRAKT_CLIENT_ID)
        assertNotNull(BuildConfig.TRAKT_CLIENT_SECRET)
        assertEquals("https://api.trakt.tv/", BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
        assertEquals("urn:ietf:wg:oauth:2.0:oob", BuildConfig.TRAKT_REDIRECT_URI.ifBlank { "urn:ietf:wg:oauth:2.0:oob" })
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
cd /Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.BuildConfigContractTest'
```

Expected: FAIL because `TMDB_API_KEY`, `TMDB_API_URL`, `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, `TRAKT_API_URL`, and `TRAKT_REDIRECT_URI` do not exist.

- [ ] **Step 3: Add local properties loading and BuildConfig fields**

Modify `sample/build.gradle.kts` near the top:

```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun prop(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name, defaultValue).replace("\\", "\\\\").replace("\"", "\\\"")
```

Inside `android.defaultConfig`, add:

```kotlin
buildConfigField("String", "TMDB_API_KEY", "\"${prop("TMDB_API_KEY")}\"")
buildConfigField("String", "TMDB_API_URL", "\"${prop("TMDB_API_URL", "https://api.themoviedb.org/3/")}\"")
buildConfigField("String", "TRAKT_CLIENT_ID", "\"${prop("TRAKT_CLIENT_ID")}\"")
buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${prop("TRAKT_CLIENT_SECRET")}\"")
buildConfigField("String", "TRAKT_API_URL", "\"${prop("TRAKT_API_URL", "https://api.trakt.tv/")}\"")
buildConfigField("String", "TRAKT_REDIRECT_URI", "\"${prop("TRAKT_REDIRECT_URI", "urn:ietf:wg:oauth:2.0:oob")}\"")
```

Add dependencies:

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
implementation("com.squareup.okhttp3:okhttp:5.3.2")
implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
implementation("androidx.core:core-ktx:1.17.0")

testImplementation("junit:junit:4.13.2")
testImplementation("org.robolectric:robolectric:4.16")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
```

- [ ] **Step 4: Document local.properties**

Append to `README.md`:

```markdown
## Local Generator Build Properties

Create `local.properties` in the project root:

```properties
TMDB_API_KEY=your_tmdb_v3_api_key
TMDB_API_URL=https://api.themoviedb.org/3/
TRAKT_CLIENT_ID=your_trakt_client_id
TRAKT_CLIENT_SECRET=your_trakt_client_secret
TRAKT_API_URL=https://api.trakt.tv/
TRAKT_REDIRECT_URI=urn:ietf:wg:oauth:2.0:oob
```

The local generator APK does not require a TVBG Suite server. It uses Trakt for anticipated catalogs and TMDB for artwork.
```

- [ ] **Step 5: Run test and commit**

Run:

```bash
cd /Users/jneerdael/Scripts/projectivy-tvbgsuite-plugin
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.BuildConfigContractTest'
```

Expected: PASS.

Commit:

```bash
git add sample/build.gradle.kts README.md sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/BuildConfigContractTest.kt
git commit -m "build: add local provider credentials"
```

## Task 2: Local Data Models And Catalog Mapping

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperModels.kt`
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepository.kt`
- Test: `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepositoryTest.kt`

- [ ] **Step 1: Add mapping tests**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepositoryTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class AnticipatedCatalogRepositoryTest {
    @Test
    fun mapsMovieAndShowItemsToCandidates() {
        val movie = TraktAnticipatedItem(
            listCount = 5362,
            movie = TraktMediaSummary(
                title = "The Martian",
                year = 2015,
                ids = TraktIds(trakt = 183371, slug = "the-martian-2015", imdb = "tt3659388", tmdb = 286217, tvdb = null)
            ),
            show = null
        )
        val show = TraktAnticipatedItem(
            listCount = 5383,
            movie = null,
            show = TraktMediaSummary(
                title = "Supergirl",
                year = 2015,
                ids = TraktIds(trakt = 99046, slug = "supergirl", imdb = "tt4016454", tmdb = 62688, tvdb = 295759)
            )
        )

        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.ANTICIPATED_MOVIE,
                title = "The Martian",
                year = 2015,
                tmdbId = 286217,
                imdbId = "tt3659388",
                traktId = 183371,
                tvdbId = null
            ),
            movie.toWallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE)
        )
        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.ANTICIPATED_SHOW,
                title = "Supergirl",
                year = 2015,
                tmdbId = 62688,
                imdbId = "tt4016454",
                traktId = 99046,
                tvdbId = 295759
            ),
            show.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW)
        )
    }

    @Test
    fun ignoresItemsWithoutTmdbId() {
        val show = TraktAnticipatedItem(
            listCount = 1481,
            movie = null,
            show = TraktMediaSummary(
                title = "The Expanse",
                year = 2015,
                ids = TraktIds(trakt = 77199, slug = "the-expanse", imdb = "tt3230854", tmdb = null, tvdb = 280619)
            )
        )

        assertEquals(null, show.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW))
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.AnticipatedCatalogRepositoryTest'
```

Expected: FAIL because models do not exist.

- [ ] **Step 3: Add models**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperModels.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.google.gson.annotations.SerializedName

enum class CatalogKind {
    ANTICIPATED_MOVIE,
    ANTICIPATED_SHOW
}

data class TraktIds(
    val trakt: Long? = null,
    val slug: String? = null,
    val imdb: String? = null,
    val tmdb: Long? = null,
    val tvdb: Long? = null
)

data class TraktMediaSummary(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

data class TraktAnticipatedItem(
    @SerializedName("list_count") val listCount: Int,
    val movie: TraktMediaSummary? = null,
    val show: TraktMediaSummary? = null
)

data class WallpaperCandidate(
    val catalog: CatalogKind,
    val title: String,
    val year: Int?,
    val tmdbId: Long,
    val imdbId: String?,
    val traktId: Long?,
    val tvdbId: Long?
)

data class TmdbDetails(
    val id: Long,
    val title: String,
    val year: Int?,
    val backdropUrl: String,
    val logoUrl: String?
)

data class GeneratedWallpaper(
    val title: String,
    val filePath: String,
    val contentUri: String
)
```

- [ ] **Step 4: Add mapper**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepository.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

fun TraktAnticipatedItem.toWallpaperCandidate(catalog: CatalogKind): WallpaperCandidate? {
    val media = when (catalog) {
        CatalogKind.ANTICIPATED_MOVIE -> movie
        CatalogKind.ANTICIPATED_SHOW -> show
    } ?: return null

    val tmdbId = media.ids.tmdb ?: return null
    return WallpaperCandidate(
        catalog = catalog,
        title = media.title,
        year = media.year,
        tmdbId = tmdbId,
        imdbId = media.ids.imdb,
        traktId = media.ids.trakt,
        tvdbId = media.ids.tvdb
    )
}
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.AnticipatedCatalogRepositoryTest'
```

Expected: PASS.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperModels.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepository.kt sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/AnticipatedCatalogRepositoryTest.kt
git commit -m "feat: add anticipated catalog models"
```

## Task 3: Trakt Device OAuth

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktLocalApi.kt`
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepository.kt`
- Modify: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/PreferencesManager.kt`
- Test: `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepositoryTest.kt`

- [ ] **Step 1: Add token status tests**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepositoryTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TraktDeviceAuthRepositoryTest {
    @Test
    fun mapsPollStatusCodes() {
        assertEquals(TraktPollResult.Pending, TraktDeviceAuthRepository.mapPollStatus(400, null))
        assertEquals(TraktPollResult.InvalidDeviceCode, TraktDeviceAuthRepository.mapPollStatus(404, null))
        assertEquals(TraktPollResult.AlreadyUsed, TraktDeviceAuthRepository.mapPollStatus(409, null))
        assertEquals(TraktPollResult.Expired, TraktDeviceAuthRepository.mapPollStatus(410, null))
        assertEquals(TraktPollResult.Denied, TraktDeviceAuthRepository.mapPollStatus(418, null))
        assertEquals(TraktPollResult.SlowDown, TraktDeviceAuthRepository.mapPollStatus(429, null))
        assertEquals(TraktPollResult.Failed("HTTP 500"), TraktDeviceAuthRepository.mapPollStatus(500, null))
    }

    @Test
    fun tokenExpiryUsesSixtySecondLeeway() {
        assertEquals(true, TraktDeviceAuthRepository.isTokenUsable(nowSeconds = 1000, expiresAtSeconds = 1200))
        assertEquals(false, TraktDeviceAuthRepository.isTokenUsable(nowSeconds = 1000, expiresAtSeconds = 1050))
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.TraktDeviceAuthRepositoryTest'
```

Expected: FAIL because repository/result classes do not exist.

- [ ] **Step 3: Add Trakt API**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktLocalApi.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class TraktDeviceCodeRequest(@SerializedName("client_id") val clientId: String)

data class TraktDeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_url") val verificationUrl: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val interval: Int
)

data class TraktDeviceTokenRequest(
    val code: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String
)

data class TraktRefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("redirect_uri") val redirectUri: String,
    @SerializedName("grant_type") val grantType: String = "refresh_token"
)

data class TraktTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("created_at") val createdAt: Long
)

interface TraktLocalApi {
    @POST("oauth/device/code")
    suspend fun requestDeviceCode(@Body body: TraktDeviceCodeRequest): Response<TraktDeviceCodeResponse>

    @POST("oauth/device/token")
    suspend fun requestDeviceToken(@Body body: TraktDeviceTokenRequest): Response<TraktTokenResponse>

    @POST("oauth/token")
    suspend fun refreshToken(@Body body: TraktRefreshTokenRequest): Response<TraktTokenResponse>

    @GET("movies/anticipated")
    suspend fun anticipatedMovies(@Header("Authorization") authorization: String, @Query("limit") limit: Int = 20): Response<List<TraktAnticipatedItem>>

    @GET("shows/anticipated")
    suspend fun anticipatedShows(@Header("Authorization") authorization: String, @Query("limit") limit: Int = 20): Response<List<TraktAnticipatedItem>>
}
```

- [ ] **Step 4: Extend preferences**

Add to `PreferencesManager.kt`:

```kotlin
private const val TRAKT_ACCESS_TOKEN_KEY = "trakt_access_token"
private const val TRAKT_REFRESH_TOKEN_KEY = "trakt_refresh_token"
private const val TRAKT_EXPIRES_AT_KEY = "trakt_expires_at"
private const val TRAKT_DEVICE_CODE_KEY = "trakt_device_code"
private const val TRAKT_USER_CODE_KEY = "trakt_user_code"
private const val TRAKT_VERIFICATION_URL_KEY = "trakt_verification_url"
private const val SELECTED_CATALOGS_KEY = "selected_catalogs"

var traktAccessToken: String
    get() = PreferencesManager[TRAKT_ACCESS_TOKEN_KEY, ""]
    set(value) { PreferencesManager[TRAKT_ACCESS_TOKEN_KEY] = value }

var traktRefreshToken: String
    get() = PreferencesManager[TRAKT_REFRESH_TOKEN_KEY, ""]
    set(value) { PreferencesManager[TRAKT_REFRESH_TOKEN_KEY] = value }

var traktExpiresAt: Long
    get() = PreferencesManager[TRAKT_EXPIRES_AT_KEY, 0L]
    set(value) { PreferencesManager[TRAKT_EXPIRES_AT_KEY] = value }

var traktDeviceCode: String
    get() = PreferencesManager[TRAKT_DEVICE_CODE_KEY, ""]
    set(value) { PreferencesManager[TRAKT_DEVICE_CODE_KEY] = value }

var traktUserCode: String
    get() = PreferencesManager[TRAKT_USER_CODE_KEY, ""]
    set(value) { PreferencesManager[TRAKT_USER_CODE_KEY] = value }

var traktVerificationUrl: String
    get() = PreferencesManager[TRAKT_VERIFICATION_URL_KEY, ""]
    set(value) { PreferencesManager[TRAKT_VERIFICATION_URL_KEY] = value }

var selectedCatalogs: String
    get() = PreferencesManager[SELECTED_CATALOGS_KEY, "anticipated_movies,anticipated_shows"]
    set(value) { PreferencesManager[SELECTED_CATALOGS_KEY] = value }
```

- [ ] **Step 5: Add repository**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepository.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.butch708.projectivy.tvbgsuite.BuildConfig

sealed class TraktPollResult {
    data object Pending : TraktPollResult()
    data object InvalidDeviceCode : TraktPollResult()
    data object AlreadyUsed : TraktPollResult()
    data object Expired : TraktPollResult()
    data object Denied : TraktPollResult()
    data object SlowDown : TraktPollResult()
    data class Approved(val accessToken: String) : TraktPollResult()
    data class Failed(val message: String) : TraktPollResult()
}

class TraktDeviceAuthRepository(
    private val api: TraktLocalApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000L }
) {
    suspend fun start(): Result<TraktDeviceCodeResponse> {
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        if (clientId.isBlank()) return Result.failure(IllegalStateException("TRAKT_CLIENT_ID is missing"))
        val response = api.requestDeviceCode(TraktDeviceCodeRequest(clientId))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            return Result.failure(IllegalStateException("Trakt device code failed: HTTP ${response.code()}"))
        }
        PreferencesManager.traktDeviceCode = body.deviceCode
        PreferencesManager.traktUserCode = body.userCode
        PreferencesManager.traktVerificationUrl = body.verificationUrl
        return Result.success(body)
    }

    suspend fun poll(): TraktPollResult {
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        val clientSecret = BuildConfig.TRAKT_CLIENT_SECRET.trim()
        val deviceCode = PreferencesManager.traktDeviceCode
        if (clientId.isBlank() || clientSecret.isBlank()) return TraktPollResult.Failed("Trakt credentials are missing")
        if (deviceCode.isBlank()) return TraktPollResult.InvalidDeviceCode

        val response = api.requestDeviceToken(TraktDeviceTokenRequest(deviceCode, clientId, clientSecret))
        if (!response.isSuccessful) return mapPollStatus(response.code(), null)
        val body = response.body() ?: return TraktPollResult.Failed("Trakt token response was empty")
        saveToken(body)
        return TraktPollResult.Approved(body.accessToken)
    }

    suspend fun authorizationHeader(): String? {
        if (isTokenUsable(nowSeconds(), PreferencesManager.traktExpiresAt) && PreferencesManager.traktAccessToken.isNotBlank()) {
            return "Bearer ${PreferencesManager.traktAccessToken}"
        }
        val refreshToken = PreferencesManager.traktRefreshToken
        if (refreshToken.isBlank()) return null
        val response = api.refreshToken(
            TraktRefreshTokenRequest(
                refreshToken = refreshToken,
                clientId = BuildConfig.TRAKT_CLIENT_ID.trim(),
                clientSecret = BuildConfig.TRAKT_CLIENT_SECRET.trim(),
                redirectUri = BuildConfig.TRAKT_REDIRECT_URI.ifBlank { "urn:ietf:wg:oauth:2.0:oob" }
            )
        )
        val body = response.body()
        if (!response.isSuccessful || body == null) return null
        saveToken(body)
        return "Bearer ${body.accessToken}"
    }

    private fun saveToken(body: TraktTokenResponse) {
        PreferencesManager.traktAccessToken = body.accessToken
        PreferencesManager.traktRefreshToken = body.refreshToken
        PreferencesManager.traktExpiresAt = nowSeconds() + body.expiresIn
        PreferencesManager.traktDeviceCode = ""
        PreferencesManager.traktUserCode = ""
    }

    companion object {
        fun mapPollStatus(statusCode: Int, message: String?): TraktPollResult = when (statusCode) {
            400 -> TraktPollResult.Pending
            404 -> TraktPollResult.InvalidDeviceCode
            409 -> TraktPollResult.AlreadyUsed
            410 -> TraktPollResult.Expired
            418 -> TraktPollResult.Denied
            429 -> TraktPollResult.SlowDown
            else -> TraktPollResult.Failed(message ?: "HTTP $statusCode")
        }

        fun isTokenUsable(nowSeconds: Long, expiresAtSeconds: Long): Boolean =
            expiresAtSeconds - 60L > nowSeconds
    }
}
```

- [ ] **Step 6: Run tests and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.TraktDeviceAuthRepositoryTest'
```

Expected: PASS.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktLocalApi.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepository.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/PreferencesManager.kt sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TraktDeviceAuthRepositoryTest.kt
git commit -m "feat: add trakt device oauth"
```

## Task 4: TMDB Artwork Fetching

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbLocalApi.kt`
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepository.kt`
- Test: `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepositoryTest.kt`

- [ ] **Step 1: Add artwork selection tests**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepositoryTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbArtworkRepositoryTest {
    @Test
    fun selectsEnglishLogoBeforeOtherLogos() {
        val images = TmdbImagesResponse(
            logos = listOf(
                TmdbLogo(filePath = "/other.png", iso6391 = "fr"),
                TmdbLogo(filePath = "/english.png", iso6391 = "en")
            )
        )

        assertEquals("https://image.tmdb.org/t/p/original/english.png", TmdbArtworkRepository.selectLogoUrl(images))
    }

    @Test
    fun buildsBackdropUrl() {
        assertEquals(
            "https://image.tmdb.org/t/p/original/backdrop.jpg",
            TmdbArtworkRepository.imageUrl("/backdrop.jpg")
        )
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.TmdbArtworkRepositoryTest'
```

Expected: FAIL because TMDB types do not exist.

- [ ] **Step 3: Add TMDB API models**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbLocalApi.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class TmdbDetailsResponse(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null
)

data class TmdbImagesResponse(val logos: List<TmdbLogo> = emptyList())

data class TmdbLogo(
    @SerializedName("file_path") val filePath: String,
    @SerializedName("iso_639_1") val iso6391: String? = null
)

interface TmdbLocalApi {
    @GET("movie/{id}")
    suspend fun movieDetails(@Path("id") id: Long, @Query("api_key") apiKey: String): Response<TmdbDetailsResponse>

    @GET("tv/{id}")
    suspend fun showDetails(@Path("id") id: Long, @Query("api_key") apiKey: String): Response<TmdbDetailsResponse>

    @GET("movie/{id}/images")
    suspend fun movieImages(@Path("id") id: Long, @Query("api_key") apiKey: String): Response<TmdbImagesResponse>

    @GET("tv/{id}/images")
    suspend fun showImages(@Path("id") id: Long, @Query("api_key") apiKey: String): Response<TmdbImagesResponse>
}
```

- [ ] **Step 4: Add artwork repository**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepository.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.butch708.projectivy.tvbgsuite.BuildConfig

class TmdbArtworkRepository(private val api: TmdbLocalApi) {
    suspend fun details(candidate: WallpaperCandidate): TmdbDetails? {
        val apiKey = BuildConfig.TMDB_API_KEY.trim()
        if (apiKey.isBlank()) return null
        val isMovie = candidate.catalog == CatalogKind.ANTICIPATED_MOVIE

        val detailsResponse = if (isMovie) api.movieDetails(candidate.tmdbId, apiKey) else api.showDetails(candidate.tmdbId, apiKey)
        val detailsBody = detailsResponse.body()
        if (!detailsResponse.isSuccessful || detailsBody?.backdropPath.isNullOrBlank()) return null

        val imagesResponse = if (isMovie) api.movieImages(candidate.tmdbId, apiKey) else api.showImages(candidate.tmdbId, apiKey)
        val logoUrl = imagesResponse.body()?.let(::selectLogoUrl)
        val title = detailsBody.title ?: detailsBody.name ?: candidate.title
        val year = (detailsBody.releaseDate ?: detailsBody.firstAirDate)?.take(4)?.toIntOrNull() ?: candidate.year

        return TmdbDetails(
            id = candidate.tmdbId,
            title = title,
            year = year,
            backdropUrl = imageUrl(detailsBody.backdropPath!!),
            logoUrl = logoUrl
        )
    }

    companion object {
        fun imageUrl(path: String): String =
            "https://image.tmdb.org/t/p/original/${path.trimStart('/')}"

        fun selectLogoUrl(images: TmdbImagesResponse): String? {
            val selected = images.logos.firstOrNull { it.iso6391 == "en" } ?: images.logos.firstOrNull()
            return selected?.filePath?.let(::imageUrl)
        }
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.TmdbArtworkRepositoryTest'
```

Expected: PASS.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbLocalApi.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepository.kt sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/TmdbArtworkRepositoryTest.kt
git commit -m "feat: add tmdb artwork lookup"
```

## Task 5: Android Canvas Wallpaper Renderer

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpec.kt`
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderer.kt`
- Test: `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpecTest.kt`

- [ ] **Step 1: Add layout math tests**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpecTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRenderSpecTest {
    @Test
    fun logoBoxMatchesRequestedTopLeftComposition() {
        val spec = WallpaperRenderSpec.default()
        assertEquals(RectF(380f, 250f, 1420f, 620f), spec.logoBox)
    }

    @Test
    fun coverRectBiasesBackdropToUpperRight() {
        val spec = WallpaperRenderSpec.default()
        val rect = spec.coverCropRect(sourceWidth = 1920, sourceHeight = 1080)
        assertEquals(0f, rect.left)
        assertEquals(0f, rect.top)
        assertEquals(3840f, rect.right)
        assertEquals(2160f, rect.bottom)
    }

    @Test
    fun containRectCentersLogoInsideBox() {
        val spec = WallpaperRenderSpec.default()
        val rect = spec.containRect(sourceWidth = 1000, sourceHeight = 300, box = spec.logoBox)
        assertEquals(380f, rect.left)
        assertEquals(279f, rect.top)
        assertEquals(1420f, rect.right)
        assertEquals(591f, rect.bottom)
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.WallpaperRenderSpecTest'
```

Expected: FAIL because render spec does not exist.

- [ ] **Step 3: Add render spec**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpec.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.RectF
import kotlin.math.max

data class WallpaperRenderSpec(
    val width: Int,
    val height: Int,
    val logoBox: RectF,
    val leftGradientEndX: Float,
    val bottomGradientStartY: Float,
    val backdropBiasX: Float,
    val backdropBiasY: Float
) {
    fun coverCropRect(sourceWidth: Int, sourceHeight: Int): RectF {
        val scale = max(width / sourceWidth.toFloat(), height / sourceHeight.toFloat())
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val overflowX = scaledW - width
        val overflowY = scaledH - height
        val left = -overflowX * backdropBiasX
        val top = -overflowY * backdropBiasY
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    fun containRect(sourceWidth: Int, sourceHeight: Int, box: RectF): RectF {
        val scale = minOf(box.width() / sourceWidth.toFloat(), box.height() / sourceHeight.toFloat())
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val left = box.left + (box.width() - scaledW) / 2f
        val top = box.top + (box.height() - scaledH) / 2f
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    companion object {
        fun default(): WallpaperRenderSpec = WallpaperRenderSpec(
            width = 3840,
            height = 2160,
            logoBox = RectF(380f, 250f, 1420f, 620f),
            leftGradientEndX = 1900f,
            bottomGradientStartY = 1580f,
            backdropBiasX = 0.72f,
            backdropBiasY = 0.18f
        )
    }
}
```

- [ ] **Step 4: Add renderer**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderer.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream

class WallpaperRenderer(private val spec: WallpaperRenderSpec = WallpaperRenderSpec.default()) {
    fun render(
        backdrop: Bitmap,
        logo: Bitmap?,
        outputFile: File
    ) {
        val output = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(backdrop, Rect(0, 0, backdrop.width, backdrop.height), spec.coverCropRect(backdrop.width, backdrop.height), paint)
        drawLeftGradient(canvas)
        drawBottomGradient(canvas)

        if (logo != null) {
            canvas.drawBitmap(logo, Rect(0, 0, logo.width, logo.height), spec.containRect(logo.width, logo.height, spec.logoBox), paint)
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }
        output.recycle()
    }

    private fun drawLeftGradient(canvas: Canvas) {
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f, spec.leftGradientEndX, 0f,
            intArrayOf(Color.argb(230, 10, 4, 0), Color.argb(160, 20, 8, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, spec.leftGradientEndX, spec.height.toFloat(), paint)
    }

    private fun drawBottomGradient(canvas: Canvas) {
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, spec.bottomGradientStartY, 0f, spec.height.toFloat(),
            Color.TRANSPARENT,
            Color.argb(245, 8, 4, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, spec.bottomGradientStartY, spec.width.toFloat(), spec.height.toFloat(), paint)
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.WallpaperRenderSpecTest'
```

Expected: PASS.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpec.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderer.kt sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperRenderSpecTest.kt
git commit -m "feat: add local wallpaper renderer"
```

## Task 6: Wallpaper Cache And FileProvider

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCache.kt`
- Create: `sample/src/main/res/xml/file_paths.xml`
- Modify: `sample/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add cache file naming test**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCacheTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperCacheTest {
    @Test
    fun safeFileNameRemovesDangerousCharacters() {
        assertEquals("Avatar Fire and Ash - 2025.jpg", WallpaperCache.safeFileName("Avatar: Fire/and Ash", 2025))
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.WallpaperCacheTest'
```

Expected: FAIL because `WallpaperCache` does not exist.

- [ ] **Step 3: Add cache**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCache.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

class WallpaperCache(private val context: Context) {
    private val directory: File = File(context.cacheDir, "generated_wallpapers")

    fun outputFile(details: TmdbDetails): File =
        File(directory, safeFileName(details.title, details.year))

    fun contentUri(file: File): String =
        FileProvider.getUriForFile(context, "${context.packageName}.wallpaperfiles", file).toString()

    companion object {
        fun safeFileName(title: String, year: Int?): String {
            val cleanTitle = title.map { char ->
                if (char.isLetterOrDigit() || char == ' ' || char == '.' || char == '_' || char == '-') char else ' '
            }.joinToString("").trim().replace(Regex("\\s+"), " ")
            return "$cleanTitle - ${year ?: "Unknown"}.jpg"
        }
    }
}
```

- [ ] **Step 4: Add FileProvider**

Create `sample/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="generated_wallpapers" path="generated_wallpapers/" />
</paths>
```

In `AndroidManifest.xml`, inside `<application>`, add:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.wallpaperfiles"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

- [ ] **Step 5: Run test and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.WallpaperCacheTest'
```

Expected: PASS.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCache.kt sample/src/main/res/xml/file_paths.xml sample/src/main/AndroidManifest.xml sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/WallpaperCacheTest.kt
git commit -m "feat: expose generated wallpapers via file provider"
```

## Task 7: Local Generation Pipeline

**Files:**
- Create: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperGenerator.kt`
- Modify: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/WallpaperProviderService.kt`

- [ ] **Step 1: Add generator selection test**

Create `sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperGeneratorTest.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalWallpaperGeneratorTest {
    @Test
    fun alternatesMovieAndShowCandidatesByDefault() {
        val movies = listOf(
            WallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE, "Movie A", 2026, 1, "tt1", 1, null),
            WallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE, "Movie B", 2026, 2, "tt2", 2, null)
        )
        val shows = listOf(
            WallpaperCandidate(CatalogKind.ANTICIPATED_SHOW, "Show A", 2026, 3, "tt3", 3, 4)
        )

        assertEquals(listOf("Movie A", "Show A", "Movie B"), LocalWallpaperGenerator.interleave(movies, shows).map { it.title })
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.LocalWallpaperGeneratorTest'
```

Expected: FAIL because `LocalWallpaperGenerator` does not exist.

- [ ] **Step 3: Add generator**

Create `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperGenerator.kt`:

```kotlin
package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.content.Context
import android.graphics.BitmapFactory
import com.butch708.projectivy.tvbgsuite.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocalWallpaperGenerator(
    private val context: Context,
    private val http: OkHttpClient = OkHttpClient()
) {
    private val traktApi = Retrofit.Builder()
        .baseUrl(BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TraktLocalApi::class.java)

    private val tmdbApi = Retrofit.Builder()
        .baseUrl(BuildConfig.TMDB_API_URL.ifBlank { "https://api.themoviedb.org/3/" })
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbLocalApi::class.java)

    private val auth = TraktDeviceAuthRepository(traktApi)
    private val tmdb = TmdbArtworkRepository(tmdbApi)
    private val renderer = WallpaperRenderer()
    private val cache = WallpaperCache(context)

    suspend fun generate(): GeneratedWallpaper? = withContext(Dispatchers.IO) {
        val authorization = auth.authorizationHeader() ?: return@withContext null
        val moviesResponse = traktApi.anticipatedMovies(authorization = authorization, limit = 20)
        val showsResponse = traktApi.anticipatedShows(authorization = authorization, limit = 20)
        val movies = moviesResponse.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE) }
        val shows = showsResponse.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW) }
        val candidates = interleave(movies, shows)

        for (candidate in candidates) {
            val details = tmdb.details(candidate) ?: continue
            val backdrop = downloadBitmap(details.backdropUrl) ?: continue
            val logo = details.logoUrl?.let { downloadBitmap(it) }
            val file = cache.outputFile(details)
            renderer.render(backdrop = backdrop, logo = logo, outputFile = file)
            return@withContext GeneratedWallpaper(
                title = details.title,
                filePath = file.absolutePath,
                contentUri = cache.contentUri(file)
            )
        }

        null
    }

    private fun downloadBitmap(url: String) =
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.byteStream()?.use(BitmapFactory::decodeStream)
        }

    companion object {
        fun interleave(movies: List<WallpaperCandidate>, shows: List<WallpaperCandidate>): List<WallpaperCandidate> {
            val result = mutableListOf<WallpaperCandidate>()
            val max = maxOf(movies.size, shows.size)
            for (index in 0 until max) {
                movies.getOrNull(index)?.let(result::add)
                shows.getOrNull(index)?.let(result::add)
            }
            return result
        }
    }
}
```

- [ ] **Step 4: Wire service**

In `WallpaperProviderService.kt`, replace the Retrofit TVBG Suite server block inside `if (event is Event.TimeElapsed || forceRefresh)` with:

```kotlin
val generated = kotlinx.coroutines.runBlocking {
    LocalWallpaperGenerator(this@WallpaperProviderService).generate()
}
if (generated != null) {
    PreferencesManager.lastWallpaperUri = generated.contentUri
    PreferencesManager.lastWallpaperAuthor = generated.title
    return listOf(
        Wallpaper(
            uri = generated.contentUri,
            type = WallpaperType.IMAGE,
            displayMode = WallpaperDisplayMode.CROP,
            title = generated.title,
            author = generated.title,
            actionUri = null
        )
    )
}

val lastUri = PreferencesManager.lastWallpaperUri
if (lastUri.isNotBlank()) {
    return listOf(
        Wallpaper(
            uri = lastUri,
            type = WallpaperType.IMAGE,
            displayMode = WallpaperDisplayMode.CROP,
            author = PreferencesManager.lastWallpaperAuthor.ifBlank { null },
            actionUri = null
        )
    )
}
return emptyList()
```

Remove unused imports for `Retrofit`, `GsonConverterFactory`, `ApiService`, and client deep-link handling from `WallpaperProviderService.kt`.

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :sample:testDebugUnitTest --tests 'tv.projectivy.plugin.wallpaperprovider.sample.local.LocalWallpaperGeneratorTest'
./gradlew :sample:assembleDebug
```

Expected: PASS and APK builds.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperGenerator.kt sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/WallpaperProviderService.kt sample/src/test/java/tv/projectivy/plugin/wallpaperprovider/sample/local/LocalWallpaperGeneratorTest.kt
git commit -m "feat: generate wallpapers locally"
```

## Task 8: Leanback Trakt OAuth Settings

**Files:**
- Modify: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/SettingsFragment.kt`
- Modify: `sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/PreferencesManager.kt`
- Modify: `sample/src/main/res/values/strings.xml`

- [ ] **Step 1: Replace obsolete settings actions**

In `SettingsFragment.kt`, replace constants with:

```kotlin
private const val ACTION_ID_TRAKT_STATUS = 1L
private const val ACTION_ID_TRAKT_START = 2L
private const val ACTION_ID_TRAKT_POLL = 3L
private const val ACTION_ID_CATALOGS = 4L
private const val ACTION_ID_EVENT_IDLE = 5L
```

In `onCreateActions`, replace server/layout/filter actions with:

```kotlin
actions.add(GuidedAction.Builder(context)
    .id(ACTION_ID_TRAKT_STATUS)
    .title("Trakt")
    .description(if (PreferencesManager.traktAccessToken.isNotBlank()) "Connected" else "Not connected")
    .enabled(false)
    .build())

actions.add(GuidedAction.Builder(context)
    .id(ACTION_ID_TRAKT_START)
    .title("Start Trakt OAuth")
    .description("Get a device code for trakt.tv/activate")
    .build())

actions.add(GuidedAction.Builder(context)
    .id(ACTION_ID_TRAKT_POLL)
    .title("Check Trakt OAuth")
    .description(if (PreferencesManager.traktUserCode.isNotBlank()) "${PreferencesManager.traktVerificationUrl}/${PreferencesManager.traktUserCode}" else "Start OAuth first")
    .build())

actions.add(GuidedAction.Builder(context)
    .id(ACTION_ID_CATALOGS)
    .title("Catalogs")
    .description(PreferencesManager.selectedCatalogs)
    .subActions(listOf(
        GuidedAction.Builder(context)
            .id(401L)
            .title("Anticipated Movies")
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(PreferencesManager.selectedCatalogs.contains("anticipated_movies"))
            .build(),
        GuidedAction.Builder(context)
            .id(402L)
            .title("Anticipated Shows")
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(PreferencesManager.selectedCatalogs.contains("anticipated_shows"))
            .build()
    ))
    .build())

actions.add(GuidedAction.Builder(context)
    .id(ACTION_ID_EVENT_IDLE)
    .title("Refresh on idle exit")
    .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
    .checked(PreferencesManager.refreshOnIdleExit)
    .build())
```

- [ ] **Step 2: Add OAuth actions**

In `onGuidedActionClicked`, replace old action handling with:

```kotlin
ACTION_ID_TRAKT_START -> {
    kotlinx.coroutines.MainScope().launch {
        val api = Retrofit.Builder()
            .baseUrl(com.butch708.projectivy.tvbgsuite.BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktLocalApi::class.java)
        val result = TraktDeviceAuthRepository(api).start()
        val message = result.fold(
            onSuccess = { "${it.verificationUrl}/${it.userCode}" },
            onFailure = { it.message ?: "Failed to start OAuth" }
        )
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        notifySettingsChanged()
        reloadActions()
    }
}
ACTION_ID_TRAKT_POLL -> {
    kotlinx.coroutines.MainScope().launch {
        val api = Retrofit.Builder()
            .baseUrl(com.butch708.projectivy.tvbgsuite.BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktLocalApi::class.java)
        val result = TraktDeviceAuthRepository(api).poll()
        Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
        notifySettingsChanged()
        reloadActions()
    }
}
ACTION_ID_EVENT_IDLE -> {
    val newState = !PreferencesManager.refreshOnIdleExit
    PreferencesManager.refreshOnIdleExit = newState
    action.isChecked = newState
    notifyActionChanged(findActionPositionById(ACTION_ID_EVENT_IDLE))
}
```

Add helper:

```kotlin
private fun reloadActions() {
    actions.clear()
    onCreateActions(actions, null)
    notifyActionRangeChanged(0, actions.size)
}
```

- [ ] **Step 3: Remove server refresh code**

Delete `refreshAllData`, `updateLayoutAction`, `updateGenreAction`, `updateAgeRatingAction`, `updateYearAction`, `updateClientAction`, `createRatingSubActions`, and old URL/filter handlers from `SettingsFragment.kt`. Keep only catalog selection, Trakt auth, and idle refresh.

- [ ] **Step 4: Run build and commit**

Run:

```bash
./gradlew :sample:assembleDebug
```

Expected: APK builds.

Commit:

```bash
git add sample/src/main/java/tv/projectivy/plugin/wallpaperprovider/sample/SettingsFragment.kt sample/src/main/res/values/strings.xml
git commit -m "feat: add local generator settings"
```

## Task 9: README And Manual APK Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Rewrite README overview**

Replace web-backend language in `README.md` with:

```markdown
# TVBG Suite Local Projectivy Wallpaper Plugin

This Projectivy wallpaper provider APK generates TV wallpapers directly on Android TV. It authenticates with Trakt using device OAuth, reads Trakt anticipated movies and shows, fetches backdrop and logo artwork from TMDB using built-in developer credentials, renders wallpapers locally, and returns local image URIs to Projectivy.
```

Add:

```markdown
## Runtime Flow

1. Projectivy calls `WallpaperProviderService.getWallpapers`.
2. The plugin refreshes or reuses Trakt OAuth tokens.
3. The plugin fetches `/movies/anticipated` and `/shows/anticipated`.
4. The plugin resolves TMDB details and logo/backdrop images.
5. The plugin renders a 3840x2160 JPEG with upper-right artwork placement, left fade, bottom fade, and top-left title logo.
6. The plugin returns a local FileProvider URI to Projectivy.
```

- [ ] **Step 2: Build APK**

Run:

```bash
./gradlew :sample:assembleDebug
```

Expected:

```text
BUILD SUCCESSFUL
```

APK path:

```text
sample/build/outputs/apk/debug/sample-debug.apk
```

- [ ] **Step 3: Install manually**

Run:

```bash
adb install -r sample/build/outputs/apk/debug/sample-debug.apk
```

Expected: `Success`.

- [ ] **Step 4: OAuth manual verification**

On Android TV:

1. Open Projectivy wallpaper plugin settings.
2. Click **Start Trakt OAuth**.
3. Visit shown `https://trakt.tv/activate/<code>` on another device.
4. Approve.
5. Click **Check Trakt OAuth**.
6. Confirm Trakt status changes to connected.

- [ ] **Step 5: Wallpaper manual verification**

In Projectivy:

1. Select this APK as wallpaper provider.
2. Trigger refresh or wait for time-elapsed refresh.
3. Confirm wallpaper is generated locally.
4. Confirm image resembles the provided design:
   - Backdrop subject biased upper-right.
   - Left gradient makes logo readable.
   - Bottom gradient fades to dark.
   - Logo is top-left, fit within its box.
   - No web TVBG Suite backend is required.

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: document local android generator"
```

## Self-Review

Spec coverage:

- Existing Projectivy plugin inspected: plan is based on current AIDL service, ContentProvider, Leanback settings, Retrofit web API, and PreferencesManager.
- Local wallpaper generation: Task 5 renders JPEG locally with Android Canvas.
- No web-hosted TVBG Suite dependency: Task 7 replaces `/api/wallpaper/status` service path with `LocalWallpaperGenerator`.
- Backdrop placement: Task 5 uses `coverCropRect` with upper-right bias.
- Left and bottom gradients: Task 5 draws left and bottom `LinearGradient` overlays.
- Logo placement and sizing: Task 5 uses a fixed top-left `logoBox` and contain fit.
- Trakt device OAuth: Task 3 implements device code, poll, refresh, and stored tokens.
- Anticipated movies/shows: Tasks 2 and 7 fetch and map both catalogs.
- TMDB artwork with built-in developer API: Tasks 1 and 4 use `BuildConfig.TMDB_API_KEY` and TMDB image URLs.
- Installable APK: Task 9 builds and installs `sample-debug.apk`.

Placeholder scan:

- The plan contains no `TODO`, `TBD`, or unspecified “handle edge cases” steps.
- Every new file has concrete code for the first implementation pass.
- Manual verification steps are explicit and tied to concrete expected outcomes.

Type consistency:

- Catalog enum names are `ANTICIPATED_MOVIE` and `ANTICIPATED_SHOW` throughout.
- The candidate model always uses `tmdbId` for TMDB artwork lookup.
- Trakt OAuth stores token fields in `PreferencesManager.trakt*`.
- Render dimensions are fixed at 3840x2160.
- Projectivy output remains `Wallpaper(type=IMAGE, displayMode=CROP)`.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-18-local-projectivy-wallpaper-generator.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
