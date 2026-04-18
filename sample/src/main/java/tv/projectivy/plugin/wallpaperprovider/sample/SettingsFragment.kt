package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import com.traktlistbackdrops.tv.BuildConfig
import com.traktlistbackdrops.tv.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktDeviceAuthRepository
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktLocalApi
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktPopularListOption
import tv.projectivy.plugin.wallpaperprovider.sample.local.toOption

class SettingsFragment : GuidedStepSupportFragment() {

    companion object {
        private const val ACTION_ID_TRAKT_STATUS = 1L
        private const val ACTION_ID_TRAKT_START = 2L
        private const val ACTION_ID_REFRESH_POPULAR = 3L
        private const val ACTION_ID_CATALOGS = 4L
        private const val ACTION_ID_EVENT_IDLE = 5L
    }

    override fun onResume() {
        super.onResume()
        if (actions.isNotEmpty()) reloadActions()
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            getString(R.string.plugin_name),
            "Local Trakt + TMDB wallpaper generation",
            getString(R.string.settings),
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_plugin)
        )
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        PreferencesManager.init(requireContext())

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_TRAKT_STATUS)
                .title("Trakt")
                .description(if (PreferencesManager.traktAccessToken.isNotBlank()) "Connected" else "Not connected")
                .enabled(false)
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_TRAKT_START)
                .title("Connect Trakt")
                .description("Show QR code and connect automatically")
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_REFRESH_POPULAR)
                .title("Fetch popular Trakt lists")
                .description(popularListDescription())
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_CATALOGS)
                .title("Catalogs")
                .description(PreferencesManager.selectedCatalogs)
                .subActions(createCatalogActions())
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_EVENT_IDLE)
                .title("Refresh on idle exit")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(PreferencesManager.refreshOnIdleExit)
                .build()
        )
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        catalogKeyForAction(action)?.let { key ->
            val selected = PreferencesManager.selectedCatalogs
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (selected.contains(key)) selected.remove(key) else selected.add(key)
            PreferencesManager.selectedCatalogs = selected.joinToString(",")
            reloadActions()
            notifySettingsChanged()
            return true
        }
        return super.onSubGuidedActionClicked(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_ID_TRAKT_START -> startActivity(Intent(requireContext(), TraktOAuthActivity::class.java))
            ACTION_ID_REFRESH_POPULAR -> fetchPopularLists()
            ACTION_ID_CATALOGS -> {
                action.subActions = createCatalogActions()
                notifyActionChanged(findActionPositionById(ACTION_ID_CATALOGS))
            }
            ACTION_ID_EVENT_IDLE -> {
                val newState = !PreferencesManager.refreshOnIdleExit
                PreferencesManager.refreshOnIdleExit = newState
                action.isChecked = newState
                notifyActionChanged(findActionPositionById(ACTION_ID_EVENT_IDLE))
                notifySettingsChanged()
            }
        }
    }

    private fun createCatalogActions(): List<GuidedAction> {
        val selected = PreferencesManager.selectedCatalogs
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val builtIns = listOf(
            401L to ("anticipated_movies" to "Anticipated Movies"),
            402L to ("anticipated_shows" to "Anticipated Shows"),
            403L to ("trending_movies" to "Trending Movies"),
            404L to ("trending_shows" to "Trending Shows")
        )
        val actions = builtIns.map { (id, pair) ->
            val (key, title) = pair
            GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(key in selected)
                .build()
        }.toMutableList()

        popularListOptions().forEachIndexed { index, option ->
            actions += GuidedAction.Builder(context)
                .id(1000L + index)
                .title(option.title)
                .description("${option.userId}/${option.listId} - ${option.itemCount} items")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(option.key in selected)
                .build()
        }
        return actions
    }

    private fun catalogKeyForAction(action: GuidedAction): String? = when (action.id) {
        401L -> "anticipated_movies"
        402L -> "anticipated_shows"
        403L -> "trending_movies"
        404L -> "trending_shows"
        in 1000L..1999L -> popularListOptions().getOrNull((action.id - 1000L).toInt())?.key
        else -> null
    }

    private fun fetchPopularLists() {
        MainScope().launch {
            val api = traktApi()
            val auth = TraktDeviceAuthRepository(api).authorizationHeader()
            if (auth == null) {
                Toast.makeText(context, "Connect Trakt first", Toast.LENGTH_LONG).show()
                return@launch
            }
            val response = api.popularLists(
                apiKey = BuildConfig.TRAKT_CLIENT_ID.trim(),
                authorization = auth,
                limit = 30
            )
            if (!response.isSuccessful) {
                Toast.makeText(context, "Unable to fetch lists: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                return@launch
            }
            val options = response.body().orEmpty().mapNotNull { it.toOption() }
            PreferencesManager.popularListOptions = serializePopularListOptions(options)
            Toast.makeText(context, "Fetched ${options.size} popular lists", Toast.LENGTH_LONG).show()
            reloadActions()
        }
    }

    private fun traktApi(): TraktLocalApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktLocalApi::class.java)

    private fun popularListOptions(): List<TraktPopularListOption> =
        PreferencesManager.popularListOptions
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size !in 4..5) return@mapNotNull null
                TraktPopularListOption(
                    key = parts[0],
                    title = parts[1],
                    userId = parts[2],
                    listId = parts[3],
                    itemCount = parts.getOrNull(4)?.toIntOrNull() ?: 0
                )
            }
            .toList()

    private fun serializePopularListOptions(options: List<TraktPopularListOption>): String =
        options.joinToString("\n") { "${it.key}|${it.title.replace("|", " ")}|${it.userId}|${it.listId}|${it.itemCount}" }

    private fun popularListDescription(): String {
        val count = popularListOptions().size
        return if (count == 0) "Fetch selectable public lists from Trakt" else "$count lists available"
    }

    private fun reloadActions() {
        actions.clear()
        onCreateActions(actions, null)
        actions.indices.forEach(::notifyActionChanged)
    }

    private fun notifySettingsChanged() {
        (requireActivity() as? SettingsActivity)?.requestWallpaperUpdate()
    }
}
