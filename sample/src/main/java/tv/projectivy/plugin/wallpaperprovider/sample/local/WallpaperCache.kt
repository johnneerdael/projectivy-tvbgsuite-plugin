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
