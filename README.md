# TVBG Suite Local Projectivy Wallpaper Plugin

This Projectivy wallpaper provider APK generates TV wallpapers directly on Android TV. It authenticates with Trakt using device OAuth, reads Trakt anticipated movies and shows, fetches backdrop and logo artwork from TMDB using built-in developer credentials, renders wallpapers locally, and returns local image URIs to Projectivy.

It no longer requires a TVBG Suite web server for the local generation flow.

## Runtime Flow

1. Projectivy calls `WallpaperProviderService.getWallpapers`.
2. The plugin refreshes or reuses Trakt OAuth tokens.
3. The plugin fetches `/movies/anticipated` and `/shows/anticipated`.
4. The plugin resolves TMDB details and logo/backdrop images.
5. The plugin renders a 3840x2160 JPEG with upper-right artwork placement, left fade, bottom fade, and top-left title logo.
6. The plugin returns a local FileProvider URI to Projectivy.

## Wallpaper Style

- Output: 3840x2160 JPEG.
- Backdrop: cover-cropped with an upper-right bias.
- Logo: fit into a top-left logo box.
- Gradients: left readability fade and bottom dark fade.
- Catalogs: anticipated movies and anticipated shows.

## Installation & Setup

1. Install the debug APK from:

   ```text
   sample/build/outputs/apk/debug/sample-debug.apk
   ```

2. Open Projectivy Launcher settings.
3. Go to **Appearance** > **Wallpaper**.
4. Select this plugin as the wallpaper source.
5. Open plugin settings.
6. Start Trakt OAuth, approve the device code, then check OAuth.
7. Keep anticipated movies, anticipated shows, or both enabled.

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

The device-code OAuth flow uses `client_id` to start and `code + client_id + client_secret` to poll. The refresh flow uses `TRAKT_REDIRECT_URI`, defaulting to `urn:ietf:wg:oauth:2.0:oob`.

## Build

```bash
./gradlew :sample:assembleDebug
```

## Package

```text
com.butch708.projectivy.tvbgsuite
```

## Architecture

- `api/`: Projectivy wallpaper provider AIDL contract.
- `sample/src/main/java/.../WallpaperProviderService.kt`: Projectivy service entry point.
- `sample/src/main/java/.../local/TraktDeviceAuthRepository.kt`: Trakt OAuth.
- `sample/src/main/java/.../local/AnticipatedCatalogRepository.kt`: Trakt catalog mapping.
- `sample/src/main/java/.../local/TmdbArtworkRepository.kt`: TMDB details and artwork selection.
- `sample/src/main/java/.../local/WallpaperRenderer.kt`: Android Canvas renderer.
- `sample/src/main/java/.../local/WallpaperCache.kt`: Generated wallpaper files and FileProvider URIs.

## License

Based on the Projectivy Plugin Sample.
