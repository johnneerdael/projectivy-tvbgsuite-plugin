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
