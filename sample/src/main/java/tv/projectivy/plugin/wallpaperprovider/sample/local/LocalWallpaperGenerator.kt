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
        val moviesResponse = traktApi.anticipatedMovies(apiKey = apiKey, authorization = authorization, limit = 20)
        val showsResponse = traktApi.anticipatedShows(apiKey = apiKey, authorization = authorization, limit = 20)
        val selectedCatalogs = PreferencesManager.selectedCatalogs
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val movies = if ("anticipated_movies" in selectedCatalogs) {
            moviesResponse.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE) }
        } else {
            emptyList()
        }
        val shows = if ("anticipated_shows" in selectedCatalogs) {
            showsResponse.body().orEmpty().mapNotNull { it.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW) }
        } else {
            emptyList()
        }
        val candidates = randomizedCandidates(movies, shows)

        for (candidate in candidates) {
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
    }
}
