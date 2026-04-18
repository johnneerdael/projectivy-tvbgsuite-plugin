package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.content.Context
import android.graphics.BitmapFactory
import com.traktlistbackdrops.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random
import tv.projectivy.plugin.wallpaperprovider.sample.PreferencesManager

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
        val apiKey = BuildConfig.TRAKT_CLIENT_ID.trim()
        val selectedCatalogs = PreferencesManager.selectedCatalogs
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val candidates = mutableListOf<WallpaperCandidate>()

        if ("anticipated_movies" in selectedCatalogs) {
            val response = traktApi.anticipatedMovies(apiKey = apiKey, authorization = authorization, limit = 20)
            candidates += response.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE) }
        }
        if ("anticipated_shows" in selectedCatalogs) {
            val response = traktApi.anticipatedShows(apiKey = apiKey, authorization = authorization, limit = 20)
            candidates += response.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW) }
        }
        if ("trending_movies" in selectedCatalogs) {
            val response = traktApi.trendingMovies(apiKey = apiKey, authorization = authorization, limit = 20)
            candidates += response.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.TRENDING_MOVIE) }
        }
        if ("trending_shows" in selectedCatalogs) {
            val response = traktApi.trendingShows(apiKey = apiKey, authorization = authorization, limit = 20)
            candidates += response.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.TRENDING_SHOW) }
        }

        selectedCatalogs.filter { it.startsWith("popular_list:") }.forEach { key ->
            val option = parsePopularListKey(key) ?: return@forEach
            val movieItems = traktApi.listItems(
                userId = option.userId,
                listId = option.listId,
                type = "movies",
                apiKey = apiKey,
                authorization = authorization,
                limit = 20
            )
            candidates += movieItems.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.POPULAR_LIST_MOVIE) }

            val showItems = traktApi.listItems(
                userId = option.userId,
                listId = option.listId,
                type = "shows",
                apiKey = apiKey,
                authorization = authorization,
                limit = 20
            )
            candidates += showItems.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.POPULAR_LIST_SHOW) }
        }

        val randomized = randomizedCandidates(candidates, emptyList())

        for (candidate in randomized) {
            val details = tmdb.details(candidate) ?: continue
            if (details.logoUrl.isNullOrBlank()) continue
            val backdrop = downloadBitmap(details.backdropUrl) ?: continue
            val logo = downloadBitmap(details.logoUrl) ?: continue
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
            response.body.byteStream().use(BitmapFactory::decodeStream)
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

        fun randomizedCandidates(
            movies: List<WallpaperCandidate>,
            shows: List<WallpaperCandidate>,
            random: Random = Random.Default
        ): List<WallpaperCandidate> =
            (movies + shows).shuffled(random)

        fun isRenderable(details: TmdbDetails, hasBackdrop: Boolean, hasLogo: Boolean): Boolean =
            hasBackdrop && hasLogo && details.backdropUrl.isNotBlank() && !details.logoUrl.isNullOrBlank()

        fun parsePopularListKey(key: String): TraktPopularListOption? {
            val parts = key.split(':')
            if (parts.size != 3 || parts[0] != "popular_list") return null
            return TraktPopularListOption(
                key = key,
                title = "${parts[1]}/${parts[2]}",
                userId = parts[1],
                listId = parts[2]
            )
        }
    }
}
