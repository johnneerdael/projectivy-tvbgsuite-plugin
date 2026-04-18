package tv.projectivy.plugin.wallpaperprovider.sample.local

fun TraktAnticipatedItem.toWallpaperCandidate(catalog: CatalogKind): WallpaperCandidate? {
    val media = if (catalog.isMovie) movie else show
    return media?.toWallpaperCandidate(catalog)
}

fun TraktTrendingItem.toWallpaperCandidate(catalog: CatalogKind): WallpaperCandidate? {
    val media = if (catalog.isMovie) movie else show
    return media?.toWallpaperCandidate(catalog)
}

fun TraktListItem.toWallpaperCandidate(catalog: CatalogKind): WallpaperCandidate? {
    val media = if (catalog.isMovie) movie else show
    return media?.toWallpaperCandidate(catalog)
}

private fun TraktMediaSummary.toWallpaperCandidate(catalog: CatalogKind): WallpaperCandidate? {
    val tmdbId = ids.tmdb ?: return null
    return WallpaperCandidate(
        catalog = catalog,
        title = title,
        year = year,
        tmdbId = tmdbId,
        imdbId = ids.imdb,
        traktId = ids.trakt,
        tvdbId = ids.tvdb
    )
}

fun TraktPopularListItem.toOption(): TraktPopularListOption? {
    val summary = list ?: return null
    val userId = user?.ids?.slug
        ?: user?.username
        ?: summary.user?.ids?.slug
        ?: summary.user?.username
        ?: return null
    val listId = summary.ids?.slug ?: return null
    val title = summary.name ?: return null
    return TraktPopularListOption(
        key = "popular_list:$userId:$listId",
        title = title,
        userId = userId,
        listId = listId,
        itemCount = summary.itemCount ?: 0
    )
}
