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
