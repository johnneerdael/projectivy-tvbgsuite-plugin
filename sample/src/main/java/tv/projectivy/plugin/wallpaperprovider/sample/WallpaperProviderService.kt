package tv.projectivy.plugin.wallpaperprovider.sample

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.runBlocking
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperDisplayMode
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import tv.projectivy.plugin.wallpaperprovider.sample.local.LocalWallpaperGenerator

class WallpaperProviderService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.e("WallpaperService", "PROJECTIVY_LOG: Service onCreate")
        PreferencesManager.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e("WallpaperService", "PROJECTIVY_LOG: Service onBind")
        return binder
    }

    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> {
            Log.e("WallpaperService", "PROJECTIVY_LOG: getWallpapers | Event: ${event?.eventType} (${event?.javaClass?.simpleName}) ")

            var forceRefresh = false

            if (event is Event.LauncherIdleModeChanged) {
                Log.e("WallpaperService", "PROJECTIVY_LOG: AIDL Idle Changed | isIdle: ${event.isIdle}")

                if (!event.isIdle) {
                    if (PreferencesManager.refreshOnIdleExit) {
                        Log.e("WallpaperService", "PROJECTIVY_LOG: Idle exit detected and preference is ON. Triggering refresh.")
                        forceRefresh = true
                    } else {
                        return lastWallpaper()
                    }
                } else {
                    Log.e("WallpaperService", "PROJECTIVY_LOG: Idle enter detected. No refresh needed.")
                    return emptyList()
                }
            }

            if (event is Event.TimeElapsed || forceRefresh) {
                Log.e("WallpaperService", "PROJECTIVY_LOG: Executing local wallpaper generation...")
                try {
                    val generated = runBlocking {
                        LocalWallpaperGenerator(this@WallpaperProviderService).generate()
                    }
                    if (generated != null) {
                        PreferencesManager.lastWallpaperUri = generated.contentUri
                        PreferencesManager.lastWallpaperAuthor = generated.title
                        grantWallpaperRead(generated.contentUri)
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
                } catch (error: Exception) {
                    Log.e("WallpaperService", "PROJECTIVY_LOG: Local generation failed", error)
                }

                return lastWallpaper()
            }

            Log.e("WallpaperService", "PROJECTIVY_LOG: Event type ${event?.javaClass?.simpleName} not handled by service for refresh.")
            return emptyList()
        }

        override fun getPreferences(): String = PreferencesManager.export()
        override fun setPreferences(params: String) { PreferencesManager.import(params) }
    }

    private fun lastWallpaper(): List<Wallpaper> {
        val lastUri = PreferencesManager.lastWallpaperUri
        val lastAuthor = PreferencesManager.lastWallpaperAuthor
        return if (lastUri.isNotBlank()) {
            grantWallpaperRead(lastUri)
            listOf(
                Wallpaper(
                    uri = lastUri,
                    type = WallpaperType.IMAGE,
                    displayMode = WallpaperDisplayMode.CROP,
                    author = lastAuthor.ifBlank { null },
                    actionUri = null
                )
            )
        } else {
            emptyList()
        }
    }

    private fun grantWallpaperRead(uri: String) {
        try {
            grantUriPermission(PROJECTIVY_PACKAGE_ID, Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Log.e("WallpaperService", "PROJECTIVY_LOG: Granted Projectivy read permission for $uri")
        } catch (error: Exception) {
            Log.e("WallpaperService", "PROJECTIVY_LOG: Failed to grant wallpaper URI permission", error)
        }
    }

    companion object {
        private const val PROJECTIVY_PACKAGE_ID = "com.spocky.projengmenu"
    }
}
