package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.traktlistbackdrops.tv.BuildConfig

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
            backdropUrl = imageUrl(detailsBody.backdropPath),
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
